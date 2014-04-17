package com.mobicrave.eventtracker.storage;

import com.mobicrave.eventtracker.base.DB;

import java.io.Closeable;
import java.io.IOException;


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
    db.put(ID_KEY, "" + (++nextAvailableId));
    return availableId;
  }

  public void put(String externalId, int id) {
    db.put(externalId, id);
  }

  public Integer get(String externalUserId) {
    String value = db.get(externalUserId);
    if (value == null) {
      //noinspection ReturnOfNull
      return null;
    }
    return Integer.parseInt(value);
  }

  public int getCurrentId() {
    return nextAvailableId;
  }

  @Override
  public void close() throws IOException {
    db.close();
  }

  public static IdMap create(DB db) {
    String idString = db.get(ID_KEY);
    int currentId = idString == null ? 0 : Integer.parseInt(idString);
    return new IdMap(db, currentId);
  }
}
