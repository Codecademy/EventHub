package com.codecademy.eventhub.storage.filter;

import com.codecademy.eventhub.storage.visitor.Visitor;

public class ExactMatch implements Filter {
  private final String key;
  private final String value;

  public ExactMatch(String key, String value) {
    this.key = key;
    this.value = value;
  }

  @Override
  public boolean accept(Visitor visitor) {
    return visitor.visit(this);
  }

  public String getKey() {
    return key;
  }

  public String getValue() {
    return value;
  }
}
