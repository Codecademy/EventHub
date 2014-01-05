package com.mobicrave.eventtracker;

import com.google.common.collect.Lists;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicLong;

public class EventStorage {
  // TODO: 4B constraints
  private final ArrayList<Event> events;
  private final ArrayList<Event.MetaData> metaDatas;
  private AtomicLong numEvents;

  private EventStorage(ArrayList<Event> events, ArrayList<Event.MetaData> metaDatas, AtomicLong numEvents) {
    this.events = events;
    this.metaDatas = metaDatas;
    this.numEvents = numEvents;
  }

  public long addEvent(Event event, long userId, int eventTypeId) {
    long id = numEvents.incrementAndGet();
    events.add(event);
    metaDatas.add(event.getMetaData(userId, eventTypeId));
    return id;
  }

  public Event.MetaData getEventMetaData(long eventId) {
    return metaDatas.get((int) eventId);
  }

  public static EventStorage build() {
    return new EventStorage(Lists.<Event>newArrayList(), Lists.<Event.MetaData>newArrayList(),
        new AtomicLong(-1));
  }
}
