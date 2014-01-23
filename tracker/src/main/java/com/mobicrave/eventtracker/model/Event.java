package com.mobicrave.eventtracker.model;

import com.google.common.collect.Maps;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.TreeMap;

public class Event {
  private static final int META_DATA_SIZE_IN_BYTES = 4; /* bytes */
  private final ByteBuffer byteBuffer;

  private Event(ByteBuffer byteBuffer) {
    this.byteBuffer = byteBuffer;
  }

  public String getEventType() {
    return get("eventType");
  }

  public String getDate() {
    return get("date");
  }

  public String getExternalUserId() {
    return get("externalUserId");
  }

  public String get(String key) {
    ByteBuffer currentBuffer = byteBuffer.duplicate();
    currentBuffer.position(0);
    int numProperties = currentBuffer.getInt();
    return get(key, 0, numProperties, numProperties);
  }

  public void enumerate(Callback callback) {
    ByteBuffer currentBuffer = byteBuffer.duplicate();
    currentBuffer.position(0);

    ByteBuffer pointersBuffer = currentBuffer.duplicate();
    pointersBuffer.position(META_DATA_SIZE_IN_BYTES);
    int numProperties = currentBuffer.getInt();
    for (int i = 0; i < numProperties; i++) {
      // TODO: can be optimized
      callback.callback(getKey(currentBuffer, i, numProperties),
          getValue(currentBuffer, i, numProperties));
    }
  }

  public ByteBuffer toByteBuffer() {
    ByteBuffer buffer = byteBuffer.duplicate();
    buffer.position(0);
    return buffer;
  }

  private String get(String targetKey, int start, int end, int numProperties) {
    if (start >= end) {
      return null;
    }
    ByteBuffer currentBuffer = byteBuffer.duplicate();

    int currentRecordOffset = (start + end) / 2;
    String key = getKey(currentBuffer, currentRecordOffset, numProperties);

    int comparisonResult = key.compareTo(targetKey);
    if (comparisonResult == 0) {
      return getValue(currentBuffer, currentRecordOffset, numProperties);
    } else if (comparisonResult < 0) {
      return get(targetKey, currentRecordOffset + 1, end, numProperties);
    } else {
      return get(targetKey, start, currentRecordOffset, numProperties);
    }
  }

  private String getKey(ByteBuffer buffer, int recordOffset, int numProperties) {
    buffer.position(META_DATA_SIZE_IN_BYTES + recordOffset * 4);
    int startOffsetInBytes;
    if (recordOffset == 0) {
      startOffsetInBytes = META_DATA_SIZE_IN_BYTES + 2 * numProperties * 4;
    } else {
      buffer.position(META_DATA_SIZE_IN_BYTES + (recordOffset - 1) * 4);
      startOffsetInBytes = buffer.getInt();
    }
    int finishOffsetInBytes = buffer.getInt();
    return getString(buffer, startOffsetInBytes, finishOffsetInBytes);
  }

  private String getValue(ByteBuffer buffer, int recordOffset, int numProperties) {
    buffer.position(META_DATA_SIZE_IN_BYTES + (numProperties + recordOffset - 1) * 4);
    int startOffsetInBytes = buffer.getInt();
    buffer.position(META_DATA_SIZE_IN_BYTES + (numProperties + recordOffset) * 4);
    int finishOffsetInBytes = buffer.getInt();
    return getString(buffer, startOffsetInBytes, finishOffsetInBytes);
  }

  private String getString(ByteBuffer buffer, int startOffsetInBytes, int finishOffsetInBytes) {
    buffer.position(startOffsetInBytes);
    byte[] keyBytes = new byte[finishOffsetInBytes - startOffsetInBytes];
    buffer.get(keyBytes);
    return new String(keyBytes);
  }

  public static Event fromByteBuffer(ByteBuffer byteBuffer) {
    return new Event(byteBuffer.duplicate());
  }

  public static interface Callback {
    public void callback(String key, String value);
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
      TreeMap<String, String> sortedProperties = Maps.newTreeMap();
      sortedProperties.putAll(properties);
      sortedProperties.put("eventType", eventType);
      sortedProperties.put("date", date);
      sortedProperties.put("externalUserId", externalUserId);

      int propertiesSizeInBytes = 0;
      for (Map.Entry<String, String> entry : sortedProperties.entrySet()) {
        propertiesSizeInBytes += entry.getKey().getBytes().length;
        propertiesSizeInBytes += entry.getValue().getBytes().length;
      }

      int pointersSizeInBytes = 2 * sortedProperties.size() * 4;
      byte[] bytes = new byte[META_DATA_SIZE_IN_BYTES + pointersSizeInBytes + propertiesSizeInBytes];

      // initialize metadata
      ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
      byteBuffer.putInt(sortedProperties.size());

      // initialize keys and key pointers
      byteBuffer.position(META_DATA_SIZE_IN_BYTES);
      ByteBuffer propertiesBuffer = byteBuffer.duplicate();
      propertiesBuffer.position(META_DATA_SIZE_IN_BYTES + pointersSizeInBytes);
      for (String key : sortedProperties.keySet()) {
        propertiesBuffer.put(key.getBytes());
        byteBuffer.putInt(propertiesBuffer.position());
      }

      // initialize values and value pointers
      for (String value : sortedProperties.values()) {
        propertiesBuffer.put(value.getBytes());
        byteBuffer.putInt(propertiesBuffer.position());
      }

      return new Event(byteBuffer);
    }
  }
}
