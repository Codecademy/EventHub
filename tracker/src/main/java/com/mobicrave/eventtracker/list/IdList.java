package com.mobicrave.eventtracker.list;

import java.io.Closeable;

public interface IdList extends Closeable {
  public void add(long id);
  public MemIdList.Iterator subList(long firstStepEventId, long maxLastEventId);
  public MemIdList.Iterator iterator();

  public static interface Iterator {
    public boolean hasNext();
    public long next();
  }
}
