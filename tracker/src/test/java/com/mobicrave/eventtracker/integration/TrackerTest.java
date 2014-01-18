package com.mobicrave.eventtracker.integration;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mobicrave.eventtracker.*;
import com.mobicrave.eventtracker.index.EventIndex;
import com.mobicrave.eventtracker.index.UserEventIndex;
import com.mobicrave.eventtracker.model.Event;
import com.mobicrave.eventtracker.model.User;
import com.mobicrave.eventtracker.storage.EventStorage;
import com.mobicrave.eventtracker.storage.JournalEventStorage;
import com.mobicrave.eventtracker.storage.JournalUserStorage;
import com.mobicrave.eventtracker.storage.UserStorage;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.util.Collections;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

public class TrackerTest {
  @Rule
  public TemporaryFolder folder = new TemporaryFolder();

  @Test
  public void testSingleUser() throws Exception {
    String directory = folder.newFolder("tracker-test").getCanonicalPath() + "/";
    EventTracker tracker = EventTracker.build(directory);

    final String[] USER_IDS = { "10" };
    final String[] EVENT_TYPES = { "eventType1", "eventType2", "eventType3", "eventType4", "eventType5" };
    final String[] DATES = { "20130101", "20130102", "20130103", "20130104", "20130105" };

    for (String eventType : EVENT_TYPES) {
      tracker.addEventType(eventType);
    }
    addUser(tracker, USER_IDS[0], Maps.<String, String>newHashMap());
    addEvent(tracker, EVENT_TYPES[0], USER_IDS[0], DATES[0], Maps.<String, String>newHashMap());
    addEvent(tracker, EVENT_TYPES[1], USER_IDS[0], DATES[1], Maps.<String, String>newHashMap());
    addEvent(tracker, EVENT_TYPES[2], USER_IDS[0], DATES[2], Maps.<String, String>newHashMap());
    addEvent(tracker, EVENT_TYPES[3], USER_IDS[0], DATES[3], Maps.<String, String>newHashMap());
    addEvent(tracker, EVENT_TYPES[4], USER_IDS[0], DATES[4], Maps.<String, String>newHashMap());

    final String[] funnelSteps = { EVENT_TYPES[1], EVENT_TYPES[2], EVENT_TYPES[3] };
    Assert.assertArrayEquals(new int[] { 1, 1, 1 },
        tracker.getCounts(DATES[0], DATES[4], funnelSteps, 7 /* numDaysToCompleteFunnel */,
            Collections.EMPTY_LIST, Collections.EMPTY_LIST));
    Assert.assertArrayEquals(new int[] { 0, 0, 0 },
        tracker.getCounts(DATES[0], DATES[1], funnelSteps, 7 /* numDaysToCompleteFunnel */,
            Collections.EMPTY_LIST, Collections.EMPTY_LIST));
    Assert.assertArrayEquals(new int[] { 1, 1, 1 },
        tracker.getCounts(DATES[1], DATES[2], funnelSteps, 7 /* numDaysToCompleteFunnel */,
            Collections.EMPTY_LIST, Collections.EMPTY_LIST));
    Assert.assertArrayEquals(new int[] { 1, 0, 0 },
        tracker.getCounts(DATES[0], DATES[4], funnelSteps, 1 /* numDaysToCompleteFunnel */,
            Collections.EMPTY_LIST, Collections.EMPTY_LIST));
    Assert.assertArrayEquals(new int[] { 1, 1, 1 },
        tracker.getCounts(DATES[1], DATES[2], funnelSteps, 3 /* numDaysToCompleteFunnel */,
            Collections.EMPTY_LIST, Collections.EMPTY_LIST));
    Assert.assertArrayEquals(new int[] { 1, 1, 0 },
        tracker.getCounts(DATES[1], DATES[2], funnelSteps, 2 /* numDaysToCompleteFunnel */,
            Collections.EMPTY_LIST, Collections.EMPTY_LIST));
    Assert.assertArrayEquals(new int[] { 1, 0, 0 },
        tracker.getCounts(DATES[1], DATES[2], funnelSteps, 1 /* numDaysToCompleteFunnel */,
            Collections.EMPTY_LIST, Collections.EMPTY_LIST));
  }

