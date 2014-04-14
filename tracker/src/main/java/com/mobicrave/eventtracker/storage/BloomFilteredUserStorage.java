package com.mobicrave.eventtracker.storage;

import com.mobicrave.eventtracker.storage.filter.ExactMatch;
import com.mobicrave.eventtracker.base.BloomFilter;
import com.mobicrave.eventtracker.base.KeyValueCallback;
import com.mobicrave.eventtracker.list.DmaList;
import com.mobicrave.eventtracker.model.User;
import com.mobicrave.eventtracker.storage.filter.Regex;
import com.mobicrave.eventtracker.storage.visitor.DelayedVisitorProxy;
import com.mobicrave.eventtracker.storage.visitor.Visitor;

import javax.inject.Provider;
import java.io.IOException;

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
  public Visitor getFilterVisitor(final int userId) {
    return new DelayedVisitorProxy(new Provider<Visitor>() {
      @Override
      public Visitor get() {
        final BloomFilter bloomFilter = bloomFilterDmaList.get(userId);
        final Visitor visitorFromSuper = BloomFilteredUserStorage.super.getFilterVisitor(userId);
        numConditionCheck++;
        return new BloomFilteredFilterVisitor(bloomFilter, visitorFromSuper);
      }
    });
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

  private class BloomFilteredFilterVisitor implements Visitor {
    private final BloomFilter bloomFilter;
    private final Visitor visitor;

    public BloomFilteredFilterVisitor(BloomFilter bloomFilter, Visitor visitor) {
      this.bloomFilter = bloomFilter;
      this.visitor = visitor;
    }

    @Override
    public boolean visit(ExactMatch exactMatch) {
      String bloomFilterKey = getBloomFilterKey(exactMatch.getKey(), exactMatch.getValue());
      if (!bloomFilter.isPresent(bloomFilterKey)) {
        numBloomFilterRejection++;
        return false;
      }
      return visitor.visit(exactMatch);
    }

    @Override
    public boolean visit(Regex regex) {
      return visitor.visit(regex);
    }
  }
}
