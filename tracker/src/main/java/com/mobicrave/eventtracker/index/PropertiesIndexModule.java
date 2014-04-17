package com.mobicrave.eventtracker.index;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.mobicrave.eventtracker.base.DB;
import org.fusesource.leveldbjni.JniDBFactory;
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
    return new PropertiesIndex(new DB(
        JniDBFactory.factory.open(new File(eventIndexDirectory + "/properties_index.db"), options)));
  }
}
