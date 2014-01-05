package com.mobicrave.eventtracker;

import java.util.Map;

public class Event {
  private final String eventType;
  private final String externalUserId;
  private final String date;
  private final Map<String, String> properties;

  public Event(String eventType, String externalUserId, String date, Map<String, String> properties) {
    this.eventType = eventType;
    this.externalUserId = externalUserId;
    this.date = date;
    this.properties = properties;
  }

  public String getEventType() {
    return eventType;
  }

  public String getDate() {
    return date;
  }

  public String getExternalUserId() {
    return externalUserId;
  }

  public MetaData getMetaData(long userId, int eventTypeId) {
    return new MetaData(userId, eventTypeId);
  }

  public static class MetaData {
    private final long userId;
    private int eventTypeId;

    public MetaData(long userId, int eventTypeId) {
      this.userId = userId;
      this.eventTypeId = eventTypeId;
    }

    public long getUserId() {
      return userId;
    }

    public int getEventTypeId() {
      return eventTypeId;
    }
  }

  public static class Builder {
    private final String eventType;
    private final String externalUserId;
    private final String date;
    private Map<String, String> properties;

    public Builder(String eventType, String externalUserId, String date, Map<String, String> properties) {
      this.eventType = eventType;
      this.externalUserId = externalUserId;
      this.date = date;
      this.properties = properties;
    }

    public Builder add(String key, String value) {
      properties.put(key, value);
      return this;
    }

    public Event build() {
      Event event = new Event(eventType, externalUserId, date, properties);
      return event;
    }
  }
}
