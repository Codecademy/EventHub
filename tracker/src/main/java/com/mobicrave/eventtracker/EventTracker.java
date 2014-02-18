package com.mobicrave.eventtracker;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.mobicrave.eventtracker.index.EventIndex;
import com.mobicrave.eventtracker.index.ShardedEventIndex;
import com.mobicrave.eventtracker.index.UserEventIndex;
import com.mobicrave.eventtracker.list.IdList;
import com.mobicrave.eventtracker.list.MemIdList;
import com.mobicrave.eventtracker.model.Event;
import com.mobicrave.eventtracker.model.User;
import com.mobicrave.eventtracker.storage.EventStorage;
import com.mobicrave.eventtracker.storage.UserStorage;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;

// TODO: refactor ShardedEventIndex (EventTypeManager and DateManager)
// TODO: refactor JournalEventStorage and JournalUserStorage (separate bloomfilter, metadata, and core storage)
// TODO: testAddConcurrentUsers

// TODO: support identify and alias
// TODO: frontend integration
// TODO: finish README.md
// --------------- End of V1 Beta
// TODO: jersey integration
// TODO: property statistics for segmentation
// TODO: consider column oriented storage
// TODO: separate cache for previously computed result? same binary or redis?
// TODO: query language
// TODO: move synchronization responsibility to low level
// TODO: compression of DmaIdList
// TODO: native byte order for performance
/**
 * The corresponding user has to be added before his/her event can be tracked.
 * The date of the receiving events have to be monotonically increasing
 */
public class EventTracker implements Closeable {
  private final String directory;
  private final ShardedEventIndex shardedEventIndex;
  private final UserEventIndex userEventIndex;
  private final EventStorage eventStorage;
  private final UserStorage userStorage;

  public EventTracker(String directory, ShardedEventIndex shardedEventIndex,
      UserEventIndex userEventIndex, EventStorage eventStorage, UserStorage userStorage) {
    this.directory = directory;
    this.shardedEventIndex = shardedEventIndex;
    this.userEventIndex = userEventIndex;
    this.eventStorage = eventStorage;
    this.userStorage = userStorage;
  }

  public List<Event> getEventsByExternalUserId(String externalUserId, int kthPage, int numPerPage) {
    List<Event> events = Lists.newArrayList();
    CollectEventCallback callback = new CollectEventCallback(events, eventStorage);
    userEventIndex.enumerateEventIdsByOffset(userStorage.getId(externalUserId),
        kthPage * numPerPage, numPerPage, callback);
    return events;
  }

  public int[] getCounts(String startDate, String endDate, String[] funnelStepsEventTypes,
      int numDaysToCompleteFunnel, List<Criterion> eventCriteria, List<Criterion> userCriteria) {
    IdList userIdList = new MemIdList(new long[10000], 0);
    IdList firstStepEventIdList = new MemIdList(new long[10000], 0);
    int[] funnelStepsEventTypeIds = getEventTypeIds(funnelStepsEventTypes);

    EventIndex.Callback aggregateUserIdsCallback = new AggregateUserIds(eventStorage, userStorage,
        userIdList, firstStepEventIdList, eventCriteria, userCriteria, Sets.<Integer>newHashSet());
    shardedEventIndex.enumerateEventIds(funnelStepsEventTypes[0], startDate, endDate,
        aggregateUserIdsCallback);
    int[] numFunnelStepsMatched = new int[funnelStepsEventTypes.length];
    IdList.Iterator userIdIterator = userIdList.iterator();
    IdList.Iterator firstStepEventIdIterator = firstStepEventIdList.iterator();
    while (userIdIterator.hasNext()) {
      int userId = (int) userIdIterator.next();
      long firstStepEventId = firstStepEventIdIterator.next();
      long maxLastStepEventId = shardedEventIndex.findFirstEventIdOnDate(firstStepEventId, numDaysToCompleteFunnel);
      CountFunnelStepsMatched countFunnelStepsMatched = new CountFunnelStepsMatched(
          eventStorage, userStorage, funnelStepsEventTypeIds, 1 /* first step already matched*/,
          eventCriteria, userCriteria);
      userEventIndex.enumerateEventIds(userId, firstStepEventId, maxLastStepEventId, countFunnelStepsMatched);
      for (int i = 0; i < countFunnelStepsMatched.getNumMatchedSteps(); i++) {
        numFunnelStepsMatched[i]++;
      }
    }
    return numFunnelStepsMatched;
  }

  public synchronized long addUser(User user) {
    int id = userStorage.getId(user.getExternalId());
    if (id != UserStorage.USER_NOT_FOUND) {
      return id;
    }
    return userStorage.addUser(user);
  }

  public synchronized void addEventType(String eventType) {
    shardedEventIndex.addEventType(eventType);
  }

