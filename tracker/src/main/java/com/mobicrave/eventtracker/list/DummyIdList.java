package com.mobicrave.eventtracker.list;

import java.io.IOException;

public class DummyIdList implements IdList {
  @Override
  public void add(long id) {}

  @Override
  public Iterator subList(long firstStepEventId, long maxLastEventId) {
    return null;
  }

  @Override
  public Iterator subListByOffset(int startOffset, int numIds) {
    return null;
  }

  @Override
  public Iterator iterator() {
    return null;
  }

  @Override
  public void close() throws IOException {}
}
