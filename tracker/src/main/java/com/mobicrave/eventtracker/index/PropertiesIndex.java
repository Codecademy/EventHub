package com.mobicrave.eventtracker.index;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.mobicrave.eventtracker.base.KeyValueCallback;
import com.mobicrave.eventtracker.model.Event;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.DBIterator;
import org.iq80.leveldb.WriteBatch;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;
import java.util.Set;

import static org.fusesource.leveldbjni.JniDBFactory.asString;
import static org.fusesource.leveldbjni.JniDBFactory.bytes;

public class PropertiesIndex implements Closeable {
  private static final Set<String> KEYS_IGNORED = Sets.newHashSet("", "date",
      "external_user_id", "event_type");
  public static final Joiner JOINER = Joiner.on("@@");
  private static final byte[] VALUE = new byte[0];

  private final DB keyDb;
  private final DB valueDb;

  public PropertiesIndex(DB keyDb, DB valueDb) {
    this.keyDb = keyDb;
    this.valueDb = valueDb;
  }

  @Override
  public void close() throws IOException {
    keyDb.close();
    valueDb.close();
  }

  public void addEvent(Event event) {
    final String eventType = event.getEventType();
    final WriteBatch keyBatch = keyDb.createWriteBatch();
    final WriteBatch valueBatch = keyDb.createWriteBatch();
    event.enumerate(new KeyValueCallback() {
      @Override
      public void callback(String key, String value) {
        if (KEYS_IGNORED.contains(key)) {
          return;
        }
        keyBatch.put(bytes(JOINER.join(eventType, key)), VALUE);
        valueBatch.put(bytes(JOINER.join(eventType, key, value)), VALUE);
      }
    });
    keyDb.write(keyBatch);
    valueDb.write(valueBatch);
  }

  public List<String> getKeys(String eventType) {
    return findByPrefix(JOINER.join(eventType, ""), keyDb);
  }

  public List<String> getValues(String eventType, String key) {
    return findByPrefix(JOINER.join(eventType, key, ""), valueDb);
  }

  private List<String> findByPrefix(String prefix, DB db) {
    try (DBIterator iterator = db.iterator()) {
      List<String> keys = Lists.newArrayList();
      for (iterator.seek(bytes(prefix)); iterator.hasNext(); iterator.next()) {
        String key = asString(iterator.peekNext().getKey());
        if (!key.startsWith(prefix)) {
          break;
        }
        keys.add(key.substring(prefix.length()));
      }
      return keys;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
