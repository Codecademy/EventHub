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

  public int getEventOffset(int userId, long eventId) {
    IdList idList = index.getUnchecked(userId);
    return idList.getStartOffset(eventId);
  }

  public void enumerateEventIds(int userId, int offset, int maxRecords,
      Callback callback) {
    IdList idList = index.getUnchecked(userId);
    IdList.Iterator eventIdIterator = idList.subList(offset, maxRecords);
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

  public String getVarz(int indentation) {
    String indent  = new String(new char[indentation]).replace('\0', ' ');
    return String.format(indent + "index: %s", index.stats().toString());
  }

  public static interface Callback {
    // return shouldContinue
    public boolean shouldContinueOnEventId(long eventId);
  }
}
