package com.mobicrave.eventtracker;

import java.io.Closeable;

public interface UserStorage extends Closeable {
  public int addUser(User user);
  public int getId(String externalUserId);
  public User.MetaData getUserMetaData(int userId);
  public User getUser(int userId);
}
