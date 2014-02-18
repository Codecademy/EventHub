package com.mobicrave.eventtracker.index;

import com.google.common.cache.LoadingCache;
import com.mobicrave.eventtracker.list.IdList;

import java.io.Closeable;
import java.io.IOException;

public class UserEventIndex implements Closeable {
  private LoadingCache<Integer, IdList> index;

  public UserEventIndex(LoadingCache<Integer, IdList> index) {
    this.index = index;
  }

  public void enumerateEventIds(int userId, long firstStepEventId, long maxLastEventId,
      Callback callback) {
    IdList.Iterator eventIdIterator = index.getUnchecked(userId).subList(firstStepEventId, maxLastEventId);
    while (eventIdIterator.hasNext()) {
      if (!callback.shouldContinueOnEventId(eventIdIterator.next())) {
        return;
      }
    }
  }

  public void enumerateEventIdsByOffset(int userId, int offset, int numRecords, Callback callback) {
    IdList.Iterator eventIdIterator = index.getUnchecked(userId).subListByOffset(offset, numRecords);
    while (eventIdIterator.hasNext()) {
      if (!callback.shouldContinueOnEventId(eventIdIterator.next())) {
        return;
      }
    }
  }

  public void addEvent(int userId, long eventId) {
    index.getUnchecked(userId).add(eventId);
  }

  @Override
  public void close() throws IOException {
    index.invalidateAll();
  }

  public String getVarz() {
    return String.format("index: %s\n", index.stats().toString());
  }

  public static interface Callback {
    // return shouldContinue
    public boolean shouldContinueOnEventId(long eventId);
  }
}
