package com.mobicrave.eventtracker.storage;

import com.mobicrave.eventtracker.Criterion;
import com.mobicrave.eventtracker.model.User;

import java.io.Closeable;
import java.util.List;

public interface UserStorage extends Closeable {
  public static final int USER_NOT_FOUND = -1;
  public int addUser(User user);
  public int getId(String externalUserId);
  public User getUser(int userId);
  public boolean satisfy(int userId, List<Criterion> criteria);
}
