package com.mobicrave.eventtracker;

import com.google.common.collect.Sets;
import com.mobicrave.eventtracker.index.EventIndex;
import com.mobicrave.eventtracker.index.UserEventIndex;
import com.mobicrave.eventtracker.list.IdList;
import com.mobicrave.eventtracker.list.SimpleIdList;
import com.mobicrave.eventtracker.model.Event;
import com.mobicrave.eventtracker.model.User;
import com.mobicrave.eventtracker.storage.EventStorage;
import com.mobicrave.eventtracker.storage.JournalEventStorage;
import com.mobicrave.eventtracker.storage.JournalUserStorage;
import com.mobicrave.eventtracker.storage.UserStorage;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;

// TODO: bloomfilter
// TODO: charting
// TODO: property statistics for segmentation
// TODO: query language
// TODO: more compact serialization format for Event & User
// TODO: 4B & 4G constraints
// TODO: need to add user before adding events
// TODO: DmaList still have 4G size constraint
// TODO: native byte order for performance
// TODO: poorly formatted user event index idlist filename
// TODO: UserEventIndex assumes userId and numRecords in sync
/**
 * The corresponding user has to be added before his/her event can be tracked
 */
public class EventTracker implements Closeable {
  private final String directory;
  private final EventIndex eventIndex;
  private final UserEventIndex userEventIndex;
  private final EventStorage eventStorage;
  private final UserStorage userStorage;

  public EventTracker(String directory, EventIndex eventIndex, UserEventIndex userEventIndex,
      EventStorage eventStorage, UserStorage userStorage) {
    this.directory = directory;
    this.eventIndex = eventIndex;
    this.userEventIndex = userEventIndex;
    this.eventStorage = eventStorage;
    this.userStorage = userStorage;
  }

  public int[] getCounts(String startDate, String endDate, String[] funnelStepsEventTypes,
      int numDaysToCompleteFunnel, List<Criterion> criteria) {
    IdList userIdList = SimpleIdList.build("", 10000);
    IdList firstStepEventIdList = SimpleIdList.build("", 10000);
    int[] funnelStepsEventTypeIds = getEventTypeIds(funnelStepsEventTypes);

    EventIndex.Callback aggregateUserIdsCallback = new AggregateUserIds(eventStorage, userStorage,
        userIdList, firstStepEventIdList, criteria, Sets.<Integer>newHashSet());
    eventIndex.enumerateEventIds(funnelStepsEventTypes[0], startDate, endDate,
        aggregateUserIdsCallback);
    int[] numFunnelStepsMatched = new int[funnelStepsEventTypes.length];
    IdList.Iterator userIdIterator = userIdList.iterator();
    IdList.Iterator firstStepEventIdIterator = firstStepEventIdList.iterator();
    while (userIdIterator.hasNext()) {
      int userId = (int) userIdIterator.next();
      long firstStepEventId = firstStepEventIdIterator.next();
      long maxLastStepEventId = eventIndex.findFirstEventIdOnDate(firstStepEventId, numDaysToCompleteFunnel);
      CountFunnelStepsMatched countFunnelStepsMatched = new CountFunnelStepsMatched(
          eventStorage, userStorage, funnelStepsEventTypeIds, 1 /* first step already matched*/,
          criteria);
      userEventIndex.enumerateEventIds(userId, firstStepEventId, maxLastStepEventId, countFunnelStepsMatched);
      for (int i = 0; i < countFunnelStepsMatched.getNumMatchedSteps(); i++) {
        numFunnelStepsMatched[i]++;
      }
    }
    return numFunnelStepsMatched;
  }

  public long addUser(User user) {
    int id = userStorage.getId(user.getExternalId());
    if (id != UserStorage.USER_NOT_FOUND) {
      return id;
    }
    int userId = userStorage.addUser(user);
    userEventIndex.addUser(userId);
    return userId;
  }

  public void addEventType(String eventType) {
    eventIndex.addEventType(eventType);
  }

  public long addEvent(Event event) {
    int userId = userStorage.getId(event.getExternalUserId());
    long eventId = eventStorage.addEvent(event, userId,
        eventIndex.getEventTypeId(event.getEventType()));
    eventIndex.addEvent(eventId, event.getEventType(), event.getDate());
    userEventIndex.addEvent(userId, eventId);
    return eventId;
  }

  private int[] getEventTypeIds(String[] eventTypes) {
    int[] eventTypeIds = new int[eventTypes.length];
    for (int i = 0; i < eventTypeIds.length; i++) {
      eventTypeIds[i] = eventIndex.getEventTypeId(eventTypes[i]);
    }
    return eventTypeIds;
  }

  @Override
  public void close() throws IOException {
    //noinspection ResultOfMethodCallIgnored
    new File(directory).mkdirs();
    eventStorage.close();
    userStorage.close();
    eventIndex.close();
    userEventIndex.close();
  }

  public static EventTracker build(String directory) {
    String eventIndexDirectory = directory + "/event_index/";
    String userEventIndexDirectory = directory + "/user_event_index/";
    String eventStorageDirectory = directory + "/event_storage/";
    String userStorageDirectory = directory + "/user_storage/";

    EventIndex eventIndex = EventIndex.build(eventIndexDirectory);
    UserEventIndex userEventIndex = UserEventIndex.build(userEventIndexDirectory);
    EventStorage eventStorage = JournalEventStorage.build(eventStorageDirectory);
    UserStorage userStorage = JournalUserStorage.build(userStorageDirectory);

    return new EventTracker(directory, eventIndex, userEventIndex, eventStorage, userStorage);
  }

  private static class AggregateUserIds implements EventIndex.Callback {
    private final EventStorage eventStorage;
    private final UserStorage userStorage;
    private final IdList userIdList;
    private final IdList earliestEventIdList;
    private final List<Criterion> criteria;
    private final Set<Integer> seenUserIdSet;

    public AggregateUserIds(EventStorage eventStorage, UserStorage userStorage, IdList userIdList,
        IdList earliestEventIdList, List<Criterion> criteria, Set<Integer> seenUserIdSet) {
      this.eventStorage = eventStorage;
      this.userStorage = userStorage;
      this.userIdList = userIdList;
      this.earliestEventIdList = earliestEventIdList;
      this.criteria = criteria;
      this.seenUserIdSet = seenUserIdSet;
    }

    @Override
    public void onEventId(long eventId) {
      if (!eventStorage.satisfy(eventId, criteria)) {
        return;
      }
      int userId = eventStorage.getUserId(eventId);
      if (!userStorage.satisfy(userId, criteria)) {
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
    private final List<Criterion> criteria;

    public CountFunnelStepsMatched(EventStorage eventStorage, UserStorage userStorage,
        int[] funnelStepsEventTypeIds, int numMatchedSteps, List<Criterion> criteria) {
      this.eventStorage = eventStorage;
      this.userStorage = userStorage;
      this.funnelStepsEventTypeIds = funnelStepsEventTypeIds;
      this.numMatchedSteps = numMatchedSteps;
      this.criteria = criteria;
    }

    @Override
    public boolean onEventId(long eventId) {
      if (!eventStorage.satisfy(eventId, criteria)) {
        return true;
      }
      int userId = eventStorage.getUserId(eventId);
      if (!userStorage.satisfy(userId, criteria)) {
        return true;
      }
      int eventTypeId = eventStorage.getEventTypeId(eventId);
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
