package com.mobicrave.eventtracker;

import com.google.common.io.ByteStreams;
import org.fusesource.hawtjournal.api.Journal;
import org.fusesource.hawtjournal.api.Location;

import java.io.IOException;

public class JournalEventStorage implements EventStorage {
  private final Journal eventJournal;
  private final MemMappedList<Event.MetaData> metaDataList;
  private long currentId;

  private JournalEventStorage(Journal eventJournal, MemMappedList<Event.MetaData> metaDataList,
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
      metaDataList.write(metaData);
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
  public void close() {
    try {
      eventJournal.close();
      metaDataList.close();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static JournalEventStorage build(String dataDir) {
    Journal eventJournal = JournalUtil.createJournal(dataDir + "/event_journal/");
    MemMappedList<Event.MetaData> metaDataList = MemMappedList.build(Event.MetaData.getSchema(),
        dataDir + "/meta_data_list.mem", 1024 * 1024 /* defaultCapacity */);
    return new JournalEventStorage(eventJournal, metaDataList, metaDataList.getNumRecords());
  }
}
