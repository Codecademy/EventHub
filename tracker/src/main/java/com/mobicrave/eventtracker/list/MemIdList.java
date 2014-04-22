package com.mobicrave.eventtracker.list;

import java.util.Arrays;

public class MemIdList implements IdList {
  private long[] list;
  private int numRecords;

  public MemIdList(long[] list, int numRecords) {
    this.list = list;
    this.numRecords = numRecords;
  }

  @Override
  public int getStartOffset(long eventId) {
    int offset = Arrays.binarySearch(list, 0, numRecords, eventId);
    if (offset < 0) {
      offset = -offset - 1;
    }
    return offset;
  }

  @Override
  public void add(long id) {
    if (list.length == 0) {
      list = new long[1];
    }
    if (numRecords == list.length) {
      long[] newList = new long[list.length * 2];
      System.arraycopy(list, 0, newList, 0, list.length);
      list = newList;
    }
    list[numRecords++] = id;
  }

  @Override
  public Iterator subList(int start, int maxRecords) {
    int end = start + maxRecords;
    end = Math.min(end < 0 ? Integer.MAX_VALUE : end, numRecords);
    return new Iterator(list, start, end);
  }

  @Override
  public Iterator iterator() {
    return new Iterator(list, 0, numRecords);
  }

  @Override
  public void close() {}

  public long[] getList() {
    return Arrays.copyOf(list, numRecords);
  }

  public static class Iterator implements IdList.Iterator {
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

    @Override
    public boolean hasNext() {
      return start + offset < end;
    }

    @Override
    public long next() {
      return list[start + (offset++)];
    }
  }
}
