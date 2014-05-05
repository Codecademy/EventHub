package com.codecademy.eventhub.integration;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.name.Names;
import com.codecademy.eventhub.*;
import com.codecademy.eventhub.index.DatedEventIndex;
import com.codecademy.eventhub.index.DatedEventIndexModule;
import com.codecademy.eventhub.index.EventIndex;
import com.codecademy.eventhub.index.PropertiesIndex;
import com.codecademy.eventhub.index.PropertiesIndexModule;
import com.codecademy.eventhub.index.ShardedEventIndex;
import com.codecademy.eventhub.index.ShardedEventIndexModule;
import com.codecademy.eventhub.index.UserEventIndex;
import com.codecademy.eventhub.index.UserEventIndexModule;
import com.codecademy.eventhub.list.DmaIdListModule;
import com.codecademy.eventhub.model.Event;
import com.codecademy.eventhub.model.User;
import com.codecademy.eventhub.storage.EventStorage;
import com.codecademy.eventhub.storage.EventStorageModule;
import com.codecademy.eventhub.storage.JournalEventStorage;
import com.codecademy.eventhub.storage.JournalUserStorage;
import com.codecademy.eventhub.storage.UserStorageModule;
import com.codecademy.eventhub.storage.UserStorage;
import com.codecademy.eventhub.storage.filter.ExactMatch;
import com.codecademy.eventhub.storage.filter.Filter;
import com.codecademy.eventhub.storage.filter.Regex;
import com.codecademy.eventhub.storage.filter.TrueFilter;
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

public class EventHubTest extends GuiceTestCase {
  @Test
  public void testSingleUser() throws Exception {
    Provider<EventHub> eventHubProvider = getEventHubProvider();
    EventHub eventHub = eventHubProvider.get();

    final String[] USER_IDS = { "10" };
    final String[] EVENT_TYPES = { "eventType1", "eventType2", "eventType3", "eventType4", "eventType5" };
    final String[] DATES = { "20130101", "20130102", "20130103", "20130104", "20130105" };

    addEvent(eventHub, EVENT_TYPES[0], USER_IDS[0], DATES[0], Maps.<String, String>newHashMap());
    addEvent(eventHub, EVENT_TYPES[1], USER_IDS[0], DATES[1], Maps.<String, String>newHashMap());
    addEvent(eventHub, EVENT_TYPES[2], USER_IDS[0], DATES[2], Maps.<String, String>newHashMap());
    addEvent(eventHub, EVENT_TYPES[3], USER_IDS[0], DATES[3], Maps.<String, String>newHashMap());
    addEvent(eventHub, EVENT_TYPES[4], USER_IDS[0], DATES[4], Maps.<String, String>newHashMap());

    final String[] funnelSteps = { EVENT_TYPES[1], EVENT_TYPES[2], EVENT_TYPES[3] };
    List<Filter> eventFilters = Lists.<Filter>newArrayList(TrueFilter.INSTANCE, TrueFilter.INSTANCE, TrueFilter.INSTANCE);
    Assert.assertArrayEquals(new int[] { 1 },
        eventHub.getFunnelCounts(DATES[0], DATES[4], new String[] { EVENT_TYPES[1] }, 7 /* numDaysToCompleteFunnel */,
            eventFilters, TrueFilter.INSTANCE));
    Assert.assertArrayEquals(new int[] { 1, 1, 1 },
        eventHub.getFunnelCounts(DATES[0], DATES[4], funnelSteps, 7 /* numDaysToCompleteFunnel */,
            eventFilters, TrueFilter.INSTANCE));
    Assert.assertArrayEquals(new int[] { 0, 0, 0 },
        eventHub.getFunnelCounts(DATES[0], DATES[1], funnelSteps, 7 /* numDaysToCompleteFunnel */,
            eventFilters, TrueFilter.INSTANCE));
    Assert.assertArrayEquals(new int[] { 1, 1, 1 },
        eventHub.getFunnelCounts(DATES[1], DATES[2], funnelSteps, 7 /* numDaysToCompleteFunnel */,
            eventFilters, TrueFilter.INSTANCE));
    Assert.assertArrayEquals(new int[] { 1, 0, 0 },
        eventHub.getFunnelCounts(DATES[0], DATES[4], funnelSteps, 1 /* numDaysToCompleteFunnel */,
            eventFilters, TrueFilter.INSTANCE));
    Assert.assertArrayEquals(new int[] { 1, 1, 1 },
        eventHub.getFunnelCounts(DATES[1], DATES[2], funnelSteps, 3 /* numDaysToCompleteFunnel */,
            eventFilters, TrueFilter.INSTANCE));
    Assert.assertArrayEquals(new int[] { 1, 1, 0 },
        eventHub.getFunnelCounts(DATES[1], DATES[2], funnelSteps, 2 /* numDaysToCompleteFunnel */,
            eventFilters, TrueFilter.INSTANCE));
    Assert.assertArrayEquals(new int[] { 1, 0, 0 },
        eventHub.getFunnelCounts(DATES[1], DATES[2], funnelSteps, 1 /* numDaysToCompleteFunnel */,
            eventFilters, TrueFilter.INSTANCE));
  }

