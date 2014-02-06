package com.mobicrave.eventtracker.storage;

import com.google.common.io.ByteStreams;
import com.mobicrave.eventtracker.Criterion;
import com.mobicrave.eventtracker.base.BloomFilter;
import com.mobicrave.eventtracker.base.KeyValueCallback;
import com.mobicrave.eventtracker.base.Schema;
import com.mobicrave.eventtracker.list.DmaList;
import com.mobicrave.eventtracker.model.Event;
import org.fusesource.hawtjournal.api.Journal;
import org.fusesource.hawtjournal.api.Location;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.BitSet;
import java.util.List;

public class JournalEventStorage implements EventStorage {
  private final String directory;
  private final Journal eventJournal;
  private final DmaList<MetaData> metaDataList;
  private long currentId;
  private long numConditionCheck;
  private long numBloomFilterRejection;

  private JournalEventStorage(String directory, Journal eventJournal, DmaList<MetaData> metaDataList,
      long currentId) {
    this.directory = directory;
    this.eventJournal = eventJournal;
    this.metaDataList = metaDataList;
    this.currentId = currentId;
    this.numConditionCheck = 0;
    this.numBloomFilterRejection = 0;
  }

  @Override
  public long addEvent(Event event, int userId, int eventTypeId) {
    try {
      long id = currentId++;
      byte[] location = JournalUtil.locationToBytes(eventJournal.write(event.toByteBuffer(), true));
      final BloomFilter bloomFilter = BloomFilter.build(MetaData.NUM_HASHES, MetaData.BLOOM_FILTER_SIZE);
      event.enumerate(new KeyValueCallback() {
        @Override
        public void callback(String key, String value) {
          bloomFilter.add(getBloomFilterKey(key, value));
        }
      });
      MetaData metaData = new MetaData(userId, eventTypeId, bloomFilter,location);
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
  public int getUserId(long eventId) {
    return getEventMetaData(eventId).getUserId();
  }

  @Override
  public boolean satisfy(long eventId, List<Criterion> criteria) {
    if (criteria.isEmpty()) {
      return true;
    }
    numConditionCheck++;

    BloomFilter bloomFilter = getEventMetaData(eventId).getBloomFilter();
    for (Criterion criterion : criteria) {
      String bloomFilterKey = getBloomFilterKey(criterion.getKey(), criterion.getValue());
      if (!bloomFilter.isPresent(bloomFilterKey)) {
        numBloomFilterRejection++;
        return false;
      }
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
  public String getVarz() {
    return String.format(
        "current id: %d\n" +
        "num condition check: %d\n" +
        "num bloomfilter rejection: %d\n" +
        "directory: %s\n",
        currentId, numConditionCheck, numBloomFilterRejection, directory);
  }

  private MetaData getEventMetaData(long eventId) {
    return metaDataList.get(eventId);
  }

  private static String getBloomFilterKey(String key, String value) {
    return key + value;
  }

  private static String getMetaDataDirectory(String directory) {
    return directory + "/meta_data/";
  }

  private static String getJournalDirectory(String directory) {
    return directory + "/event_journal/";
  }

  public static JournalEventStorage build(String directory) {
    Journal eventJournal = JournalUtil.createJournal(getJournalDirectory(directory));
    DmaList<MetaData> metaDataList = DmaList.build(MetaData.getSchema(),
        getMetaDataDirectory(directory), 10 * 1024 * 1024 /* numRecordsPerFile */);
    return new JournalEventStorage(directory, eventJournal, metaDataList, metaDataList.getNumRecords());
  }

  private static class MetaData {
    private static final int BLOOM_FILTER_SIZE = 64; // in bytes
    private static final int NUM_HASHES = 5;

    private final int userId;
    private final int eventTypeId;
    private final BloomFilter bloomFilter;
    private final byte[] location;

    public MetaData(int userId, int eventTypeId, BloomFilter bloomFilter, byte[] location) {
      this.userId = userId;
      this.eventTypeId = eventTypeId;
      this.bloomFilter = bloomFilter;
      this.location = location;
    }

    public int getUserId() {
      return userId;
    }

    public int getEventTypeId() {
      return eventTypeId;
    }

    public BloomFilter getBloomFilter() {
      return bloomFilter;
    }

    public byte[] getLocation() {
      return location;
    }

    public static Schema<MetaData> getSchema() {
      return new MetaDataSchema();
    }

    private static class MetaDataSchema implements Schema<MetaData> {
      private static final int LOCATION_SIZE = 13; // in bytes

      @Override
      public int getObjectSize() {
        return 8 /* userId + eventTypeId */ + LOCATION_SIZE + BLOOM_FILTER_SIZE;
      }

      @Override
      public byte[] toBytes(MetaData metaData) {
        ByteBuffer byteBuffer = ByteBuffer.allocate(getObjectSize());
        byteBuffer.putInt(metaData.userId)
            .putInt(metaData.eventTypeId)
            .put(metaData.bloomFilter.getBitSet().toByteArray())
            .put(metaData.location);
        return byteBuffer.array();
      }

      @Override
      public MetaData fromBytes(byte[] bytes) {
        ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
        int userId = byteBuffer.getInt();
        int eventTypeId = byteBuffer.getInt();
        byte[] bloomFilter = new byte[BLOOM_FILTER_SIZE];
        byteBuffer.get(bloomFilter);
        byte[] location = new byte[LOCATION_SIZE];
        byteBuffer.get(location);
        return new MetaData(userId, eventTypeId,
            new BloomFilter(MetaData.NUM_HASHES, BitSet.valueOf(bloomFilter)), location);
      }
    }
  }
}
