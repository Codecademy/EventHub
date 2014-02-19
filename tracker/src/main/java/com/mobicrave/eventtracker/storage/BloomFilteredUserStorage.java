package com.mobicrave.eventtracker.storage;

import com.mobicrave.eventtracker.Criterion;
import com.mobicrave.eventtracker.base.BloomFilter;
import com.mobicrave.eventtracker.base.KeyValueCallback;
import com.mobicrave.eventtracker.list.DmaList;
import com.mobicrave.eventtracker.model.User;

import javax.inject.Provider;
import java.io.IOException;
import java.util.List;

public class BloomFilteredUserStorage implements UserStorage {
  private final UserStorage userStorage;
  private final DmaList<BloomFilter> bloomFilterDmaList;
  private final Provider<BloomFilter> bloomFilterProvider;
  private long numConditionCheck;
  private long numBloomFilterRejection;

  public BloomFilteredUserStorage(UserStorage userStorage,
      DmaList<BloomFilter> bloomFilterDmaList, Provider<BloomFilter> bloomFilterProvider) {
    this.userStorage = userStorage;
    this.bloomFilterDmaList = bloomFilterDmaList;
    this.bloomFilterProvider = bloomFilterProvider;
    this.numConditionCheck = 0;
    this.numBloomFilterRejection = 0;
  }

  @Override
  public int addUser(User user) {
    final BloomFilter bloomFilter = bloomFilterProvider.get();
    user.enumerate(new KeyValueCallback() {
      @Override
      public void callback(String key, String value) {
        bloomFilter.add(getBloomFilterKey(key, value));
      }
    });
    bloomFilterDmaList.add(bloomFilter);
    return userStorage.addUser(user);
  }

  @Override
  public int getId(String externalUserId) {
    return userStorage.getId(externalUserId);
  }

  @Override
  public User getUser(int userId) {
    return userStorage.getUser(userId);
  }

  @Override
  public boolean satisfy(int userId, List<Criterion> criteria) {
    if (criteria.isEmpty()) {
      return true;
    }
    numConditionCheck++;

    BloomFilter bloomFilter = bloomFilterDmaList.get(userId);
    for (Criterion criterion : criteria) {
      String bloomFilterKey = getBloomFilterKey(criterion.getKey(), criterion.getValue());
      if (!bloomFilter.isPresent(bloomFilterKey)) {
        numBloomFilterRejection++;
        return false;
      }
    }

    return userStorage.satisfy(userId, criteria);
  }

  @Override
  public String getVarz() {
    return String.format(
        "%s\n"+
        "num condition check: %d\n" +
        "num bloomfilter rejection: %d\n",
        userStorage.getVarz(), numConditionCheck, numBloomFilterRejection);
  }

  @Override
  public void close() throws IOException {
    bloomFilterDmaList.close();
    userStorage.close();
  }

  private static String getBloomFilterKey(String key, String value) {
    return key + value;
  }
}
