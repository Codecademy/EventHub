package com.codecademy.eventhub.index;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.codecademy.eventhub.base.ByteBufferUtil;
import com.codecademy.eventhub.list.DmaList;

import javax.inject.Named;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.nio.MappedByteBuffer;

public class UserEventIndexModule extends AbstractModule {
  @Override
  protected void configure() {}

  @Provides
  @Named("eventhub.usereventindex.directory")
  public String getUserEventIndexDirectory(@Named("eventhub.directory") String directory) {
    return directory + "/user_event_index/";
  }

  @Provides
  public DmaList<UserEventIndex.IndexEntry> getIndexEntryDmaList(
      @Named("eventhub.usereventindex.directory") String directory,
      @Named("eventhub.usereventindex.numPointersPerIndexEntry") int numPointers,
      @Named("eventhub.usereventindex.numIndexEntryPerFile") int numIndexEntryPerFile,
      @Named("eventhub.usereventindex.indexEntryFileCacheSize") int indexEntryFileCacheSize) {
    return DmaList.build(new UserEventIndex.IndexEntry.Schema(numPointers),
        directory, numIndexEntryPerFile, indexEntryFileCacheSize);
  }

  @Provides
  public UserEventIndex.IndexEntry.Factory getIndexEntryFactory(
      @Named("eventhub.usereventindex.numPointersPerIndexEntry") int numPointers) {
    return new UserEventIndex.IndexEntry.Factory(numPointers);
  }

  @Provides
  public UserEventIndex.Block.Factory getBlockFactory(
      final @Named("eventhub.usereventindex.directory") String directory,
      @Named("eventhub.usereventindex.blockCacheSize") int blockCacheSize,
      @Named("eventhub.usereventindex.numRecordsPerBlock") int numRecordsPerBlock,
      @Named("eventhub.usereventindex.numBlocksPerFile") int numBlocksPerFile) {
    final int fileSize = numBlocksPerFile * (
        numRecordsPerBlock * UserEventIndex.ID_SIZE + UserEventIndex.Block.MetaData.SIZE);
    LoadingCache<Integer, MappedByteBuffer> buffers = CacheBuilder.newBuilder()
        .maximumSize(blockCacheSize)
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
                String.format("%s/block_%d.mem", directory, key), fileSize);
          }
        });

    String filename = directory + "block_factory.ser";
    File file = new File(filename);
    if (file.exists()) {
      try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
        long currentPointer = ois.readLong();
        return new UserEventIndex.Block.Factory(filename, buffers, numRecordsPerBlock,
            numBlocksPerFile, currentPointer);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
    return new UserEventIndex.Block.Factory(filename, buffers, numRecordsPerBlock,
        numBlocksPerFile, 0);
  }

  @Provides
  public UserEventIndex getUserEventIndex(
      final @Named("eventhub.usereventindex.directory") String directory,
      DmaList<UserEventIndex.IndexEntry> index,
      UserEventIndex.IndexEntry.Factory indexEntryFactory,
      UserEventIndex.Block.Factory blockFactory) {
    return new UserEventIndex(index, indexEntryFactory, blockFactory);
  }
}
