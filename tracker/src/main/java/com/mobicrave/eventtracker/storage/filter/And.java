package com.mobicrave.eventtracker.storage.filter;

import com.google.common.collect.Lists;
import com.mobicrave.eventtracker.storage.visitor.Visitor;

import java.util.List;

public class And implements Filter {
  private final List<Filter> filters;

  public And(List<Filter> filters) {
    this.filters = filters;
  }

  @Override
  public boolean accept(Visitor visitor) {
    for (Filter filter : filters) {
      if (!filter.accept(visitor)) {
        return false;
      }
    }
    return true;
  }

  public static And of(Filter... filters) {
    return new And(Lists.newArrayList(filters));
  }
}
