package com.codecademy.eventhub.storage;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.codecademy.eventhub.base.BloomFilter;
import com.codecademy.eventhub.base.DB;
import com.codecademy.eventhub.list.DmaList;
import com.codecademy.eventhub.model.User;
import org.fusesource.hawtjournal.api.Journal;
import org.fusesource.leveldbjni.JniDBFactory;
import org.iq80.leveldb.Options;

import javax.inject.Named;
import javax.inject.Provider;
import java.io.File;
import java.io.IOException;

public class UserStorageModule extends AbstractModule {
  @Override
  protected void configure() {}

  @Provides
  @Named("eventhub.userstorage.directory")
  public String getJournalUserStorageDirectory(@Named("eventhub.directory") String directory) {
    return directory + "/user_storage/";
  }

  @Provides
  public DmaList<JournalUserStorage.MetaData> getJournalUserStorageMetaDataList(
      JournalUserStorage.MetaData.Schema schema,
      @Named("eventhub.userstorage.directory") String userStorageDirectory,
      @Named("eventhub.journaluserstorage.numMetaDataPerFile") int numMetaDataPerFile,
      @Named("eventhub.journaluserstorage.metaDataFileCacheSize") int metaDataFileCacheSize) {
    return DmaList.build(schema, userStorageDirectory + "/meta_data/", numMetaDataPerFile,
        metaDataFileCacheSize);
  }

  @Provides
  @Named("eventhub.journaluserstorage")
  public Journal getUserJournal(
      @Named("eventhub.userstorage.directory") String userStorageDirectory,
      @Named("eventhub.journaluserstorage.journalFileSize") int journalFileSize,
      @Named("eventhub.journaluserstorage.journalWriteBatchSize") int journalWriteBatchSize) {
    return JournalUtil.createJournal(userStorageDirectory + "/user_journal/",
        journalFileSize, journalWriteBatchSize);
  }

  @Provides
  public IdMap getIdMap(@Named("eventhub.userstorage.directory") String userStorageDirectory) throws IOException {
    String filename = userStorageDirectory + "/id_map.db";
    //noinspection ResultOfMethodCallIgnored
    new File(userStorageDirectory).mkdirs();
    Options options = new Options();
    options.createIfMissing(true);
    return IdMap.create(new DB(JniDBFactory.factory.open(new File(filename), options)));
  }

  @Provides
  public JournalUserStorage getJournalUserStorage(
      final @Named("eventhub.journaluserstorage") Journal userJournal,
      final DmaList<JournalUserStorage.MetaData> metaDataList,
      IdMap idMap) {
    return new JournalUserStorage(userJournal, metaDataList, idMap);
  }

  @Provides
  public CachedUserStorage getCachedEventStorage(
      JournalUserStorage journalUserStorage,
      @Named("eventhub.cacheduserstorage.recordCacheSize") int recordCacheSize) {
    Cache<Integer, User> userCache = CacheBuilder.newBuilder()
        .maximumSize(recordCacheSize)
        .recordStats()
        .build();

    return new CachedUserStorage(journalUserStorage, userCache);
  }

  @Provides
  @Named("eventhub.bloomfiltereduserstorage")
  public DmaList<BloomFilter> getBloomFilterDmaList(
      @Named("eventhub.userstorage.directory") String userStorageDirectory,
      @Named("eventhub.bloomfiltereduserstorage.numHashes") int numHashes,
      @Named("eventhub.bloomfiltereduserstorage.bloomFilterSize") int bloomFilterSize,
      @Named("eventhub.bloomfiltereduserstorage.numMetaDataPerFile") int numMetaDataPerFile,
      @Named("eventhub.bloomfiltereduserstorage.metaDataFileCacheSize") int metaDataFileCacheSize) {
    return DmaList.build(new BloomFilter.Schema(numHashes, bloomFilterSize),
        userStorageDirectory + "/bloom_filtered_user_storage_meta_data/",
        numMetaDataPerFile,
        metaDataFileCacheSize);
  }

  @Provides
  @Named("eventhub.bloomfiltereduserstorage")
  public BloomFilter getBloomFilter(
      @Named("eventhub.bloomfiltereduserstorage.numHashes") int numHashes,
      @Named("eventhub.bloomfiltereduserstorage.bloomFilterSize") int bloomFilterSize) {
    return BloomFilter.build(numHashes, bloomFilterSize);
  }

  @Provides
  public BloomFilteredUserStorage getBloomFilteredUserStorage(
      @Named("eventhub.bloomfiltereduserstorage.numHashes") int numHashes,
      @Named("eventhub.bloomfiltereduserstorage.bloomFilterSize") int bloomFilterSize,
      @Named("eventhub.bloomfiltereduserstorage") DmaList<BloomFilter> bloomFilterDmaList,
      @Named("eventhub.bloomfiltereduserstorage") Provider<BloomFilter> bloomFilterProvider,
      CachedUserStorage cachedUserStorage) {
    return new BloomFilteredUserStorage(cachedUserStorage, bloomFilterDmaList, bloomFilterProvider);
  }
}
