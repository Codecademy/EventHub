package com.mobicrave.eventtracker.storage;

import com.mobicrave.eventtracker.Criterion;
import com.mobicrave.eventtracker.model.Event;

import java.io.Closeable;
import java.util.List;

public interface EventStorage extends Closeable {
  public long addEvent(Event event, int userId, int eventTypeId);
  public Event getEvent(long eventId);
  public int getUserId(long eventId);
  public int getEventTypeId(long eventId);
  public boolean satisfy(long eventId, List<Criterion> criteria);
}
