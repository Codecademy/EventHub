package com.mobicrave.eventtracker;

import com.google.common.collect.ArrayTable;
import com.google.common.collect.ContiguousSet;
import com.google.common.collect.DiscreteDomain;
import com.google.common.collect.Lists;
import com.google.common.collect.Range;
import com.google.common.collect.Sets;
import com.google.common.collect.Table;
import com.mobicrave.eventtracker.index.EventIndex;
import com.mobicrave.eventtracker.index.ShardedEventIndex;
import com.mobicrave.eventtracker.index.UserEventIndex;
import com.mobicrave.eventtracker.list.DummyIdList;
import com.mobicrave.eventtracker.list.IdList;
import com.mobicrave.eventtracker.list.MemIdList;
import com.mobicrave.eventtracker.model.Event;
import com.mobicrave.eventtracker.model.User;
import com.mobicrave.eventtracker.storage.EventStorage;
import com.mobicrave.eventtracker.storage.UserStorage;
import org.joda.time.DateTime;
import org.joda.time.Days;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Set;

// TODO: refactor varz (add indentation, print headings based on name of the instance variable, not class)
// TODO: User.toString and Event.toString
// TODO: have more debugging endpoint (print paginated events by event type, print paginated users)
// TODO: snapshot user properties to event properties
// TODO: finish README.md
// --------------- End of V1 Beta
// TODO: retention table add user criteria (a/b testing)
// TODO: optimize user storage for update
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
  private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormat.forPattern("yyyyMMdd");

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

  public int[][] getRetentionTable(String startDateString,
      String endDateString, int numDaysPerRow, int numColumns, String rowEventType,
      String columnEventType) {
    DateTime startDate = DATE_TIME_FORMATTER.parseDateTime(startDateString);
    DateTime endDate = DATE_TIME_FORMATTER.parseDateTime(endDateString);
    int numRows = (Days.daysBetween(startDate, endDate).getDays() + 1) / numDaysPerRow;

    List<Set<Integer>> rows = Lists.newArrayListWithCapacity(numRows);
    for (int i = 0; i < numRows; i++) {
      DateTime currentStartDate = startDate.plusDays(i * numDaysPerRow);
      DateTime currentEndDate = startDate.plusDays((i + 1) * numDaysPerRow);
      List<Integer> userIdsList = Lists.newArrayList();
      Set<Integer> userIdsSet = Sets.newHashSet();
      EventIndex.Callback aggregateUserIdsCallback = new AggregateUserIds(eventStorage, userStorage,
          new DummyIdList(), Collections.EMPTY_LIST, Collections.EMPTY_LIST, userIdsList, userIdsSet);
      shardedEventIndex.enumerateEventIds(
          rowEventType,
          currentStartDate.toString(DATE_TIME_FORMATTER),
          currentEndDate.toString(DATE_TIME_FORMATTER),
          aggregateUserIdsCallback);
      rows.add(userIdsSet);
    }
    List<Set<Integer>> columns = Lists.newArrayListWithCapacity(numColumns + numRows);
    for (int i = 0; i < numColumns + numRows; i++) {
      DateTime currentStartDate = startDate.plusDays(i * numDaysPerRow);
      DateTime currentEndDate = startDate.plusDays((i + 1) * numDaysPerRow);
      List<Integer> userIdsList = Lists.newArrayList();
      Set<Integer> userIdsSet = Sets.newHashSet();
      EventIndex.Callback aggregateUserIdsCallback = new AggregateUserIds(eventStorage, userStorage,
          new DummyIdList(), Collections.EMPTY_LIST, Collections.EMPTY_LIST, userIdsList, userIdsSet);
      shardedEventIndex.enumerateEventIds(
          columnEventType,
          currentStartDate.toString(DATE_TIME_FORMATTER),
          currentEndDate.toString(DATE_TIME_FORMATTER),
          aggregateUserIdsCallback);
      columns.add(userIdsSet);
    }

    Table<Integer, Integer, Integer> retentionTable = ArrayTable.create(
        ContiguousSet.create(Range.closedOpen(0, numRows), DiscreteDomain.integers()),
        ContiguousSet.create(Range.closedOpen(0, numColumns + 1), DiscreteDomain.integers()));
    retentionTable.put(0, 0, 0);
    for (int i = 0; i < numRows; i++) {
      retentionTable.put(i, 0, rows.get(i).size());
    }
    for (int i = 0; i < numRows; i++) {
      for (int j = 0; j < numColumns; j++) {
        Set<Integer> rowSet = rows.get(i);
        Set<Integer> columnSet = columns.get(j + i);
        int count = 0;
        for (Integer columnValue : columnSet) {
          if (rowSet.contains(columnValue)) {
            count++;
          }
        }
        retentionTable.put(i, j + 1, count);
      }
    }
    int[][] result = new int[numRows][numColumns + 1];
    for (int i = 0; i < numRows; i++) {
      for (int j = 0; j < numColumns + 1; j++) {
        result[i][j] = retentionTable.get(i, j);
      }
    }
    return result;
  }

  public synchronized int[] getFunnelCounts(String startDate, String endDate, String[] funnelStepsEventTypes,
      int numDaysToCompleteFunnel, List<Criterion> eventCriteria, List<Criterion> userCriteria) {
    IdList firstStepEventIdList = new MemIdList(new long[10000], 0);
    int[] funnelStepsEventTypeIds = getEventTypeIds(funnelStepsEventTypes);

    List<Integer> userIdsList = Lists.newArrayList();
    Set<Integer> userIdsSet = Sets.newHashSet();
    EventIndex.Callback aggregateUserIdsCallback = new AggregateUserIds(eventStorage, userStorage,
        firstStepEventIdList, eventCriteria, userCriteria, userIdsList, userIdsSet);
    shardedEventIndex.enumerateEventIds(funnelStepsEventTypes[0], startDate, endDate,
        aggregateUserIdsCallback);
    int[] numFunnelStepsMatched = new int[funnelStepsEventTypes.length];
    IdList.Iterator firstStepEventIdIterator = firstStepEventIdList.iterator();
    for (int userId : userIdsList) {
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

  public synchronized void aliasUser(String fromExternalUserId, String toExternalUserId) {
    int id = userStorage.getId(toExternalUserId);
    if (id == UserStorage.USER_NOT_FOUND) {
      throw new IllegalArgumentException(String .format("User: %s does not exist!!!", toExternalUserId));
    }
    userStorage.alias(fromExternalUserId, id);
  }

  public synchronized int addOrUpdateUser(User user) {
    userStorage.ensureUser(user.getExternalId());
    return userStorage.updateUser(user);
  }

  public User getUser(int userId) {
    return userStorage.getUser(userId);
  }

  public Event getEvent(long eventId) {
    return eventStorage.getEvent(eventId);
  }

  public synchronized long addEvent(Event event) {
    // ensure the given event type has an id associated
    int eventTypeId = shardedEventIndex.ensureEventType(event.getEventType());
    // ensure the given user has an id associated
    int userId = userStorage.ensureUser(event.getExternalUserId());

    long eventId = eventStorage.addEvent(event, userId, eventTypeId);
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
        "Event Storage:\n==============\n%s\n\n" +
        "User Storage:\n==============\n%s\n\n" +
        "Event Index:\n==============\n%s\n\n" +
        "User Event Index:\n==============\n%s",
        eventStorage.getVarz(1),
        userStorage.getVarz(1),
        shardedEventIndex.getVarz(1),
        userEventIndex.getVarz(1));
  }

  private static class AggregateUserIds implements EventIndex.Callback {
    private final EventStorage eventStorage;
    private final UserStorage userStorage;
    private final IdList earliestEventIdList;
    private final List<Criterion> eventCriteria;
    private final List<Criterion> userCriteria;
    private final List<Integer> seenUserIdList;
    private final Set<Integer> seenUserIdSet;

    public AggregateUserIds(EventStorage eventStorage, UserStorage userStorage,
        IdList earliestEventIdList, List<Criterion> eventCriteria, List<Criterion> userCriteria,
        List<Integer> seenUserIdList, Set<Integer> seenUserIdSet) {
      this.eventStorage = eventStorage;
      this.userStorage = userStorage;
      this.earliestEventIdList = earliestEventIdList;
      this.eventCriteria = eventCriteria;
      this.userCriteria = userCriteria;
      this.seenUserIdList = seenUserIdList;
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
        seenUserIdList.add(userId);
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