  @Test
  public void testAll() throws Exception {
    String directory = folder.newFolder("tracker-test").getCanonicalPath() + "/";
    EventTracker tracker = EventTracker.build(directory);

    final String[] EVENT_TYPES = { "eventType1", "eventType2", "eventType3", "eventType4" };
    final String[] USER_IDS = { "10", "11", "12", "13", "14", "15", "16", "17", "18" };
    final String[] DATES = { "20130101", "20130102", "20130103", "20130104", "20130105" };

    for (String eventType : EVENT_TYPES) {
      tracker.addEventType(eventType);
    }
    for (String userId : USER_IDS) {
      addUser(tracker, userId, Maps.<String, String>newHashMap());
    }
    addEvent(tracker, EVENT_TYPES[0], USER_IDS[0], DATES[0], Maps.<String, String>newHashMap());
    addEvent(tracker, EVENT_TYPES[0], USER_IDS[1], DATES[0], Maps.<String, String>newHashMap());
    addEvent(tracker, EVENT_TYPES[0], USER_IDS[3], DATES[0], Maps.<String, String>newHashMap());
    addEvent(tracker, EVENT_TYPES[0], USER_IDS[6], DATES[0], Maps.<String, String>newHashMap());
    addEvent(tracker, EVENT_TYPES[1], USER_IDS[6], DATES[0], Maps.<String, String>newHashMap());
    addEvent(tracker, EVENT_TYPES[0], USER_IDS[7], DATES[0], Maps.<String, String>newHashMap());

    addEvent(tracker, EVENT_TYPES[0], USER_IDS[0], DATES[1], Maps.<String, String>newHashMap());
    addEvent(tracker, EVENT_TYPES[1], USER_IDS[1], DATES[1], Maps.<String, String>newHashMap());
    addEvent(tracker, EVENT_TYPES[0], USER_IDS[2], DATES[1], Maps.<String, String>newHashMap());
    addEvent(tracker, EVENT_TYPES[1], USER_IDS[3], DATES[1], Maps.<String, String>newHashMap());
    addEvent(tracker, EVENT_TYPES[0], USER_IDS[5], DATES[1], Maps.<String, String>newHashMap());
    addEvent(tracker, EVENT_TYPES[3], USER_IDS[6], DATES[1], Maps.<String, String>newHashMap());
    addEvent(tracker, EVENT_TYPES[1], USER_IDS[7], DATES[1], Maps.<String, String>newHashMap());
    addEvent(tracker, EVENT_TYPES[3], USER_IDS[7], DATES[1], Maps.<String, String>newHashMap());
    addEvent(tracker, EVENT_TYPES[0], USER_IDS[8], DATES[1], Maps.<String, String>newHashMap());
    addEvent(tracker, EVENT_TYPES[1], USER_IDS[8], DATES[1], Maps.<String, String>newHashMap());

    addEvent(tracker, EVENT_TYPES[1], USER_IDS[0], DATES[2], Maps.<String, String>newHashMap());
    addEvent(tracker, EVENT_TYPES[3], USER_IDS[1], DATES[2], Maps.<String, String>newHashMap());
    addEvent(tracker, EVENT_TYPES[1], USER_IDS[2], DATES[2], Maps.<String, String>newHashMap());
    addEvent(tracker, EVENT_TYPES[2], USER_IDS[3], DATES[2], Maps.<String, String>newHashMap());
    addEvent(tracker, EVENT_TYPES[1], USER_IDS[4], DATES[2], Maps.<String, String>newHashMap());
    addEvent(tracker, EVENT_TYPES[3], USER_IDS[5], DATES[2], Maps.<String, String>newHashMap());

    addEvent(tracker, EVENT_TYPES[3], USER_IDS[0], DATES[3], Maps.<String, String>newHashMap());
    addEvent(tracker, EVENT_TYPES[3], USER_IDS[2], DATES[3], Maps.<String, String>newHashMap());
    addEvent(tracker, EVENT_TYPES[3], USER_IDS[3], DATES[3], Maps.<String, String>newHashMap());
    addEvent(tracker, EVENT_TYPES[2], USER_IDS[4], DATES[3], Maps.<String, String>newHashMap());

    final String[] funnelSteps = { EVENT_TYPES[0], EVENT_TYPES[1], EVENT_TYPES[3] };
    Assert.assertArrayEquals(new int[] { 8, 7, 6 },
        tracker.getCounts(DATES[0], DATES[4], funnelSteps, 7 /* numDaysToCompleteFunnel */,
            Collections.EMPTY_LIST, Collections.EMPTY_LIST));
    Assert.assertArrayEquals(new int[] { 4, 3, 2 },
        tracker.getCounts(DATES[1], DATES[4], funnelSteps, 7 /* numDaysToCompleteFunnel */,
            Collections.EMPTY_LIST, Collections.EMPTY_LIST));
    Assert.assertArrayEquals(new int[] { 4, 1, 0 },
        tracker.getCounts(DATES[1], DATES[2], funnelSteps, 1 /* numDaysToCompleteFunnel */,
            Collections.EMPTY_LIST, Collections.EMPTY_LIST));

    tracker.close();
    tracker = EventTracker.build(directory);

    Assert.assertArrayEquals(new int[] { 8, 7, 6 },
        tracker.getCounts(DATES[0], DATES[4], funnelSteps, 7 /* numDaysToCompleteFunnel */,
            Collections.EMPTY_LIST, Collections.EMPTY_LIST));
    Assert.assertArrayEquals(new int[] { 4, 3, 2 },
        tracker.getCounts(DATES[1], DATES[4], funnelSteps, 7 /* numDaysToCompleteFunnel */,
            Collections.EMPTY_LIST, Collections.EMPTY_LIST));
    Assert.assertArrayEquals(new int[] { 4, 1, 0 },
        tracker.getCounts(DATES[1], DATES[2], funnelSteps, 1 /* numDaysToCompleteFunnel */,
            Collections.EMPTY_LIST, Collections.EMPTY_LIST));
  }

