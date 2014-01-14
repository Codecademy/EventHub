package com.mobicrave.eventtracker.index;

import com.mobicrave.eventtracker.index.UserEventIndex;
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
    userEventIndex.addUser(0);
    userEventIndex.addUser(1);
    userEventIndex.addUser(2);

    userEventIndex.addEvent(0, 10);
    userEventIndex.addEvent(1, 20);
    userEventIndex.addEvent(2, 30);
    userEventIndex.addEvent(0, 40);
    userEventIndex.addEvent(1, 50);
    userEventIndex.addEvent(2, 60);
    userEventIndex.addEvent(0, 70);
    userEventIndex.addEvent(1, 80);
    userEventIndex.addEvent(2, 90);
    userEventIndex.addEvent(0, 100);
    userEventIndex.addEvent(1, 110);
    userEventIndex.addEvent(2, 120);

    IdVerificationCallback callback = new IdVerificationCallback(new int[] { 20, 50, 80, 110 });
    userEventIndex.enumerateEventIds(1, 1, 1000, callback);
    callback.verify();

    callback = new IdVerificationCallback(new int[] { 50, 80 });
    userEventIndex.enumerateEventIds(1, 50, 81, callback);
    callback.verify();

    callback = new IdVerificationCallback(new int[] { 50 });
    userEventIndex.enumerateEventIds(1, 50, 80, callback);
    callback.verify();

    userEventIndex.close();
    userEventIndex = UserEventIndex.build(dataDir);

    callback = new IdVerificationCallback(new int[] { 20, 50, 80, 110 });
    userEventIndex.enumerateEventIds(1, 1, 1000, callback);
    callback.verify();

    callback = new IdVerificationCallback(new int[] { 50, 80 });
    userEventIndex.enumerateEventIds(1, 50, 81, callback);
    callback.verify();

    callback = new IdVerificationCallback(new int[] { 50 });
    userEventIndex.enumerateEventIds(1, 50, 80, callback);
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
