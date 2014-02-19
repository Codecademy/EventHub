package com.mobicrave.eventtracker.storage;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.io.ByteStreams;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.mobicrave.eventtracker.base.BloomFilter;
import com.mobicrave.eventtracker.list.DmaList;
import com.mobicrave.eventtracker.model.Event;
import org.fusesource.hawtjournal.api.Journal;
import org.fusesource.hawtjournal.api.Location;

import javax.inject.Named;
import javax.inject.Provider;
import java.io.IOException;

public class EventStorageModule extends AbstractModule {
  @Override
  protected void configure() {}

  @Provides
  @Named("eventtracker.eventstorage.directory")
  public String getJournalUserStorageDirectory(@Named("eventtracker.directory") String directory) {
    return directory + "/event_storage/";
  }

  @Provides
  public BloomFilteredEventStorage.Schema getBloomFilterSchema(
      @Named("eventtracker.bloomfilteredeventstorage.numHashes") int numHashes,
      @Named("eventtracker.bloomfilteredeventstorage.bloomFilterSize") int bloomFilterSize) {
    return new BloomFilteredEventStorage.Schema(numHashes, bloomFilterSize);
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
      @Named("eventtracker.journaleventstorage.recordCacheSize") int recordCacheSize,
      final @Named("eventtracker.journaleventstorage") Journal eventJournal,
      final DmaList<JournalEventStorage.MetaData> metaDataList) {
    final JournalEventStorage.MetaData.Schema schema = new JournalEventStorage.MetaData.Schema();
    LoadingCache<Long, Event> eventCache = CacheBuilder.newBuilder()
        .maximumSize(recordCacheSize)
        .recordStats()
        .build(new CacheLoader<Long, Event>() {
          @Override
          public Event load(Long eventId) throws Exception {
            try {
              Location location = new Location();
              JournalEventStorage.MetaData metaData = metaDataList.get(eventId);
              location.readExternal(ByteStreams.newDataInput(metaData.getLocation()));
              return Event.fromByteBuffer(eventJournal.read(location));
            } catch (IOException e) {
              throw new RuntimeException(e);
            }
          }
        });

    return new JournalEventStorage(
        eventJournal, eventCache, schema, metaDataList, metaDataList.getNumRecords());
  }

  @Provides
  public DmaList<BloomFilter> getBloomFilterDmaList(
      @Named("eventtracker.eventstorage.directory") String eventStorageDirectory,
      @Named("eventtracker.bloomfilteredeventstorage.numHashes") int numHashes,
      @Named("eventtracker.bloomfilteredeventstorage.bloomFilterSize") int bloomFilterSize,
      @Named("eventtracker.bloomfilteredeventstorage.numMetaDataPerFile") int numMetaDataPerFile,
      @Named("eventtracker.bloomfilteredeventstorage.metaDataFileCacheSize") int metaDataFileCacheSize) {
    return DmaList.build(new BloomFilteredEventStorage.Schema(numHashes, bloomFilterSize),
        eventStorageDirectory + "/bloom_filtered_event_storage_meta_data/",
        numMetaDataPerFile,
        metaDataFileCacheSize);
  }

  @Provides
  public BloomFilter getBloomFilter(
      @Named("eventtracker.bloomfilteredeventstorage.numHashes") int numHashes,
      @Named("eventtracker.bloomfilteredeventstorage.bloomFilterSize") int bloomFilterSize) {
    return BloomFilter.build(numHashes, bloomFilterSize);
  }

  @Provides
  public BloomFilteredEventStorage getBloomFilteredEventStorage(
      JournalEventStorage journalEventStorage,
      DmaList<BloomFilter> bloomFilterDmaList,
      Provider<BloomFilter> bloomFilterProvider) {
    return new BloomFilteredEventStorage(journalEventStorage, bloomFilterDmaList,
        bloomFilterProvider);
  }
}
