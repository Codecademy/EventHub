package com.mobicrave.eventtracker.model;

import com.google.common.collect.Maps;
import com.mobicrave.eventtracker.base.ByteBufferMap;
import com.mobicrave.eventtracker.base.KeyValueCallback;

import java.nio.ByteBuffer;
import java.util.Map;

public class User {
  private final ByteBufferMap byteBufferMap;

  private User(ByteBufferMap byteBufferMap) {
    this.byteBufferMap = byteBufferMap;
  }

  public String getExternalId() {
    return get("external_id");
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

  @Override
  public String toString() {
    return String.format("external user id: %s" + getExternalId());
  }

  public static User fromByteBuffer(ByteBuffer byteBuffer) {
    return new User(new ByteBufferMap(byteBuffer.duplicate()));
  }

  public static class Builder {
    private final String externalId;
    private Map<String, String> properties;

    public Builder(String externalId, Map<String, String> properties) {
      this.externalId = externalId;
      this.properties = properties;
    }

    public Builder add(String key, String value) {
      properties.put(key, value);
      return this;
    }

    public User build() {
      Map<String, String> allProperties = Maps.newHashMap();
      allProperties.putAll(properties);
      allProperties.put("external_id", externalId);
      return new User(ByteBufferMap.build(allProperties));
    }
  }
}
