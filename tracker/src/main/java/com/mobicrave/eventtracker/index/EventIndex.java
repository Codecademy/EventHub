package com.mobicrave.eventtracker.index;

import com.google.common.collect.Lists;
import com.mobicrave.eventtracker.list.DmaIdList;
import com.mobicrave.eventtracker.list.IdList;

import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;

public class EventIndex implements Closeable {
  private final String directory;
  private final DmaIdList.Factory dmaIdListFactor;
  // from date string to IdList of eventId
  private final SortedMap<String, IdList> eventIdListMap;

  public EventIndex(String directory, DmaIdList.Factory dmaIdListFactor,
      SortedMap<String, IdList> eventIdListMap) {
    this.directory = directory;
    this.dmaIdListFactor = dmaIdListFactor;
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
      idList = dmaIdListFactor.build(getEventIdListFilename(directory, date));
      eventIdListMap.put(date, idList);
    }
    idList.add(eventId);
  }

  @Override
  public void close() throws IOException {
    String filename = getFilename(directory);
    //noinspection ResultOfMethodCallIgnored
    new File(filename).getParentFile().mkdirs();
    try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(filename))) {
      List<String> dates = Lists.newArrayList();
      for (Map.Entry<String, IdList> entry : eventIdListMap.entrySet()) {
        entry.getValue().close();
        dates.add(entry.getKey());
      }
      oos.writeObject(dates);
    }
  }

  public static String getEventIdListFilename(String directory, String date) {
    return String.format("%s/%s.ser", directory, date);
  }

  public static String getFilename(String directory) {
    return String.format("%s/individual_event_index.ser", directory);
  }

  public interface Factory {
    EventIndex build(String eventType);
  }

  public interface Callback {
    void onEventId(long eventId);
  }
}
