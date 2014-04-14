package com.mobicrave.eventtracker.storage.visitor;

import com.mobicrave.eventtracker.model.Event;
import com.mobicrave.eventtracker.storage.filter.ExactMatch;
import com.mobicrave.eventtracker.storage.filter.Regex;

public class EventFilterVisitor implements Visitor {
  private final Event event;

  public EventFilterVisitor(Event event) {
    this.event = event;
  }

  @Override
  public boolean visit(ExactMatch exactMatch) {
    String property = event.get(exactMatch.getKey());
    if (property == null) {
      return false;
    }
    return exactMatch.getValue().equals(property);
  }

  @Override
  public boolean visit(Regex regex) {
    String property = event.get(regex.getKey());
    if (property == null) {
      return false;
    }
    return regex.getPattern().matcher(property).matches();
  }
}
