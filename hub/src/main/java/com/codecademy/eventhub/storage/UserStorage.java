package com.codecademy.eventhub.storage;

import com.codecademy.eventhub.model.User;
import com.codecademy.eventhub.storage.visitor.Visitor;

import java.io.Closeable;

public interface UserStorage extends Closeable {
  static final int USER_NOT_FOUND = -1;

  int updateUser(User user);
  int ensureUser(String externalUserId);
  int getId(String externalUserId);
  User getUser(int userId);
  Visitor getFilterVisitor(int userId);
  void alias(String fromExternalUserId, int toUserId);
  int getNumRecords();
  String getVarz(int indentation);
}
