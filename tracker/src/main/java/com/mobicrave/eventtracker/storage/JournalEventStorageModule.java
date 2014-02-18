package com.mobicrave.eventtracker.storage;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.io.ByteStreams;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.mobicrave.eventtracker.list.DmaList;
import com.mobicrave.eventtracker.model.Event;
import org.fusesource.hawtjournal.api.Journal;
import org.fusesource.hawtjournal.api.Location;

import javax.inject.Named;
import java.io.IOException;

public class JournalEventStorageModule extends AbstractModule {
  @Override
  protected void configure() {}

  @Provides
  @Named("eventtracker.journaleventstorage.directory")
  public String getJournalUserStorageDirectory(@Named("eventtracker.directory") String directory) {
    return directory + "/event_storage/";
  }

  @Provides
  public JournalEventStorage.MetaData.Schema getJournalEventStorageMetadataSchema(
      @Named("eventtracker.journaleventstorage.metadata.numHashes") int numHashes,
      @Named("eventtracker.journaleventstorage.metadata.bloomFilterSize") int bloomFilterSize) {
    return new JournalEventStorage.MetaData.Schema(numHashes, bloomFilterSize);
  }

  @Provides
  public DmaList<JournalEventStorage.MetaData> getJournalEventStorageMetaDataList(
      JournalEventStorage.MetaData.Schema schema,
      @Named("eventtracker.journaleventstorage.directory") String journalEventStorageDirectory,
      @Named("eventtracker.journaleventstorage.numMetaDataPerFile") int numMetaDataPerFile,
      @Named("eventtracker.journaleventstorage.metaDataCacheSize") int metaDataCacheSize) {
    return DmaList.build(schema, journalEventStorageDirectory + "/meta_data/", numMetaDataPerFile,
        metaDataCacheSize);
  }

  @Provides
  @Named("eventtracker.journaleventstorage")
  public Journal getEventJournal(
      @Named("eventtracker.journaleventstorage.directory") String journalEventStorageDirectory,
      @Named("eventtracker.journaleventstorage.journalFileSize") int journalFileSize,
      @Named("eventtracker.journaleventstorage.journalWriteBatchSize") int journalWriteBatchSize) {
    return JournalUtil.createJournal(journalEventStorageDirectory + "/event_journal/",
        journalFileSize, journalWriteBatchSize);
  }

  @Provides
  public JournalEventStorage getJournalEventStorage(
      @Named("eventtracker.journaleventstorage.directory") String journalEventStorageDirectory,
      @Named("eventtracker.journaleventstorage.recordCacheSize") int recordCacheSize,
      @Named("eventtracker.journaleventstorage.metadata.numHashes") int numHashes,
      @Named("eventtracker.journaleventstorage.metadata.bloomFilterSize") int bloomFilterSize,
      final @Named("eventtracker.journaleventstorage") Journal eventJournal,
      final DmaList<JournalEventStorage.MetaData> metaDataList) {
    final JournalEventStorage.MetaData.Schema schema = new JournalEventStorage.MetaData.Schema(
        numHashes, bloomFilterSize);
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

    return new JournalEventStorage(journalEventStorageDirectory, numHashes, bloomFilterSize,
        eventJournal, eventCache, schema, metaDataList, metaDataList.getNumRecords());
  }


}