  public synchronized long addEvent(Event event) {
    int userId = userStorage.getId(event.getExternalUserId());
    long eventId = eventStorage.addEvent(event, userId,
        shardedEventIndex.getEventTypeId(event.getEventType()));
    shardedEventIndex.addEvent(eventId, event.getEventType(), event.getDate());
    userEventIndex.addEvent(userId, eventId);
    return eventId;
  }

  public List<String> getEventTypes() {
    return shardedEventIndex.getEventTypes();
  }

  private int[] getEventTypeIds(String[] eventTypes) {
    int[] eventTypeIds = new int[eventTypes.length];
    for (int i = 0; i < eventTypeIds.length; i++) {
      eventTypeIds[i] = shardedEventIndex.getEventTypeId(eventTypes[i]);
    }
    return eventTypeIds;
  }

  @Override
  public void close() throws IOException {
    //noinspection ResultOfMethodCallIgnored
    new File(directory).mkdirs();
    eventStorage.close();
    userStorage.close();
    shardedEventIndex.close();
    userEventIndex.close();
  }

  public String getVarz() {
    return String.format(
        "Event Storage:\n=========\n%s\n" +
        "User Storage:\n=========\n%s\n" +
        "Event Index:\n=========\n%s\n" +
        "User Event Index:\n=========\n%s\n",
        eventStorage.getVarz(),
        userStorage.getVarz(),
        shardedEventIndex.getVarz(),
        userEventIndex.getVarz());
  }

  private static class AggregateUserIds implements EventIndex.Callback {
    private final EventStorage eventStorage;
    private final UserStorage userStorage;
    private final IdList userIdList;
    private final IdList earliestEventIdList;
    private final List<Criterion> eventCriteria;
    private final List<Criterion> userCriteria;
    private final Set<Integer> seenUserIdSet;

    public AggregateUserIds(EventStorage eventStorage, UserStorage userStorage, IdList userIdList,
        IdList earliestEventIdList, List<Criterion> eventCriteria, List<Criterion> userCriteria,
        Set<Integer> seenUserIdSet) {
      this.eventStorage = eventStorage;
      this.userStorage = userStorage;
      this.userIdList = userIdList;
      this.earliestEventIdList = earliestEventIdList;
      this.eventCriteria = eventCriteria;
      this.userCriteria = userCriteria;
      this.seenUserIdSet = seenUserIdSet;
    }

    @Override
    public void onEventId(long eventId) {
      if (seenUserIdSet.contains(eventStorage.getUserId(eventId))) {
        return;
      }
      if (!eventStorage.satisfy(eventId, eventCriteria)) {
        return;
      }
      int userId = eventStorage.getUserId(eventId);
      if (!userStorage.satisfy(userId, userCriteria)) {
        return;
      }
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
    private final UserStorage userStorage;
    private final int[] funnelStepsEventTypeIds;
    private int numMatchedSteps;
    private final List<Criterion> eventCriteria;
    private final List<Criterion> userCriteria;

    public CountFunnelStepsMatched(EventStorage eventStorage, UserStorage userStorage,
        int[] funnelStepsEventTypeIds, int numMatchedSteps, List<Criterion> eventCriteria,
        List<Criterion> userCriteria) {
      this.eventStorage = eventStorage;
      this.userStorage = userStorage;
      this.funnelStepsEventTypeIds = funnelStepsEventTypeIds;
      this.numMatchedSteps = numMatchedSteps;
      this.eventCriteria = eventCriteria;
      this.userCriteria = userCriteria;
    }

    @Override
    public boolean shouldContinueOnEventId(long eventId) {
      int eventTypeId = eventStorage.getEventTypeId(eventId);
      if (eventTypeId != funnelStepsEventTypeIds[numMatchedSteps]) {
        return true;
      }

      if (!eventStorage.satisfy(eventId, eventCriteria)) {
        return true;
      }
      int userId = eventStorage.getUserId(eventId);
      if (!userStorage.satisfy(userId, userCriteria)) {
        return true;
      }
      numMatchedSteps++;
      return numMatchedSteps != funnelStepsEventTypeIds.length;
    }

    public int getNumMatchedSteps() {
      return numMatchedSteps;
    }
  }

  private static class CollectEventCallback implements UserEventIndex.Callback, EventIndex.Callback {
    private final List<Event> events;
    private final EventStorage eventStorage;

    private CollectEventCallback(List<Event> events, EventStorage eventStorage) {
      this.events = events;
      this.eventStorage = eventStorage;
    }

    @Override
    public boolean shouldContinueOnEventId(long eventId) {
      events.add(eventStorage.getEvent(eventId));
      return true;
    }

    @Override
    public void onEventId(long eventId) {
      events.add(eventStorage.getEvent(eventId));
    }
  }
}