  @Test
  public void testConcurrentAddEvent() throws Exception {
    final String directory = folder.newFolder("tracker-test").getCanonicalPath() + "/";
    String eventIndexDirectory = directory + "/event_index/";
    String userEventIndexDirectory = directory + "/user_event_index/";
    String eventStorageDirectory = directory + "/event_storage/";
    String userStorageDirectory = directory + "/user_storage/";

    final EventIndex eventIndex = EventIndex.build(eventIndexDirectory);
    final UserEventIndex userEventIndex = UserEventIndex.build(userEventIndexDirectory);
    final EventStorage eventStorage = JournalEventStorage.build(eventStorageDirectory);
    final UserStorage userStorage = JournalUserStorage.build(userStorageDirectory);

    final EventTracker tracker = new EventTracker(directory, eventIndex, userEventIndex,
        eventStorage, userStorage);

    final int NUM_EVENTS = 2000;
    final int NUM_THREADS = 20; // NUM_EVENTS needs to be muliple of NUM_THREADS
    final String[] EVENT_TYPES = { "eventType1", "eventType2", "eventType3", "eventType4" };
    final String[] USER_IDS = { "10", "11", "12", "13", "14", "15", "16", "17", "18" };
    final String[] DATES = { "20130101", "20130102", "20130103", "20130104", "20130105" };

    for (String eventType : EVENT_TYPES) {
      tracker.addEventType(eventType);
    }
    for (String userId : USER_IDS) {
      addUser(tracker, userId, Maps.<String, String>newHashMap());
    }

    final AtomicInteger counter = new AtomicInteger(0);
    final Random random = new Random();
    final CountDownLatch latch = new CountDownLatch(NUM_THREADS);
    Thread[] threads = new Thread[NUM_THREADS];
    for (int i = 0; i < NUM_THREADS; i++) {
      Thread thread = new Thread(new Runnable() {
        @Override
        public void run() {
          latch.countDown();
          try {
            latch.await();
          } catch (InterruptedException e) {
            throw new RuntimeException(e);
          }
          for (int j = 0; j < NUM_EVENTS / NUM_THREADS; j++) {
            int eventTypeIndex = random.nextInt(EVENT_TYPES.length);
            int userIdIndex = random.nextInt(USER_IDS.length);
            int dateIndex = counter.getAndIncrement() * DATES.length / NUM_EVENTS;
            addEvent(tracker, EVENT_TYPES[eventTypeIndex], USER_IDS[userIdIndex], DATES[dateIndex], Maps.<String, String>newHashMap());
          }
        }
      });
      threads[i] = thread;
      thread.start();
    }
    for (int i = 0; i < NUM_THREADS; i++) {
      threads[i].join();
    }

    for (int eventId = 0; eventId < NUM_EVENTS; eventId++) {
      Event event = eventStorage.getEvent(eventId);
      Assert.assertEquals(eventIndex.getEventTypeId(event.getEventType()), eventStorage.getEventTypeId(eventId));
      Assert.assertEquals(userStorage.getId(event.getExternalUserId()), eventStorage.getUserId(eventId));
    }

    for (int eventTypeId = 0; eventTypeId < EVENT_TYPES.length; eventTypeId++) {
      final int EVENT_TYPE_ID = eventTypeId;
      // didn't bother check the callback is actually called
      eventIndex.enumerateEventIds(EVENT_TYPES[eventTypeId], DATES[0], "21991231",
          new EventIndex.Callback() {
        @Override
        public void onEventId(long eventId) {
          Assert.assertEquals(EVENT_TYPES[EVENT_TYPE_ID],
              eventStorage.getEvent(eventId).getEventType());
        }
      });
    }

    for (int userId = 0; userId < USER_IDS.length; userId++) {
      final int USER_ID = userId;
      userEventIndex.enumerateEventIds(userId, 0, NUM_EVENTS, new UserEventIndex.Callback() {
        @Override
        public boolean onEventId(long eventId) {
          Assert.assertEquals(USER_ID, userStorage.getId(
              eventStorage.getEvent(eventId).getExternalUserId()));
          return true;
        }
      });
    }
  }

