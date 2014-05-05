package com.codecademy.eventhub.storage;

import com.google.common.cache.Cache;
import com.codecademy.eventhub.model.User;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

public class CachedUserStorage extends DelegateUserStorage {
  private final Cache<Integer, User> userCache;

  public CachedUserStorage(UserStorage userStorage, Cache<Integer, User> userCache) {
    super(userStorage);
    this.userCache = userCache;
  }

  @Override
  public int updateUser(User user) {
    int userId = super.updateUser(user);
    userCache.invalidate(userId);
    return userId;
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
  public String getVarz(int indentation) {
    String indent  = new String(new char[indentation]).replace('\0', ' ');
    return String.format(
        "%s\n\n" +
        indent + this.getClass().getName() + "\n" +
        indent + "==================\n" +
        indent + "userCache: %s",
        super.getVarz(indentation), userCache.stats().toString());
  }
}
