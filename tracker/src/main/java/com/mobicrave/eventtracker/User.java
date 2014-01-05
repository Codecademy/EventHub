package com.mobicrave.eventtracker;

import java.util.Map;

public class User {
  private final Map<String, String> properties;
  private final String externalId;
  private MetaData metaData;

  public User(String externalId, Map<String, String> properties) {
    this.externalId = externalId;
    this.properties = properties;
  }

  public String getExternalId() {
    return externalId;
  }

  public MetaData getMetaData() {
    return null;
  }

  public static class MetaData {
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
      User user = new User(externalUserId, properties);
      return user;
    }
  }
}
