package com.mobicrave.eventtracker;

import com.google.common.collect.Sets;

import java.util.Set;

public class EventTracker {
  private final EventIndex eventIndex;
  private final UserEventIndex userEventIndex;
  private final EventStorage eventStorage;
  private final UserStorage userStorage;

  public EventTracker(EventIndex eventIndex, UserEventIndex userEventIndex,
      EventStorage eventStorage, UserStorage userStorage) {
    this.eventIndex = eventIndex;
    this.userEventIndex = userEventIndex;
    this.eventStorage = eventStorage;
    this.userStorage = userStorage;
  }

  public int[] getCounts(String startDate, String endDate, String[] funnelStepsEventTypes,
      int numDaysToCompleteFunnel) {
    IdList userIdList = IdList.build();
    IdList firstStepEventIdList = IdList.build();
    int[] funnelStepsEventTypeIds = getEventTypeIds(funnelStepsEventTypes);

    EventIndex.Callback aggregateUserIdsCallback = new AggregateUserIds(eventStorage, userStorage,
        userIdList, firstStepEventIdList, Sets.<Long>newHashSet());
    eventIndex.enumerateEventIds(funnelStepsEventTypes[0], startDate, endDate,
        aggregateUserIdsCallback);
    int[] numFunnelStepsMatched = new int[funnelStepsEventTypes.length];
    IdList.Iterator userIdIterator = userIdList.iterator();
    IdList.Iterator firstStepEventIdIterator = firstStepEventIdList.iterator();
    while (userIdIterator.hasNext()) {
      long userId = userIdIterator.next();
      long firstStepEventId = firstStepEventIdIterator.next();
      long maxLastStepEventId = eventIndex.findFirstEventIdOnDate(firstStepEventId, numDaysToCompleteFunnel);
      CountFunnelStepsMatched countFunnelStepsMatched = new CountFunnelStepsMatched(
          eventStorage, funnelStepsEventTypeIds, 1 /* first step already matched*/);
      userEventIndex.enumerateEventIds(userId, firstStepEventId, maxLastStepEventId, countFunnelStepsMatched);
      for (int i = 0; i < countFunnelStepsMatched.getNumMatchedSteps(); i++) {
        numFunnelStepsMatched[i]++;
      }
    }
    return numFunnelStepsMatched;
  }

  public long addUser(User user) {
    long userId = userStorage.addUser(user);
    userEventIndex.addUser(userId);
    return userId;
  }

  public int addEventType(String eventType) {
    return eventIndex.add(eventType);
  }

  public long addEvent(Event event) {
    long userId = userStorage.getId(event.getExternalUserId());
    long eventId = eventStorage.addEvent(event, userId,
        eventIndex.getEventTypeId(event.getEventType()));
    eventIndex.addEvent(eventId, event.getEventType(), event.getDate());
    userEventIndex.addEvent(eventId, userId);
    return eventId;
  }

  private int[] getEventTypeIds(String[] eventTypes) {
    int[] eventTypeIds = new int[eventTypes.length];
    for (int i = 0; i < eventTypeIds.length; i++) {
      eventTypeIds[i] = eventIndex.getEventTypeId(eventTypes[i]);
    }
    return eventTypeIds;
  }

  private static class AggregateUserIds implements EventIndex.Callback {
    private final EventStorage eventStorage;
    private final UserStorage userStorage;
    private final IdList userIdList;
    private final IdList earliestEventIdList;
    private final Set<Long> seenUserIdSet;

    public AggregateUserIds(EventStorage eventStorage, UserStorage userStorage,IdList userIdList,
        IdList earliestEventIdList, Set<Long> seenUserIdSet) {
      this.eventStorage = eventStorage;
      this.userStorage = userStorage;
      this.userIdList = userIdList;
      this.earliestEventIdList = earliestEventIdList;
      this.seenUserIdSet = seenUserIdSet;
    }

    @Override
    public void onEventId(long eventId) {
      Event.MetaData eventMetaData = eventStorage.getEventMetaData(eventId);
      // TODO: filter user by event filter & user filter
      long userId = eventMetaData.getUserId();
      // TODO: consider other higher performing Set implementation
      if (!seenUserIdSet.contains(userId)) {
        seenUserIdSet.add(userId);
        userIdList.add(userId);
        earliestEventIdList.add(eventId);
      }
    }
  }

  private static class CountFunnelStepsMatched implements UserEventIndex.Callback {
    private final EventStorage eventStorage;
    private final int[] funnelStepsEventTypeIds;
    private int numMatchedSteps;

    public CountFunnelStepsMatched(EventStorage eventStorage,
        int[] funnelStepsEventTypeIds, int numMatchedSteps) {
      this.eventStorage = eventStorage;
      this.funnelStepsEventTypeIds = funnelStepsEventTypeIds;
      this.numMatchedSteps = numMatchedSteps;
    }

    @Override
    public boolean onEventId(long eventId) {
      int eventTypeId = eventStorage.getEventMetaData(eventId).getEventTypeId();
      if (eventTypeId == funnelStepsEventTypeIds[numMatchedSteps]) {
        numMatchedSteps++;
      }
      return numMatchedSteps != funnelStepsEventTypeIds.length;
    }

    public int getNumMatchedSteps() {
      return numMatchedSteps;
    }
  }
}
