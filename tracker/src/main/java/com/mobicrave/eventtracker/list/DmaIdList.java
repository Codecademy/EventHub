package com.mobicrave.eventtracker.list;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

// TODO: compression
public class DmaIdList implements IdList, Closeable {
  private static final int META_DATA_SIZE = 8; // offset for numRecords
  private static final int SIZE_OF_DATA = 8; // each data is a long number
  private final String filename;
  private MappedByteBuffer buffer;
  private long numRecords;
  private long capacity;

  public DmaIdList(String filename, MappedByteBuffer buffer, long numRecords, long capacity) {
    this.filename = filename;
    this.buffer = buffer;
    this.numRecords = numRecords;
    this.capacity = capacity;
  }

  public void add(long id) {
    if (numRecords == capacity) {
      expandBuffer(META_DATA_SIZE + 2 * capacity * SIZE_OF_DATA);
    }
    buffer.putLong(0, ++numRecords);
    buffer.putLong(id);
  }

  @Override
  public Iterator subList(long firstStepEventId, long maxLastEventId) {
    long startOffset = binarySearchOffset(0, numRecords, firstStepEventId);
    long endOffset = binarySearchOffset(startOffset, numRecords, maxLastEventId);
    return new Iterator(buffer, startOffset, endOffset);
  }

  @Override
  public Iterator iterator() {
    return new Iterator(buffer, 0, numRecords);
  }

  @Override
  public void close() {
    buffer.force();
  }

  private long binarySearchOffset(long startOffset, long endOffset, long id) {
    if (startOffset == endOffset) {
      return endOffset;
    }
    long offset = (startOffset + endOffset) / 2;
    // TODO: 4B constraint
    long value = buffer.getLong(META_DATA_SIZE + (int) offset * SIZE_OF_DATA);
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
      long size = buffer.getLong();
      long capacity = (raf.length() - META_DATA_SIZE) / SIZE_OF_DATA;
      // TODO: 4B constraint
      buffer.position((int) (META_DATA_SIZE + size * SIZE_OF_DATA));
      return new DmaIdList(filename, buffer, size, capacity);
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
      // TODO: 4B constraint
      return buffer.getLong(META_DATA_SIZE + (int) kthRecord * SIZE_OF_DATA);
    }
  }
}
