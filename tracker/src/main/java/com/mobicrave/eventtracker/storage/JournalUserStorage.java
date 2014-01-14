package com.mobicrave.eventtracker.storage;

import com.google.common.collect.Maps;
import com.google.common.io.ByteStreams;
import com.mobicrave.eventtracker.list.DmaList;
import com.mobicrave.eventtracker.model.User;
import org.fusesource.hawtjournal.api.Journal;
import org.fusesource.hawtjournal.api.Location;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Map;

public class JournalUserStorage implements UserStorage {
  private final Journal userJournal;
  private DmaList<User.MetaData> metaDataList;
  private final Map<String, Integer> idMap;
  private int currentId;

  public JournalUserStorage(Journal userJournal, DmaList<User.MetaData> metaDataList,
      Map<String, Integer> idMap, int currentId) {
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
      User.MetaData metaData = user.getMetaData(location);
      metaDataList.add(metaData);
      idMap.put(user.getExternalId(), id);
      return id;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public User.MetaData getUserMetaData(int userId) {
    return metaDataList.get(userId);
  }

  @Override
  public int getId(String externalUserId) {
    return idMap.get(externalUserId);
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
  public void close() throws IOException {
    userJournal.close();
    metaDataList.close();
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
    DmaList<User.MetaData> metaDataList = DmaList.build(User.MetaData.getSchema(),
        getMetaDataSerializationFile(directory), 10 * 1024 /* defaultCapacity */);
    File file = new File(getIdMapSerializationFile(directory));
    Map<String,Integer> idMap = Maps.newConcurrentMap();
    if (file.exists()) {
      try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
        idMap = (Map<String, Integer>) ois.readObject();
      } catch (ClassNotFoundException | IOException e) {
        throw new RuntimeException(e);
      }
    }
    return new JournalUserStorage(userJournal, metaDataList, idMap, (int) metaDataList.getNumRecords());
  }
}
