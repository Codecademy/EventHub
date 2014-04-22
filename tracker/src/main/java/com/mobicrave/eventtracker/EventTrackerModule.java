package com.mobicrave.eventtracker;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.name.Names;
import com.mobicrave.eventtracker.index.DatedEventIndex;
import com.mobicrave.eventtracker.index.PropertiesIndex;
import com.mobicrave.eventtracker.index.ShardedEventIndex;
import com.mobicrave.eventtracker.index.UserEventIndex;
import com.mobicrave.eventtracker.storage.BloomFilteredEventStorage;
import com.mobicrave.eventtracker.storage.BloomFilteredUserStorage;

import javax.inject.Named;
import java.io.File;
import java.util.Properties;

public class EventTrackerModule extends AbstractModule {
  private final Properties properties;

  public EventTrackerModule(Properties properties) {
    this.properties = properties;
  }

  @Override
  protected void configure() {
    Names.bindProperties(super.binder(), properties);
    new File(properties.getProperty("eventtracker.directory")).mkdirs();
  }

  @Provides
  public EventTracker getEventTracker(
      @Named("eventtracker.directory") String directory,
      ShardedEventIndex shardedEventIndex,
      DatedEventIndex datedEventIndex,
      PropertiesIndex propertiesIndex,
      UserEventIndex userEventIndex,
      BloomFilteredEventStorage eventStorage,
      BloomFilteredUserStorage userStorage) {
    return new EventTracker(directory, shardedEventIndex, datedEventIndex, propertiesIndex,
        userEventIndex, eventStorage, userStorage);
  }
}
