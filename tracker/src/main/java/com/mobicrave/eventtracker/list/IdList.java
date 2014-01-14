package com.mobicrave.eventtracker.list;

import java.io.Closeable;

public interface IdList extends Closeable {
  public void add(long id);
  public IdList.Iterator subList(long firstStepEventId, long maxLastEventId);
  public IdList.Iterator iterator();

  public static interface Iterator {
    public boolean hasNext();
    public long next();
  }
}
