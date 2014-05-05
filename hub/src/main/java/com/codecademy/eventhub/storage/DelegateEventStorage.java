package com.codecademy.eventhub.storage;

import com.codecademy.eventhub.model.Event;
import com.codecademy.eventhub.storage.visitor.Visitor;

import java.io.IOException;

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
  public Visitor getFilterVisitor(long eventId) {
    return eventStorage.getFilterVisitor(eventId);
  }

  @Override
  public String getVarz(int indentation) {
    return eventStorage.getVarz(indentation);
  }

  @Override
  public void close() throws IOException {
    eventStorage.close();
  }
}
