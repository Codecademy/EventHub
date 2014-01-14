package com.mobicrave.eventtracker;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class UserEventIndex implements Closeable {
  private final String directory;
  private IdList[] index;
  private int numRecords;

  private UserEventIndex(String directory, IdList[] index, int numRecords) {
    this.directory = directory;
    this.index = index;
    this.numRecords = numRecords;
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

  public void addEvent(long userId, long eventId) {
    index[(int) userId].add(eventId);
  }

  public void addUser(long userId) {
    if (numRecords != userId) {
      throw new IllegalStateException("numRecords and userId do not match. Likely, users addition" +
          "are not synchronized properly");
    }
    if (userId == index.length) {
      IdList[] newIndex = new IdList[index.length * 2];
      System.arraycopy(index, 0, newIndex, 0, index.length);
      index = newIndex;
    }
    index[(int) userId] = MemIdList.build(getIdListSerializationFile(directory, userId), 128);
    numRecords++;
  }

  @Override
  public void close() throws IOException {
    new File(directory).mkdirs();
    try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(
        getSerializationFile(directory)))) {
      oos.writeInt(numRecords);
    }
    for (int i = 0; i < numRecords; i++) {
      index[i].close();
    }
  }

  private static String getSerializationFile(String directory) {
    return directory + "/user_event_index.ser";
  }

  private static String getIdListSerializationFile(String directory, long id) {
    return String.format("%s/%d/%d/%d/%d.ser", directory, id % 100, id / 100 % 100,
        id / 10000 % 100, id);
  }

  public static UserEventIndex build(String directory) {
    File file = new File(getSerializationFile(directory));
    if (file.exists()) {
      try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
        int numRecords =  ois.readInt();
        MemIdList[] index = new MemIdList[numRecords];
        for (int i = 0; i < numRecords; i++) {
          index[i] = MemIdList.build(getIdListSerializationFile(directory, i), 100);
        }
        return new UserEventIndex(directory, index, numRecords);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
    return new UserEventIndex(directory, new MemIdList[1024], 0);
  }

  public static interface Callback {
    public boolean onEventId(long eventId);
  }
}
