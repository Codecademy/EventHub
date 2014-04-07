package com.mobicrave.eventtracker.index;

import com.google.common.collect.Lists;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;

import javax.inject.Named;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.List;

public class DatedEventIndexModule extends AbstractModule {
  @Override
  protected void configure() {}

  @Provides
  @Named("eventtracker.datedeventindex.filename")
  public String getDatedEventIndexFile(
      @Named("eventtracker.directory") String eventIndexDirectory) {
    return eventIndexDirectory + "/dated_event_index.ser";
  }

  @Provides
  public DatedEventIndex getDatedEventIndex(
      @Named("eventtracker.datedeventindex.filename") String datedEventIndexFilename) {
    File file = new File(datedEventIndexFilename);
    if (file.exists()) {
      try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
        @SuppressWarnings("unchecked")
        List<String> dates = (List<String>) ois.readObject();
        @SuppressWarnings("unchecked")
        List<Long> earliestEventIds = (List<Long>) ois.readObject();
        String currentDate = (String) ois.readObject();
        return new DatedEventIndex(datedEventIndexFilename, dates, earliestEventIds, currentDate);
      } catch (IOException | ClassNotFoundException e) {
        throw new RuntimeException(e);
      }
    }
    return new DatedEventIndex(datedEventIndexFilename, Lists.<String>newArrayList(),
        Lists.<Long>newArrayList(), null);
  }
}
