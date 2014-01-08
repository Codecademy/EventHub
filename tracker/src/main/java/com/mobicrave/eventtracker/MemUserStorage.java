package com.mobicrave.eventtracker;

import com.google.common.collect.Maps;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class MemUserStorage implements UserStorage {
  private User[] users;
  private User.MetaData[] metaDatas;
  private final Map<String, Integer> idMap;
  private AtomicInteger numUsers;

  private MemUserStorage(User[] users, User.MetaData[] metaDatas,
      Map<String, Integer> idMap, AtomicInteger numUsers) {
    this.users = users;
    this.metaDatas = metaDatas;
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
          User.MetaData[] newMetaDatas = new User.MetaData[users.length * 2];
          System.arraycopy(metaDatas, 0, newMetaDatas, 0, metaDatas.length);
          metaDatas = newMetaDatas;
        }
      }
    }
    users[id] = user;
    idMap.put(user.getExternalId(), id);
    metaDatas[id] = user.getMetaData(user.getExternalId(), null);
    return id;
  }

  @Override
  public User.MetaData getUserMetaData(int userId) {
    return metaDatas[userId];
  }

  @Override
  public User getUser(int userId) {
    return users[userId];
  }

  @Override
  public int getId(String externalUserId) {
    return idMap.get(externalUserId);
  }

  @Override
  public void close() {}

  public static UserStorage build() {
    return new MemUserStorage(new User[1024], new User.MetaData[1024],
        Maps.<String, Integer>newConcurrentMap(), new AtomicInteger(-1));
  }
}
