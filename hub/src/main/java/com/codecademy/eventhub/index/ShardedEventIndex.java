package com.codecademy.eventhub.index;

import com.google.common.collect.Ordering;
import com.google.common.io.Files;

import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.List;
import java.util.Map;

/**
 * ShardedEventIndex is responsible for managing individual EventIndex sharded by event type.
 */
public class ShardedEventIndex implements Closeable {
  private final String filename;
  private final EventIndex.Factory eventIndexFactory;
  // O(numEventTypes), from eventType to its index
  private final Map<String, EventIndex> eventIndexMap;
  // O(numEventTypes)
  private final Map<String, Integer> eventTypeIdMap;

  public ShardedEventIndex(String filename, EventIndex.Factory eventIndexFactory,
      Map<String, EventIndex> eventIndexMap, Map<String, Integer> eventTypeIdMap) {
    this.filename = filename;
    this.eventIndexFactory = eventIndexFactory;
    this.eventIndexMap = eventIndexMap;
    this.eventTypeIdMap = eventTypeIdMap;
  }

  public void enumerateEventIds(String eventType, String startDate, String endDate,
      EventIndex.Callback callback) {
    eventIndexMap.get(eventType).enumerateEventIds(startDate, endDate, callback);
  }

  public int ensureEventType(String eventType) {
    if (eventTypeIdMap.containsKey(eventType)) {
      return eventTypeIdMap.get(eventType);
    }
    synchronized (this) {
      int eventTypeId = eventIndexMap.size();
      eventTypeIdMap.put(eventType, eventTypeId);
      eventIndexMap.put(eventType, eventIndexFactory.build(eventType));
      persistEventTypeIdMap();
      return eventTypeId;
    }
  }

  public synchronized void addEvent(long eventId, String eventType, String date) {
    eventIndexMap.get(eventType).addEvent(eventId, date);
  }

  public List<String> getEventTypes() {
    return Ordering.from(String.CASE_INSENSITIVE_ORDER).sortedCopy(eventTypeIdMap.keySet());
  }

  public int getEventTypeId(String eventType) {
    return eventTypeIdMap.get(eventType);
  }

  @Override
  public void close() throws IOException {
    for (String eventType : eventIndexMap.keySet()) {
      eventIndexMap.get(eventType).close();
    }
    persistEventTypeIdMap();
  }

  public String getVarz(int indentation) {
    String indent  = new String(new char[indentation]).replace('\0', ' ');
    return String.format(indent + "filename: %s", filename);
  }

  private void persistEventTypeIdMap() {
    //noinspection ResultOfMethodCallIgnored
    new File(filename).getParentFile().mkdirs();
    String newFilename = filename + ".new";
    try {
      try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(newFilename))) {
        oos.writeObject(eventTypeIdMap);
      }
      Files.move(new File(newFilename), new File(filename));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
