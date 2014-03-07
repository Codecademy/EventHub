package com.mobicrave.eventtracker.storage;

import com.google.common.io.ByteStreams;
import com.mobicrave.eventtracker.Criterion;
import com.mobicrave.eventtracker.list.DmaList;
import com.mobicrave.eventtracker.model.Event;
import org.fusesource.hawtjournal.api.Journal;
import org.fusesource.hawtjournal.api.Location;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;

public class JournalEventStorage implements EventStorage {
  private final Journal eventJournal;
  private final MetaData.Schema schema;
  private final DmaList<MetaData> metaDataList;
  private long currentId;

  public JournalEventStorage(Journal eventJournal, MetaData.Schema schema,
      DmaList<MetaData> metaDataList, long currentId) {
    this.eventJournal = eventJournal;
    this.schema = schema;
    this.metaDataList = metaDataList;
    this.currentId = currentId;
  }

  @Override
  public long addEvent(Event event, int userId, int eventTypeId) {
    try {
      long id = currentId++;
      byte[] location = JournalUtil.locationToBytes(eventJournal.write(event.toByteBuffer(), true));
      MetaData metaData = new MetaData(userId, eventTypeId,location);
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
      JournalEventStorage.MetaData metaData = metaDataList.get(eventId);
      location.readExternal(ByteStreams.newDataInput(metaData.getLocation()));
      return Event.fromByteBuffer(eventJournal.read(location));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public int getEventTypeId(long eventId) {
    return schema.getEventTypeId(metaDataList.getBytes(eventId));
  }

  @Override
  public int getUserId(long eventId) {
    return schema.getUserId(metaDataList.getBytes(eventId));
  }

  @Override
  public boolean satisfy(long eventId, List<Criterion> criteria) {
    if (criteria.isEmpty()) {
      return true;
    }

    Event event = getEvent(eventId);
    for (Criterion criterion : criteria) {
      if (!criterion.getValue().equals(event.get(criterion.getKey()))) {
        return false;
      }
    }
    return true;
  }

  @Override
  public void close() throws IOException {
    eventJournal.close();
    metaDataList.close();
  }

  @Override
  public String getVarz(int indentation) {
    String indent  = new String(new char[indentation]).replace('\0', ' ');
    return String.format(
        indent + this.getClass().getName() + "\n" +
        indent + "==================\n" +
        indent + "current id: %d\n" +
        indent + "metaDataList:\n%s",
        currentId, metaDataList.getVarz(indentation + 1));
  }

  public static class MetaData {
    private final int userId;
    private final int eventTypeId;
    private final byte[] location;

    public MetaData(int userId, int eventTypeId, byte[] location) {
      this.userId = userId;
      this.eventTypeId = eventTypeId;
      this.location = location;
    }

    public byte[] getLocation() {
      return location;
    }

    public static class Schema implements com.mobicrave.eventtracker.base.Schema<MetaData> {
      private static final int LOCATION_SIZE = 13; // in bytes

      @Override
      public int getObjectSize() {
        return 8 /* userId + eventTypeId */ + LOCATION_SIZE;
      }

      @Override
      public byte[] toBytes(MetaData metaData) {
        ByteBuffer byteBuffer = ByteBuffer.allocate(getObjectSize());
        byteBuffer.putInt(metaData.userId)
            .putInt(metaData.eventTypeId)
            .put(metaData.location);
        return byteBuffer.array();
      }

      @Override
      public MetaData fromBytes(byte[] bytes) {
        ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
        int userId = byteBuffer.getInt();
        int eventTypeId = byteBuffer.getInt();
        byte[] location = new byte[LOCATION_SIZE];
        byteBuffer.get(location);
        return new MetaData(userId, eventTypeId, location);
      }

      public int getEventTypeId(byte[] bytes) {
        ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
        return byteBuffer.getInt(4 /* the first 4 bytes are userId*/);
      }

      public int getUserId(byte[] bytes) {
        ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
        return byteBuffer.getInt();
      }
    }
  }
}
