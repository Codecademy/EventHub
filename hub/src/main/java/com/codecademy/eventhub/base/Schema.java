package com.codecademy.eventhub.base;

public interface Schema<T> {
  int getObjectSize();
  byte[] toBytes(T t);
  T fromBytes(byte[] bytes);
}
