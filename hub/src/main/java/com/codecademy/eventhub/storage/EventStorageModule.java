package com.codecademy.eventhub.storage;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.codecademy.eventhub.base.BloomFilter;
import com.codecademy.eventhub.list.DmaList;
import com.codecademy.eventhub.model.Event;
import org.fusesource.hawtjournal.api.Journal;

import javax.inject.Named;
import javax.inject.Provider;

public class EventStorageModule extends AbstractModule {
  @Override
  protected void configure() {}

  @Provides
  @Named("eventhub.eventstorage.directory")
  public String getJournalUserStorageDirectory(@Named("eventhub.directory") String directory) {
    return directory + "/event_storage/";
  }

  @Provides
  public BloomFilter.Schema getBloomFilterSchema(
      @Named("eventhub.bloomfilteredeventstorage.numHashes") int numHashes,
      @Named("eventhub.bloomfilteredeventstorage.bloomFilterSize") int bloomFilterSize) {
    return new BloomFilter.Schema(numHashes, bloomFilterSize);
  }

  @Provides
  public DmaList<JournalEventStorage.MetaData> getJournalEventStorageMetaDataList(
      JournalEventStorage.MetaData.Schema schema,
      @Named("eventhub.eventstorage.directory") String eventStorageDirectory,
      @Named("eventhub.journaleventstorage.numMetaDataPerFile") int numMetaDataPerFile,
      @Named("eventhub.journaleventstorage.metaDataFileCacheSize") int metaDataFileCacheSize) {
    return DmaList.build(schema, eventStorageDirectory + "/journal_event_storage_meta_data/", numMetaDataPerFile,
        metaDataFileCacheSize);
  }

  @Provides
  @Named("eventhub.journaleventstorage")
  public Journal getEventJournal(
      @Named("eventhub.eventstorage.directory") String eventStorageDirectory,
      @Named("eventhub.journaleventstorage.journalFileSize") int journalFileSize,
      @Named("eventhub.journaleventstorage.journalWriteBatchSize") int journalWriteBatchSize) {
    return JournalUtil.createJournal(eventStorageDirectory + "/event_journal/",
        journalFileSize, journalWriteBatchSize);
  }

  @Provides
  public JournalEventStorage getJournalEventStorage(
      @Named("eventhub.journaleventstorage") Journal eventJournal,
      DmaList<JournalEventStorage.MetaData> metaDataList) {
    JournalEventStorage.MetaData.Schema schema = new JournalEventStorage.MetaData.Schema();
    return new JournalEventStorage(
        eventJournal, schema, metaDataList, metaDataList.getMaxId());
  }

  @Provides
  public CachedEventStorage getCachedEventStorage(
      JournalEventStorage journalEventStorage,
      @Named("eventhub.cachedeventstorage.recordCacheSize") int recordCacheSize) {
    Cache<Long, Event> eventCache = CacheBuilder.newBuilder()
        .maximumSize(recordCacheSize)
        .recordStats()
        .build();

    return new CachedEventStorage(journalEventStorage, eventCache);
  }

  @Provides
  @Named("eventhub.bloomfilteredeventstorage")
  public DmaList<BloomFilter> getBloomFilterDmaList(
      @Named("eventhub.eventstorage.directory") String eventStorageDirectory,
      @Named("eventhub.bloomfilteredeventstorage.numHashes") int numHashes,
      @Named("eventhub.bloomfilteredeventstorage.bloomFilterSize") int bloomFilterSize,
      @Named("eventhub.bloomfilteredeventstorage.numMetaDataPerFile") int numMetaDataPerFile,
      @Named("eventhub.bloomfilteredeventstorage.metaDataFileCacheSize") int metaDataFileCacheSize) {
    return DmaList.build(new BloomFilter.Schema(numHashes, bloomFilterSize),
        eventStorageDirectory + "/bloom_filtered_event_storage_meta_data/",
        numMetaDataPerFile,
        metaDataFileCacheSize);
  }

  @Provides
  @Named("eventhub.bloomfilteredeventstorage")
  public BloomFilter getBloomFilter(
      @Named("eventhub.bloomfilteredeventstorage.numHashes") int numHashes,
      @Named("eventhub.bloomfilteredeventstorage.bloomFilterSize") int bloomFilterSize) {
    return BloomFilter.build(numHashes, bloomFilterSize);
  }

  @Provides
  public BloomFilteredEventStorage getBloomFilteredEventStorage(
      CachedEventStorage cachedEventStorage,
      @Named("eventhub.bloomfilteredeventstorage") DmaList<BloomFilter> bloomFilterDmaList,
      @Named("eventhub.bloomfilteredeventstorage") Provider<BloomFilter> bloomFilterProvider) {
    return new BloomFilteredEventStorage(cachedEventStorage, bloomFilterDmaList,
        bloomFilterProvider);
  }
}
