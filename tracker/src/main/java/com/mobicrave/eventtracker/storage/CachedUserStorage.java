package com.mobicrave.eventtracker.storage;

import com.google.common.cache.Cache;
import com.mobicrave.eventtracker.model.User;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

public class CachedUserStorage extends DelegateUserStorage {
  private final Cache<Integer, User> userCache;

  public CachedUserStorage(UserStorage userStorage, Cache<Integer, User> userCache) {
    super(userStorage);
    this.userCache = userCache;
  }

  @Override
  public User getUser(final int userId) {
    try {
      return userCache.get(userId, new Callable<User>() {
        @Override
        public User call() {
          return CachedUserStorage.super.getUser(userId);
        }
      });
    } catch (ExecutionException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public String getVarz() {
    return String.format(
        "%s\nuserCache: %s\n",
        super.getVarz(), userCache.stats().toString());
  }
}
