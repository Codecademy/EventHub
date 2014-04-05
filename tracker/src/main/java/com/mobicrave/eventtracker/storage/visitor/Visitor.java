package com.mobicrave.eventtracker.storage.visitor;

import com.mobicrave.eventtracker.storage.filter.ExactMatch;

public interface Visitor {
  boolean visit(ExactMatch exactMatch);
}
