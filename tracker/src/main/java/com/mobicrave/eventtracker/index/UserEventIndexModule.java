package com.mobicrave.eventtracker.index;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.mobicrave.eventtracker.base.ByteBufferUtil;
import com.mobicrave.eventtracker.list.DmaList;

import javax.inject.Named;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.nio.MappedByteBuffer;
import java.util.BitSet;

public class UserEventIndexModule extends AbstractModule {
  @Override
  protected void configure() {}

  @Provides
  @Named("eventtracker.usereventindex.directory")
  public String getUserEventIndexDirectory(@Named("eventtracker.directory") String directory) {
    return directory + "/user_event_index/";
  }

  @Provides
  public DmaList<UserEventIndex.IndexEntry> getIndexEntryDmaList(
      @Named("eventtracker.usereventindex.directory") String directory,
      @Named("eventtracker.usereventindex.numPointersPerIndexEntry") int numPointers,
      @Named("eventtracker.usereventindex.numIndexEntryPerFile") int numIndexEntryPerFile,
      @Named("eventtracker.usereventindex.indexEntryFileCacheSize") int indexEntryFileCacheSize) {
    return DmaList.build(new UserEventIndex.IndexEntry.Schema(numPointers),
        directory, numIndexEntryPerFile, indexEntryFileCacheSize);
  }

  @Provides
  public UserEventIndex.IndexEntry.Factory getIndexEntryFactory(
      @Named("eventtracker.usereventindex.numPointersPerIndexEntry") int numPointers) {
    return new UserEventIndex.IndexEntry.Factory(numPointers);
  }

  @Provides
  public UserEventIndex.Block.Factory getBlockFactory(
      final @Named("eventtracker.usereventindex.directory") String directory,
      @Named("eventtracker.usereventindex.blockCacheSize") int blockCacheSize,
      @Named("eventtracker.usereventindex.numRecordsPerBlock") int numRecordsPerBlock,
      @Named("eventtracker.usereventindex.numBlocksPerFile") int numBlocksPerFile) {
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
      final @Named("eventtracker.usereventindex.directory") String directory,
      DmaList<UserEventIndex.IndexEntry> index,
      UserEventIndex.IndexEntry.Factory indexEntryFactory,
      UserEventIndex.Block.Factory blockFactory) {
    String filename = directory + "user_event_index.ser";
    File file = new File(filename);
    if (file.exists()) {
      try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
        BitSet userIdSet = (BitSet) ois.readObject();
        return new UserEventIndex(filename, index, indexEntryFactory, blockFactory, userIdSet);
      } catch (IOException | ClassNotFoundException e) {
        throw new RuntimeException(e);
      }
    }

    return new UserEventIndex(filename, index, indexEntryFactory, blockFactory, new BitSet());
  }
}
