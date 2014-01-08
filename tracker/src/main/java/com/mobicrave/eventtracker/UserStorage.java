package com.mobicrave.eventtracker;

public interface UserStorage {
  public int addUser(User user);
  public int getId(String externalUserId);
  public User.MetaData getUserMetaData(int userId);
  public User getUser(int userId);
  public void close();
}
