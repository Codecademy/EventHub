package com.mobicrave.eventtracker.storage;

import com.mobicrave.eventtracker.Filter;
import com.mobicrave.eventtracker.base.BloomFilter;
import com.mobicrave.eventtracker.base.KeyValueCallback;
import com.mobicrave.eventtracker.list.DmaList;
import com.mobicrave.eventtracker.model.Event;

import javax.inject.Provider;
import java.io.IOException;
import java.util.List;

public class BloomFilteredEventStorage extends DelegateEventStorage {
  private final DmaList<BloomFilter> bloomFilterDmaList;
  private final Provider<BloomFilter> bloomFilterProvider;
  private long numConditionCheck;
  private long numBloomFilterRejection;

  public BloomFilteredEventStorage(EventStorage eventStorage,
      DmaList<BloomFilter> bloomFilterDmaList, Provider<BloomFilter> bloomFilterProvider) {
    super(eventStorage);
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
    return super.addEvent(event, userId, eventTypeId);
  }

  @Override
  public boolean satisfy(long eventId, List<Filter> filters) {
    if (filters.isEmpty()) {
      return true;
    }
    numConditionCheck++;

    BloomFilter bloomFilter = bloomFilterDmaList.get(eventId);
    for (Filter filter : filters) {
      String bloomFilterKey = getBloomFilterKey(filter.getKey(), filter.getValue());
      if (!bloomFilter.isPresent(bloomFilterKey)) {
        numBloomFilterRejection++;
        return false;
      }
    }

    return super.satisfy(eventId, filters);
  }

  @Override
  public String getVarz(int indentation) {
    String indent  = new String(new char[indentation]).replace('\0', ' ');
    return String.format(
        "%s\n\n" +
        indent + this.getClass().getName() + "\n" +
        indent + "==================\n" +
        indent + "num condition check: %d\n" +
        indent + "num bloomfilter rejection: %d",
        super.getVarz(indentation), numConditionCheck, numBloomFilterRejection);
  }

  @Override
  public void close() throws IOException {
    bloomFilterDmaList.close();
    super.close();
  }

  private static String getBloomFilterKey(String key, String value) {
    return key + value;
  }
}
