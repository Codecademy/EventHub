package com.mobicrave.eventtracker.index;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mobicrave.eventtracker.list.DmaIdList;
import com.mobicrave.eventtracker.list.IdList;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;

public class EventIndex implements Closeable {
  private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormat.forPattern("yyyyMMdd");
  // O(numEventTypes), from eventType to its index
  private final Map<String, IndividualEventIndex> eventIndexMap;
  // O(numEventTypes)
  private final Map<String, Integer> eventTypeIdMap;
  // O(numDays)
  private final List<String> dates;
  // O(numDays)
  private final List<Long> earliestEventIds;
  private String currentDate;
  private final String directory;

  private EventIndex(String directory, Map<String, IndividualEventIndex> eventIndexMap,
      Map<String, Integer> eventTypeIdMap, List<String> dates,
      List<Long> earliestEventIds, String currentDate) {
    this.eventIndexMap = eventIndexMap;
    this.eventTypeIdMap = eventTypeIdMap;
    this.dates = dates;
    this.earliestEventIds = earliestEventIds;
    this.currentDate = currentDate;
    this.directory = directory;
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

  public void addEventType(String eventType) {
    IndividualEventIndex individualEventIndex = eventIndexMap.get(eventType);
    if (individualEventIndex != null) {
      return ;
    }
    synchronized (this) {
      eventTypeIdMap.put(eventType, eventIndexMap.size());
      eventIndexMap.put(eventType, IndividualEventIndex.build(
          getIndividualEventIndexSerializationDirectory(directory, eventType)));
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
    return eventTypeIdMap.get(eventType);
  }

  @Override
  public void close() throws IOException {
    //noinspection ResultOfMethodCallIgnored
    new File(directory).mkdirs();
    try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(getSerializationFile(directory)))) {
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
    return String.format("current date: %s\ndirectory: %s\n", currentDate, directory);
  }

  private static String getSerializationFile(String directory) {
    return directory + "/event_index.ser";
  }

  private static String getIndividualEventIndexSerializationDirectory(String directory,
      String eventType) {
    return String.format("%s/%s/", directory, eventType);
  }

  public static EventIndex build(String directory) {
    File file = new File(getSerializationFile(directory));
    if (file.exists()) {
      try {
        ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file));
        List<String> eventTypes = (List<String>) ois.readObject();
        Map<String, Integer> eventTypeIdMap = (Map<String, Integer>) ois.readObject();
        List<String> dates = (List<String>) ois.readObject();
        List<Long> earliestEventIds = (List<Long>) ois.readObject();
        String currentDate = (String) ois.readObject();
        Map<String, IndividualEventIndex> eventIndexMap = Maps.newHashMap();
        for (String eventType : eventTypes) {
          eventIndexMap.put(eventType, IndividualEventIndex.build(
              getIndividualEventIndexSerializationDirectory(directory, eventType)));
        }
        return new EventIndex(directory, eventIndexMap, eventTypeIdMap, dates, earliestEventIds,
            currentDate);
      } catch (IOException | ClassNotFoundException e) {
        throw new RuntimeException(e);
      }
    }
    return new EventIndex(directory, Maps.<String,IndividualEventIndex>newHashMap(), Maps.<String, Integer>newHashMap(),
        Lists.<String>newArrayList(), Lists.<Long>newArrayList(), null);
  }

  private static class IndividualEventIndex implements Closeable {
    private final String directory;
    // from date string to IdList of eventId
    private final SortedMap<String, IdList> eventIdListMap;

    private IndividualEventIndex(String directory, SortedMap<String, IdList> eventIdListMap) {
      this.directory = directory;
      this.eventIdListMap = eventIdListMap;
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
        idList = DmaIdList.build(getIdListSerializationFile(directory, date), 1024);
        eventIdListMap.put(date, idList);
      }
      idList.add(eventId);
    }

    @Override
    public void close() throws IOException {
      //noinspection ResultOfMethodCallIgnored
      new File(directory).mkdirs();
      try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(
          getSerializationFile(directory)))) {
        List<String> dates = Lists.newArrayList();
        for (Map.Entry<String, IdList> entry : eventIdListMap.entrySet()) {
          entry.getValue().close();
          dates.add(entry.getKey());
        }
        oos.writeObject(dates);
      }
    }

    private static String getIdListSerializationFile(String directory, String date) {
      return String.format("%s/%s.ser", directory, date);
    }

    private static String getSerializationFile(String directory) {
      return String.format("%s/individual_event_index.ser", directory);
    }

    public static IndividualEventIndex build(String directory) {
      File file = new File(getSerializationFile(directory));
      if (file.exists()) {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
          List<String> dates = (List<String>) ois.readObject();
          SortedMap<String, IdList> eventIdListMap = Maps.newTreeMap();
          for (String date : dates) {
            eventIdListMap.put(date, DmaIdList.build(
                getIdListSerializationFile(directory, date), 1024));
          }
          return new IndividualEventIndex(directory, eventIdListMap);
        } catch (IOException | ClassNotFoundException e) {
          throw new RuntimeException(e);
        }
      }
      return new IndividualEventIndex(directory, Maps.<String, IdList>newTreeMap());
    }
  }

  public static interface Callback {
    public void onEventId(long eventId);
  }
}
