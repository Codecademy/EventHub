package com.mobicrave.eventtracker;

import java.util.Arrays;

public class IdList {
  private long[] list;
  private int numElements;

  public IdList(long[] list) {
    this.list = list;
  }

  // TODO: may need synchronization
  public void add(long id) {
    if (numElements == list.length) {
      long[] newList = new long[list.length * 2];
      System.arraycopy(list, 0, newList, 0, list.length);
      list = newList;
    }
    list[numElements++] = id;
  }

  public Iterator subList(long firstStepEventId, long maxLastEventId) {
    int start = Arrays.binarySearch(list, 0, numElements, firstStepEventId);
    if (start < 0) {
      start = -start - 1;
    }
    int end = Arrays.binarySearch(list, 0, numElements, maxLastEventId);
    if (end < 0) {
      end = -end - 1;
    }
    return new Iterator(list, start, end);
  }

  public Iterator iterator() {
    return new Iterator(list, 0, numElements);
  }

  public static IdList build() {
    return new IdList(new long[128]);
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
    }

    public boolean hasNext() {
      return start + offset < end;
    }

    public long next() {
      return list[start + (offset++)];
    }
  }
}
