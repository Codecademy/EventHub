package com.mobicrave.eventtracker.integration;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.name.Names;
import com.mobicrave.eventtracker.*;
import com.mobicrave.eventtracker.index.DatedEventIndex;
import com.mobicrave.eventtracker.index.DatedEventIndexModule;
import com.mobicrave.eventtracker.index.EventIndex;
import com.mobicrave.eventtracker.index.PropertiesIndex;
import com.mobicrave.eventtracker.index.PropertiesIndexModule;
import com.mobicrave.eventtracker.index.ShardedEventIndex;
import com.mobicrave.eventtracker.index.ShardedEventIndexModule;
import com.mobicrave.eventtracker.index.UserEventIndex;
import com.mobicrave.eventtracker.index.UserEventIndexModule;
import com.mobicrave.eventtracker.list.DmaIdListModule;
import com.mobicrave.eventtracker.model.Event;
import com.mobicrave.eventtracker.model.User;
import com.mobicrave.eventtracker.storage.EventStorage;
import com.mobicrave.eventtracker.storage.EventStorageModule;
import com.mobicrave.eventtracker.storage.JournalEventStorage;
import com.mobicrave.eventtracker.storage.JournalUserStorage;
import com.mobicrave.eventtracker.storage.UserStorageModule;
import com.mobicrave.eventtracker.storage.UserStorage;
import com.mobicrave.eventtracker.storage.filter.ExactMatch;
import com.mobicrave.eventtracker.storage.filter.Filter;
import com.mobicrave.eventtracker.storage.filter.Regex;
import com.mobicrave.eventtracker.storage.filter.TrueFilter;
import org.junit.Assert;
import org.junit.Test;

import javax.inject.Provider;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

public class EventTrackerTest extends GuiceTestCase {
  @Test
  public void testSingleUser() throws Exception {
    Provider<EventTracker> eventTrackerProvider = getEventTrackerProvider();
    EventTracker tracker = eventTrackerProvider.get();

    final String[] USER_IDS = { "10" };
    final String[] EVENT_TYPES = { "eventType1", "eventType2", "eventType3", "eventType4", "eventType5" };
    final String[] DATES = { "20130101", "20130102", "20130103", "20130104", "20130105" };

    addEvent(tracker, EVENT_TYPES[0], USER_IDS[0], DATES[0], Maps.<String, String>newHashMap());
    addEvent(tracker, EVENT_TYPES[1], USER_IDS[0], DATES[1], Maps.<String, String>newHashMap());
    addEvent(tracker, EVENT_TYPES[2], USER_IDS[0], DATES[2], Maps.<String, String>newHashMap());
    addEvent(tracker, EVENT_TYPES[3], USER_IDS[0], DATES[3], Maps.<String, String>newHashMap());
    addEvent(tracker, EVENT_TYPES[4], USER_IDS[0], DATES[4], Maps.<String, String>newHashMap());

    final String[] funnelSteps = { EVENT_TYPES[1], EVENT_TYPES[2], EVENT_TYPES[3] };
    List<Filter> eventFilters = Lists.<Filter>newArrayList(TrueFilter.INSTANCE, TrueFilter.INSTANCE, TrueFilter.INSTANCE);
    Assert.assertArrayEquals(new int[] { 1, 1, 1 },
        tracker.getFunnelCounts(DATES[0], DATES[4], funnelSteps, 7 /* numDaysToCompleteFunnel */,
            eventFilters, TrueFilter.INSTANCE));
    Assert.assertArrayEquals(new int[] { 0, 0, 0 },
        tracker.getFunnelCounts(DATES[0], DATES[1], funnelSteps, 7 /* numDaysToCompleteFunnel */,
            eventFilters, TrueFilter.INSTANCE));
    Assert.assertArrayEquals(new int[] { 1, 1, 1 },
        tracker.getFunnelCounts(DATES[1], DATES[2], funnelSteps, 7 /* numDaysToCompleteFunnel */,
            eventFilters, TrueFilter.INSTANCE));
    Assert.assertArrayEquals(new int[] { 1, 0, 0 },
        tracker.getFunnelCounts(DATES[0], DATES[4], funnelSteps, 1 /* numDaysToCompleteFunnel */,
            eventFilters, TrueFilter.INSTANCE));
    Assert.assertArrayEquals(new int[] { 1, 1, 1 },
        tracker.getFunnelCounts(DATES[1], DATES[2], funnelSteps, 3 /* numDaysToCompleteFunnel */,
            eventFilters, TrueFilter.INSTANCE));
    Assert.assertArrayEquals(new int[] { 1, 1, 0 },
        tracker.getFunnelCounts(DATES[1], DATES[2], funnelSteps, 2 /* numDaysToCompleteFunnel */,
            eventFilters, TrueFilter.INSTANCE));
    Assert.assertArrayEquals(new int[] { 1, 0, 0 },
        tracker.getFunnelCounts(DATES[1], DATES[2], funnelSteps, 1 /* numDaysToCompleteFunnel */,
            eventFilters, TrueFilter.INSTANCE));
  }

