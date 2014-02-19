package com.mobicrave.eventtracker.storage;

import com.google.common.cache.LoadingCache;
import com.mobicrave.eventtracker.Criterion;
import com.mobicrave.eventtracker.base.BloomFilter;
import com.mobicrave.eventtracker.base.KeyValueCallback;
import com.mobicrave.eventtracker.list.DmaList;
import com.mobicrave.eventtracker.model.User;
import org.fusesource.hawtjournal.api.Journal;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.BitSet;
import java.util.List;

public class JournalUserStorage implements UserStorage {
  private final int numHashes;
  private final int bloomFilterSize;
  private final Journal userJournal;
  private final LoadingCache<Integer, User> userCache;
  private DmaList<MetaData> metaDataList;
  private final IdMap idMap;
  private long numConditionCheck;
  private long numBloomFilterRejection;

  public JournalUserStorage(int numHashes, int bloomFilterSize,
      Journal userJournal, LoadingCache<Integer, User> userCache, DmaList<MetaData> metaDataList,
      IdMap idMap) {
    this.numHashes = numHashes;
    this.bloomFilterSize = bloomFilterSize;
    this.userJournal = userJournal;
    this.userCache = userCache;
    this.metaDataList = metaDataList;
    this.idMap = idMap;
    this.numConditionCheck = 0;
    this.numBloomFilterRejection = 0;
  }

  @Override
  public synchronized int addUser(User user) {
    try {
      int id = idMap.getAndIncrementCurrentId();
      byte[] location = JournalUtil.locationToBytes(userJournal.write(user.toByteBuffer(), true));
      final BloomFilter bloomFilter = BloomFilter.build(numHashes, bloomFilterSize);
      user.enumerate(new KeyValueCallback() {
        @Override
        public void callback(String key, String value) {
          bloomFilter.add(getBloomFilterKey(key, value));
        }
      });
      MetaData metaData = new MetaData(bloomFilter, location);
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
    numConditionCheck++;

    BloomFilter bloomFilter = metaDataList.get(userId).getBloomFilter();
    for (Criterion criterion : criteria) {
      String bloomFilterKey = getBloomFilterKey(criterion.getKey(), criterion.getValue());
      if (!bloomFilter.isPresent(bloomFilterKey)) {
        numBloomFilterRejection++;
        return false;
      }
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
        "num condition check: %d\n" +
        "num bloomfilter rejection: %d\n" +
        "userCache: %s\n" +
        "metaDataList: %s\n",
        idMap.getCurrentId(), numConditionCheck, numBloomFilterRejection,
        userCache.stats().toString(), metaDataList.getVarz());
  }

  private String getBloomFilterKey(String key, String value) {
    return key + value;
  }

  public static class MetaData {
    private final BloomFilter bloomFilter;
    private final byte[] location;

    public MetaData(BloomFilter bloomFilter, byte[] location) {
      this.bloomFilter = bloomFilter;
      this.location = location;
    }

    public BloomFilter getBloomFilter() {
      return bloomFilter;
    }

    public byte[] getLocation() {
      return location;
    }

    public static class Schema implements com.mobicrave.eventtracker.base.Schema<MetaData> {
      private static final int LOCATION_SIZE = 13; // in bytes
      private final int numHashes;
      private final int bloomFilterSize;

      public Schema(int numHashes, int bloomFilterSize) {
        this.numHashes = numHashes;
        this.bloomFilterSize = bloomFilterSize;
      }

      @Override
      public int getObjectSize() {
        return 8 /* userId + eventTypeId */ + LOCATION_SIZE + bloomFilterSize;
      }

      @Override
      public byte[] toBytes(MetaData metaData) {
        ByteBuffer byteBuffer = ByteBuffer.allocate(getObjectSize());
        byteBuffer.put(metaData.getBloomFilter().getBitSet().toByteArray())
            .put(metaData.location);
        return byteBuffer.array();
      }

      @Override
      public MetaData fromBytes(byte[] bytes) {
        ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
        byte[] bloomFilter = new byte[bloomFilterSize];
        byteBuffer.get(bloomFilter);
        byte[] location = new byte[LOCATION_SIZE];
        byteBuffer.get(location);
        return new MetaData(new BloomFilter(numHashes, BitSet.valueOf(bloomFilter)), location);
      }
    }
  }
}
