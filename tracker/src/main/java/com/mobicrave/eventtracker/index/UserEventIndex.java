package com.mobicrave.eventtracker.index;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import com.mobicrave.eventtracker.list.DmaIdList;
import com.mobicrave.eventtracker.list.IdList;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;

public class UserEventIndex implements Closeable {
  private static final int NUMBER_FILES_PER_DIR = 1000;
  private final String directory;
  private LoadingCache<Integer, IdList> index;

  private UserEventIndex(String directory, LoadingCache<Integer, IdList> index) {
    this.directory = directory;
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
    //noinspection ResultOfMethodCallIgnored
    new File(directory).mkdirs();
    index.invalidateAll();
  }

  public String getVarz() {
    return String.format(
        "directory: %s\n" +
        "---------------\n" +
        "%s\n" +
        "---------------\n",
        directory, index.stats().toString());
  }

  private static String getIdListSerializationFile(String directory, long id) {
    StringBuilder filenameBuilder = new StringBuilder(directory);
    while (id >= NUMBER_FILES_PER_DIR) {
      filenameBuilder.append('/').append(String.format("%02d", id % NUMBER_FILES_PER_DIR));
      id /= NUMBER_FILES_PER_DIR;
    }
    return filenameBuilder.append('/').append(id).append(".ser").toString();
  }

  public static UserEventIndex build(final String directory) {
    LoadingCache<Integer, IdList> index = CacheBuilder.newBuilder()
        .maximumSize(5000)
        .recordStats()
        .removalListener(new RemovalListener<Integer, IdList>() {
          @Override
          public void onRemoval(RemovalNotification<Integer, IdList> notification) {
            try {
              //noinspection ConstantConditions
              notification.getValue().close();
            } catch (IOException e) {
              throw new RuntimeException(e);
            }
          }
        })
        .build(new CacheLoader<Integer, IdList>() {
          @Override
          public IdList load(Integer key) throws Exception {
            return DmaIdList.build(getIdListSerializationFile(directory, key), 100);
          }
        });
    return new UserEventIndex(directory, index);
  }

  public static interface Callback {
    // return shouldContinue
    public boolean shouldContinueOnEventId(long eventId);
  }
}
