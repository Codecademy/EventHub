package com.mobicrave.eventtracker.index;

import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ShardedEventIndex implements Closeable {
  private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormat.forPattern("yyyyMMdd");

  private final String filename;
  private final EventIndex.Factory eventIndexFactory;
  // O(numEventTypes), from eventType to its index
  private final Map<String, EventIndex> eventIndexMap;
  // O(numEventTypes)
  private final Map<String, Integer> eventTypeIdMap;
  // O(numDays)
  private final List<String> dates;
  // O(numDays)
  private final List<Long> earliestEventIds;
  private String currentDate;

  public ShardedEventIndex(String filename, EventIndex.Factory eventIndexFactory,
      Map<String, EventIndex> eventIndexMap, Map<String, Integer> eventTypeIdMap,
      List<String> dates, List<Long> earliestEventIds, String currentDate) {
    this.eventIndexFactory = eventIndexFactory;
    this.eventIndexMap = eventIndexMap;
    this.eventTypeIdMap = eventTypeIdMap;
    this.dates = dates;
    this.earliestEventIds = earliestEventIds;
    this.currentDate = currentDate;
    this.filename = filename;
  }

  public void enumerateEventIds(String eventType, String startDate, String endDate,
      EventIndex.Callback aggregateUserIdsCallback) {
    eventIndexMap.get(eventType).enumerateEventIds(startDate, endDate, aggregateUserIdsCallback);
  }

  public long findFirstEventIdOnDate(long eventIdForStartDate, int numDaysAfter) {
    int startDateOffset = Collections.binarySearch(earliestEventIds, eventIdForStartDate);
    if (startDateOffset < 0) {
      if (startDateOffset == -1) {
        startDateOffset = 0;
      } else {
        startDateOffset = -startDateOffset - 2;
      }
    }
    String dateOfEvent = dates.get(startDateOffset);
    String endDate = DATE_TIME_FORMATTER.print(
        DateTime.parse(dateOfEvent, DATE_TIME_FORMATTER).plusDays(numDaysAfter));
    int endDateOffset = Collections.binarySearch(dates, endDate);
    if (endDateOffset < 0) {
      endDateOffset = -endDateOffset - 1;
      if (endDateOffset >= earliestEventIds.size()) {
        return Long.MAX_VALUE;
      }
    }
    return earliestEventIds.get(endDateOffset);
  }

  public void addEventType(String eventType) {
    EventIndex individualEventIndex = eventIndexMap.get(eventType);
    if (individualEventIndex != null) {
      return ;
    }
    synchronized (this) {
      eventTypeIdMap.put(eventType, eventIndexMap.size());
      eventIndexMap.put(eventType, eventIndexFactory.build(eventType));
    }
  }

  public void addEvent(long eventId, String eventType, String date) {
    if (!date.equals(currentDate)) {
      currentDate = date;
      dates.add(date);
      earliestEventIds.add(eventId);
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
      oos.writeObject(dates);
      oos.writeObject(earliestEventIds);
      oos.writeObject(currentDate);
    }
  }

  public String getVarz() {
    return String.format("current date: %s\nfilename: %s\n", currentDate, filename);
  }
}
