package com.codecademy.eventhub.storage.visitor;

import com.codecademy.eventhub.storage.filter.ExactMatch;
import com.codecademy.eventhub.storage.filter.Regex;

public interface Visitor {
  boolean visit(ExactMatch exactMatch);
  boolean visit(Regex regex);
}
