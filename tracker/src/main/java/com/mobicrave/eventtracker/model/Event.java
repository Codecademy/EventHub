package com.mobicrave.eventtracker.model;

import com.google.common.collect.Maps;
import com.mobicrave.eventtracker.base.ByteBufferMap;
import com.mobicrave.eventtracker.base.KeyValueCallback;

import java.nio.ByteBuffer;
import java.util.Map;

public class Event {
  private final ByteBufferMap byteBufferMap;

  private Event(ByteBufferMap byteBufferMap) {
    this.byteBufferMap = byteBufferMap;
  }

  public String getEventType() {
    return byteBufferMap.get("event_type");
  }

  public String getDate() {
    return byteBufferMap.get("date");
  }

  public String getExternalUserId() {
    return byteBufferMap.get("external_user_id");
  }

  public String get(String key) {
    return byteBufferMap.get(key);
  }

  public void enumerate(KeyValueCallback callback) {
    byteBufferMap.enumerate(callback);
  }

  public ByteBuffer toByteBuffer() {
    return byteBufferMap.toByteBuffer();
  }

  public static Event fromByteBuffer(ByteBuffer byteBuffer) {
    return new Event(new ByteBufferMap(byteBuffer.duplicate()));
  }

  @Override
  public String toString() {
    return String.format(
        "event type: %s\n" +
        "date: %s\n" +
        "external user id: %s",
        getEventType(), getDate(), getExternalUserId());
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
      Map<String, String> allProperties = Maps.newHashMap();
      allProperties.putAll(properties);
      allProperties.put("event_type", eventType);
      allProperties.put("date", date);
      allProperties.put("external_user_id", externalUserId);
      return new Event(ByteBufferMap.build(allProperties));
    }
  }
}
