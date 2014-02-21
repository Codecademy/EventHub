package com.mobicrave.eventtracker.storage;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Maps;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.mobicrave.eventtracker.base.BloomFilter;
import com.mobicrave.eventtracker.list.DmaList;
import com.mobicrave.eventtracker.model.User;
import org.fusesource.hawtjournal.api.Journal;

import javax.inject.Named;
import javax.inject.Provider;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Map;

public class UserStorageModule extends AbstractModule {
  @Override
  protected void configure() {}

  @Provides
  @Named("eventtracker.userstorage.directory")
  public String getJournalUserStorageDirectory(@Named("eventtracker.directory") String directory) {
    return directory + "/user_storage/";
  }

  @Provides
  public DmaList<JournalUserStorage.MetaData> getJournalUserStorageMetaDataList(
      JournalUserStorage.MetaData.Schema schema,
      @Named("eventtracker.userstorage.directory") String userStorageDirectory,
      @Named("eventtracker.journaluserstorage.numMetaDataPerFile") int numMetaDataPerFile,
      @Named("eventtracker.journaluserstorage.metaDataFileCacheSize") int metaDataFileCacheSize) {
    return DmaList.build(schema, userStorageDirectory + "/meta_data/", numMetaDataPerFile,
        metaDataFileCacheSize);
  }

  @Provides
  @Named("eventtracker.journaluserstorage")
  public Journal getUserJournal(
      @Named("eventtracker.userstorage.directory") String userStorageDirectory,
      @Named("eventtracker.journaluserstorage.journalFileSize") int journalFileSize,
      @Named("eventtracker.journaluserstorage.journalWriteBatchSize") int journalWriteBatchSize) {
    return JournalUtil.createJournal(userStorageDirectory + "/user_journal/",
        journalFileSize, journalWriteBatchSize);
  }

  @Provides
  public IdMap getIdMap(@Named("eventtracker.userstorage.directory") String userStorageDirectory) {
    String filename = userStorageDirectory + "/id_map.ser";
    File file = new File(filename);
    Map<String, Integer> idMap = Maps.newConcurrentMap();
    int currentId = 0;
    if (file.exists()) {
      try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
        idMap = (Map<String, Integer>) ois.readObject();
        currentId = ois.readInt();
      } catch (ClassNotFoundException | IOException e) {
        throw new RuntimeException(e);
      }
    }
    return new IdMap(filename, idMap, currentId);
  }

  @Provides
  public JournalUserStorage getJournalUserStorage(
      final @Named("eventtracker.journaluserstorage") Journal userJournal,
      final DmaList<JournalUserStorage.MetaData> metaDataList,
      IdMap idMap) {
    return new JournalUserStorage(userJournal, metaDataList, idMap);
  }

  @Provides
  public CachedUserStorage getCachedEventStorage(
      JournalUserStorage journalUserStorage,
      @Named("eventtracker.cacheduserstorage.recordCacheSize") int recordCacheSize) {
    Cache<Integer, User> userCache = CacheBuilder.newBuilder()
        .maximumSize(recordCacheSize)
        .recordStats()
        .build();

    return new CachedUserStorage(journalUserStorage, userCache);
  }

  @Provides
  @Named("eventtracker.bloomfiltereduserstorage")
  public DmaList<BloomFilter> getBloomFilterDmaList(
      @Named("eventtracker.userstorage.directory") String userStorageDirectory,
      @Named("eventtracker.bloomfiltereduserstorage.numHashes") int numHashes,
      @Named("eventtracker.bloomfiltereduserstorage.bloomFilterSize") int bloomFilterSize,
      @Named("eventtracker.bloomfiltereduserstorage.numMetaDataPerFile") int numMetaDataPerFile,
      @Named("eventtracker.bloomfiltereduserstorage.metaDataFileCacheSize") int metaDataFileCacheSize) {
    return DmaList.build(new BloomFilter.Schema(numHashes, bloomFilterSize),
        userStorageDirectory + "/bloom_filtered_user_storage_meta_data/",
        numMetaDataPerFile,
        metaDataFileCacheSize);
  }

  @Provides
  @Named("eventtracker.bloomfiltereduserstorage")
  public BloomFilter getBloomFilter(
      @Named("eventtracker.bloomfiltereduserstorage.numHashes") int numHashes,
      @Named("eventtracker.bloomfiltereduserstorage.bloomFilterSize") int bloomFilterSize) {
    return BloomFilter.build(numHashes, bloomFilterSize);
  }

  @Provides
  public BloomFilteredUserStorage getBloomFilteredUserStorage(
      @Named("eventtracker.bloomfiltereduserstorage.numHashes") int numHashes,
      @Named("eventtracker.bloomfiltereduserstorage.bloomFilterSize") int bloomFilterSize,
      @Named("eventtracker.bloomfiltereduserstorage") DmaList<BloomFilter> bloomFilterDmaList,
      @Named("eventtracker.bloomfiltereduserstorage") Provider<BloomFilter> bloomFilterProvider,
      CachedUserStorage cachedUserStorage) {
    return new BloomFilteredUserStorage(cachedUserStorage, bloomFilterDmaList, bloomFilterProvider);
  }
}
