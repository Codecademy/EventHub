package com.mobicrave.eventtracker.index;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.mobicrave.eventtracker.base.KeyValueCallback;
import com.mobicrave.eventtracker.model.Event;

import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

public class PropertiesIndex implements Closeable {
  private static final Set<String> KEYS_IGNORED = Sets.newHashSet("", "date",
      "external_user_id", "event_type");
  private final String filename;
  private final ConcurrentHashMap<String, ConcurrentSkipListSet<String>> keysMap;
  private final ConcurrentHashMap<String, ConcurrentSkipListSet<String>> valuesMap;

  public PropertiesIndex(String filename,
      ConcurrentHashMap<String, ConcurrentSkipListSet<String>> keysMap,
      ConcurrentHashMap<String, ConcurrentSkipListSet<String>> valuesMap) {
    this.filename = filename;
    this.keysMap = keysMap;
    this.valuesMap = valuesMap;
  }

  @Override
  public void close() throws IOException {
    //noinspection ResultOfMethodCallIgnored
    new File(filename).getParentFile().mkdirs();
    try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(filename))) {
      oos.writeObject(keysMap);
      oos.writeObject(valuesMap);
    }
  }

  public void addEvent(Event event) {
    final String eventType = event.getEventType();
    keysMap.putIfAbsent(eventType, new ConcurrentSkipListSet<String>());
    final ConcurrentSkipListSet<String> keysSet = keysMap.get(eventType);
    event.enumerate(new KeyValueCallback() {
      @Override
      public void callback(String key, String value) {
        if (KEYS_IGNORED.contains(key)) {
          return;
        }
        keysSet.add(key);
        String valueMapKey = createKey(eventType, key);
        valuesMap.putIfAbsent(valueMapKey, new ConcurrentSkipListSet<String>());
        ConcurrentSkipListSet<String> valuesSet = valuesMap.get(valueMapKey);
        valuesSet.add(value);
      }
    });
  }

  public List<String> getKeys(String eventType) {
    return Lists.newArrayList(keysMap.get(eventType));
  }

  public List<String> getValues(String eventType, String key) {
    return Lists.newArrayList(valuesMap.get(createKey(eventType, key)));
  }

  private String createKey(String eventType, String key) {
    return String.format("%s@@@%s", eventType, key);
  }
}