  @Test
  public void testAll() throws Exception {
    Provider<EventTracker> eventTrackerProvider = getEventTrackerProvider();
    EventTracker tracker = eventTrackerProvider.get();

    final String[] EVENT_TYPES = { "eventType1", "eventType2", "eventType3", "eventType4" };
    final String[] USER_IDS = { "10", "11", "12", "13", "14", "15", "16", "17", "18" };
    final String[] DATES = { "20130101", "20130102", "20130103", "20130104", "20130105" };

    tracker.addOrUpdateUser(new User.Builder("10", Maps.<String, String>newHashMap()).build());
    tracker.addOrUpdateUser(new User.Builder("11", ImmutableMap.of("hello", "world")).build());
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
    List<Filter> eventFilters = Lists.<Filter>newArrayList(TrueFilter.INSTANCE, TrueFilter.INSTANCE, TrueFilter.INSTANCE);
    Assert.assertArrayEquals(new int[] { 8, 7, 6 },
        tracker.getFunnelCounts(DATES[0], DATES[4], funnelSteps, 7 /* numDaysToCompleteFunnel */,
            eventFilters, TrueFilter.INSTANCE));
    Assert.assertArrayEquals(new int[] { 4, 3, 2 },
        tracker.getFunnelCounts(DATES[1], DATES[4], funnelSteps, 7 /* numDaysToCompleteFunnel */,
            eventFilters, TrueFilter.INSTANCE));
    Assert.assertArrayEquals(new int[] { 4, 1, 0 },
        tracker.getFunnelCounts(DATES[1], DATES[2], funnelSteps, 1 /* numDaysToCompleteFunnel */,
            eventFilters, TrueFilter.INSTANCE));
    Assert.assertArrayEquals(new long[] { 0, 1, 2, 3, 4, 5, 6, 7, 8 },
        tracker.findUsers(TrueFilter.INSTANCE));
    Assert.assertArrayEquals(new long[] { 1 },
        tracker.findUsers(new ExactMatch("hello", "world")));
    Assert.assertArrayEquals(new long[] { 3 },
        tracker.findUsers(new ExactMatch("external_user_id", "16")));

    tracker.close();
    tracker = eventTrackerProvider.get();

    Assert.assertArrayEquals(new int[] { 8, 7, 6 },
        tracker.getFunnelCounts(DATES[0], DATES[4], funnelSteps, 7 /* numDaysToCompleteFunnel */,
            eventFilters, TrueFilter.INSTANCE));
    Assert.assertArrayEquals(new int[] { 4, 3, 2 },
        tracker.getFunnelCounts(DATES[1], DATES[4], funnelSteps, 7 /* numDaysToCompleteFunnel */,
            eventFilters, TrueFilter.INSTANCE));
    Assert.assertArrayEquals(new int[] { 4, 1, 0 },
        tracker.getFunnelCounts(DATES[1], DATES[2], funnelSteps, 1 /* numDaysToCompleteFunnel */,
            eventFilters, TrueFilter.INSTANCE));
    Assert.assertArrayEquals(new long[] { 0, 1, 2, 3, 4, 5, 6, 7, 8 },
        tracker.findUsers(TrueFilter.INSTANCE));
    Assert.assertArrayEquals(new long[] { 1 },
        tracker.findUsers(new ExactMatch("hello", "world")));
    Assert.assertArrayEquals(new long[] { 3 },
        tracker.findUsers(new ExactMatch("external_user_id", "16")));
  }