  @Test
  public void testAll() throws Exception {
    Provider<EventHub> eventHubProvider = getEventHubProvider();
    EventHub eventHub = eventHubProvider.get();

    final String[] EVENT_TYPES = { "eventType1", "eventType2", "eventType3", "eventType4" };
    final String[] USER_IDS = { "10", "11", "12", "13", "14", "15", "16", "17", "18" };
    final String[] DATES = { "20130101", "20130102", "20130103", "20130104", "20130105" };

    eventHub.addOrUpdateUser(new User.Builder("10", Maps.<String, String>newHashMap()).build());
    eventHub.addOrUpdateUser(new User.Builder("11", ImmutableMap.of("hello", "world")).build());
    addEvent(eventHub, EVENT_TYPES[0], USER_IDS[0], DATES[0], Maps.<String, String>newHashMap());
    addEvent(eventHub, EVENT_TYPES[0], USER_IDS[1], DATES[0], Maps.<String, String>newHashMap());
    addEvent(eventHub, EVENT_TYPES[0], USER_IDS[3], DATES[0], Maps.<String, String>newHashMap());
    addEvent(eventHub, EVENT_TYPES[0], USER_IDS[6], DATES[0], Maps.<String, String>newHashMap());
    addEvent(eventHub, EVENT_TYPES[1], USER_IDS[6], DATES[0], Maps.<String, String>newHashMap());
    addEvent(eventHub, EVENT_TYPES[0], USER_IDS[7], DATES[0], Maps.<String, String>newHashMap());

    addEvent(eventHub, EVENT_TYPES[0], USER_IDS[0], DATES[1], Maps.<String, String>newHashMap());
    addEvent(eventHub, EVENT_TYPES[1], USER_IDS[1], DATES[1], Maps.<String, String>newHashMap());
    addEvent(eventHub, EVENT_TYPES[0], USER_IDS[2], DATES[1], Maps.<String, String>newHashMap());
    addEvent(eventHub, EVENT_TYPES[1], USER_IDS[3], DATES[1], Maps.<String, String>newHashMap());
    addEvent(eventHub, EVENT_TYPES[0], USER_IDS[5], DATES[1], Maps.<String, String>newHashMap());
    addEvent(eventHub, EVENT_TYPES[3], USER_IDS[6], DATES[1], Maps.<String, String>newHashMap());
    addEvent(eventHub, EVENT_TYPES[1], USER_IDS[7], DATES[1], Maps.<String, String>newHashMap());
    addEvent(eventHub, EVENT_TYPES[3], USER_IDS[7], DATES[1], Maps.<String, String>newHashMap());
    addEvent(eventHub, EVENT_TYPES[0], USER_IDS[8], DATES[1], Maps.<String, String>newHashMap());
    addEvent(eventHub, EVENT_TYPES[1], USER_IDS[8], DATES[1], Maps.<String, String>newHashMap());

    addEvent(eventHub, EVENT_TYPES[1], USER_IDS[0], DATES[2], Maps.<String, String>newHashMap());
    addEvent(eventHub, EVENT_TYPES[3], USER_IDS[1], DATES[2], Maps.<String, String>newHashMap());
    addEvent(eventHub, EVENT_TYPES[1], USER_IDS[2], DATES[2], Maps.<String, String>newHashMap());
    addEvent(eventHub, EVENT_TYPES[2], USER_IDS[3], DATES[2], Maps.<String, String>newHashMap());
    addEvent(eventHub, EVENT_TYPES[1], USER_IDS[4], DATES[2], Maps.<String, String>newHashMap());
    addEvent(eventHub, EVENT_TYPES[3], USER_IDS[5], DATES[2], Maps.<String, String>newHashMap());

    addEvent(eventHub, EVENT_TYPES[3], USER_IDS[0], DATES[3], Maps.<String, String>newHashMap());
    addEvent(eventHub, EVENT_TYPES[3], USER_IDS[2], DATES[3], Maps.<String, String>newHashMap());
    addEvent(eventHub, EVENT_TYPES[3], USER_IDS[3], DATES[3], Maps.<String, String>newHashMap());
    addEvent(eventHub, EVENT_TYPES[2], USER_IDS[4], DATES[3], Maps.<String, String>newHashMap());

    final String[] funnelSteps = { EVENT_TYPES[0], EVENT_TYPES[1], EVENT_TYPES[3] };
    List<Filter> eventFilters = Lists.<Filter>newArrayList(TrueFilter.INSTANCE, TrueFilter.INSTANCE, TrueFilter.INSTANCE);
    Assert.assertArrayEquals(new int[] { 8, 7, 6 },
        eventHub.getFunnelCounts(DATES[0], DATES[4], funnelSteps, 7 /* numDaysToCompleteFunnel */,
            eventFilters, TrueFilter.INSTANCE));
    Assert.assertArrayEquals(new int[] { 4, 3, 2 },
        eventHub.getFunnelCounts(DATES[1], DATES[4], funnelSteps, 7 /* numDaysToCompleteFunnel */,
            eventFilters, TrueFilter.INSTANCE));
    Assert.assertArrayEquals(new int[] { 4, 1, 0 },
        eventHub.getFunnelCounts(DATES[1], DATES[2], funnelSteps, 1 /* numDaysToCompleteFunnel */,
            eventFilters, TrueFilter.INSTANCE));
    Function<User, String> getExternalUserId = new Function<User, String>() {
      @Override
      public String apply(User user) {
        return user.getExternalId();
      }
    };
    Assert.assertEquals(Lists.newArrayList("10", "11", "13", "16", "17", "12", "15", "18", "14"),
        Lists.transform(eventHub.findUsers(TrueFilter.INSTANCE), getExternalUserId));
    Assert.assertEquals(Lists.newArrayList("11"),
        Lists.transform(eventHub.findUsers(new ExactMatch("hello", "world")), getExternalUserId));
    Assert.assertEquals(Lists.newArrayList("16"),
        Lists.transform(eventHub.findUsers(new ExactMatch("external_user_id", "16")), getExternalUserId));

    eventHub.close();
    eventHub = eventHubProvider.get();

    Assert.assertArrayEquals(new int[] { 8, 7, 6 },
        eventHub.getFunnelCounts(DATES[0], DATES[4], funnelSteps, 7 /* numDaysToCompleteFunnel */,
            eventFilters, TrueFilter.INSTANCE));
    Assert.assertArrayEquals(new int[] { 4, 3, 2 },
        eventHub.getFunnelCounts(DATES[1], DATES[4], funnelSteps, 7 /* numDaysToCompleteFunnel */,
            eventFilters, TrueFilter.INSTANCE));
    Assert.assertArrayEquals(new int[] { 4, 1, 0 },
        eventHub.getFunnelCounts(DATES[1], DATES[2], funnelSteps, 1 /* numDaysToCompleteFunnel */,
            eventFilters, TrueFilter.INSTANCE));
    Assert.assertEquals(Lists.newArrayList("10", "11", "13", "16", "17", "12", "15", "18", "14"),
        Lists.transform(eventHub.findUsers(TrueFilter.INSTANCE), getExternalUserId));
    Assert.assertEquals(Lists.newArrayList("11"),
        Lists.transform(eventHub.findUsers(new ExactMatch("hello", "world")), getExternalUserId));
    Assert.assertEquals(Lists.newArrayList("16"),
        Lists.transform(eventHub.findUsers(new ExactMatch("external_user_id", "16")), getExternalUserId));
  }

