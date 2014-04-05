package com.mobicrave.eventtracker.storage.filter;

import com.mobicrave.eventtracker.storage.visitor.Visitor;

public interface Filter {
  boolean accept(Visitor visitor);
}
