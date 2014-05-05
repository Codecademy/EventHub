package com.codecademy.eventhub.storage;

import com.codecademy.eventhub.model.User;
import com.codecademy.eventhub.storage.visitor.Visitor;

import java.io.IOException;

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
  public int getNumRecords() {
    return userStorage.getNumRecords();
  }

  @Override
  public Visitor getFilterVisitor(int userId) {
    return userStorage.getFilterVisitor(userId);
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
