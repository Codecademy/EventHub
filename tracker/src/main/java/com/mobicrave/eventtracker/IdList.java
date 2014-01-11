package com.mobicrave.eventtracker;

import java.io.Serializable;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

// TODO: manage serialization myself
public class IdList implements Serializable {
  private static final long serialVersionUID = -8811306746467325812L;

  private long[] list;
  private final AtomicInteger currentOffset;

  public IdList(long[] list, AtomicInteger currentOffset) {
    this.list = list;
    this.currentOffset = currentOffset;
  }

  public void add(long id) {
    int offset = currentOffset.getAndIncrement();
    if (offset >= list.length) {
      synchronized (this) {
        if (offset >= list.length) {
          long[] newList = new long[list.length * 2];
          System.arraycopy(list, 0, newList, 0, list.length);
          list = newList;
        }
      }
    }
    list[offset] = id;
  }

  public Iterator subList(long firstStepEventId, long maxLastEventId) {
    int offset = currentOffset.get();
    int start = Arrays.binarySearch(list, 0, offset, firstStepEventId);
    if (start < 0) {
      start = -start - 1;
    }
    int end = Arrays.binarySearch(list, 0, offset, maxLastEventId);
    if (end < 0) {
      end = -end - 1;
    }
    return new Iterator(list, start, end);
  }

  public Iterator iterator() {
    return new Iterator(list, 0, currentOffset.get());
  }

  public static IdList build() {
    return new IdList(new long[128], new AtomicInteger(0));
  }

  public static class Iterator {
    private final long[] list;
    private final int start;
    private final int end;
    private int offset;

    public Iterator(long[] list, int start, int end) {
      this.list = list;
      this.start = start;
      this.end = end;
      this.offset = 0;
    }

    public boolean hasNext() {
      return start + offset < end;
    }

    public long next() {
      return list[start + (offset++)];
    }
  }
}
