package com.codecademy.eventhub.storage;

import com.codecademy.eventhub.model.Event;
import com.codecademy.eventhub.storage.visitor.Visitor;

import java.io.Closeable;

public interface EventStorage extends Closeable {
  long addEvent(Event event, int userId, int eventTypeId);
  Event getEvent(long eventId);
  int getUserId(long eventId);
  int getEventTypeId(long eventId);
  Visitor getFilterVisitor(long eventId);
  String getVarz(int indentation);
}
