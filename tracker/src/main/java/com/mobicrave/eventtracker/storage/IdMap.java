package com.mobicrave.eventtracker.storage;

import org.iq80.leveldb.DB;

import java.io.Closeable;
import java.io.IOException;

import static org.fusesource.leveldbjni.JniDBFactory.asString;
import static org.fusesource.leveldbjni.JniDBFactory.bytes;

public class IdMap implements Closeable {
  private static final String ID_KEY = "__eventtracker__id";

  private final DB db;
  private int nextAvailableId;

  private IdMap(DB db, int nextAvailableId) {
    this.db = db;
    this.nextAvailableId = nextAvailableId;
  }

  public int incrementNextAvailableId() {
    int availableId = nextAvailableId;
    db.put(bytes(ID_KEY), bytes("" + (++nextAvailableId)));
    return availableId;
  }

  public void put(String externalId, int id) {
    db.put(bytes(externalId), bytes("" + id));
  }

  public Integer get(String externalUserId) {
    byte[] value = db.get(bytes(externalUserId));
    if (value == null) {
      //noinspection ReturnOfNull
      return null;
    }
    return Integer.parseInt(asString(value));
  }

  public int getCurrentId() {
    return nextAvailableId;
  }

  @Override
  public void close() throws IOException {
    db.close();
  }

  public static IdMap create(DB db) {
    byte[] bytes = db.get(bytes(ID_KEY));
    int currentId = bytes == null ? 0 : Integer.parseInt(asString(bytes));
    return new IdMap(db, currentId);
  }
}
