package com.mobicrave.eventtracker.storage;

import com.google.common.collect.Maps;
import com.mobicrave.eventtracker.Criterion;
import com.mobicrave.eventtracker.model.User;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Memory implementation doesn't support persistence nor can it store >4B events
 */
public class MemUserStorage implements UserStorage {
  private User[] users;
  private final Map<String, Integer> idMap;
  private AtomicInteger numUsers;

  private MemUserStorage(User[] users, Map<String, Integer> idMap, AtomicInteger numUsers) {
    this.users = users;
    this.idMap = idMap;
    this.numUsers = numUsers;
  }

  @Override
  public int addUser(User user) {
    int id = numUsers.incrementAndGet();
    if (id >= users.length) {
      synchronized (this) {
        if (id >= users.length) {
          User[] newUsers = new User[users.length * 2];
          System.arraycopy(users, 0, newUsers, 0, users.length);
          users = newUsers;
        }
      }
    }
    users[id] = user;
    idMap.put(user.getExternalId(), id);
    return id;
  }

  @Override
  public User getUser(int userId) {
    return users[userId];
  }

  @Override
  public int getId(String externalUserId) {
    Integer id = idMap.get(externalUserId);
    return id == null ? USER_NOT_FOUND : id;
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
  public void close() {
    throw new UnsupportedOperationException();
  }

  public static UserStorage build() {
    return new MemUserStorage(new User[1024], Maps.<String, Integer>newConcurrentMap(), new AtomicInteger(-1));
  }
}
