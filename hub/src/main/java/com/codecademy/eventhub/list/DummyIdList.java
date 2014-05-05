package com.codecademy.eventhub.list;

import java.io.IOException;

public class DummyIdList implements IdList {
  @Override
  public void add(long id) {}

  @Override
  public Iterator subList(int offset, int maxRecords) {
    //noinspection ReturnOfNull
    return null;
  }

  @Override
  public int getStartOffset(long eventId) {
    return -1;
  }

  @Override
  public Iterator iterator() {
    //noinspection ReturnOfNull
    return null;
  }

  @Override
  public void close() throws IOException {}
}
