package com.mobicrave.eventtracker.storage;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Maps;
import com.google.common.io.ByteStreams;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.mobicrave.eventtracker.list.DmaList;
import com.mobicrave.eventtracker.model.User;
import org.fusesource.hawtjournal.api.Journal;
import org.fusesource.hawtjournal.api.Location;

import javax.inject.Named;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Map;

public class JournalUserStorageModule extends AbstractModule {
  @Override
  protected void configure() {}

  @Provides
  @Named("eventtracker.journaluserstorage.directory")
  public String getJournalUserStorageDirectory(@Named("eventtracker.directory") String directory) {
    return directory + "/user_storage/";
  }

  @Provides
  public JournalUserStorage.MetaData.Schema getJournalUserStorageMetadataSchema(
      @Named("eventtracker.journaluserstorage.metadata.numHashes") int numHashes,
      @Named("eventtracker.journaluserstorage.metadata.bloomFilterSize") int bloomFilterSize) {
    return new JournalUserStorage.MetaData.Schema(numHashes, bloomFilterSize);
  }

  @Provides
  public DmaList<JournalUserStorage.MetaData> getJournalUserStorageMetaDataList(
      JournalUserStorage.MetaData.Schema schema,
      @Named("eventtracker.journaluserstorage.directory") String journalUserStorageDirectory,
      @Named("eventtracker.journaluserstorage.numMetaDataPerFile") int numMetaDataPerFile,
      @Named("eventtracker.journaluserstorage.metaDataFileCacheSize") int metaDataFileCacheSize) {
    return DmaList.build(schema, journalUserStorageDirectory + "/meta_data/", numMetaDataPerFile,
        metaDataFileCacheSize);
  }

  @Provides
  @Named("eventtracker.journaluserstorage")
  public Journal getUserJournal(
      @Named("eventtracker.journaluserstorage.directory") String journalUserStorageDirectory,
      @Named("eventtracker.journaluserstorage.journalFileSize") int journalFileSize,
      @Named("eventtracker.journaluserstorage.journalWriteBatchSize") int journalWriteBatchSize) {
    return JournalUtil.createJournal(journalUserStorageDirectory + "/user_journal/",
        journalFileSize, journalWriteBatchSize);
  }

  @Provides
  public IdMap getIdMap(@Named("eventtracker.journaluserstorage.directory") String directory) {
    String filename = directory + "/id_map.ser";
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
  public JournalUserStorage getUserStorage(
      @Named("eventtracker.journaluserstorage.recordCacheSize") int recordCacheSize,
      @Named("eventtracker.journaluserstorage.metadata.numHashes") int numHashes,
      @Named("eventtracker.journaluserstorage.metadata.bloomFilterSize") int bloomFilterSize,
      final @Named("eventtracker.journaluserstorage") Journal userJournal,
      final DmaList<JournalUserStorage.MetaData> metaDataList,
      IdMap idMap) {
    LoadingCache<Integer, User> userCache = CacheBuilder.newBuilder()
        .maximumSize(recordCacheSize)
        .recordStats()
        .build(new CacheLoader<Integer, User>() {
          @Override
          public User load(Integer userId) throws Exception {
            try {
              Location location = new Location();
              JournalUserStorage.MetaData metaData = metaDataList.get(userId);
              location.readExternal(ByteStreams.newDataInput(metaData.getLocation()));
              return User.fromByteBuffer(userJournal.read(location));
            } catch (IOException e) {
              throw new RuntimeException(e);
            }
          }
        });

    return new JournalUserStorage(numHashes, bloomFilterSize,
        userJournal, userCache, metaDataList, idMap);
  }
}
