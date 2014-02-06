package com.mobicrave.eventtracker.storage;

import com.google.common.collect.Maps;
import com.google.common.io.ByteStreams;
import com.mobicrave.eventtracker.Criterion;
import com.mobicrave.eventtracker.base.BloomFilter;
import com.mobicrave.eventtracker.base.KeyValueCallback;
import com.mobicrave.eventtracker.base.Schema;
import com.mobicrave.eventtracker.list.DmaList;
import com.mobicrave.eventtracker.model.User;
import org.fusesource.hawtjournal.api.Journal;
import org.fusesource.hawtjournal.api.Location;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;
import java.util.BitSet;
import java.util.List;
import java.util.Map;

public class JournalUserStorage implements UserStorage {
  private final String directory;
  private final Journal userJournal;
  private DmaList<MetaData> metaDataList;
  private final Map<String, Integer> idMap;
  private int currentId;
  private long numConditionCheck;
  private long numBloomFilterRejection;

  public JournalUserStorage(String directory, Journal userJournal, DmaList<MetaData> metaDataList,
      Map<String, Integer> idMap, int currentId) {
    this.directory = directory;
    this.userJournal = userJournal;
    this.metaDataList = metaDataList;
    this.idMap = idMap;
    this.currentId = currentId;
    this.numConditionCheck = 0;
    this.numBloomFilterRejection = 0;
  }

  @Override
  public synchronized int addUser(User user) {
    try {
      int id = currentId++;
      byte[] location = JournalUtil.locationToBytes(userJournal.write(user.toByteBuffer(), true));
      final BloomFilter bloomFilter = BloomFilter.build(MetaData.NUM_HASHES, MetaData.BLOOM_FILTER_SIZE);
      user.enumerate(new KeyValueCallback() {
        @Override
        public void callback(String key, String value) {
          bloomFilter.add(getBloomFilterKey(key, value));
        }
      });
      MetaData metaData = new MetaData(bloomFilter, location);
      metaDataList.add(metaData);
      idMap.put(user.getExternalId(), id);
      return id;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public int getId(String externalUserId) {
    Integer id = idMap.get(externalUserId);
    return id == null ? USER_NOT_FOUND : id;
  }

  @Override
  public User getUser(int userId) {
    try {
      Location location = new Location();
      location.readExternal(ByteStreams.newDataInput(getUserMetaData(userId).getLocation()));
      return User.fromByteBuffer(userJournal.read(location));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public boolean satisfy(int userId, List<Criterion> criteria) {
    if (criteria.isEmpty()) {
      return true;
    }
    numConditionCheck++;

    BloomFilter bloomFilter = getUserMetaData(userId).getBloomFilter();
    for (Criterion criterion : criteria) {
      String bloomFilterKey = getBloomFilterKey(criterion.getKey(), criterion.getValue());
      if (!bloomFilter.isPresent(bloomFilterKey)) {
        numBloomFilterRejection++;
        return false;
      }
    }

    User user = getUser(userId);
    for (Criterion criterion : criteria) {
      if (!criterion.getValue().equals(user.get(criterion.getKey()))) {
        return false;
      }
    }
    return true;
  }

  @Override
  public void close() throws IOException {
    //noinspection ResultOfMethodCallIgnored
    new File(directory).mkdirs();

    try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(
        getIdMapSerializationFile(directory)))) {
      oos.writeObject(idMap);
      oos.writeInt(currentId);
    }
    userJournal.close();
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

  private String getBloomFilterKey(String key, String value) {
    return key + value;
  }

  private MetaData getUserMetaData(int userId) {
    return metaDataList.get(userId);
  }

  private static String getMetaDataDirectory(String directory) {
    return directory + "/meta_data/";
  }

  private static String getJournalDirectory(String directory) {
    return directory + "/user_journal/";
  }

  private static String getIdMapSerializationFile(String directory) {
    return directory + "/id_map.ser";
  }

  public static JournalUserStorage build(String directory) {
    Journal userJournal = JournalUtil.createJournal(getJournalDirectory(directory));
    DmaList<MetaData> metaDataList = DmaList.build(MetaData.getSchema(),
        getMetaDataDirectory(directory), 1024 * 1024 /* numRecordsPerFile */);
    File file = new File(getIdMapSerializationFile(directory));
    Map<String,Integer> idMap = Maps.newConcurrentMap();
    int currentId = 0;
    if (file.exists()) {
      try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
        idMap = (Map<String, Integer>) ois.readObject();
        currentId = ois.readInt();
      } catch (ClassNotFoundException | IOException e) {
        throw new RuntimeException(e);
      }
    }
    return new JournalUserStorage(directory, userJournal, metaDataList, idMap,
        currentId);
  }

  private static class MetaData {
    public static final int BLOOM_FILTER_SIZE = 64; // in bytes
    public static final int NUM_HASHES = 5;

    private final BloomFilter bloomFilter;
    private final byte[] location;

    public MetaData(BloomFilter bloomFilter, byte[] location) {
      this.bloomFilter = bloomFilter;
      this.location = location;
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
        byteBuffer.put(metaData.getBloomFilter().getBitSet().toByteArray())
            .put(metaData.location);
        return byteBuffer.array();
      }

      @Override
      public MetaData fromBytes(byte[] bytes) {
        ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
        byte[] bloomFilter = new byte[BLOOM_FILTER_SIZE];
        byteBuffer.get(bloomFilter);
        byte[] location = new byte[LOCATION_SIZE];
        byteBuffer.get(location);
        return new MetaData(
            new BloomFilter(MetaData.NUM_HASHES, BitSet.valueOf(bloomFilter)), location);
      }
    }
  }
}
