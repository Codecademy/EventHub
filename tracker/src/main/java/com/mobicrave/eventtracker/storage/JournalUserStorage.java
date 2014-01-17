package com.mobicrave.eventtracker.storage;

import com.google.common.collect.Maps;
import com.google.common.io.ByteStreams;
import com.mobicrave.eventtracker.Criterion;
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
import java.util.List;
import java.util.Map;

public class JournalUserStorage implements UserStorage {
  private final String directory;
  private final Journal userJournal;
  private DmaList<MetaData> metaDataList;
  private final Map<String, Integer> idMap;
  private int currentId;

  public JournalUserStorage(String directory, Journal userJournal, DmaList<MetaData> metaDataList,
      Map<String, Integer> idMap, int currentId) {
    this.directory = directory;
    this.userJournal = userJournal;
    this.metaDataList = metaDataList;
    this.idMap = idMap;
    this.currentId = currentId;
  }

  @Override
  public synchronized int addUser(User user) {
    try {
      int id = currentId++;
      byte[] location = JournalUtil.locationToBytes(userJournal.write(user.toByteBuffer(), true));
      MetaData metaData = new MetaData(location);
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
    User user = getUser(userId);
    Map<String,String> properties = user.getProperties();
    for (Criterion criterion : criteria) {
      if (!criterion.getValue().equals(properties.get(criterion.getKey()))) {
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

  private MetaData getUserMetaData(int userId) {
    return metaDataList.get(userId);
  }

  private static String getMetaDataSerializationFile(String directory) {
    return directory + "/meta_data_list.mem";
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
        getMetaDataSerializationFile(directory), 10 * 1024 /* defaultCapacity */);
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
    private final byte[] location;

    public MetaData(byte[] location) {
      this.location = location;
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
        byteBuffer.put(metaData.location);
        return byteBuffer.array();
      }

      @Override
      public MetaData fromBytes(byte[] bytes) {
        ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
        byte[] location = new byte[13];
        byteBuffer.get(location);
        return new MetaData(location);
      }
    }
  }
}
