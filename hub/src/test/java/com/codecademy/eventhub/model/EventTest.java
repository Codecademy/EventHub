package com.codecademy.eventhub.model;

import com.google.common.collect.Maps;
import com.codecademy.eventhub.base.KeyValueCallback;
import org.junit.Assert;
import org.junit.Test;

public class EventTest {
  @Test
  public void testAll() throws Exception {
    String eventType = "eventType1";
    String externalUserId = "foo";
    String date = "20131101";
    Event event = new Event.Builder(
        eventType, externalUserId, date, Maps.<String, String>newHashMap())
        .add("key1", "value1")
        .add("key3", "value3")
        .add("key2", "value2")
        .build();

    Assert.assertEquals("eventType1", event.getEventType());
    Assert.assertEquals("foo", event.getExternalUserId());
    Assert.assertEquals("20131101", event.getDate());
    Assert.assertEquals("value1", event.get("key1"));
    Assert.assertEquals("value2", event.get("key2"));
    Assert.assertEquals("value3", event.get("key3"));
    Assert.assertNull(event.get("key_not_exists"));

    event = Event.fromByteBuffer(event.toByteBuffer());
    Assert.assertEquals("eventType1", event.getEventType());
    Assert.assertEquals("foo", event.getExternalUserId());
    Assert.assertEquals("20131101", event.getDate());
    Assert.assertEquals("value1", event.get("key1"));
    Assert.assertEquals("value2", event.get("key2"));
    Assert.assertEquals("value3", event.get("key3"));
    Assert.assertNull(event.get("key_not_exists"));

    MyCallback callback = new MyCallback(
        new String[] { "date", "event_type", "external_user_id", "key1", "key2", "key3" },
        new String[] { "20131101", "eventType1", "foo", "value1", "value2", "value3" });
    event.enumerate(callback);
    callback.verify(6);
  }

  private static class MyCallback implements KeyValueCallback {
    private final String[] expectedKeys;
    private final String[] expectedValues;
    private int counter;

    private MyCallback(String[] expectedKeys, String[] expectedValues) {
      this.expectedKeys = expectedKeys;
      this.expectedValues = expectedValues;
      this.counter = 0;
    }

    @Override
    public void callback(String key, String value) {
      Assert.assertEquals(expectedKeys[counter], key);
      Assert.assertEquals(expectedValues[counter], value);
      counter++;
    }

    public void verify(int expectedCount) {
      Assert.assertEquals(expectedCount, counter);
    }
  }
}
