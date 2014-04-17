package com.mobicrave.eventtracker.index;

import com.google.common.base.Joiner;
import com.google.common.collect.Sets;
import com.mobicrave.eventtracker.base.DB;
import com.mobicrave.eventtracker.base.KeyValueCallback;
import com.mobicrave.eventtracker.model.Event;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;
import java.util.Set;

public class PropertiesIndex implements Closeable {
  private static final Set<String> KEYS_IGNORED = Sets.newHashSet("", "date",
      "external_user_id", "event_type");
  private static final byte[] DUMMY = new byte[0];

  private final DB db;

  public PropertiesIndex(DB db) {
    this.db = db;
  }

  public void addEvent(final Event event) {
    final String eventType = event.getEventType();
    db.put(new DB.AtomicWrite() {
      public void write(final DB.WriteBatch writeBatch) {
        event.enumerate(new KeyValueCallback() {
          @Override
          public void callback(String key, String value) {
            if (KEYS_IGNORED.contains(key)) {
              return;
            }
            writeBatch.put(getKeyKey(eventType) + key, DUMMY);
            writeBatch.put(getValueKey(eventType, key) + value, DUMMY);
          }
        });
      }
    });
  }

  public List<String> getKeys(String eventType) {
    return db.findByPrefix(getKeyKey(eventType));
  }

  public List<String> getValues(String eventType, String key) {
    return db.findByPrefix(getValueKey(eventType, key));
  }

  @Override
  public void close() throws IOException {
    db.close();
  }

  private String getKeyKey(String eventType) {
    return Joiner.on("@@").join(eventType, "__KEY");
  }

  private String getValueKey(String eventType, String key) {
    return Joiner.on("@@").join(eventType, key);
  }
}
