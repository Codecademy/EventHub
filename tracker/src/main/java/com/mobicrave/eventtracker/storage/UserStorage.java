package com.mobicrave.eventtracker.storage;

import com.mobicrave.eventtracker.model.User;
import com.mobicrave.eventtracker.storage.visitor.Visitor;

import java.io.Closeable;

public interface UserStorage extends Closeable {
  static final int USER_NOT_FOUND = -1;

  int updateUser(User user);
  int ensureUser(String externalUserId);
  int getId(String externalUserId);
  User getUser(int userId);
  Visitor getFilterVisitor(int userId);
  void alias(String fromExternalUserId, int toUserId);
  String getVarz(int indentation);
}