  @Test
  public void testConcurrentAddEvent() throws Exception {
    Injector injector = getInjector();

    final String directory = injector.getInstance(Key.get(String.class, Names.named("eventtracker.directory")));
    final ShardedEventIndex shardedEventIndex = injector.getInstance(ShardedEventIndex.class);
    final DatedEventIndex datedEventIndex = injector.getInstance(DatedEventIndex.class);
    final PropertiesIndex propertiesIndex = injector.getInstance(PropertiesIndex.class);
    final UserEventIndex userEventIndex = injector.getInstance(UserEventIndex.class);
    final EventStorage eventStorage = injector.getInstance(JournalEventStorage.class);
    final UserStorage userStorage = injector.getInstance(JournalUserStorage.class);

    final EventTracker tracker = new EventTracker(directory, shardedEventIndex, datedEventIndex,
        propertiesIndex, userEventIndex, eventStorage, userStorage);

    final int NUM_EVENTS = 2000;
    final int NUM_THREADS = 20; // NUM_EVENTS needs to be muliple of NUM_THREADS
    final String[] EVENT_TYPES = { "eventType1", "eventType2", "eventType3", "eventType4" };
    final String[] EXTERNAL_USER_IDS = { "10", "11", "12", "13", "14", "15", "16", "17", "18" };
    final String[] DATES = { "20130101", "20130102", "20130103", "20130104", "20130105" };

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
            int userIdIndex = random.nextInt(EXTERNAL_USER_IDS.length);
            int dateIndex = counter.getAndIncrement() * DATES.length / NUM_EVENTS;
            addEvent(tracker, EVENT_TYPES[eventTypeIndex], EXTERNAL_USER_IDS[userIdIndex], DATES[dateIndex],
                Maps.<String, String>newHashMap());
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
      Assert.assertEquals(shardedEventIndex.getEventTypeId(event.getEventType()), eventStorage.getEventTypeId(eventId));
      Assert.assertEquals(userStorage.getId(event.getExternalUserId()), eventStorage.getUserId(eventId));
    }

    Set<String> externalUserIds = Sets.newHashSet();
    for (int i = 0; i < EXTERNAL_USER_IDS.length; i++) {
      externalUserIds.add(userStorage.getUser(i).getExternalId());
    }
    Assert.assertEquals(Sets.newHashSet(EXTERNAL_USER_IDS), externalUserIds);
    try {
      userStorage.getUser(EXTERNAL_USER_IDS.length);
      Assert.fail("Should fail when fetching an user with inexistent id.");
    } catch (RuntimeException e) {}

    for (int eventTypeId = 0; eventTypeId < EVENT_TYPES.length; eventTypeId++) {
      final int EVENT_TYPE_ID = eventTypeId;
      // didn't bother check the callback is actually called
      shardedEventIndex.enumerateEventIds(EVENT_TYPES[eventTypeId], DATES[0], "21991231",
          new EventIndex.Callback() {
        @Override
        public void onEventId(long eventId) {
          Assert.assertEquals(EVENT_TYPES[EVENT_TYPE_ID],
              eventStorage.getEvent(eventId).getEventType());
        }
      });
    }