  @Test
  public void testConcurrentAddEvent() throws Exception {
    Injector injector = getInjector();

    final String directory = injector.getInstance(Key.get(String.class, Names.named("eventhub.directory")));
    final ShardedEventIndex shardedEventIndex = injector.getInstance(ShardedEventIndex.class);
    final DatedEventIndex datedEventIndex = injector.getInstance(DatedEventIndex.class);
    final PropertiesIndex propertiesIndex = injector.getInstance(PropertiesIndex.class);
    final UserEventIndex userEventIndex = injector.getInstance(UserEventIndex.class);
    final EventStorage eventStorage = injector.getInstance(JournalEventStorage.class);
    final UserStorage userStorage = injector.getInstance(JournalUserStorage.class);

    final EventHub eventHub = new EventHub(directory, shardedEventIndex, datedEventIndex,
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
            addEvent(eventHub, EVENT_TYPES[eventTypeIndex], EXTERNAL_USER_IDS[userIdIndex], DATES[dateIndex],
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
    Provider<EventHub> eventHubProvider = getEventHubProvider();
    EventHub eventHub = eventHubProvider.get();

    final String[] USER_IDS = { "10", "11" };
    final String[] EVENT_TYPES = { "eventType1", "eventType2", "eventType3", "eventType4", "eventType5" };
    final String[] DATES = { "20130101", "20130102", "20130103", "20130104", "20130105" };
    @SuppressWarnings("unchecked") final Map<String, String>[] properties = (Map<String, String>[]) new Map[] {
        ImmutableMap.<String, String>builder().put("foo1", "bar1").put("foo2", "bar2").build(),
        ImmutableMap.<String, String>builder().put("foo2", "bar2").put("foo3", "bar3").build(),
        ImmutableMap.<String, String>builder().put("foo3", "bar3").build()
    };

    eventHub.addOrUpdateUser(new User.Builder(USER_IDS[0], properties[0]).build());
    eventHub.addOrUpdateUser(new User.Builder(USER_IDS[1], properties[1]).build());
    addEvent(eventHub, EVENT_TYPES[0], USER_IDS[0], DATES[0], properties[0]);
    addEvent(eventHub, EVENT_TYPES[0], USER_IDS[1], DATES[0], properties[0]);
    addEvent(eventHub, EVENT_TYPES[1], USER_IDS[0], DATES[1], properties[0]);
    addEvent(eventHub, EVENT_TYPES[1], USER_IDS[1], DATES[1], properties[0]);
    addEvent(eventHub, EVENT_TYPES[2], USER_IDS[0], DATES[2], properties[1]);
    addEvent(eventHub, EVENT_TYPES[2], USER_IDS[1], DATES[2], properties[1]);
    addEvent(eventHub, EVENT_TYPES[3], USER_IDS[0], DATES[3], properties[1]);
    addEvent(eventHub, EVENT_TYPES[3], USER_IDS[1], DATES[3], properties[1]);
    addEvent(eventHub, EVENT_TYPES[4], USER_IDS[0], DATES[4], properties[2]);
    addEvent(eventHub, EVENT_TYPES[4], USER_IDS[1], DATES[4], properties[2]);

    final String[] funnelSteps = { EVENT_TYPES[1], EVENT_TYPES[2], EVENT_TYPES[4] };
    List<Filter> eventFilters = Lists.<Filter>newArrayList(TrueFilter.INSTANCE, TrueFilter.INSTANCE, TrueFilter.INSTANCE);
    Assert.assertArrayEquals(new int[] { 2, 2, 2 },
        eventHub.getFunnelCounts(DATES[0], DATES[4], funnelSteps, 7 /* numDaysToCompleteFunnel */,
            eventFilters, TrueFilter.INSTANCE));
    Assert.assertArrayEquals(new int[] {2, 0, 0},
        eventHub.getFunnelCounts(DATES[0], DATES[4], funnelSteps, 7 /* numDaysToCompleteFunnel */,
            Lists.<Filter>newArrayList(
                new ExactMatch("foo1", "bar1"),
                new Regex("foo4", Pattern.compile("b.*4")),
                TrueFilter.INSTANCE),
            TrueFilter.INSTANCE));
    Assert.assertArrayEquals(new int[] { 2, 2, 0 },
        eventHub.getFunnelCounts(DATES[0], DATES[4], funnelSteps, 7 /* numDaysToCompleteFunnel */,
            Lists.<Filter>newArrayList(
                new ExactMatch("foo2", "bar2"),
                new Regex("foo2", Pattern.compile("bar2")),
                new ExactMatch("foo2", "bar2")),
            TrueFilter.INSTANCE));
    Assert.assertArrayEquals(new int[] {2, 2, 2},
        eventHub.getFunnelCounts(DATES[0], DATES[4], funnelSteps, 7 /* numDaysToCompleteFunnel */,
            eventFilters, new ExactMatch("foo2", "bar2")));
    Assert.assertArrayEquals(new int[] { 1, 1, 1 },
        eventHub.getFunnelCounts(DATES[0], DATES[4], funnelSteps, 7 /* numDaysToCompleteFunnel */,
            eventFilters, new ExactMatch("foo3", "bar3")));
  }

  @Test
  public void testGetEventsByExternalUserId() throws Exception {
    Provider<EventHub> eventHubProvider = getEventHubProvider();
    EventHub eventHub = eventHubProvider.get();

    final String[] USER_IDS = { "10" };
    final String[] EVENT_TYPES = { "eventType1", "eventType2", "eventType3", "eventType4", "eventType5" };
    final String[] DATES = { "20130101", "20130102", "20130103", "20130104", "20130105" };

    addEvent(eventHub, EVENT_TYPES[0], USER_IDS[0], DATES[0], Maps.<String, String>newHashMap());
    addEvent(eventHub, EVENT_TYPES[1], USER_IDS[0], DATES[1], Maps.<String, String>newHashMap());
    addEvent(eventHub, EVENT_TYPES[2], USER_IDS[0], DATES[2], Maps.<String, String>newHashMap());
    addEvent(eventHub, EVENT_TYPES[3], USER_IDS[0], DATES[3], Maps.<String, String>newHashMap());
    addEvent(eventHub, EVENT_TYPES[4], USER_IDS[0], DATES[4], Maps.<String, String>newHashMap());

    List<Event> events = eventHub.getUserEvents(USER_IDS[0], 2, 10);
    for (int i = 0; i < events.size(); i++) {
      Assert.assertEquals(EVENT_TYPES[i + 2], events.get(i).getEventType());
      Assert.assertEquals(DATES[i + 2], events.get(i).getDate());
      Assert.assertEquals(USER_IDS[0], events.get(i).getExternalUserId());
    }

    events = eventHub.getUserEvents(USER_IDS[0], 2, 2);
    for (int i = 0; i < events.size(); i++) {
      Assert.assertEquals(EVENT_TYPES[i + 2], events.get(i).getEventType());
      Assert.assertEquals(DATES[i + 2], events.get(i).getDate());
      Assert.assertEquals(USER_IDS[0], events.get(i).getExternalUserId());
    }
  }

  @Test
  public void testGetRetentionTable() throws Exception {
    Provider<EventHub> eventHubProvider = getEventHubProvider();
    EventHub eventHub = eventHubProvider.get();

    final String[] USER_IDS = { "10", "11", "12", "13", "14" };
    final String[] EVENT_TYPES = { "receive_email", "purchase", "dummy" };
    final String[] DATES = { "20130101", "20130102", "20130103", "20130104", "20130105", "20130106",
        "20130107" };

    addEvent(eventHub, EVENT_TYPES[2], USER_IDS[0], DATES[0], Maps.<String, String>newHashMap());
    addEvent(eventHub, EVENT_TYPES[0], USER_IDS[0], DATES[0], ImmutableMap.of("foo1", "bar1"));

    addEvent(eventHub, EVENT_TYPES[0], USER_IDS[1], DATES[0], ImmutableMap.of("foo1", "bar1"));
    addEvent(eventHub, EVENT_TYPES[1], USER_IDS[1], DATES[0], ImmutableMap.of("foo2", "bar2"));
    addEvent(eventHub, EVENT_TYPES[1], USER_IDS[1], DATES[1], ImmutableMap.of("foo2", "bar2"));
    addEvent(eventHub, EVENT_TYPES[1], USER_IDS[1], DATES[1], ImmutableMap.of("foo2", "bar2"));
    addEvent(eventHub, EVENT_TYPES[0], USER_IDS[1], DATES[2], ImmutableMap.of("foo1", "bar1"));
    addEvent(eventHub, EVENT_TYPES[1], USER_IDS[1], DATES[2], ImmutableMap.of("foo2", "bar2"));
    addEvent(eventHub, EVENT_TYPES[1], USER_IDS[1], DATES[2], ImmutableMap.of("foo2", "bar2"));
    addEvent(eventHub, EVENT_TYPES[1], USER_IDS[1], DATES[2], ImmutableMap.of("foo2", "bar2"));
    addEvent(eventHub, EVENT_TYPES[1], USER_IDS[1], DATES[3], ImmutableMap.of("foo2", "bar2"));
    addEvent(eventHub, EVENT_TYPES[1], USER_IDS[1], DATES[3], ImmutableMap.of("foo2", "bar2"));

    addEvent(eventHub, EVENT_TYPES[2], USER_IDS[2], DATES[0], Maps.<String, String>newHashMap());
    addEvent(eventHub, EVENT_TYPES[0], USER_IDS[2], DATES[1], ImmutableMap.of("foo1", "bar1"));

    addEvent(eventHub, EVENT_TYPES[0], USER_IDS[3], DATES[2], ImmutableMap.of("foo1", "bar1"));
    addEvent(eventHub, EVENT_TYPES[1], USER_IDS[3], DATES[2], ImmutableMap.of("foo2", "bar2"));
    addEvent(eventHub, EVENT_TYPES[1], USER_IDS[3], DATES[3], ImmutableMap.of("foo2", "bar2"));
    addEvent(eventHub, EVENT_TYPES[1], USER_IDS[3], DATES[3], ImmutableMap.of("foo2", "bar2"));
    addEvent(eventHub, EVENT_TYPES[0], USER_IDS[3], DATES[4], ImmutableMap.of("foo1", "bar1"));
    addEvent(eventHub, EVENT_TYPES[1], USER_IDS[3], DATES[4], ImmutableMap.of("foo2", "bar2"));
    addEvent(eventHub, EVENT_TYPES[1], USER_IDS[3], DATES[4], ImmutableMap.of("foo2", "bar2"));
    addEvent(eventHub, EVENT_TYPES[1], USER_IDS[3], DATES[4], ImmutableMap.of("foo2", "bar2"));
    addEvent(eventHub, EVENT_TYPES[1], USER_IDS[3], DATES[5], ImmutableMap.of("foo2", "bar2"));
    addEvent(eventHub, EVENT_TYPES[1], USER_IDS[3], DATES[5], ImmutableMap.of("foo2", "bar2"));

    addEvent(eventHub, EVENT_TYPES[0], USER_IDS[4], DATES[6], ImmutableMap.of("foo1", "bar1"));
    addEvent(eventHub, EVENT_TYPES[1], USER_IDS[4], DATES[6], ImmutableMap.of("foo2", "bar2"));
    addEvent(eventHub, EVENT_TYPES[1], USER_IDS[4], DATES[6], ImmutableMap.of("foo2", "bar2"));
    addEvent(eventHub, EVENT_TYPES[1], USER_IDS[4], DATES[6], ImmutableMap.of("foo2", "bar2"));
    addEvent(eventHub, EVENT_TYPES[0], USER_IDS[4], DATES[6], ImmutableMap.of("foo1", "bar1"));
    addEvent(eventHub, EVENT_TYPES[1], USER_IDS[4], DATES[6], ImmutableMap.of("foo2", "bar2"));
    addEvent(eventHub, EVENT_TYPES[1], USER_IDS[4], DATES[6], ImmutableMap.of("foo2", "bar2"));
    addEvent(eventHub, EVENT_TYPES[1], USER_IDS[4], DATES[6], ImmutableMap.of("foo2", "bar2"));
    addEvent(eventHub, EVENT_TYPES[1], USER_IDS[4], DATES[6], ImmutableMap.of("foo2", "bar2"));
    addEvent(eventHub, EVENT_TYPES[1], USER_IDS[4], DATES[6], ImmutableMap.of("foo2", "bar2"));

    int[][] expectedCount = {
 /*D0,1*/ { 3 /*0,1,2*/, 1 /*1*/  , 1 /*1*/, 0 /**/ },
 /*D2,3*/ { 2 /*1,3*/  , 2 /*1,3*/, 1 /*3*/, 0 /**/  },
 /*D4,5*/ { 1 /*3*/    , 1 /*3*/  , 0 /**/ , 0 /**/ }
    };
    int[][] retentionTable = eventHub.getRetentionTable(
        "20130101", "20130106", 2, 3, EVENT_TYPES[0], EVENT_TYPES[1], TrueFilter.INSTANCE,
        TrueFilter.INSTANCE);
    for (int i = 0; i < expectedCount.length; i++) {
      for (int j = 0; j < expectedCount[i].length; j++) {
        Assert.assertEquals(expectedCount[i][j], retentionTable[i][j]);
      }
    }

    retentionTable = eventHub.getRetentionTable(
        "20130101", "20130106", 2, 3, EVENT_TYPES[0], EVENT_TYPES[1], new ExactMatch("foo1", "bar1"),
        TrueFilter.INSTANCE);
    for (int i = 0; i < expectedCount.length; i++) {
      for (int j = 0; j < expectedCount[i].length; j++) {
        Assert.assertEquals(expectedCount[i][j], retentionTable[i][j]);
      }
    }

    retentionTable = eventHub.getRetentionTable(
        "20130101", "20130106", 2, 3, EVENT_TYPES[0], EVENT_TYPES[1], TrueFilter.INSTANCE,
        new ExactMatch("foo2", "bar2"));
    for (int i = 0; i < expectedCount.length; i++) {
      for (int j = 0; j < expectedCount[i].length; j++) {
        Assert.assertEquals(expectedCount[i][j], retentionTable[i][j]);
      }
    }

    retentionTable = eventHub.getRetentionTable(
        "20130101", "20130106", 2, 3, EVENT_TYPES[0], EVENT_TYPES[1], new ExactMatch("foo1", "bar2"),
        TrueFilter.INSTANCE);
    for (int i = 0; i < expectedCount.length; i++) {
      for (int j = 0; j < expectedCount[i].length; j++) {
        Assert.assertEquals(0, retentionTable[i][j]);
      }
    }
  }

  private void addEvent(EventHub eventHub, String eventType, String externalUserId, String day,
      Map<String, String> property) {
    eventHub.addEvent(new Event.Builder(eventType, externalUserId, day, property).build());
  }

  private Provider<EventHub> getEventHubProvider() {
    return getInjector().getProvider(EventHub.class);
  }

  private Injector getInjector() {
    Properties prop = new Properties();
    prop.put("eventhub.directory", getTempDirectory());
    prop.put("eventhub.eventindex.initialNumEventIdsPerDay", "10");
    prop.put("eventhub.usereventindex.numPointersPerIndexEntry", "2");
    prop.put("eventhub.usereventindex.numIndexEntryPerFile", "2");
    prop.put("eventhub.usereventindex.indexEntryFileCacheSize", "2");
    prop.put("eventhub.usereventindex.numRecordsPerBlock", "2");
    prop.put("eventhub.usereventindex.numBlocksPerFile", "2");
    prop.put("eventhub.usereventindex.blockCacheSize", "2");
    prop.put("eventhub.journaleventstorage.numMetaDataPerFile", "10");
    prop.put("eventhub.journaleventstorage.metaDataFileCacheSize", "10");
    prop.put("eventhub.journaleventstorage.journalFileSize", "1024");
    prop.put("eventhub.journaleventstorage.journalWriteBatchSize", "1024");
    prop.put("eventhub.cachedeventstorage.recordCacheSize", "10");
    prop.put("eventhub.bloomfilteredeventstorage.bloomFilterSize", "64");
    prop.put("eventhub.bloomfilteredeventstorage.numHashes", "1");
    prop.put("eventhub.bloomfilteredeventstorage.numMetaDataPerFile", "10");
    prop.put("eventhub.bloomfilteredeventstorage.metaDataFileCacheSize", "10");
    prop.put("eventhub.journaluserstorage.numMetaDataPerFile", "10");
    prop.put("eventhub.journaluserstorage.metaDataFileCacheSize", "10");
    prop.put("eventhub.journaluserstorage.journalFileSize", "1024");
    prop.put("eventhub.journaluserstorage.journalWriteBatchSize", "1024");
    prop.put("eventhub.cacheduserstorage.recordCacheSize", "10");
    prop.put("eventhub.bloomfiltereduserstorage.numMetaDataPerFile", "10");
    prop.put("eventhub.bloomfiltereduserstorage.metaDataFileCacheSize", "10");
    prop.put("eventhub.bloomfiltereduserstorage.bloomFilterSize", "64");
    prop.put("eventhub.bloomfiltereduserstorage.numHashes", "1");

    return createInjectorFor(new Properties(),
        new EventHubModule(prop),
        new DmaIdListModule(),
        new ShardedEventIndexModule(),
        new DatedEventIndexModule(),
        new PropertiesIndexModule(),
        new UserEventIndexModule(),
        new EventStorageModule(),
        new UserStorageModule());
  }
}
