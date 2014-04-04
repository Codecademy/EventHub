package com.mobicrave.eventtracker;

public class Filter {
  private final String key;
  private final String value;

  public Filter(String key, String value) {
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
