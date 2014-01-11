package com.mobicrave.eventtracker;

import com.google.gson.Gson;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.Map;

public class User {
  private final String externalId;
  private final Map<String, String> properties;

  public User(String externalId, Map<String, String> properties) {
    this.externalId = externalId;
    this.properties = properties;
  }

  public String getExternalId() {
    return externalId;
  }

  public Map<String, String> getProperties() {
    return properties;
  }

  public MetaData getMetaData(String externalId, byte[] location) {
    return new MetaData(externalId, location);
  }

  public ByteBuffer toByteBuffer() {
    Gson gson = new Gson();
    return ByteBuffer.wrap(gson.toJson(this).getBytes());
  }

  public static User fromByteBuffer(ByteBuffer byteBuffer) {
    Gson gson = new Gson();
    return gson.fromJson(new String(byteBuffer.array()), User.class);
  }

  public static class MetaData implements Serializable {
    private static final long serialVersionUID = 8287763037256921937L;
    private final String externalId;
    private final byte[] location;

    public MetaData(String externalId, byte[] location) {
      this.externalId = externalId;
      this.location = location;
    }

    public ByteBuffer toByteBuffer() {
      Gson gson = new Gson();
      return ByteBuffer.wrap(gson.toJson(this).getBytes());
    }

    public static MetaData fromByteBuffer(ByteBuffer byteBuffer) {
      Gson gson = new Gson();
      return gson.fromJson(new String(byteBuffer.array()), MetaData.class);
    }

    public String getExternalId() {
      return externalId;
    }

    public byte[] getLocation() {
      return location;
    }
  }

  public static class Builder {
    private final String externalUserId;
    private Map<String, String> properties;

    public Builder(String externalUserId, Map<String, String> properties) {
      this.externalUserId = externalUserId;
      this.properties = properties;
    }

    public Builder add(String key, String value) {
      properties.put(key, value);
      return this;
    }

    public User build() {
      return new User(externalUserId, properties);
    }
  }
}
