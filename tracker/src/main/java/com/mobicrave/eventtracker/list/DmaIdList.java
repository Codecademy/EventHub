package com.mobicrave.eventtracker.list;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

/**
 * Currently, it can contain up to MAX_NUM_RECORDS records. ((2^31 - 1) - 4) / 8, i.e. largest
 * indexable address in (MappedByteBuffer - size of metadata) / size of a long typed id.
 * Since it's used in IndividualEventIndex and UserEventIndex, this implied that no single date
 * nor single user can have number of events exceeding this limit.
 */
public class DmaIdList implements IdList, Closeable {
  private static final int META_DATA_SIZE = 4; // offset for numRecords
  private static final int SIZE_OF_DATA = 8; // each data is a long number
  private static final int MAX_NUM_RECORDS = (Integer.MAX_VALUE - META_DATA_SIZE) / SIZE_OF_DATA;

  private final String filename;
  private MappedByteBuffer buffer;
  private int numRecords;
  private long capacity;

  public DmaIdList(String filename, MappedByteBuffer buffer, int numRecords, int capacity) {
    this.filename = filename;
    this.buffer = buffer;
    this.numRecords = numRecords;
    this.capacity = capacity;
  }

  public void add(long id) {
    if (numRecords == MAX_NUM_RECORDS) {
      throw new IllegalStateException(
          String.format("DmaIdList reaches its maximum number of records: %d", numRecords));
    }
    if (numRecords == capacity) {
      expandBuffer(META_DATA_SIZE + Math.min(MAX_NUM_RECORDS, 2 * capacity) * SIZE_OF_DATA);
    }
    buffer.putLong(id);
    buffer.putInt(0, ++numRecords);
  }

  @Override
  public Iterator subList(long firstStepEventId, long maxLastEventId) {
    int startOffset = binarySearchOffset(0, numRecords, firstStepEventId);
    int endOffset = binarySearchOffset(startOffset, numRecords, maxLastEventId);
    return new Iterator(buffer, startOffset, endOffset);
  }

  @Override
  public IdList.Iterator subListByOffset(int startOffset, int numIds) {
    return new Iterator(buffer, startOffset, startOffset + numIds);
  }

  @Override
  public Iterator iterator() {
    return new Iterator(buffer, 0, numRecords);
  }

  @Override
  public void close() {
    buffer.force();
  }

  private int binarySearchOffset(int startOffset, int endOffset, long id) {
    if (startOffset == endOffset) {
      return endOffset;
    }
    int offset = (startOffset + endOffset) >>> 1;
    long value = buffer.getLong(META_DATA_SIZE + offset * SIZE_OF_DATA);
    if (value == id) {
      return offset;
    } else if (value < id) {
      return binarySearchOffset(offset + 1, endOffset, id);
    } else {
      return binarySearchOffset(startOffset, offset, id);
    }
  }

  private void expandBuffer(long newSize) {
    buffer.force();
    try {
      int oldPosition = buffer.position();
      RandomAccessFile raf = new RandomAccessFile(filename, "rw");
      raf.setLength(newSize);
      buffer = raf.getChannel().map(FileChannel.MapMode.READ_WRITE, 0, raf.length());
      buffer.position(oldPosition);
      capacity *= 2;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static DmaIdList build(String filename, int defaultCapacity) {
    try {
      File file = new File(filename);
      if (!file.exists()) {
        //noinspection ResultOfMethodCallIgnored
        file.getParentFile().mkdirs();
        //noinspection ResultOfMethodCallIgnored
        file.createNewFile();
        RandomAccessFile raf = new RandomAccessFile(new File(filename), "rw");
        raf.setLength(META_DATA_SIZE + defaultCapacity * SIZE_OF_DATA);
        raf.close();
      }
      RandomAccessFile raf = new RandomAccessFile(new File(filename), "rw");
      MappedByteBuffer buffer = raf.getChannel().map(FileChannel.MapMode.READ_WRITE, 0, raf.length());
      int numRecords = buffer.getInt();
      int capacity = (int) (raf.length() - META_DATA_SIZE) / SIZE_OF_DATA;
      buffer.position(META_DATA_SIZE + numRecords * SIZE_OF_DATA);
      return new DmaIdList(filename, buffer, numRecords, capacity);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static class Iterator implements IdList.Iterator {
    private final MappedByteBuffer buffer;
    private final long start;
    private final long end;
    private long offset;

    public Iterator(MappedByteBuffer buffer, long start, long end) {
      this.buffer = buffer;
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
      long kthRecord = start + (offset++);
      return buffer.getLong(META_DATA_SIZE + (int) kthRecord * SIZE_OF_DATA);
    }
  }
}
