package com.mobicrave.eventtracker;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class UserEventIndexTest {
  @Rule
  public TemporaryFolder folder = new TemporaryFolder();

  @Test
  public void testAll() throws Exception {
    String dataDir = folder.newFolder("user-event-index-test").getCanonicalPath() + "/";

    UserEventIndex userEventIndex = UserEventIndex.build(dataDir);
    userEventIndex.addUser(1);
    userEventIndex.addUser(3);
    userEventIndex.addUser(5);

    userEventIndex.addEvent(10, 1);
    userEventIndex.addEvent(20, 3);
    userEventIndex.addEvent(30, 5);
    userEventIndex.addEvent(40, 1);
    userEventIndex.addEvent(50, 3);
    userEventIndex.addEvent(60, 5);
    userEventIndex.addEvent(70, 1);
    userEventIndex.addEvent(80, 3);
    userEventIndex.addEvent(90, 5);
    userEventIndex.addEvent(100, 1);
    userEventIndex.addEvent(110, 3);
    userEventIndex.addEvent(120, 5);

    IdVerificationCallback callback = new IdVerificationCallback(new int[] { 20, 50, 80, 110 });
    userEventIndex.enumerateEventIds(3, 1, 1000, callback);
    callback.verify();

    callback = new IdVerificationCallback(new int[] { 50, 80 });
    userEventIndex.enumerateEventIds(3, 50, 81, callback);
    callback.verify();

    callback = new IdVerificationCallback(new int[] { 50 });
    userEventIndex.enumerateEventIds(3, 50, 80, callback);
    callback.verify();

    userEventIndex.close(dataDir);
    userEventIndex = UserEventIndex.build(dataDir);

    callback = new IdVerificationCallback(new int[] { 20, 50, 80, 110 });
    userEventIndex.enumerateEventIds(3, 1, 1000, callback);
    callback.verify();

    callback = new IdVerificationCallback(new int[] { 50, 80 });
    userEventIndex.enumerateEventIds(3, 50, 81, callback);
    callback.verify();

    callback = new IdVerificationCallback(new int[] { 50 });
    userEventIndex.enumerateEventIds(3, 50, 80, callback);
    callback.verify();
  }

  private static class IdVerificationCallback implements UserEventIndex.Callback {
    private final int[] expectedIds;
    private int counter;

    public IdVerificationCallback(int[] expectedIds) {
      this.expectedIds = expectedIds;
      this.counter = 0;
    }

    @Override
    public boolean onEventId(long eventId) {
      Assert.assertEquals(expectedIds[counter++], eventId);
      return true;
    }

    public void verify() {
      Assert.assertEquals(expectedIds.length, counter);
    }
  }
}
