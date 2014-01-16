package com.mobicrave.eventtracker.storage;

import com.google.common.io.ByteStreams;
import com.mobicrave.eventtracker.base.Schema;
import com.mobicrave.eventtracker.list.DmaList;
import com.mobicrave.eventtracker.model.Event;
import org.fusesource.hawtjournal.api.Journal;
import org.fusesource.hawtjournal.api.Location;

import java.io.IOException;
import java.nio.ByteBuffer;

public class JournalEventStorage implements EventStorage {
  private final Journal eventJournal;
  private final DmaList<MetaData> metaDataList;
  private long currentId;

  private JournalEventStorage(Journal eventJournal, DmaList<MetaData> metaDataList,
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
      MetaData metaData = new MetaData(userId, eventTypeId, location);
      metaDataList.add(metaData);
      return id;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
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
  public int getEventTypeId(long eventId) {
    return getEventMetaData(eventId).getEventTypeId();
  }

  @Override
  public long getUserId(long eventId) {
    return getEventMetaData(eventId).getUserId();
  }

  @Override
  public void close() throws IOException {
    eventJournal.close();
    metaDataList.close();
  }

  private MetaData getEventMetaData(long eventId) {
    return metaDataList.get(eventId);
  }

  private static String getMetaDataSerializationFile(String directory) {
    return directory + "/meta_data_list.mem";
  }

  private static String getJournalDirectory(String directory) {
    return directory + "/event_journal/";
  }

  public static JournalEventStorage build(String directory) {
    Journal eventJournal = JournalUtil.createJournal(getJournalDirectory(directory));
    DmaList<MetaData> metaDataList = DmaList.build(MetaData.getSchema(),
        getMetaDataSerializationFile(directory), 1024 * 1024 /* defaultCapacity */);
    return new JournalEventStorage(eventJournal, metaDataList, metaDataList.getNumRecords());
  }

  private static class MetaData {
    private final long userId;
    private final byte[] location;
    private final int eventTypeId;

    public MetaData(long userId, int eventTypeId, byte[] location) {
      this.userId = userId;
      this.eventTypeId = eventTypeId;
      this.location = location;
    }

    public long getUserId() {
      return userId;
    }

    public int getEventTypeId() {
      return eventTypeId;
    }

    public byte[] getLocation() {
      return location;
    }

    public static Schema<MetaData> getSchema() {
      return new MetaDataSchema();
    }

    private static class MetaDataSchema implements Schema<MetaData> {
      @Override
      public int getObjectSize() {
        return 8 + 13 + 4;
      }

      @Override
      public byte[] toBytes(MetaData metaData) {
        ByteBuffer byteBuffer = ByteBuffer.allocate(getObjectSize());
        byteBuffer.putLong(metaData.userId)
            .putInt(metaData.eventTypeId)
            .put(metaData.location);
        return byteBuffer.array();
      }

      @Override
      public MetaData fromBytes(byte[] bytes) {
        ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
        long userId = byteBuffer.getLong();
        int eventTypeId = byteBuffer.getInt();
        byte[] location = new byte[13];
        byteBuffer.get(location);
        return new MetaData(userId, eventTypeId, location);
      }
    }
  }
}
