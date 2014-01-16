package com.mobicrave.eventtracker.model;

import com.google.gson.Gson;

import java.nio.ByteBuffer;
import java.util.Map;

public class Event {
  private final String eventType;
  private final String externalUserId;
  private final String date;
  private final Map<String, String> properties;

  public Event(String eventType, String externalUserId,
      String date, Map<String, String> properties) {
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

  public Map<String, String> getProperties() {
    return properties;
  }

  public ByteBuffer toByteBuffer() {
    Gson gson = new Gson();
    return ByteBuffer.wrap(gson.toJson(this).getBytes());
  }

  public static Event fromByteBuffer(ByteBuffer byteBuffer) {
    Gson gson = new Gson();
    return gson.fromJson(new String(byteBuffer.array()), Event.class);
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
      return new Event(eventType, externalUserId, date, properties);
    }
  }
}
