package com.mobicrave.eventtracker.storage;

import com.mobicrave.eventtracker.Criterion;
import com.mobicrave.eventtracker.base.BloomFilter;
import com.mobicrave.eventtracker.base.KeyValueCallback;
import com.mobicrave.eventtracker.list.DmaList;
import com.mobicrave.eventtracker.model.Event;

import javax.inject.Provider;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.BitSet;
import java.util.List;

public class BloomFilteredEventStorage implements EventStorage {
  private final EventStorage eventStorage;
  private final DmaList<BloomFilter> bloomFilterDmaList;
  private final Provider<BloomFilter> bloomFilterProvider;
  private long numConditionCheck;
  private long numBloomFilterRejection;

  public BloomFilteredEventStorage(EventStorage eventStorage,
      DmaList<BloomFilter> bloomFilterDmaList, Provider<BloomFilter> bloomFilterProvider) {
    this.eventStorage = eventStorage;
    this.bloomFilterDmaList = bloomFilterDmaList;
    this.bloomFilterProvider = bloomFilterProvider;
    this.numConditionCheck = 0;
    this.numBloomFilterRejection = 0;
  }

  @Override
  public long addEvent(Event event, int userId, int eventTypeId) {
    final BloomFilter bloomFilter = bloomFilterProvider.get();
    event.enumerate(new KeyValueCallback() {
      @Override
      public void callback(String key, String value) {
        bloomFilter.add(getBloomFilterKey(key, value));
      }
    });
    bloomFilterDmaList.add(bloomFilter);
    return eventStorage.addEvent(event, userId, eventTypeId);
  }

  @Override
  public Event getEvent(long eventId) {
    return eventStorage.getEvent(eventId);
  }

  @Override
  public int getUserId(long eventId) {
    return eventStorage.getUserId(eventId);
  }

  @Override
  public int getEventTypeId(long eventId) {
    return eventStorage.getEventTypeId(eventId);
  }

  @Override
  public boolean satisfy(long eventId, List<Criterion> criteria) {
    if (criteria.isEmpty()) {
      return true;
    }
    numConditionCheck++;

    BloomFilter bloomFilter = bloomFilterDmaList.get(eventId);
    for (Criterion criterion : criteria) {
      String bloomFilterKey = getBloomFilterKey(criterion.getKey(), criterion.getValue());
      if (!bloomFilter.isPresent(bloomFilterKey)) {
        numBloomFilterRejection++;
        return false;
      }
    }

    return eventStorage.satisfy(eventId, criteria);
  }

  @Override
  public String getVarz() {
    return String.format(
        "%s\n"+
        "num condition check: %d\n" +
        "num bloomfilter rejection: %d\n",
        eventStorage.getVarz(), numConditionCheck, numBloomFilterRejection);
  }

  @Override
  public void close() throws IOException {
    bloomFilterDmaList.close();
    eventStorage.close();
  }

  private static String getBloomFilterKey(String key, String value) {
    return key + value;
  }

  public static class Schema implements com.mobicrave.eventtracker.base.Schema<BloomFilter> {
    private final int numHashes;
    private final int bloomFilterSize;

    public Schema(int numHashes, int bloomFilterSize) {
      this.numHashes = numHashes;
      this.bloomFilterSize = bloomFilterSize;
    }

    @Override
    public int getObjectSize() {
      return bloomFilterSize;
    }

    @Override
    public byte[] toBytes(BloomFilter bloomFilter) {
      ByteBuffer byteBuffer = ByteBuffer.allocate(getObjectSize());
      byteBuffer.put(bloomFilter.getBitSet().toByteArray());
      return byteBuffer.array();
    }

    @Override
    public BloomFilter fromBytes(byte[] bytes) {
      ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
      byte[] bloomFilter = new byte[bloomFilterSize];
      byteBuffer.get(bloomFilter);
      return new BloomFilter(numHashes, BitSet.valueOf(bloomFilter));
    }
  }
}
