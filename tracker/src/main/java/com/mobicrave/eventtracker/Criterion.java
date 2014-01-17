package com.mobicrave.eventtracker;

public class Criterion {
  private final String key;
  private final String value;

  public Criterion(String key, String value) {
    this.key = key;
    this.value = value;
  }

  public String getKey() {
    return key;
  }

  public String getValue() {
    return value;
  }
}
