package com.mobicrave.eventtracker.index;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.mobicrave.eventtracker.base.DB;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.io.Closeable;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * DatedEventIndex is responsible for tracking the earliest event id for a given date.
 */
public class DatedEventIndex implements Closeable {
  private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormat.forPattern("yyyyMMdd");
  private static final String DATE_PREFIX = "d";
  private static final String ID_PREFIX = "i";

  private final DB db;
  // O(numDays)
  private final List<String> dates;
  // O(numDays)
  private final List<Long> earliestEventIds;
  private String currentDate;

  public DatedEventIndex(DB db, List<String> dates, List<Long> earliestEventIds,
      String currentDate) {
    this.db = db;
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

  public synchronized void addEvent(long eventId, String date) {
    if (currentDate != null && date.compareTo(currentDate) <= 0) {
      return;
    }
    currentDate = date;
    dates.add(date);
    earliestEventIds.add(eventId);

    db.put(DATE_PREFIX + date, "");
    db.put(ID_PREFIX + String.format("%020d", eventId), "");
  }

  public String getCurrentDate() {
    return currentDate;
  }

  @Override
  public void close() throws IOException {
    db.close();
  }

  public static DatedEventIndex create(DB db) {
    List<String> dates = db.findByPrefix(DATE_PREFIX, DATE_PREFIX.length());
    List<Long> earliestEventIds = Lists.newArrayList(
        Lists.transform(db.findByPrefix(ID_PREFIX, ID_PREFIX.length()), new Function<String, Long>() {
          @Override
          public Long apply(String s) {
            return Long.parseLong(s);
          }
        }));
    return new DatedEventIndex(db, dates, earliestEventIds,
        dates.isEmpty() ? "" : dates.get(dates.size() - 1));
  }
}
