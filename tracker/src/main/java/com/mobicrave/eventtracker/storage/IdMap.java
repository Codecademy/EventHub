package com.mobicrave.eventtracker.storage;

import java.io.Closeable;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Map;

public class IdMap implements Closeable {
  private final String filename;
  private final Map<String, Integer> idMap;
  private int currentId;

  public IdMap(String filename, Map<String, Integer> idMap, int currentId) {
    this.filename = filename;
    this.idMap = idMap;
    this.currentId = currentId;
  }

  public int getAndIncrementCurrentId() {
    return currentId++;
  }

  public void put(String externalId, int id) {
    idMap.put(externalId, id);
  }

  public Integer get(String externalUserId) {
    return idMap.get(externalUserId);
  }

  public int getCurrentId() {
    return currentId;
  }

  @Override
  public void close() throws IOException {
    try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(filename))) {
      oos.writeObject(idMap);
      oos.writeInt(currentId);
    }
  }
}
