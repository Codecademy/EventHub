package com.mobicrave.eventtracker.index;

import com.google.inject.Injector;
import com.mobicrave.eventtracker.integration.GuiceTestCase;
import com.mobicrave.eventtracker.list.DmaIdListModule;
import org.junit.Assert;
import org.junit.Test;

import javax.inject.Provider;
import java.util.Properties;

public class UserEventIndexTest extends GuiceTestCase {
  @Test
  public void testAll() throws Exception {
    Provider<UserEventIndex> userEventIndexProvider = getUserEventIndexProvider();
    UserEventIndex userEventIndex = userEventIndexProvider.get();
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
    userEventIndex = userEventIndexProvider.get();

    callback = new IdVerificationCallback(new int[] { 20, 50, 80, 110 });
    userEventIndex.enumerateEventIds(1, 1, 1000, callback);
    callback.verify();

    callback = new IdVerificationCallback(new int[] { 50, 80 });
    userEventIndex.enumerateEventIds(1, 50, 81, callback);
    callback.verify();

    callback = new IdVerificationCallback(new int[] { 50 });
    userEventIndex.enumerateEventIds(1, 50, 80, callback);
    callback.verify();

    callback = new IdVerificationCallback(new int[] { 50, 80 });
    userEventIndex.enumerateEventIdsByOffset(1, 1, 2, callback);
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
    public boolean shouldContinueOnEventId(long eventId) {
      Assert.assertEquals(expectedIds[counter++], eventId);
      return true;
    }

    public void verify() {
      Assert.assertEquals(expectedIds.length, counter);
    }
  }

  private Provider<UserEventIndex> getUserEventIndexProvider() {
    Properties prop = new Properties();
    prop.put("eventtracker.directory", getTempDirectory());
    prop.put("eventtracker.usereventindex.numFilesPerDir", "10");
    prop.put("eventtracker.usereventindex.metaDataCacheSize", "1");
    prop.put("eventtracker.usereventindex.initialNumEventIdsPerUserDay", "1");

    Injector injector = createInjectorFor(
        prop, new DmaIdListModule(), new UserEventIndexModule());
    return injector.getProvider(UserEventIndex.class);
  }
}
