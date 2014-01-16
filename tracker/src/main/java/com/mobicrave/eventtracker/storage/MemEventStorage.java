package com.mobicrave.eventtracker.storage;

import com.mobicrave.eventtracker.model.Event;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Memory implementation doesn't support persistence nor can it store >4B events
 */
public class MemEventStorage implements EventStorage {
  private Event[] events;
  private MetaData[] metaDatas;
  private AtomicLong numEvents;

  private MemEventStorage(Event[] events, MetaData[] metaDatas, AtomicLong numEvents) {
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
          MetaData[] newMetaDatas = new MetaData[events.length * 2];
          System.arraycopy(metaDatas, 0, newMetaDatas, 0, metaDatas.length);
          metaDatas = newMetaDatas;
        }
      }
    }
    events[id] = event;
    metaDatas[id]= new MetaData(userId, eventTypeId);
    return id;
  }

  @Override
  public Event getEvent(long eventId) {
    return events[(int) eventId];
  }

  @Override
  public long getUserId(long eventId) {
    return getEventMetaData(eventId).getUserId();
  }

  @Override
  public int getEventTypeId(long eventId) {
    return getEventMetaData(eventId).getEventTypeId();
  }

  private MetaData getEventMetaData(long eventId) {
    return metaDatas[(int) eventId];
  }

  @Override
  public void close() {
    throw new UnsupportedOperationException();
  }

  public static MemEventStorage build() {
    return new MemEventStorage(new Event[1024], new MetaData[1024], new AtomicLong(-1));
  }

  public static class MetaData {
    private final long userId;
    private final int eventTypeId;

    public MetaData(long userId, int eventTypeId) {
      this.userId = userId;
      this.eventTypeId = eventTypeId;
    }

    public long getUserId() {
      return userId;
    }

    public int getEventTypeId() {
      return eventTypeId;
    }
  }
}
