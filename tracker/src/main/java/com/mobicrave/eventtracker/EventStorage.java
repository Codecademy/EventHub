package com.mobicrave.eventtracker;

public interface EventStorage {
  public long addEvent(Event event, long userId, int eventTypeId);
  public Event.MetaData getEventMetaData(long eventId);
  public Event getEvent(long eventId);
}
