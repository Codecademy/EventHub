package com.mobicrave.eventtracker;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public class UserStorage {
  // TODO: 4B constraints
  private User[] users;
  private User.MetaData[] metaDatas;
  private final Map<String, Long> idMap;
  private AtomicLong numEvents;

  private UserStorage(User[] users, User.MetaData[] metaDatas,
      Map<String, Long> idMap, AtomicLong numEvents) {
    this.users = users;
    this.metaDatas = metaDatas;
    this.idMap = idMap;
    this.numEvents = numEvents;
  }

  public long addUser(User user) {
    int id = (int) numEvents.incrementAndGet();
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
    idMap.put(user.getExternalId(), new Long(id));
    metaDatas[id] = user.getMetaData();
    return id;
  }

  public long getId(String externalUserId) {
    return idMap.get(externalUserId);
  }

  public static UserStorage build() {
    return new UserStorage(new User[1024], new User.MetaData[1024],
        Maps.<String, Long>newConcurrentMap(), new AtomicLong(-1));
  }
}
