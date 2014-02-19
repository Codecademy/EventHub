package com.mobicrave.eventtracker;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.name.Names;
import com.mobicrave.eventtracker.index.ShardedEventIndex;
import com.mobicrave.eventtracker.index.UserEventIndex;
import com.mobicrave.eventtracker.storage.BloomFilteredEventStorage;
import com.mobicrave.eventtracker.storage.JournalUserStorage;

import javax.inject.Named;
import java.io.IOException;
import java.util.Properties;

public class EventTrackerModule extends AbstractModule {
  private final Properties properties;

  public EventTrackerModule(Properties properties) {
    this.properties = properties;
  }

  @Override
  protected void configure() {
    Names.bindProperties(super.binder(), properties);
  }

  @Provides
  public EventTracker getEventTracker(
      @Named("eventtracker.directory") String directory,
      ShardedEventIndex shardedEventIndex,
      UserEventIndex userEventIndex,
      BloomFilteredEventStorage eventStorage,
      JournalUserStorage userStorage) {
    return new EventTracker(directory, shardedEventIndex, userEventIndex, eventStorage, userStorage);
  }
}
