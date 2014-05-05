package com.codecademy.eventhub.list;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import com.codecademy.eventhub.base.ByteBufferUtil;
import com.codecademy.eventhub.base.Schema;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

/**
 * numRecordsPerFile * schema.getObjectSize() can't exceed MappedByteBuffer size limit, i.e.
 * numRecordsPerFile < (2^31 - 1) / schema.getObjectSize()
 */
public class DmaList<T> implements Closeable {
  private final String directory;
  private final Schema<T> schema;
  private final MappedByteBuffer metaDataBuffer;
  // O(numFiles)
  private LoadingCache<Integer, MappedByteBuffer> buffers;
  private long maxId;
  private int numRecordsPerFile;

  public DmaList(String directory, Schema<T> schema, MappedByteBuffer metaDataBuffer,
      LoadingCache<Integer, MappedByteBuffer> buffers, long maxId, int numRecordsPerFile) {
    this.directory = directory;
    this.schema = schema;
    this.metaDataBuffer = metaDataBuffer;
    this.buffers = buffers;
    this.maxId = maxId;
    this.numRecordsPerFile = numRecordsPerFile;
  }

  public void add(T t) {
    int currentBufferIndex = (int) (maxId / numRecordsPerFile);
    ByteBuffer duplicate = buffers.getUnchecked(currentBufferIndex).duplicate();
    duplicate.position((int) (maxId % numRecordsPerFile) * schema.getObjectSize());
    duplicate.put(schema.toBytes(t));
    metaDataBuffer.putLong(0, ++maxId);
  }

  public void update(long id, T t) {
    if (id > maxId) {
      maxId = id;
    }
    int currentBufferIndex = (int) (id / numRecordsPerFile);
    ByteBuffer duplicate = buffers.getUnchecked(currentBufferIndex).duplicate();
    duplicate.position((int) (id % numRecordsPerFile) * schema.getObjectSize());
    duplicate.put(schema.toBytes(t));
  }

  public T get(long kthRecord) {
    int objectSize = schema.getObjectSize();
    byte[] bytes = new byte[objectSize];
    ByteBuffer newBuffer = buffers.getUnchecked((int) (kthRecord / numRecordsPerFile)).duplicate();
    newBuffer.position((int) (kthRecord % numRecordsPerFile) * objectSize);
    newBuffer.get(bytes, 0, objectSize);
    return schema.fromBytes(bytes);
  }

  public byte[] getBytes(long kthRecord) {
    int objectSize = schema.getObjectSize();
    byte[] bytes = new byte[objectSize];
    ByteBuffer newBuffer = buffers.getUnchecked((int) (kthRecord / numRecordsPerFile)).duplicate();
    newBuffer.position((int) (kthRecord % numRecordsPerFile) * objectSize);
    newBuffer.get(bytes, 0, objectSize);
    return bytes;
  }

  public long getMaxId() {
    return maxId;
  }

  public String getVarz(int indentation) {
    String indent  = new String(new char[indentation]).replace('\0', ' ');
    return String.format(
        indent + "directory: %s\n" +
        indent + "buffer: %s",
        directory, buffers.stats().toString());
  }

  @Override
  public void close() {
    buffers.invalidateAll();
  }

  public static <T> DmaList<T> build(final Schema<T> schema, final String directory,
      final int numRecordsPerFile, int cacheSize) {
    //noinspection ResultOfMethodCallIgnored
    new File(directory).mkdirs();
    try (RandomAccessFile raf = new RandomAccessFile(new File(
          String.format("%s/meta_data.mem", directory)), "rw")) {
      MappedByteBuffer metaDataBuffer = raf.getChannel().map(FileChannel.MapMode.READ_WRITE, 0, 8);
      long numRecords = metaDataBuffer.getLong();
      final int fileSize = numRecordsPerFile * schema.getObjectSize();
      LoadingCache<Integer, MappedByteBuffer> buffers = CacheBuilder.newBuilder()
          .maximumSize(cacheSize)
          .recordStats()
          .removalListener(new RemovalListener<Integer, MappedByteBuffer>() {
            @Override
            public void onRemoval(RemovalNotification<Integer, MappedByteBuffer> notification) {
              MappedByteBuffer value = notification.getValue();
              if (value != null) {
                value.force();
              }
            }})
          .build(new CacheLoader<Integer, MappedByteBuffer>() {
            @Override
            public MappedByteBuffer load(Integer key) throws Exception {
              return ByteBufferUtil.createNewBuffer(
                  String.format("%s/dma_list_%d.mem", directory, key), fileSize);
            }
          });
      return new DmaList<>(directory, schema, metaDataBuffer, buffers, numRecords, numRecordsPerFile);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
