package com.mobicrave.eventtracker.storage;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.mobicrave.eventtracker.base.BloomFilter;
import com.mobicrave.eventtracker.list.DmaList;
import com.mobicrave.eventtracker.model.User;
import org.fusesource.hawtjournal.api.Journal;
import org.fusesource.leveldbjni.JniDBFactory;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.Options;

import javax.inject.Named;
import javax.inject.Provider;
import java.io.File;
import java.io.IOException;

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
  public IdMap getIdMap(@Named("eventtracker.userstorage.directory") String userStorageDirectory) throws IOException {
    String filename = userStorageDirectory + "/id_map.db";
    //noinspection ResultOfMethodCallIgnored
    new File(userStorageDirectory).mkdirs();
    Options options = new Options();
    options.createIfMissing(true);
    DB db = JniDBFactory.factory.open(new File(filename), options);
    return IdMap.create(db);
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
