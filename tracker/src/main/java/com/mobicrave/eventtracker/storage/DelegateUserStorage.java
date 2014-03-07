package com.mobicrave.eventtracker.storage;

import com.mobicrave.eventtracker.Criterion;
import com.mobicrave.eventtracker.model.User;

import java.io.IOException;
import java.util.List;

public class DelegateUserStorage implements UserStorage {
  private final UserStorage userStorage;

  public DelegateUserStorage(UserStorage userStorage) {
    this.userStorage = userStorage;
  }

  @Override
  public int updateUser(User user) {
    return userStorage.updateUser(user);
  }

  @Override
  public int ensureUser(String externalUserId) {
    return userStorage.ensureUser(externalUserId);
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
    return userStorage.satisfy(userId, criteria);
  }

  @Override
  public void alias(String fromExternalUserId, int toUserId) {
    userStorage.alias(fromExternalUserId, toUserId);
  }

  @Override
  public String getVarz(int indentation) {
    return userStorage.getVarz(indentation);
  }

  @Override
  public void close() throws IOException {
    userStorage.close();
  }
}
