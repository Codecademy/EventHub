package com.mobicrave.eventtracker.list;

import java.io.Closeable;

public interface IdList extends Closeable {
  void add(long id);
  int getStartOffset(long eventId);
  IdList.Iterator subList(int offset, int maxRecords);
  IdList.Iterator iterator();

  public static interface Iterator {
    boolean hasNext();
    long next();
  }
}
