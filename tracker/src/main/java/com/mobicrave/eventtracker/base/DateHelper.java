package com.mobicrave.eventtracker.base;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

public class DateHelper {
  private static final DateTimeFormatter FORMATTER = DateTimeFormat.forPattern("yyyyMMdd");

  public synchronized String getDate() {
    return FORMATTER.print(new DateTime());
  }
}
