package com.mobicrave.eventtracker;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.io.ByteStreams;
import org.fusesource.hawtjournal.api.Journal;
import org.fusesource.hawtjournal.api.Location;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class JournalUserStorage implements UserStorage {
  private final Journal userJournal;
  private final Journal metaDataJournal;
  private User.MetaData[] metaDatas;
  private final Map<String, Integer> idMap;
  private AtomicInteger numUsers;

  public JournalUserStorage(Journal userJournal, Journal metaDataJournal, User.MetaData[] metaDatas,
      Map<String, Integer> idMap, AtomicInteger numUsers) {
    this.userJournal = userJournal;
    this.metaDataJournal = metaDataJournal;
    this.metaDatas = metaDatas;
    this.idMap = idMap;
    this.numUsers = numUsers;
  }

  @Override
  public int addUser(User user) {
    int id = numUsers.incrementAndGet();
    if (id >= metaDatas.length) {
      synchronized (this) {
        if (id >= metaDatas.length) {
          User.MetaData[] newMetaDatas = new User.MetaData[metaDatas.length * 2];
          System.arraycopy(metaDatas, 0, newMetaDatas, 0, metaDatas.length);
          metaDatas = newMetaDatas;
        }
      }
    }
    try {
      byte[] location = JournalUtil.locationToBytes(userJournal.write(user.toByteBuffer(), true));
      User.MetaData metaData = user.getMetaData(user.getExternalId(), location);
      metaDatas[id] = metaData;
      metaDataJournal.write(metaData.toByteBuffer(), true);
      idMap.put(user.getExternalId(), id);
      return id;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public User.MetaData getUserMetaData(int userId) {
    return metaDatas[userId];
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
      metaDataJournal.close();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static JournalUserStorage build(String dataDir) {
    List<User.MetaData> metaDatas = Lists.newArrayList();
    Journal userJournal = JournalUtil.createJournal(dataDir + "/user_journal/userJournal/");
    Journal metaDataJournal = JournalUtil.createJournal(dataDir + "/user_journal/metaDataJournal/");
    Map<String,Integer> idMap = Maps.newConcurrentMap();
    try {
      int id = 0;
      for (Location location : metaDataJournal) {
        User.MetaData metadata = User.MetaData.fromByteBuffer(metaDataJournal.read(location));
        metaDatas.add(metadata);
        idMap.put(metadata.getExternalId(), id++);
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return new JournalUserStorage(userJournal, metaDataJournal,
        metaDatas.toArray(new User.MetaData[1024]), idMap, new AtomicInteger(metaDatas.size() - 1));
  }
}
