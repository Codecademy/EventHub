package com.mobicrave.eventtracker.storage;

import com.mobicrave.eventtracker.Criterion;
import com.mobicrave.eventtracker.model.Event;

import java.io.IOException;
import java.util.List;

public class DelegateEventStorage implements EventStorage {
  private final EventStorage eventStorage;

  public DelegateEventStorage(EventStorage eventStorage) {
    this.eventStorage = eventStorage;
  }

  @Override
  public long addEvent(Event event, int userId, int eventTypeId) {
    return eventStorage.addEvent(event, userId, eventTypeId);
  }

  @Override
  public Event getEvent(long eventId) {
    return eventStorage.getEvent(eventId);
  }

  @Override
  public int getUserId(long eventId) {
    return eventStorage.getUserId(eventId);
  }

  @Override
  public int getEventTypeId(long eventId) {
    return eventStorage.getEventTypeId(eventId);
  }

  @Override
  public boolean satisfy(long eventId, List<Criterion> criteria) {
    return eventStorage.satisfy(eventId, criteria);
  }

  @Override
  public String getVarz() {
    return eventStorage.getVarz();
  }

  @Override
  public void close() throws IOException {
    eventStorage.close();
  }
}
