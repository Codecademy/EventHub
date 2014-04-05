package com.mobicrave.eventtracker.storage;

import com.google.common.collect.Maps;
import com.google.common.io.ByteStreams;
import com.mobicrave.eventtracker.list.DmaList;
import com.mobicrave.eventtracker.model.User;
import com.mobicrave.eventtracker.storage.visitor.DelayedVisitorProxy;
import com.mobicrave.eventtracker.storage.visitor.UserFilterVisitor;
import com.mobicrave.eventtracker.storage.visitor.Visitor;
import org.fusesource.hawtjournal.api.Journal;
import org.fusesource.hawtjournal.api.Location;

import javax.inject.Provider;
import java.io.IOException;
import java.nio.ByteBuffer;

public class JournalUserStorage implements UserStorage {
  private final Journal userJournal;
  private DmaList<MetaData> metaDataList;
  private final IdMap idMap;

  public JournalUserStorage(Journal userJournal, DmaList<MetaData> metaDataList, IdMap idMap) {
    this.userJournal = userJournal;
    this.metaDataList = metaDataList;
    this.idMap = idMap;
  }

  @Override
  public synchronized int ensureUser(String externalUserId) {
    int id = getId(externalUserId);
    if (id != USER_NOT_FOUND) {
      return id;
    }
    User user = new User.Builder(externalUserId, Maps.<String, String>newHashMap()).build();
    try {
      id = idMap.getAndIncrementCurrentId();
      byte[] location = JournalUtil.locationToBytes(userJournal.write(user.toByteBuffer(), true));
      MetaData metaData = new MetaData(location);
      metaDataList.add(metaData);
      idMap.put(externalUserId, id);
      return id;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public synchronized int updateUser(User user) {
    int id = getId(user.getExternalId());
    try {
      byte[] location = JournalUtil.locationToBytes(userJournal.write(user.toByteBuffer(), true));
      MetaData metaData = new MetaData(location);
      metaDataList.update(id, metaData);
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
      JournalUserStorage.MetaData metaData = metaDataList.get(userId);
      location.readExternal(ByteStreams.newDataInput(metaData.getLocation()));
      return User.fromByteBuffer(userJournal.read(location));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public Visitor getFilterVisitor(final int userId) {
    return new DelayedVisitorProxy(new Provider<Visitor>() {
      @Override
      public Visitor get() {
        User user = getUser(userId);
        return new UserFilterVisitor(user);
      }
    });
  }

  @Override
  public void alias(String fromExternalUserId, int toUserId) {
    idMap.put(fromExternalUserId, toUserId);
  }

  @Override
  public void close() throws IOException {
    idMap.close();
    userJournal.close();
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
        idMap.getCurrentId(),
        metaDataList.getVarz(indentation + 1));
  }

  public static class MetaData {
    private final byte[] location;

    public MetaData(byte[] location) {
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
        byteBuffer.put(metaData.location);
        return byteBuffer.array();
      }

      @Override
      public MetaData fromBytes(byte[] bytes) {
        ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
        byte[] location = new byte[LOCATION_SIZE];
        byteBuffer.get(location);
        return new MetaData(location);
      }
    }
  }
}
