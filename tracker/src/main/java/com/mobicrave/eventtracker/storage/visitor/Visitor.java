package com.mobicrave.eventtracker.storage.visitor;

import com.mobicrave.eventtracker.storage.filter.ExactMatch;
import com.mobicrave.eventtracker.storage.filter.Regex;

public interface Visitor {
  boolean visit(ExactMatch exactMatch);
  boolean visit(Regex regex);
}
