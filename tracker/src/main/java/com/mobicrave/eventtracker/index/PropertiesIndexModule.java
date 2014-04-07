package com.mobicrave.eventtracker.index;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import org.fusesource.leveldbjni.JniDBFactory;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.Options;

import javax.inject.Named;
import java.io.File;
import java.io.IOException;

public class PropertiesIndexModule extends AbstractModule {
  @Override
  protected void configure() {}

  @Provides
  public PropertiesIndex getPropertiesIndex(
      @Named("eventtracker.directory") String eventIndexDirectory) throws IOException {
    //noinspection ResultOfMethodCallIgnored
    new File(eventIndexDirectory).mkdirs();
    Options options = new Options();
    options.createIfMissing(true);
    DB keysDb = JniDBFactory.factory.open(new File(eventIndexDirectory + "/properties_index_keys.db"), options);
    DB valuesDb = JniDBFactory.factory.open(new File(eventIndexDirectory + "/properties_index_values.db"), options);
    return new PropertiesIndex(keysDb, valuesDb);
  }
}
