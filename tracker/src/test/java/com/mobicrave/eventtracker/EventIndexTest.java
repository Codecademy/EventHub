package com.mobicrave.eventtracker;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class EventIndexTest {
  @Rule
  public TemporaryFolder folder = new TemporaryFolder();

  @Test
  public void testAll() throws Exception {
    String dataDir = folder.newFolder("event-index-test").getCanonicalPath() + "/";
    EventIndex eventIndex = EventIndex.build(dataDir);
    String[] eventTypes = new String[] { "a", "b", "c" };
    String[] dates = new String[] { "20130101", "20130102", "20131111", "20131201" };

    for (String eventType : eventTypes) {
      eventIndex.addEventType(eventType);
    }
    eventIndex.addEvent(1, eventTypes[0], dates[0]);
    eventIndex.addEvent(2, eventTypes[1], dates[0]);
    eventIndex.addEvent(3, eventTypes[0], dates[1]);
    eventIndex.addEvent(4, eventTypes[0], dates[1]);
    eventIndex.addEvent(5, eventTypes[1], dates[1]);
    eventIndex.addEvent(15, eventTypes[1], dates[1]);
    eventIndex.addEvent(16, eventTypes[0], dates[2]);
    eventIndex.addEvent(17, eventTypes[1], dates[2]);
    eventIndex.addEvent(18, eventTypes[0], dates[3]);
    eventIndex.addEvent(19, eventTypes[1], dates[3]);

    for (int i = 0; i < eventTypes.length; i++) {
      Assert.assertEquals(i, eventIndex.getEventTypeId(eventTypes[i]));
    }

    IdVerificationCallback callback = new IdVerificationCallback(new int[] { 3, 4, 16 });
    eventIndex.enumerateEventIds(eventTypes[0], dates[1], dates[3], callback);
    callback.verify();

    Assert.assertEquals(3, eventIndex.findFirstEventIdOnDate(1, 1));
    Assert.assertEquals(3, eventIndex.findFirstEventIdOnDate(2, 1));
    Assert.assertEquals(16, eventIndex.findFirstEventIdOnDate(2, 2));

    eventIndex.close(dataDir);
    eventIndex = EventIndex.build(dataDir);

    for (int i = 0; i < eventTypes.length; i++) {
      Assert.assertEquals(i, eventIndex.getEventTypeId(eventTypes[i]));
    }

    callback = new IdVerificationCallback(new int[] { 3, 4, 16 });
    eventIndex.enumerateEventIds(eventTypes[0], dates[1], dates[3], callback);
    callback.verify();

    Assert.assertEquals(3, eventIndex.findFirstEventIdOnDate(1, 1));
    Assert.assertEquals(3, eventIndex.findFirstEventIdOnDate(2, 1));
    Assert.assertEquals(16, eventIndex.findFirstEventIdOnDate(2, 2));
  }

  private static class IdVerificationCallback implements EventIndex.Callback {
    private final int[] expectedIds;
    private int counter;

    public IdVerificationCallback(int[] expectedIds) {
      this.expectedIds = expectedIds;
      this.counter = 0;
    }

    @Override
    public void onEventId(long eventId) {
      Assert.assertEquals(expectedIds[counter++], eventId);
    }

    public void verify() {
      Assert.assertEquals(expectedIds.length, counter);
    }
  }
}
