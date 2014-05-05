package com.codecademy.eventhub;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.name.Names;
import com.codecademy.eventhub.index.DatedEventIndex;
import com.codecademy.eventhub.index.PropertiesIndex;
import com.codecademy.eventhub.index.ShardedEventIndex;
import com.codecademy.eventhub.index.UserEventIndex;
import com.codecademy.eventhub.storage.BloomFilteredEventStorage;
import com.codecademy.eventhub.storage.BloomFilteredUserStorage;

import javax.inject.Named;
import java.io.File;
import java.util.Properties;

public class EventHubModule extends AbstractModule {
  private final Properties properties;

  public EventHubModule(Properties properties) {
    this.properties = properties;
  }

  @Override
  protected void configure() {
    Names.bindProperties(super.binder(), properties);
    new File(properties.getProperty("eventhub.directory")).mkdirs();
  }

  @Provides
  public EventHub getEventHub(
      @Named("eventhub.directory") String directory,
      ShardedEventIndex shardedEventIndex,
      DatedEventIndex datedEventIndex,
      PropertiesIndex propertiesIndex,
      UserEventIndex userEventIndex,
      BloomFilteredEventStorage eventStorage,
      BloomFilteredUserStorage userStorage) {
    return new EventHub(directory, shardedEventIndex, datedEventIndex, propertiesIndex,
        userEventIndex, eventStorage, userStorage);
  }
}
