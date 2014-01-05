package com.mobicrave.eventtracker;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public class UserStorage {
  // TODO: 4B constraints
  private final ArrayList<User> users;
  private final ArrayList<User.MetaData> metaDatas;
  private final Map<String, Long> idMap;
  private AtomicLong numEvents;

  private UserStorage(ArrayList<User> users, ArrayList<User.MetaData> metaDatas,
      Map<String, Long> idMap, AtomicLong numEvents) {
    this.users = users;
    this.metaDatas = metaDatas;
    this.idMap = idMap;
    this.numEvents = numEvents;
  }

  public long addUser(User user) {
    long id = numEvents.incrementAndGet();
    users.add(user);
    idMap.put(user.getExternalId(), id);
    metaDatas.add(user.getMetaData());
    return id;
  }

  public long getId(String externalUserId) {
    return idMap.get(externalUserId);
  }

  public static UserStorage build() {
    return new UserStorage(Lists.<User>newArrayList(), Lists.<User.MetaData>newArrayList(),
        Maps.<String, Long>newHashMap(), new AtomicLong(-1));
  }
}
