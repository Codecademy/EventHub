package com.mobicrave.eventtracker.storage;

import com.google.common.cache.LoadingCache;
import com.mobicrave.eventtracker.Criterion;
import com.mobicrave.eventtracker.list.DmaList;
import com.mobicrave.eventtracker.model.User;
import org.fusesource.hawtjournal.api.Journal;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;

public class JournalUserStorage implements UserStorage {
  private final Journal userJournal;
  private final LoadingCache<Integer, User> userCache;
  private DmaList<MetaData> metaDataList;
  private final IdMap idMap;

  public JournalUserStorage(Journal userJournal, LoadingCache<Integer, User> userCache,
      DmaList<MetaData> metaDataList, IdMap idMap) {
    this.userJournal = userJournal;
    this.userCache = userCache;
    this.metaDataList = metaDataList;
    this.idMap = idMap;
  }

  @Override
  public synchronized int addUser(User user) {
    try {
      int id = idMap.getAndIncrementCurrentId();
      byte[] location = JournalUtil.locationToBytes(userJournal.write(user.toByteBuffer(), true));
      MetaData metaData = new MetaData(location);
      metaDataList.add(metaData);
      idMap.put(user.getExternalId(), id);
      return id;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public int getId(String externalUserId) {
    Integer id = idMap.get(externalUserId);
    return id == null ? USER_NOT_FOUND : id;
  }

  @Override
  public User getUser(int userId) {
    return userCache.getUnchecked(userId);
  }

  @Override
  public boolean satisfy(int userId, List<Criterion> criteria) {
    if (criteria.isEmpty()) {
      return true;
    }

    User user = getUser(userId);
    for (Criterion criterion : criteria) {
      if (!criterion.getValue().equals(user.get(criterion.getKey()))) {
        return false;
      }
    }
    return true;
  }

  @Override
  public void close() throws IOException {
    idMap.close();
    userJournal.close();
    metaDataList.close();
  }

  @Override
  public String getVarz() {
    return String.format(
        "current id: %d\n" +
        "userCache: %s\n" +
        "metaDataList: %s\n",
        idMap.getCurrentId(),
        userCache.stats().toString(),
        metaDataList.getVarz());
  }

  public static class MetaData {
    private final byte[] location;

    public MetaData(byte[] location) {
      this.location = location;
    }

    public byte[] getLocation() {
      return location;
    }

    public static class Schema implements com.mobicrave.eventtracker.base.Schema<MetaData> {
      private static final int LOCATION_SIZE = 13; // in bytes

      @Override
      public int getObjectSize() {
        return 8 /* userId + eventTypeId */ + LOCATION_SIZE;
      }

      @Override
      public byte[] toBytes(MetaData metaData) {
        ByteBuffer byteBuffer = ByteBuffer.allocate(getObjectSize());
        byteBuffer.put(metaData.location);
        return byteBuffer.array();
      }

      @Override
      public MetaData fromBytes(byte[] bytes) {
        ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
        byte[] location = new byte[LOCATION_SIZE];
        byteBuffer.get(location);
        return new MetaData(location);
      }
    }
  }
}
