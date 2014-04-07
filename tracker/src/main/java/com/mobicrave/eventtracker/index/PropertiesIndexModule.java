package com.mobicrave.eventtracker.index;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;

import javax.inject.Named;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

public class PropertiesIndexModule extends AbstractModule {
  @Override
  protected void configure() {}

  @Provides
  @Named("eventtracker.propertiesindex.filename")
  public String getDatedEventIndexFile(
      @Named("eventtracker.directory") String eventIndexDirectory) {
    return eventIndexDirectory + "/properties_index.ser";
  }

  @Provides
  public PropertiesIndex getDatedEventIndex(
      @Named("eventtracker.propertiesindex.filename") String propertiesIndexFilename) {
    // TODO: don't show keys if the values exceed quota
    File file = new File(propertiesIndexFilename);
    if (file.exists()) {
      try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
        @SuppressWarnings("unchecked")
        ConcurrentHashMap<String, ConcurrentSkipListSet<String>> keysMap =
            (ConcurrentHashMap<String, ConcurrentSkipListSet<String>>) ois.readObject();
        @SuppressWarnings("unchecked")
        ConcurrentHashMap<String, ConcurrentSkipListSet<String>> valuesMap =
            (ConcurrentHashMap<String, ConcurrentSkipListSet<String>>) ois.readObject();
        return new PropertiesIndex(propertiesIndexFilename, keysMap, valuesMap);
      } catch (IOException | ClassNotFoundException e) {
        throw new RuntimeException(e);
      }
    }
    return new PropertiesIndex(propertiesIndexFilename,
        new ConcurrentHashMap<String, ConcurrentSkipListSet<String>>(),
        new ConcurrentHashMap<String, ConcurrentSkipListSet<String>>());
  }
}
