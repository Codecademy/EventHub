package com.mobicrave.eventtracker;

public class UserEventIndex {
  private IdList[] index;
  private int size;

  private UserEventIndex(IdList[] index) {
    this.index = index;
  }

  public void enumerateEventIds(long userId, long firstStepEventId, long maxLastEventId,
      Callback callback) {
    IdList.Iterator eventIdIterator = index[(int) userId].subList(firstStepEventId, maxLastEventId);
    while (eventIdIterator.hasNext()) {
      if (!callback.onEventId(eventIdIterator.next())) {
        return;
      }
    }
  }

  public void addEvent(long eventId, long userId) {
    // TODO: userId needs to be < 4B
    index[(int) userId].add(eventId);
  }

  public static UserEventIndex build() {
    return new UserEventIndex(new IdList[10]);
  }

  public void addUser(long userId) {
    // TODO: userId needs to be < 4B
    if (size == index.length) {
      synchronized (this) {
        if (size == index.length) {
          IdList[] newIndex = new IdList[index.length * 2];
          System.arraycopy(index, 0, newIndex, 0, index.length);
          index = newIndex;
        }
      }
    }
    index[(int) userId] = IdList.build();
    size++;
  }

  public static interface Callback {
    public boolean onEventId(long eventId);
  }
}
