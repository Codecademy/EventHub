package com.mobicrave.eventtracker.base;

public interface Schema<T> {
  int getObjectSize();
  byte[] toBytes(T t);
  T fromBytes(byte[] bytes);
}
