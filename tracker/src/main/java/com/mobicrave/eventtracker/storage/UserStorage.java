package com.mobicrave.eventtracker.storage;

import com.mobicrave.eventtracker.Filter;
import com.mobicrave.eventtracker.model.User;

import java.io.Closeable;
import java.util.List;

public interface UserStorage extends Closeable {
  static final int USER_NOT_FOUND = -1;

  int updateUser(User user);
  int ensureUser(String externalUserId);
  int getId(String externalUserId);
  User getUser(int userId);
  boolean satisfy(int userId, List<Filter> filters);
  void alias(String fromExternalUserId, int toUserId);
  String getVarz(int indentation);
}
