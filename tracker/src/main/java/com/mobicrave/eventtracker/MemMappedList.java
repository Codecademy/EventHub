package com.mobicrave.eventtracker;

import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

/**
 * The class is not thread-safe
 */
public class MemMappedList<T> implements Closeable {
  private static final int META_DATA_SIZE = 8; // offset for numRecords
  private final String filename;
  private final Schema<T> schema;
  private MappedByteBuffer buffer;
  private long numRecords;
  private long capacity;

  public MemMappedList(String filename, Schema<T> schema, MappedByteBuffer buffer, long numRecords,
      long capacity) {
    this.filename = filename;
    this.schema = schema;
    this.buffer = buffer;
    this.numRecords = numRecords;
    this.capacity = capacity;
  }

  public void write(T t) {
    if (numRecords == capacity) {
      buffer.force();
      try {
        RandomAccessFile raf = new RandomAccessFile(filename, "rw");
        int objectSize = schema.getObjectSize();
        raf.setLength(META_DATA_SIZE + 2 * capacity * objectSize);
        buffer = raf.getChannel().map(FileChannel.MapMode.READ_WRITE, 0, raf.length());
        buffer.position(META_DATA_SIZE + (int) capacity * objectSize);
        capacity *= 2;
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
    buffer.putLong(0, ++numRecords);
    buffer.put(schema.toBytes(t));
  }

  public T get(long kthRecord) {
    int objectSize = schema.getObjectSize();
    byte[] bytes = new byte[objectSize];
    // 4B constraints
    ByteBuffer newBuffer = buffer.duplicate();
    newBuffer.position(META_DATA_SIZE + (int) kthRecord * objectSize);
    newBuffer.get(bytes, 0, objectSize);
    return schema.fromBytes(bytes);
  }

  @Override
  public void close() {
    buffer.force();
  }

  public long getNumRecords() {
    return numRecords;
  }

  public static <T> MemMappedList<T> build(Schema<T> schema, String filename, int defaultCapacity) {
    try {
      File file = new File(filename);
      if (!file.exists()) {
        file.createNewFile();
        RandomAccessFile raf = new RandomAccessFile(new File(filename), "rw");
        raf.setLength(META_DATA_SIZE + defaultCapacity * schema.getObjectSize());
        raf.close();
      }
      RandomAccessFile raf = new RandomAccessFile(new File(filename), "rw");
      MappedByteBuffer buffer = raf.getChannel().map(FileChannel.MapMode.READ_WRITE, 0, raf.length());
      long size = buffer.getLong();
      long capacity = raf.length() / schema.getObjectSize();
      // 4B constraints
      buffer.position((int) (META_DATA_SIZE + size * schema.getObjectSize()));
      return new MemMappedList<T>(filename, schema, buffer, size, capacity);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
