package com.codecademy.eventhub.storage.visitor;

import com.codecademy.eventhub.storage.filter.ExactMatch;
import com.codecademy.eventhub.storage.filter.Regex;

import javax.inject.Provider;

public class DelayedVisitorProxy implements Visitor {
  private final Provider<Visitor> visitorProvider;
  private Visitor cachedVisitor;

  public DelayedVisitorProxy(Provider<Visitor> visitorProvider) {
    this.visitorProvider = visitorProvider;
  }

  @Override
  public boolean visit(ExactMatch exactMatch) {
    if (cachedVisitor == null) {
      cachedVisitor = visitorProvider.get();
    }
    return cachedVisitor.visit(exactMatch);
  }

  @Override
  public boolean visit(Regex regex) {
    if (cachedVisitor == null) {
      cachedVisitor = visitorProvider.get();
    }
    return cachedVisitor.visit(regex);
  }
}
