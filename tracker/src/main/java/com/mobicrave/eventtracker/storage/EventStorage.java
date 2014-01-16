package com.mobicrave.eventtracker.storage;

import com.mobicrave.eventtracker.model.Event;

import java.io.Closeable;

public interface EventStorage extends Closeable {
  public long addEvent(Event event, long userId, int eventTypeId);
  public Event getEvent(long eventId);
  public long getUserId(long eventId);
  public int getEventTypeId(long eventId);
}