    for (int userId = 0; userId < EXTERNAL_USER_IDS.length; userId++) {
      final int USER_ID = userId;
      userEventIndex.enumerateEventIds(userId, 0, NUM_EVENTS, new UserEventIndex.Callback() {
        @Override
        public boolean shouldContinueOnEventId(long eventId) {
          Assert.assertEquals(USER_ID, userStorage.getId(
              eventStorage.getEvent(eventId).getExternalUserId()));
          return true;
        }
      });
    }
  }

  @Test
  public void testFilter() throws Exception {
    Provider<EventTracker> eventTrackerProvider = getEventTrackerProvider();
    EventTracker tracker = eventTrackerProvider.get();

    final String[] USER_IDS = { "10", "11" };
    final String[] EVENT_TYPES = { "eventType1", "eventType2", "eventType3", "eventType4", "eventType5" };
    final String[] DATES = { "20130101", "20130102", "20130103", "20130104", "20130105" };
    @SuppressWarnings("unchecked") final Map<String, String>[] properties = (Map<String, String>[]) new Map[] {
        ImmutableMap.<String, String>builder().put("foo1", "bar1").put("foo2", "bar2").build(),
        ImmutableMap.<String, String>builder().put("foo2", "bar2").put("foo3", "bar3").build(),
        ImmutableMap.<String, String>builder().put("foo3", "bar3").build()
    };

    tracker.addOrUpdateUser(new User.Builder(USER_IDS[0], properties[0]).build());
    tracker.addOrUpdateUser(new User.Builder(USER_IDS[1], properties[1]).build());
    addEvent(tracker, EVENT_TYPES[0], USER_IDS[0], DATES[0], properties[0]);
    addEvent(tracker, EVENT_TYPES[0], USER_IDS[1], DATES[0], properties[0]);
    addEvent(tracker, EVENT_TYPES[1], USER_IDS[0], DATES[1], properties[0]);
    addEvent(tracker, EVENT_TYPES[1], USER_IDS[1], DATES[1], properties[0]);
    addEvent(tracker, EVENT_TYPES[2], USER_IDS[0], DATES[2], properties[1]);
    addEvent(tracker, EVENT_TYPES[2], USER_IDS[1], DATES[2], properties[1]);
    addEvent(tracker, EVENT_TYPES[3], USER_IDS[0], DATES[3], properties[1]);
    addEvent(tracker, EVENT_TYPES[3], USER_IDS[1], DATES[3], properties[1]);
    addEvent(tracker, EVENT_TYPES[4], USER_IDS[0], DATES[4], properties[2]);
    addEvent(tracker, EVENT_TYPES[4], USER_IDS[1], DATES[4], properties[2]);

    final String[] funnelSteps = { EVENT_TYPES[1], EVENT_TYPES[2], EVENT_TYPES[4] };
    List<Filter> eventFilters = Lists.<Filter>newArrayList(TrueFilter.INSTANCE, TrueFilter.INSTANCE, TrueFilter.INSTANCE);
    Assert.assertArrayEquals(new int[] {2, 2, 2},
        tracker.getFunnelCounts(DATES[0], DATES[4], funnelSteps, 7 /* numDaysToCompleteFunnel */,
            eventFilters, TrueFilter.INSTANCE));
    Assert.assertArrayEquals(new int[] {2, 0, 0},
        tracker.getFunnelCounts(DATES[0], DATES[4], funnelSteps, 7 /* numDaysToCompleteFunnel */,
            Lists.<Filter>newArrayList(
                new ExactMatch("foo1", "bar1"),
                new Regex("foo4", Pattern.compile("b.*4")),
                TrueFilter.INSTANCE),
            TrueFilter.INSTANCE));
    Assert.assertArrayEquals(new int[] {2, 2, 0},
        tracker.getFunnelCounts(DATES[0], DATES[4], funnelSteps, 7 /* numDaysToCompleteFunnel */,
            Lists.<Filter>newArrayList(
                new ExactMatch("foo2", "bar2"),
                new Regex("foo2", Pattern.compile("bar2")),
                new ExactMatch("foo2", "bar2")),
            TrueFilter.INSTANCE));
    Assert.assertArrayEquals(new int[] {2, 2, 2},
        tracker.getFunnelCounts(DATES[0], DATES[4], funnelSteps, 7 /* numDaysToCompleteFunnel */,
            eventFilters, new ExactMatch("foo2", "bar2")));
    Assert.assertArrayEquals(new int[] { 1, 1, 1 },
        tracker.getFunnelCounts(DATES[0], DATES[4], funnelSteps, 7 /* numDaysToCompleteFunnel */,
            eventFilters, new ExactMatch("foo3", "bar3")));
  }

  @Test
  public void testGetEventsByExternalUserId() throws Exception {
    Provider<EventTracker> eventTrackerProvider = getEventTrackerProvider();
    EventTracker tracker = eventTrackerProvider.get();

    final String[] USER_IDS = { "10" };
    final String[] EVENT_TYPES = { "eventType1", "eventType2", "eventType3", "eventType4", "eventType5" };
    final String[] DATES = { "20130101", "20130102", "20130103", "20130104", "20130105" };

    addEvent(tracker, EVENT_TYPES[0], USER_IDS[0], DATES[0], Maps.<String, String>newHashMap());
    addEvent(tracker, EVENT_TYPES[1], USER_IDS[0], DATES[1], Maps.<String, String>newHashMap());
    addEvent(tracker, EVENT_TYPES[2], USER_IDS[0], DATES[2], Maps.<String, String>newHashMap());
    addEvent(tracker, EVENT_TYPES[3], USER_IDS[0], DATES[3], Maps.<String, String>newHashMap());
    addEvent(tracker, EVENT_TYPES[4], USER_IDS[0], DATES[4], Maps.<String, String>newHashMap());

    List<Event> events = tracker.getUserEvents(USER_IDS[0], 2, 10);
    for (int i = 0; i < events.size(); i++) {
      Assert.assertEquals(EVENT_TYPES[i + 2], events.get(i).getEventType());
      Assert.assertEquals(DATES[i + 2], events.get(i).getDate());
      Assert.assertEquals(USER_IDS[0], events.get(i).getExternalUserId());
    }

    events = tracker.getUserEvents(USER_IDS[0], 2, 2);
    for (int i = 0; i < events.size(); i++) {
      Assert.assertEquals(EVENT_TYPES[i + 2], events.get(i).getEventType());
      Assert.assertEquals(DATES[i + 2], events.get(i).getDate());
      Assert.assertEquals(USER_IDS[0], events.get(i).getExternalUserId());
    }
  }

  @Test
  public void testGetRetentionTable() throws Exception {
    Provider<EventTracker> eventTrackerProvider = getEventTrackerProvider();
    EventTracker tracker = eventTrackerProvider.get();

    final String[] USER_IDS = { "10", "11", "12", "13", "14" };
    final String[] EVENT_TYPES = { "receive_email", "purchase", "dummy" };
    final String[] DATES = { "20130101", "20130102", "20130103", "20130104", "20130105", "20130106",
        "20130107" };

    addEvent(tracker, EVENT_TYPES[2], USER_IDS[0], DATES[0], Maps.<String, String>newHashMap());
    addEvent(tracker, EVENT_TYPES[0], USER_IDS[0], DATES[0], ImmutableMap.of("foo1", "bar1"));

    addEvent(tracker, EVENT_TYPES[0], USER_IDS[1], DATES[0], ImmutableMap.of("foo1", "bar1"));
    addEvent(tracker, EVENT_TYPES[1], USER_IDS[1], DATES[0], ImmutableMap.of("foo2", "bar2"));
    addEvent(tracker, EVENT_TYPES[1], USER_IDS[1], DATES[1], ImmutableMap.of("foo2", "bar2"));
    addEvent(tracker, EVENT_TYPES[1], USER_IDS[1], DATES[1], ImmutableMap.of("foo2", "bar2"));
    addEvent(tracker, EVENT_TYPES[0], USER_IDS[1], DATES[2], ImmutableMap.of("foo1", "bar1"));
    addEvent(tracker, EVENT_TYPES[1], USER_IDS[1], DATES[2], ImmutableMap.of("foo2", "bar2"));
    addEvent(tracker, EVENT_TYPES[1], USER_IDS[1], DATES[2], ImmutableMap.of("foo2", "bar2"));
    addEvent(tracker, EVENT_TYPES[1], USER_IDS[1], DATES[2], ImmutableMap.of("foo2", "bar2"));
    addEvent(tracker, EVENT_TYPES[1], USER_IDS[1], DATES[3], ImmutableMap.of("foo2", "bar2"));
    addEvent(tracker, EVENT_TYPES[1], USER_IDS[1], DATES[3], ImmutableMap.of("foo2", "bar2"));

    addEvent(tracker, EVENT_TYPES[2], USER_IDS[2], DATES[0], Maps.<String, String>newHashMap());
    addEvent(tracker, EVENT_TYPES[0], USER_IDS[2], DATES[1], ImmutableMap.of("foo1", "bar1"));

    addEvent(tracker, EVENT_TYPES[0], USER_IDS[3], DATES[2], ImmutableMap.of("foo1", "bar1"));
    addEvent(tracker, EVENT_TYPES[1], USER_IDS[3], DATES[2], ImmutableMap.of("foo2", "bar2"));
    addEvent(tracker, EVENT_TYPES[1], USER_IDS[3], DATES[3], ImmutableMap.of("foo2", "bar2"));
    addEvent(tracker, EVENT_TYPES[1], USER_IDS[3], DATES[3], ImmutableMap.of("foo2", "bar2"));
    addEvent(tracker, EVENT_TYPES[0], USER_IDS[3], DATES[4], ImmutableMap.of("foo1", "bar1"));
    addEvent(tracker, EVENT_TYPES[1], USER_IDS[3], DATES[4], ImmutableMap.of("foo2", "bar2"));
    addEvent(tracker, EVENT_TYPES[1], USER_IDS[3], DATES[4], ImmutableMap.of("foo2", "bar2"));
    addEvent(tracker, EVENT_TYPES[1], USER_IDS[3], DATES[4], ImmutableMap.of("foo2", "bar2"));
    addEvent(tracker, EVENT_TYPES[1], USER_IDS[3], DATES[5], ImmutableMap.of("foo2", "bar2"));
    addEvent(tracker, EVENT_TYPES[1], USER_IDS[3], DATES[5], ImmutableMap.of("foo2", "bar2"));

    addEvent(tracker, EVENT_TYPES[0], USER_IDS[4], DATES[6], ImmutableMap.of("foo1", "bar1"));
    addEvent(tracker, EVENT_TYPES[1], USER_IDS[4], DATES[6], ImmutableMap.of("foo2", "bar2"));
    addEvent(tracker, EVENT_TYPES[1], USER_IDS[4], DATES[6], ImmutableMap.of("foo2", "bar2"));
    addEvent(tracker, EVENT_TYPES[1], USER_IDS[4], DATES[6], ImmutableMap.of("foo2", "bar2"));
    addEvent(tracker, EVENT_TYPES[0], USER_IDS[4], DATES[6], ImmutableMap.of("foo1", "bar1"));
    addEvent(tracker, EVENT_TYPES[1], USER_IDS[4], DATES[6], ImmutableMap.of("foo2", "bar2"));
    addEvent(tracker, EVENT_TYPES[1], USER_IDS[4], DATES[6], ImmutableMap.of("foo2", "bar2"));
    addEvent(tracker, EVENT_TYPES[1], USER_IDS[4], DATES[6], ImmutableMap.of("foo2", "bar2"));
    addEvent(tracker, EVENT_TYPES[1], USER_IDS[4], DATES[6], ImmutableMap.of("foo2", "bar2"));
    addEvent(tracker, EVENT_TYPES[1], USER_IDS[4], DATES[6], ImmutableMap.of("foo2", "bar2"));

    int[][] expectedCount = {
 /*D0,1*/ { 3 /*0,1,2*/, 1 /*1*/  , 1 /*1*/, 0 /**/ },
 /*D2,3*/ { 2 /*1,3*/  , 2 /*1,3*/, 1 /*3*/, 0 /**/  },
 /*D4,5*/ { 1 /*3*/    , 1 /*3*/  , 0 /**/ , 0 /**/ }
    };
    int[][] retentionTable = tracker.getRetentionTable(
        "20130101", "20130106", 2, 3, EVENT_TYPES[0], EVENT_TYPES[1], TrueFilter.INSTANCE,
        TrueFilter.INSTANCE);
    for (int i = 0; i < expectedCount.length; i++) {
      for (int j = 0; j < expectedCount[i].length; j++) {
        Assert.assertEquals(expectedCount[i][j], retentionTable[i][j]);
      }
    }

    retentionTable = tracker.getRetentionTable(
        "20130101", "20130106", 2, 3, EVENT_TYPES[0], EVENT_TYPES[1], new ExactMatch("foo1", "bar1"),
        TrueFilter.INSTANCE);
    for (int i = 0; i < expectedCount.length; i++) {
      for (int j = 0; j < expectedCount[i].length; j++) {
        Assert.assertEquals(expectedCount[i][j], retentionTable[i][j]);
      }
    }

    retentionTable = tracker.getRetentionTable(
        "20130101", "20130106", 2, 3, EVENT_TYPES[0], EVENT_TYPES[1], TrueFilter.INSTANCE,
        new ExactMatch("foo2", "bar2"));
    for (int i = 0; i < expectedCount.length; i++) {
      for (int j = 0; j < expectedCount[i].length; j++) {
        Assert.assertEquals(expectedCount[i][j], retentionTable[i][j]);
      }
    }

    retentionTable = tracker.getRetentionTable(
        "20130101", "20130106", 2, 3, EVENT_TYPES[0], EVENT_TYPES[1], new ExactMatch("foo1", "bar2"),
        TrueFilter.INSTANCE);
    for (int i = 0; i < expectedCount.length; i++) {
      for (int j = 0; j < expectedCount[i].length; j++) {
        Assert.assertEquals(0, retentionTable[i][j]);
      }
    }
  }

  private void addEvent(EventTracker tracker, String eventType, String externalUserId, String day,
      Map<String, String> property) {
    tracker.addEvent(new Event.Builder(eventType, externalUserId, day, property).build());
  }

  private Provider<EventTracker> getEventTrackerProvider() {
    return getInjector().getProvider(EventTracker.class);
  }

  private Injector getInjector() {
    Properties prop = new Properties();
    prop.put("eventtracker.directory", getTempDirectory());
    prop.put("eventtracker.eventindex.initialNumEventIdsPerDay", "10");
    prop.put("eventtracker.usereventindex.numPointersPerIndexEntry", "2");
    prop.put("eventtracker.usereventindex.numIndexEntryPerFile", "2");
    prop.put("eventtracker.usereventindex.indexEntryFileCacheSize", "2");
    prop.put("eventtracker.usereventindex.numRecordsPerBlock", "2");
    prop.put("eventtracker.usereventindex.numBlocksPerFile", "2");
    prop.put("eventtracker.usereventindex.blockCacheSize", "2");
    prop.put("eventtracker.journaleventstorage.numMetaDataPerFile", "10");
    prop.put("eventtracker.journaleventstorage.metaDataFileCacheSize", "10");
    prop.put("eventtracker.journaleventstorage.journalFileSize", "1024");
    prop.put("eventtracker.journaleventstorage.journalWriteBatchSize", "1024");
    prop.put("eventtracker.cachedeventstorage.recordCacheSize", "10");
    prop.put("eventtracker.bloomfilteredeventstorage.bloomFilterSize", "64");
    prop.put("eventtracker.bloomfilteredeventstorage.numHashes", "1");
    prop.put("eventtracker.bloomfilteredeventstorage.numMetaDataPerFile", "10");
    prop.put("eventtracker.bloomfilteredeventstorage.metaDataFileCacheSize", "10");
    prop.put("eventtracker.journaluserstorage.numMetaDataPerFile", "10");
    prop.put("eventtracker.journaluserstorage.metaDataFileCacheSize", "10");
    prop.put("eventtracker.journaluserstorage.journalFileSize", "1024");
    prop.put("eventtracker.journaluserstorage.journalWriteBatchSize", "1024");
    prop.put("eventtracker.cacheduserstorage.recordCacheSize", "10");
    prop.put("eventtracker.bloomfiltereduserstorage.numMetaDataPerFile", "10");
    prop.put("eventtracker.bloomfiltereduserstorage.metaDataFileCacheSize", "10");
    prop.put("eventtracker.bloomfiltereduserstorage.bloomFilterSize", "64");
    prop.put("eventtracker.bloomfiltereduserstorage.numHashes", "1");

    return createInjectorFor(new Properties(),
        new EventTrackerModule(prop),
        new DmaIdListModule(),
        new ShardedEventIndexModule(),
        new DatedEventIndexModule(),
        new PropertiesIndexModule(),
        new UserEventIndexModule(),
        new EventStorageModule(),
        new UserStorageModule());
  }
}
