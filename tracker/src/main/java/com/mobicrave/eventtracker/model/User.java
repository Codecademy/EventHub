package com.mobicrave.eventtracker.model;

import com.google.gson.Gson;
import com.mobicrave.eventtracker.base.Schema;

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

  public MetaData getMetaData(byte[] location) {
    return new MetaData(location);
  }

  public ByteBuffer toByteBuffer() {
    Gson gson = new Gson();
    return ByteBuffer.wrap(gson.toJson(this).getBytes());
  }

  public static User fromByteBuffer(ByteBuffer byteBuffer) {
    Gson gson = new Gson();
    return gson.fromJson(new String(byteBuffer.array()), User.class);
  }

  public static class MetaData {
    private final byte[] location;

    public MetaData(byte[] location) {
      this.location = location;
    }

    public byte[] getLocation() {
      return location;
    }

    public static Schema<MetaData> getSchema() {
      return new MetaDataSchema();
    }

    private static class MetaDataSchema implements Schema<MetaData> {
      @Override
      public int getObjectSize() {
        return 8 + 13 + 4;
      }

      @Override
      public byte[] toBytes(MetaData metaData) {
        ByteBuffer byteBuffer = ByteBuffer.allocate(getObjectSize());
        byteBuffer.put(metaData.location);
        return byteBuffer.array();
      }

      @Override
      public MetaData fromBytes(byte[] bytes) {
        ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
        byte[] location = new byte[13];
        byteBuffer.get(location);
        return new MetaData(location);
      }
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
