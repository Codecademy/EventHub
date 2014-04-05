package com.mobicrave.eventtracker.storage;

import com.mobicrave.eventtracker.model.Event;
import com.mobicrave.eventtracker.storage.visitor.Visitor;

import java.io.Closeable;

public interface EventStorage extends Closeable {
  long addEvent(Event event, int userId, int eventTypeId);
  Event getEvent(long eventId);
  int getUserId(long eventId);
  int getEventTypeId(long eventId);
  Visitor getFilterVisitor(long eventId);
  String getVarz(int indentation);
}
