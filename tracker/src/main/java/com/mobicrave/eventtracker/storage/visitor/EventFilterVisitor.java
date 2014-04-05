package com.mobicrave.eventtracker.storage.visitor;

import com.mobicrave.eventtracker.model.Event;
import com.mobicrave.eventtracker.storage.filter.ExactMatch;

public class EventFilterVisitor implements Visitor {
  private final Event event;

  public EventFilterVisitor(Event event) {
    this.event = event;
  }

  @Override
  public boolean visit(ExactMatch exactMatch) {
    return exactMatch.getValue().equals(event.get(exactMatch.getKey()));
  }
}