  @Test
  public void testFilter() throws Exception {
    String directory = folder.newFolder("tracker-test").getCanonicalPath() + "/";
    EventTracker tracker = EventTracker.build(directory);

    final String[] USER_IDS = { "10", "11" };
    final String[] EVENT_TYPES = { "eventType1", "eventType2", "eventType3", "eventType4", "eventType5" };
    final String[] DATES = { "20130101", "20130102", "20130103", "20130104", "20130105" };
    final Map<String, String>[] properties = (Map<String, String>[]) new Map[] {
        ImmutableMap.<String, String>builder().put("foo1", "bar1").put("foo2", "bar2").build(),
        ImmutableMap.<String, String>builder().put("foo2", "bar2").put("foo3", "bar3").build(),
        ImmutableMap.<String, String>builder().put("foo3", "bar3").build()
    };

    for (String eventType : EVENT_TYPES) {
      tracker.addEventType(eventType);
    }
    for (int i = 0; i < USER_IDS.length; i++) {
      addUser(tracker, USER_IDS[i], properties[i]);
      addEvent(tracker, EVENT_TYPES[0], USER_IDS[i], DATES[0], properties[0]);
      addEvent(tracker, EVENT_TYPES[1], USER_IDS[i], DATES[1], properties[0]);
      addEvent(tracker, EVENT_TYPES[2], USER_IDS[i], DATES[2], properties[1]);
      addEvent(tracker, EVENT_TYPES[3], USER_IDS[i], DATES[3], properties[1]);
      addEvent(tracker, EVENT_TYPES[4], USER_IDS[i], DATES[4], properties[2]);
    }
    final String[] funnelSteps = { EVENT_TYPES[1], EVENT_TYPES[2], EVENT_TYPES[4] };
    Assert.assertArrayEquals(new int[] { 2, 2, 2 },
        tracker.getCounts(DATES[0], DATES[4], funnelSteps, 7 /* numDaysToCompleteFunnel */,
            Collections.EMPTY_LIST, Collections.EMPTY_LIST));
    Assert.assertArrayEquals(new int[] { 2, 2, 0 },
        tracker.getCounts(DATES[0], DATES[4], funnelSteps, 7 /* numDaysToCompleteFunnel */,
            Lists.newArrayList(new Criterion("foo2", "bar2")), Collections.EMPTY_LIST));
    Assert.assertArrayEquals(new int[] { 2, 2, 2 },
        tracker.getCounts(DATES[0], DATES[4], funnelSteps, 7 /* numDaysToCompleteFunnel */,
            Collections.EMPTY_LIST, Lists.newArrayList(new Criterion("foo2", "bar2"))));
    Assert.assertArrayEquals(new int[] { 1, 1, 1 },
        tracker.getCounts(DATES[0], DATES[4], funnelSteps, 7 /* numDaysToCompleteFunnel */,
            Collections.EMPTY_LIST, Lists.newArrayList(new Criterion("foo3", "bar3"))));
  }

  @Test
  public void testConcurrentAddUser() throws Exception {
  }

  private void addUser(EventTracker tracker, String userId, Map<String, String> properties) {
    tracker.addUser(new User.Builder(userId, properties).build());
  }

  private void addEvent(EventTracker tracker, String eventType, String externalUserId, String day,
      Map<String, String> property) {
    tracker.addEvent(new Event.Builder(eventType, externalUserId, day, property).build());
  }
}
