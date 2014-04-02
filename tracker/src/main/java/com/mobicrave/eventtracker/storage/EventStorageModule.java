package com.mobicrave.eventtracker.storage;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.mobicrave.eventtracker.base.BloomFilter;
import com.mobicrave.eventtracker.list.DmaList;
import com.mobicrave.eventtracker.model.Event;
import org.fusesource.hawtjournal.api.Journal;

import javax.inject.Named;
import javax.inject.Provider;

public class EventStorageModule extends AbstractModule {
  @Override
  protected void configure() {}

  @Provides
  @Named("eventtracker.eventstorage.directory")
  public String getJournalUserStorageDirectory(@Named("eventtracker.directory") String directory) {
    return directory + "/event_storage/";
  }

  @Provides
  public BloomFilter.Schema getBloomFilterSchema(
      @Named("eventtracker.bloomfilteredeventstorage.numHashes") int numHashes,
      @Named("eventtracker.bloomfilteredeventstorage.bloomFilterSize") int bloomFilterSize) {
    return new BloomFilter.Schema(numHashes, bloomFilterSize);
  }

  @Provides
  public DmaList<JournalEventStorage.MetaData> getJournalEventStorageMetaDataList(
      JournalEventStorage.MetaData.Schema schema,
      @Named("eventtracker.eventstorage.directory") String eventStorageDirectory,
      @Named("eventtracker.journaleventstorage.numMetaDataPerFile") int numMetaDataPerFile,
      @Named("eventtracker.journaleventstorage.metaDataFileCacheSize") int metaDataFileCacheSize) {
    return DmaList.build(schema, eventStorageDirectory + "/journal_event_storage_meta_data/", numMetaDataPerFile,
        metaDataFileCacheSize);
  }

  @Provides
  @Named("eventtracker.journaleventstorage")
  public Journal getEventJournal(
      @Named("eventtracker.eventstorage.directory") String eventStorageDirectory,
      @Named("eventtracker.journaleventstorage.journalFileSize") int journalFileSize,
      @Named("eventtracker.journaleventstorage.journalWriteBatchSize") int journalWriteBatchSize) {
    return JournalUtil.createJournal(eventStorageDirectory + "/event_journal/",
        journalFileSize, journalWriteBatchSize);
  }

  @Provides
  public JournalEventStorage getJournalEventStorage(
      @Named("eventtracker.journaleventstorage") Journal eventJournal,
      DmaList<JournalEventStorage.MetaData> metaDataList) {
    JournalEventStorage.MetaData.Schema schema = new JournalEventStorage.MetaData.Schema();
    return new JournalEventStorage(
        eventJournal, schema, metaDataList, metaDataList.getNumRecords());
  }

  @Provides
  public CachedEventStorage getCachedEventStorage(
      JournalEventStorage journalEventStorage,
      @Named("eventtracker.cachedeventstorage.recordCacheSize") int recordCacheSize) {
    Cache<Long, Event> eventCache = CacheBuilder.newBuilder()
        .maximumSize(recordCacheSize)
        .recordStats()
        .build();

    return new CachedEventStorage(journalEventStorage, eventCache);
  }

  @Provides
  @Named("eventtracker.bloomfilteredeventstorage")
  public DmaList<BloomFilter> getBloomFilterDmaList(
      @Named("eventtracker.eventstorage.directory") String eventStorageDirectory,
      @Named("eventtracker.bloomfilteredeventstorage.numHashes") int numHashes,
      @Named("eventtracker.bloomfilteredeventstorage.bloomFilterSize") int bloomFilterSize,
      @Named("eventtracker.bloomfilteredeventstorage.numMetaDataPerFile") int numMetaDataPerFile,
      @Named("eventtracker.bloomfilteredeventstorage.metaDataFileCacheSize") int metaDataFileCacheSize) {
    return DmaList.build(new BloomFilter.Schema(numHashes, bloomFilterSize),
        eventStorageDirectory + "/bloom_filtered_event_storage_meta_data/",
        numMetaDataPerFile,
        metaDataFileCacheSize);
  }

  @Provides
  @Named("eventtracker.bloomfilteredeventstorage")
  public BloomFilter getBloomFilter(
      @Named("eventtracker.bloomfilteredeventstorage.numHashes") int numHashes,
      @Named("eventtracker.bloomfilteredeventstorage.bloomFilterSize") int bloomFilterSize) {
    return BloomFilter.build(numHashes, bloomFilterSize);
  }

  @Provides
  public BloomFilteredEventStorage getBloomFilteredEventStorage(
      CachedEventStorage cachedEventStorage,
      @Named("eventtracker.bloomfilteredeventstorage") DmaList<BloomFilter> bloomFilterDmaList,
      @Named("eventtracker.bloomfilteredeventstorage") Provider<BloomFilter> bloomFilterProvider) {
    return new BloomFilteredEventStorage(cachedEventStorage, bloomFilterDmaList,
        bloomFilterProvider);
  }
}
