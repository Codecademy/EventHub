package com.mobicrave.eventtracker.list;

import com.google.inject.AbstractModule;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public class DmaIdListModule extends AbstractModule {
  @Override
  protected void configure() {
    bind(DmaIdList.Factory.class).to(DmaIdListFactory.class);
  }

  private static class DmaIdListFactory implements DmaIdList.Factory {
    private int defaultCapacity;

    private DmaIdListFactory() {
      this.defaultCapacity = 10;
    }

    @Override
    public void setDefaultCapacity(int defaultCapacity) {
      this.defaultCapacity = defaultCapacity;
    }

    @Override
    public DmaIdList build(String filename) {
      try {
        File file = new File(filename);
        if (!file.exists()) {
          //noinspection ResultOfMethodCallIgnored
          file.getParentFile().mkdirs();
          //noinspection ResultOfMethodCallIgnored
          file.createNewFile();
          try (RandomAccessFile raf = new RandomAccessFile(new File(filename), "rw")) {
            raf.setLength(DmaIdList.META_DATA_SIZE + defaultCapacity * DmaIdList.SIZE_OF_DATA);
          }
        }
        try (RandomAccessFile raf = new RandomAccessFile(new File(filename), "rw")) {
          MappedByteBuffer buffer = raf.getChannel().map(FileChannel.MapMode.READ_WRITE, 0, raf.length());
          int numRecords = buffer.getInt();
          int capacity = (int) (raf.length() - DmaIdList.META_DATA_SIZE) / DmaIdList.SIZE_OF_DATA;
          buffer.position(DmaIdList.META_DATA_SIZE + numRecords * DmaIdList.SIZE_OF_DATA);
          return new DmaIdList(filename, buffer, numRecords, capacity);
        }
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
  }
}
