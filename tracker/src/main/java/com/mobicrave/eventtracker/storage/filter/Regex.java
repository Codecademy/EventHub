package com.mobicrave.eventtracker.storage.filter;

import com.mobicrave.eventtracker.storage.visitor.Visitor;

import java.util.regex.Pattern;

public class Regex implements Filter {
  private final String key;
  private final Pattern pattern;

  public Regex(String key, Pattern pattern) {
    this.key = key;
    this.pattern = pattern;
  }

  @Override
  public boolean accept(Visitor visitor) {
    return visitor.visit(this);
  }

  public String getKey() {
    return key;
  }

  public Pattern getPattern() {
    return pattern;
  }
}
