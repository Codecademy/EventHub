package com.mobicrave.eventtracker.storage;

import com.mobicrave.eventtracker.Criterion;
import com.mobicrave.eventtracker.base.BloomFilter;
import com.mobicrave.eventtracker.base.KeyValueCallback;
import com.mobicrave.eventtracker.list.DmaList;
import com.mobicrave.eventtracker.model.User;

import javax.inject.Provider;
import java.io.IOException;
import java.util.List;

public class BloomFilteredUserStorage extends DelegateUserStorage {
  private final DmaList<BloomFilter> bloomFilterDmaList;
  private final Provider<BloomFilter> bloomFilterProvider;
  private long numConditionCheck;
  private long numBloomFilterRejection;

  public BloomFilteredUserStorage(UserStorage userStorage,
      DmaList<BloomFilter> bloomFilterDmaList, Provider<BloomFilter> bloomFilterProvider) {
    super(userStorage);
    this.bloomFilterDmaList = bloomFilterDmaList;
    this.bloomFilterProvider = bloomFilterProvider;
    this.numConditionCheck = 0;
    this.numBloomFilterRejection = 0;
  }

  @Override
  public int ensureUser(String externalUserId) {
    int id = getId(externalUserId);
    if (id != UserStorage.USER_NOT_FOUND) {
      return id;
    }
    id = super.ensureUser(externalUserId);
    final BloomFilter bloomFilter = bloomFilterProvider.get();
    bloomFilterDmaList.add(bloomFilter);
    return id;
  }

  @Override
  public int updateUser(User user) {
    int id = getId(user.getExternalId());
    final BloomFilter bloomFilter = bloomFilterProvider.get();
    user.enumerate(new KeyValueCallback() {
      @Override
      public void callback(String key, String value) {
        bloomFilter.add(getBloomFilterKey(key, value));
      }
    });
    bloomFilterDmaList.update(id, bloomFilter);
    return super.updateUser(user);
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

    return super.satisfy(userId, criteria);
  }

  @Override
  public String getVarz(int indentation) {
    String indent  = new String(new char[indentation]).replace('\0', ' ');
    return String.format(
        "%s\n\n"+
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
