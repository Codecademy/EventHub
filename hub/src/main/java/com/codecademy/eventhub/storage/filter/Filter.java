package com.codecademy.eventhub.storage.filter;

import com.codecademy.eventhub.storage.visitor.Visitor;

public interface Filter {
  boolean accept(Visitor visitor);
}
