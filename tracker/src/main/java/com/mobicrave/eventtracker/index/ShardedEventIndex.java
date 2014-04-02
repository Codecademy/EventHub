package com.mobicrave.eventtracker.index;

import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;

import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.List;
import java.util.Map;

public class ShardedEventIndex implements Closeable {
  private final String filename;
  private final EventIndex.Factory eventIndexFactory;
  // O(numEventTypes), from eventType to its index
  private final Map<String, EventIndex> eventIndexMap;
  // O(numEventTypes)
  private final Map<String, Integer> eventTypeIdMap;
  private final DatedEventIndex datedEventIndex;

  public ShardedEventIndex(String filename, EventIndex.Factory eventIndexFactory,
      Map<String, EventIndex> eventIndexMap, Map<String, Integer> eventTypeIdMap,
      DatedEventIndex datedEventIndex) {
    this.filename = filename;
    this.eventIndexFactory = eventIndexFactory;
    this.eventIndexMap = eventIndexMap;
    this.eventTypeIdMap = eventTypeIdMap;
    this.datedEventIndex = datedEventIndex;
  }

  public long findFirstEventIdOnDate(long eventIdForStartDate, int numDaysAfter) {
    return datedEventIndex.findFirstEventIdOnDate(eventIdForStartDate, numDaysAfter);
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
      return eventTypeId;
    }
  }

  public void addEvent(long eventId, String eventType, String date) {
    if (!date.equals(datedEventIndex.getCurrentDate())) {
      datedEventIndex.addEvent(eventId, date);
    }
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
    //noinspection ResultOfMethodCallIgnored
    new File(filename).getParentFile().mkdirs();
    try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(filename))) {
      List<String> eventTypes = Lists.newArrayList();
      for (String eventType : eventIndexMap.keySet()) {
        eventTypes.add(eventType);
        eventIndexMap.get(eventType).close();
      }
      oos.writeObject(eventTypes);
      oos.writeObject(eventTypeIdMap);
    }
    datedEventIndex.close();
  }

  public String getVarz(int indentation) {
    String indent  = new String(new char[indentation]).replace('\0', ' ');
    return String.format(
        indent + "current date: %s\n" +
        indent + "filename: %s",
        datedEventIndex.getCurrentDate(), filename);
  }
}
