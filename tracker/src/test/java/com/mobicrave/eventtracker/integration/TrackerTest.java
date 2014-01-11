package com.mobicrave.eventtracker.integration;

import com.google.common.collect.Maps;
import com.mobicrave.eventtracker.*;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class TrackerTest {
  private EventTracker tracker;

  @Before
  public void setUp() throws Exception {
    EventIndex eventIndexMap = EventIndex.build("");
    UserEventIndex userEventIndex = UserEventIndex.build("");
    EventStorage eventStorage = MemEventStorage.build();
    UserStorage userStorage = MemUserStorage.build();
    tracker = new EventTracker(eventIndexMap, userEventIndex, eventStorage, userStorage);
  }

  @Test
  public void testSingleUser() throws Exception {
    final String[] USER_IDS = { "10" };
    final String[] EVENT_TYPES = { "eventType1", "eventType2", "eventType3", "eventType4", "eventType5" };
    final String[] DATES = { "20130101", "20130102", "20130103", "20130104", "20130105" };

    for (String eventType : EVENT_TYPES) {
      tracker.addEventType(eventType);
    }
    addUser(tracker, USER_IDS[0]);
    addEvent(tracker, EVENT_TYPES[0], USER_IDS[0], DATES[0]);
    addEvent(tracker, EVENT_TYPES[1], USER_IDS[0], DATES[1]);
    addEvent(tracker, EVENT_TYPES[2], USER_IDS[0], DATES[2]);
    addEvent(tracker, EVENT_TYPES[3], USER_IDS[0], DATES[3]);
    addEvent(tracker, EVENT_TYPES[4], USER_IDS[0], DATES[4]);

    final String[] funnelSteps = { EVENT_TYPES[1], EVENT_TYPES[2], EVENT_TYPES[3] };
    Assert.assertArrayEquals(new int[] { 1, 1, 1 },
        tracker.getCounts(DATES[0], DATES[4], funnelSteps, 7 /* numDaysToCompleteFunnel */));
    Assert.assertArrayEquals(new int[] { 0, 0, 0 },
        tracker.getCounts(DATES[0], DATES[1], funnelSteps, 7 /* numDaysToCompleteFunnel */));
    Assert.assertArrayEquals(new int[] { 1, 1, 1 },
        tracker.getCounts(DATES[1], DATES[2], funnelSteps, 7 /* numDaysToCompleteFunnel */));
    Assert.assertArrayEquals(new int[] { 1, 0, 0 },
        tracker.getCounts(DATES[0], DATES[4], funnelSteps, 1 /* numDaysToCompleteFunnel */));
    Assert.assertArrayEquals(new int[] { 1, 1, 1 },
        tracker.getCounts(DATES[1], DATES[2], funnelSteps, 3 /* numDaysToCompleteFunnel */));
    Assert.assertArrayEquals(new int[] { 1, 1, 0 },
        tracker.getCounts(DATES[1], DATES[2], funnelSteps, 2 /* numDaysToCompleteFunnel */));
    Assert.assertArrayEquals(new int[] { 1, 0, 0 },
        tracker.getCounts(DATES[1], DATES[2], funnelSteps, 1 /* numDaysToCompleteFunnel */));
  }

  @Test
  public void testAll() throws Exception {
    // TODO: need to add user before adding events
    final String[] EVENT_TYPES = { "eventType1", "eventType2", "eventType3", "eventType4" };
    final String[] USER_IDS = { "10", "11", "12", "13", "14", "15", "16", "17", "18" };
    final String[] DATES = { "20130101", "20130102", "20130103", "20130104", "20130105" };

    for (String eventType : EVENT_TYPES) {
      tracker.addEventType(eventType);
    }
    for (String userId : USER_IDS) {
      addUser(tracker, userId);
    }
    addEvent(tracker, EVENT_TYPES[0], USER_IDS[0], DATES[0]);
    addEvent(tracker, EVENT_TYPES[0], USER_IDS[1], DATES[0]);
    addEvent(tracker, EVENT_TYPES[0], USER_IDS[3], DATES[0]);
    addEvent(tracker, EVENT_TYPES[0], USER_IDS[6], DATES[0]);
    addEvent(tracker, EVENT_TYPES[1], USER_IDS[6], DATES[0]);
    addEvent(tracker, EVENT_TYPES[0], USER_IDS[7], DATES[0]);

    addEvent(tracker, EVENT_TYPES[0], USER_IDS[0], DATES[1]);
    addEvent(tracker, EVENT_TYPES[1], USER_IDS[1], DATES[1]);
    addEvent(tracker, EVENT_TYPES[0], USER_IDS[2], DATES[1]);
    addEvent(tracker, EVENT_TYPES[1], USER_IDS[3], DATES[1]);
    addEvent(tracker, EVENT_TYPES[0], USER_IDS[5], DATES[1]);
    addEvent(tracker, EVENT_TYPES[3], USER_IDS[6], DATES[1]);
    addEvent(tracker, EVENT_TYPES[1], USER_IDS[7], DATES[1]);
    addEvent(tracker, EVENT_TYPES[3], USER_IDS[7], DATES[1]);
    addEvent(tracker, EVENT_TYPES[0], USER_IDS[8], DATES[1]);
    addEvent(tracker, EVENT_TYPES[1], USER_IDS[8], DATES[1]);

    addEvent(tracker, EVENT_TYPES[1], USER_IDS[0], DATES[2]);
    addEvent(tracker, EVENT_TYPES[3], USER_IDS[1], DATES[2]);
    addEvent(tracker, EVENT_TYPES[1], USER_IDS[2], DATES[2]);
    addEvent(tracker, EVENT_TYPES[2], USER_IDS[3], DATES[2]);
    addEvent(tracker, EVENT_TYPES[1], USER_IDS[4], DATES[2]);
    addEvent(tracker, EVENT_TYPES[3], USER_IDS[5], DATES[2]);

    addEvent(tracker, EVENT_TYPES[3], USER_IDS[0], DATES[3]);
    addEvent(tracker, EVENT_TYPES[3], USER_IDS[2], DATES[3]);
    addEvent(tracker, EVENT_TYPES[3], USER_IDS[3], DATES[3]);
    addEvent(tracker, EVENT_TYPES[2], USER_IDS[4], DATES[3]);

    final String[] funnelSteps = { EVENT_TYPES[0], EVENT_TYPES[1], EVENT_TYPES[3] };
    Assert.assertArrayEquals(new int[] { 8, 7, 6 },
        tracker.getCounts(DATES[0], DATES[4], funnelSteps, 7 /* numDaysToCompleteFunnel */));
    Assert.assertArrayEquals(new int[] { 4, 3, 2 },
        tracker.getCounts(DATES[1], DATES[4], funnelSteps, 7 /* numDaysToCompleteFunnel */));
    Assert.assertArrayEquals(new int[] { 4, 1, 0 },
        tracker.getCounts(DATES[1], DATES[2], funnelSteps, 1 /* numDaysToCompleteFunnel */));
  }


  @Test
  public void testFilter() throws Exception {
    // TODO test filtering
  }

  private void addUser(EventTracker tracker, String userId) {
    tracker.addUser(new User.Builder(userId, Maps.<String, String>newHashMap()).build());
  }

  private void addEvent(EventTracker tracker, String eventType, String externalUserId, String day) {
    tracker.addEvent(new Event.Builder(eventType, externalUserId, day,
        Maps.<String, String>newHashMap()).build());
  }
}
