package com.mobicrave.eventtracker;

import java.util.concurrent.atomic.AtomicLong;

public class MemEventStorage implements EventStorage {
  // TODO: 4B constraints
  private Event[] events;
  private Event.MetaData[] metaDatas;
  private AtomicLong numEvents;

  private MemEventStorage(Event[] events, Event.MetaData[] metaDatas, AtomicLong numEvents) {
    this.events = events;
    this.metaDatas = metaDatas;
    this.numEvents = numEvents;
  }

  @Override
  public long addEvent(Event event, long userId, int eventTypeId) {
    int id = (int) numEvents.incrementAndGet();
    if (id >= events.length) {
      synchronized (this) {
        if (id >= events.length) {
          Event[] newEvents = new Event[events.length * 2];
          System.arraycopy(events, 0, newEvents, 0, events.length);
          events = newEvents;
          Event.MetaData[] newMetaDatas = new Event.MetaData[events.length * 2];
          System.arraycopy(metaDatas, 0, newMetaDatas, 0, metaDatas.length);
          metaDatas = newMetaDatas;
        }
      }
    }
    events[id] = event;
    metaDatas[id]= event.getMetaData(userId, eventTypeId, null);
    return id;
  }

  @Override
  public Event.MetaData getEventMetaData(long eventId) {
    return metaDatas[(int) eventId];
  }

  @Override
  public Event getEvent(long eventId) {
    return events[(int) eventId];
  }

  public static MemEventStorage build() {
    return new MemEventStorage(new Event[1024], new Event.MetaData[1024], new AtomicLong(-1));
  }
}
