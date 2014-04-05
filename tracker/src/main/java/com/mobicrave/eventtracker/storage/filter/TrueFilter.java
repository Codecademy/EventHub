package com.mobicrave.eventtracker.storage.filter;

import com.mobicrave.eventtracker.storage.visitor.Visitor;

public class TrueFilter implements Filter {
  public static final TrueFilter INSTANCE = new TrueFilter();

  private TrueFilter() {}

  @Override
  public boolean accept(Visitor visitor) {
    return true;
  }
}
