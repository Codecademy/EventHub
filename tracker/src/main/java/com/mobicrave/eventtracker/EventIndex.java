package com.mobicrave.eventtracker;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.SortedMap;

public class EventIndex implements Closeable {
  private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormat.forPattern("yyyyMMdd");
  // O(numEventTypes)
  private final Map<String, IndividualEventIndex> eventIndexMap;
  // O(numDays)
  private final ArrayList<String> dates;
  // O(numDays)
  private final ArrayList<Long> earliestEventIds;
  private String currentDate;
  private final String directory;

  private EventIndex(Map<String, IndividualEventIndex> eventIndexMap, ArrayList<String> dates,
      ArrayList<Long> earliestEventIds, String currentDate, String directory) {
    this.eventIndexMap = eventIndexMap;
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

  public int addEventType(String eventType) {
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

  @Override
  public void close() {
    new File(directory).mkdirs();
    try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(getSerializationFile(directory)))) {
      ArrayList<String> eventIndexNames = Lists.newArrayList();
      for (String eventIndexName : eventIndexMap.keySet()) {
        eventIndexNames.add(eventIndexName);
        IndividualEventIndex individualEventIndex = eventIndexMap.get(eventIndexName);
        individualEventIndex.close(getSerializationFile(directory, eventIndexName));
      }
      oos.writeObject(eventIndexNames);
      oos.writeObject(dates);
      oos.writeObject(earliestEventIds);
      oos.writeObject(currentDate);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static EventIndex build(String directory) {
    File file = new File(getSerializationFile(directory));
    if (file.exists()) {
      try {
        ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file));
        ArrayList<String> eventIndexNames = (ArrayList<String>) ois.readObject();
        ArrayList<String> dates = (ArrayList<String>) ois.readObject();
        ArrayList<Long> earliestEventIds = (ArrayList<Long>) ois.readObject();
        String currentDate = (String) ois.readObject();
        Map<String, IndividualEventIndex> eventIndexMap = Maps.newHashMap();
        for (String eventIndexName : eventIndexNames) {
          eventIndexMap.put(eventIndexName, IndividualEventIndex.build(
              getSerializationFile(directory, eventIndexName)));
        }
        return new EventIndex(eventIndexMap, dates, earliestEventIds, currentDate, directory);
      } catch (IOException | ClassNotFoundException e) {
        throw new RuntimeException(e);
      }
    }
    return new EventIndex(Maps.<String,IndividualEventIndex>newHashMap(),
        Lists.<String>newArrayList(), Lists.<Long>newArrayList(), null, directory);
  }

  private static String getSerializationFile(String directory, String eventIndexName) {
    return directory + "/" + eventIndexName + ".ser";
  }

  private static String getSerializationFile(String directory) {
    return directory + "/event_index.ser";
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

    public void close(String file) {
      try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file))) {
        oos.writeObject(id);
        oos.writeObject(eventIdListMap);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }

    public static IndividualEventIndex build(int id) {
      return new IndividualEventIndex(id, Maps.<String, IdList>newTreeMap());
    }

    public static IndividualEventIndex build(String file) {
      try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
        int id = (Integer) ois.readObject();
        SortedMap<String, IdList> eventIdListMap = (SortedMap<String, IdList>) ois.readObject();
        return new IndividualEventIndex(id, eventIdListMap);
      } catch (IOException e) {
        throw new RuntimeException(e);
      } catch (ClassNotFoundException e) {
        throw new RuntimeException(e);
      }
    }
  }

  public static interface Callback {
    public void onEventId(long eventId);
  }
}
