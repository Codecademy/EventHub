package com.mobicrave.eventtracker.index;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.mobicrave.eventtracker.list.DmaIdList;
import com.mobicrave.eventtracker.list.IdList;

import javax.inject.Named;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;

public class ShardedEventIndexModule extends AbstractModule {
  @Override
  protected void configure() {}

  @Provides
  @Named("eventtracker.shardedeventindex.directory")
  public String getEventIndexDirectory(
      @Named("eventtracker.directory") String eventTrackerDirectory) {
    return eventTrackerDirectory + "/event_index/";
  }

  @Provides
  @Named("eventtracker.shardedeventindex.filename")
  public String getEventIndexFile(
      @Named("eventtracker.shardedeventindex.directory") String eventIndexDirectory) {
    return eventIndexDirectory + "/event_index.ser";
  }

  @Provides
  @Named("eventtracker.shardedeventindex.datedeventindex.filename")
  public String getDatedEventIndexFile(
      @Named("eventtracker.shardedeventindex.directory") String eventIndexDirectory) {
    return eventIndexDirectory + "/dated_event_index.ser";
  }

  @Provides
  public DatedEventIndex getDatedEventIndex(
      @Named("eventtracker.shardedeventindex.datedeventindex.filename") String datedEventIndexFilename) {
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

  @Provides
  public ShardedEventIndex getEventIndex(
      @Named("eventtracker.shardedeventindex.filename") String eventIndexFilename,
      EventIndex.Factory individualEventIndexFactory,
      DatedEventIndex datedEventIndex) {
    File file = new File(eventIndexFilename);
    if (file.exists()) {
      try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
        @SuppressWarnings("unchecked")
        List<String> eventTypes = (List<String>) ois.readObject();
        @SuppressWarnings("unchecked")
        Map<String, Integer> eventTypeIdMap = (Map<String, Integer>) ois.readObject();
        Map<String, EventIndex> eventIndexMap = Maps.newHashMap();
        for (String eventType : eventTypes) {
          eventIndexMap.put(eventType, individualEventIndexFactory.build(eventType));
        }
        return new ShardedEventIndex(eventIndexFilename, individualEventIndexFactory, eventIndexMap,
            eventTypeIdMap, datedEventIndex);
      } catch (IOException | ClassNotFoundException e) {
        throw new RuntimeException(e);
      }
    }
    return new ShardedEventIndex(eventIndexFilename, individualEventIndexFactory,
        Maps.<String,EventIndex>newHashMap(), Maps.<String, Integer>newHashMap(), datedEventIndex);
  }

  @Provides
  public EventIndex.Factory getIndividualEventIndexFactory(
      final @Named("eventtracker.shardedeventindex.directory") String eventIndexDirectory,
      final @Named("eventtracker.eventindex.initialNumEventIdsPerDay") int initialNumEventIdsPerDay,
      final DmaIdList.Factory dmaIdListFactory) {
    dmaIdListFactory.setDefaultCapacity(initialNumEventIdsPerDay);
    return new EventIndex.Factory() {
      @Override
      public EventIndex build(String eventType) {
        String individualEventIndexDirectory =
            String.format("%s/%s/", eventIndexDirectory, eventType);
        File file = new File(
            EventIndex.getFilename(
                individualEventIndexDirectory));
        if (file.exists()) {
          try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            @SuppressWarnings("unchecked")
            List<String> dates = (List<String>) ois.readObject();
            SortedMap<String, IdList> eventIdListMap = Maps.newTreeMap();
            for (String date : dates) {
              eventIdListMap.put(date, dmaIdListFactory.build(
                  EventIndex.getEventIdListFilename(
                      individualEventIndexDirectory, date)));
            }
            return new EventIndex(individualEventIndexDirectory,
                dmaIdListFactory, eventIdListMap);
          } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
          }
        }
        return new EventIndex(individualEventIndexDirectory,
            dmaIdListFactory, Maps.<String, IdList>newTreeMap());
      }
    };
  }
}
