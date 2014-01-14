package com.mobicrave.eventtracker;

import com.google.common.collect.Maps;
import com.google.common.io.ByteStreams;
import org.fusesource.hawtjournal.api.Journal;
import org.fusesource.hawtjournal.api.Location;

import java.io.IOException;
import java.util.Map;

public class JournalUserStorage implements UserStorage {
  private final Journal userJournal;
  private MemMappedList<User.MetaData> metaDataList;
  private final Map<String, Integer> idMap;
  private int currentId;

  public JournalUserStorage(Journal userJournal, MemMappedList<User.MetaData> metaDataList,
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
  public void close() {
    try {
      userJournal.close();
      metaDataList.close();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static JournalUserStorage build(String dataDir) {
    Journal userJournal = JournalUtil.createJournal(dataDir + "/user_journal/");
    MemMappedList<User.MetaData> metaDataList = MemMappedList.build(User.MetaData.getSchema(),
        dataDir + "/meta_data_list.mem", 10 * 1024 /* defaultCapacity */);
    Map<String,Integer> idMap = Maps.newConcurrentMap();
    try {
      int id = 0;
      for (Location location : userJournal) {
        User user = User.fromByteBuffer(userJournal.read(location));
        idMap.put(user.getExternalId(), id++);
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return new JournalUserStorage(userJournal, metaDataList, idMap, (int) metaDataList.getNumRecords());
  }
}
