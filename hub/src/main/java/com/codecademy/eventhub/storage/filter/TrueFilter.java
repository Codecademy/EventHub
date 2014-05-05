package com.codecademy.eventhub.storage.filter;

import com.codecademy.eventhub.storage.visitor.Visitor;

public class TrueFilter implements Filter {
  public static final TrueFilter INSTANCE = new TrueFilter();

  private TrueFilter() {}

  @Override
  public boolean accept(Visitor visitor) {
    return true;
  }
}
