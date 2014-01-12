package com.mobicrave.eventtracker;

public interface Schema<T> {
  public int getObjectSize();
  public byte[] toBytes(T t);
  public T fromBytes(byte[] bytes);
}
