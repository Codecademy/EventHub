package com.mobicrave.eventtracker.index;

import com.google.common.cache.*;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.mobicrave.eventtracker.list.DmaIdList;
import com.mobicrave.eventtracker.list.IdList;

import javax.inject.Named;
import java.io.IOException;

public class UserEventIndexModule extends AbstractModule {
  @Override
  protected void configure() {}

  @Provides
  @Named("eventtracker.usereventindex.directory")
  public String getUserEventIndexDirectory(@Named("eventtracker.directory") String directory) {
    return directory + "/user_event_index/";
  }

  @Provides
  public UserEventIndex getUserEventIndex(
      final @Named("eventtracker.usereventindex.directory") String userEventIndexDirectory,
      final @Named("eventtracker.usereventindex.numFilesPerDir") int numFilesPerDir,
      final @Named("eventtracker.usereventindex.initialNumEventIdsPerUserDay") int initialNumEventIdsPerUserDay,
      final @Named("eventtracker.usereventindex.metaDataCacheSize") int metaDataCacheSize,
      final DmaIdList.Factory dmaIdListFactory) {
    dmaIdListFactory.setDefaultCapacity(initialNumEventIdsPerUserDay);
    LoadingCache<Integer, IdList> index = CacheBuilder.newBuilder()
        .maximumSize(metaDataCacheSize)
        .recordStats()
        .removalListener(new RemovalListener<Integer, IdList>() {
          @Override
          public void onRemoval(RemovalNotification<Integer, IdList> notification) {
            try {
              //noinspection ConstantConditions
              notification.getValue().close();
            } catch (IOException e) {
              throw new RuntimeException(e);
            }}
        })
        .build(new CacheLoader<Integer, IdList>() {
          @Override
          public IdList load(Integer key) throws Exception {
            int id = key;
            StringBuilder filenameBuilder = new StringBuilder(userEventIndexDirectory);
            while (id >= numFilesPerDir) {
              filenameBuilder.append('/').append(String.format("%02d", id % numFilesPerDir));
              id /= numFilesPerDir;
            }
            String filename = filenameBuilder.append('/').append(id).append(".ser").toString();

            return dmaIdListFactory.build(filename);
          }
        });
    return new UserEventIndex(index);
  }
}
