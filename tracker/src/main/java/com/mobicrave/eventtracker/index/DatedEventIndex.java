package com.mobicrave.eventtracker.index;

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

public class DatedEventIndex implements Closeable {
  private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormat.forPattern("yyyyMMdd");

  private final String filename;
  // O(numDays)
  private final List<String> dates;
  // O(numDays)
  private final List<Long> earliestEventIds;
  private String currentDate;

  public DatedEventIndex(String filename, List<String> dates, List<Long> earliestEventIds,
      String currentDate) {
    this.filename = filename;
    this.dates = dates;
    this.earliestEventIds = earliestEventIds;
    this.currentDate = currentDate;
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

  public void addEvent(long eventId, String date) {
    currentDate = date;
    dates.add(date);
    earliestEventIds.add(eventId);
  }

  public String getCurrentDate() {
    return currentDate;
  }

  @Override
  public void close() throws IOException {
    //noinspection ResultOfMethodCallIgnored
    new File(filename).getParentFile().mkdirs();
    try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(filename))) {
      oos.writeObject(dates);
      oos.writeObject(earliestEventIds);
      oos.writeObject(currentDate);
    }
  }
}
