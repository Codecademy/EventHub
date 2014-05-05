package com.codecademy.eventhub.base;

import com.google.common.collect.Lists;
import org.iq80.leveldb.DBIterator;

import java.io.IOException;
import java.util.List;

import static org.fusesource.leveldbjni.JniDBFactory.asString;
import static org.fusesource.leveldbjni.JniDBFactory.bytes;

public class DB {
  private final org.iq80.leveldb.DB db;

  public DB(org.iq80.leveldb.DB db) {
    this.db = db;
  }

  public List<String> findByPrefix(String prefix, int substringStartsAt) {
    try (DBIterator iterator = db.iterator()) {
      List<String> keys = Lists.newArrayList();
      for (iterator.seek(bytes(prefix)); iterator.hasNext(); iterator.next()) {
        String key = asString(iterator.peekNext().getKey());
        if (!key.startsWith(prefix)) {
          break;
        }
        keys.add(key.substring(substringStartsAt));
      }
      return keys;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public void put(String key, String value) {
    db.put(bytes(key), bytes(value));
  }

  public void put(String key, int value) {
    db.put(bytes(key), bytes(String.valueOf(value)));
  }

  public String get(String key) {
    byte[] bytes = db.get(bytes(key));
    //noinspection ReturnOfNull
    return (bytes == null ? null : asString(bytes));
  }

  public void close() throws IOException {
    db.close();
  }

  public void put(AtomicWrite atomicWrite) {
    org.iq80.leveldb.WriteBatch origWriteBatch = db.createWriteBatch();
    WriteBatch writeBatch = new WriteBatch(origWriteBatch);
    atomicWrite.write(writeBatch);
    db.write(origWriteBatch);
  }

  public interface AtomicWrite {
    public void write(WriteBatch writeBatch);
  }

  public static class WriteBatch {
    private final org.iq80.leveldb.WriteBatch writeBatch;

    private WriteBatch(org.iq80.leveldb.WriteBatch writeBatch) {
      this.writeBatch = writeBatch;
    }

    public void put(String key, byte[] value) {
      writeBatch.put(bytes(key), value);
    }
  }
}
