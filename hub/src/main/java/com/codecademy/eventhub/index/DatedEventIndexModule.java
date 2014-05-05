package com.codecademy.eventhub.index;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.codecademy.eventhub.base.DB;
import org.fusesource.leveldbjni.JniDBFactory;
import org.iq80.leveldb.Options;

import javax.inject.Named;
import java.io.File;
import java.io.IOException;

public class DatedEventIndexModule extends AbstractModule {
  @Override
  protected void configure() {}

  @Provides
  @Named("eventhub.datedeventindex.filename")
  public String getDatedEventIndexFile(
      @Named("eventhub.directory") String eventIndexDirectory) {
    return eventIndexDirectory + "/dated_event_index.db";
  }

  @Provides
  public DatedEventIndex getDatedEventIndex(
      @Named("eventhub.directory") String eventIndexDirectory) throws IOException {
    Options options = new Options();
    options.createIfMissing(true);
    DB db = new DB(
        JniDBFactory.factory.open(new File(eventIndexDirectory + "/dated_event_index.db"), options));

    return DatedEventIndex.create(db);
  }
}
