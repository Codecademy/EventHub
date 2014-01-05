package com.mobicrave.eventtracker;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.SortedMap;

// TODO: to be optimized
public class EventIndex {
  private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormat.forPattern("yyyyMMdd");
  private final Map<String, IndividualEventIndex> eventIndexMap;
  private final ArrayList<String> dates;
  private final ArrayList<Long> earliestEventIds;
  private String currentDate;

  private EventIndex(Map<String, IndividualEventIndex> eventIndexMap,
      ArrayList<String> dates, ArrayList<Long> earliestEventIds) {
    this.eventIndexMap = eventIndexMap;
    this.dates = dates;
    this.earliestEventIds = earliestEventIds;
    currentDate = null;
  }

  public void enumerateEventIds(String eventType, String startDate, String endDate, Callback aggregateUserIdsCallback) {
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

  public int add(String eventType) {
    // TODO: DI
    IndividualEventIndex individualEventIndex = eventIndexMap.get(eventType);
    if (individualEventIndex != null) {
      return individualEventIndex.getId();
    }
    synchronized (this) {
      int eventIndexId = eventIndexMap.size();
      eventIndexMap.put(eventType, IndividualEventIndex.build(eventIndexId));
      return eventIndexId;
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

  public int getEventTypeId(String eventType) {
    return eventIndexMap.get(eventType).getId();
  }

  public static EventIndex build() {
    return new EventIndex(
        Maps.<String, EventIndex.IndividualEventIndex>newHashMap(),
        Lists.<String>newArrayList(), Lists.<Long>newArrayList());
  }

  // TODO: extract interface and try different implementation
  private static class IndividualEventIndex {
    private final int id;
    // from date string to IdList of eventId
    private final SortedMap<String, IdList> eventIdListMap;

    private IndividualEventIndex(int id, SortedMap<String, IdList> eventIdListMap) {
      this.id = id;
      this.eventIdListMap = eventIdListMap;
    }

    public int getId() {
      return id;
    }

    public void enumerateEventIds(String startDate, String endDate, Callback callback) {
      for (IdList idList : eventIdListMap.subMap(startDate, endDate).values()) {
        IdList.Iterator eventIdIterator = idList.iterator();
        while (eventIdIterator.hasNext()) {
          callback.onEventId(eventIdIterator.next());
        }
      }
    }

    public void addEvent(long eventId, String date) {
      IdList idList = eventIdListMap.get(date);
      if (idList == null) {
        idList = IdList.build();
        eventIdListMap.put(date, idList);
      }
      idList.add(eventId);
    }

    public static IndividualEventIndex build(int id) {
      return new IndividualEventIndex(id, Maps.<String, IdList>newTreeMap());
    }
  }

  public static interface Callback {
    public void onEventId(long eventId);
  }
}
