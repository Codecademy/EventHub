package com.mobicrave.eventtracker;

import java.util.concurrent.atomic.AtomicLong;

public class EventStorage {
  // TODO: 4B constraints
  private Event[] events;
  private Event.MetaData[] metaDatas;
  private AtomicLong numEvents;

  private EventStorage(Event[] events, Event.MetaData[] metaDatas, AtomicLong numEvents) {
    this.events = events;
    this.metaDatas = metaDatas;
    this.numEvents = numEvents;
  }

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
    metaDatas[id]= event.getMetaData(userId, eventTypeId);
    return id;
  }

  public Event.MetaData getEventMetaData(long eventId) {
    return metaDatas[(int) eventId];
  }

  public static EventStorage build() {
    return new EventStorage(new Event[1024], new Event.MetaData[1024], new AtomicLong(-1));
  }
}
