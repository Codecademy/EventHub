package com.mobicrave.eventtracker.storage;

import com.google.common.io.ByteStreams;
import com.mobicrave.eventtracker.list.DmaList;
import com.mobicrave.eventtracker.model.Event;
import org.fusesource.hawtjournal.api.Journal;
import org.fusesource.hawtjournal.api.Location;

import java.io.IOException;

public class JournalEventStorage implements EventStorage {
  private final Journal eventJournal;
  private final DmaList<Event.MetaData> metaDataList;
  private long currentId;

  private JournalEventStorage(Journal eventJournal, DmaList<Event.MetaData> metaDataList,
      long currentId) {
    this.eventJournal = eventJournal;
    this.metaDataList = metaDataList;
    this.currentId = currentId;
  }

  @Override
  public synchronized long addEvent(Event event, long userId, int eventTypeId) {
    try {
      long id = currentId++;
      byte[] location = JournalUtil.locationToBytes(eventJournal.write(event.toByteBuffer(), true));
      Event.MetaData metaData = event.getMetaData(userId, eventTypeId, location);
      metaDataList.add(metaData);
      return id;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public Event.MetaData getEventMetaData(long eventId) {
    return metaDataList.get(eventId);
  }

  @Override
  public Event getEvent(long eventId) {
    try {
      Location location = new Location();
      location.readExternal(ByteStreams.newDataInput(getEventMetaData(eventId).getLocation()));
      return Event.fromByteBuffer(eventJournal.read(location));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void close() throws IOException {
    eventJournal.close();
    metaDataList.close();
  }

  private static String getMetaDataSerializationFile(String directory) {
    return directory + "/meta_data_list.mem";
  }

  private static String getJournalDirectory(String directory) {
    return directory + "/event_journal/";
  }

  public static JournalEventStorage build(String directory) {
    Journal eventJournal = JournalUtil.createJournal(getJournalDirectory(directory));
    DmaList<Event.MetaData> metaDataList = DmaList.build(Event.MetaData.getSchema(),
        getMetaDataSerializationFile(directory), 1024 * 1024 /* defaultCapacity */);
    return new JournalEventStorage(eventJournal, metaDataList, metaDataList.getNumRecords());
  }
}
