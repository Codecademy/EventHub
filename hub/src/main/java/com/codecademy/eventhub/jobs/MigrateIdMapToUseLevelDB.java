package com.codecademy.eventhub.jobs;

import org.fusesource.leveldbjni.JniDBFactory;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.Options;
import org.iq80.leveldb.WriteBatch;

import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.util.Map;

import static org.fusesource.leveldbjni.JniDBFactory.bytes;

public class MigrateIdMapToUseLevelDB {
  public static void main(String[] args) throws Exception {
    String userStorageDirectory = args[0];

    String filename = userStorageDirectory + "/id_map.ser";
    File file = new File(filename);
    try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
      //noinspection unchecked
      Map<String, Integer> idMap = (Map<String, Integer>) ois.readObject();
      int currentId = ois.readInt();

      Options options = new Options();
      options.createIfMissing(true);
      try (DB idMapDb = JniDBFactory.factory.open(new File(userStorageDirectory + "/id_map.db"), options)) {
        try (WriteBatch batch = idMapDb.createWriteBatch()) {
          for (Map.Entry<String, Integer> entry : idMap.entrySet()) {
            batch.put(bytes(entry.getKey()), bytes("" + entry.getValue()));
          }
          batch.put(bytes("__eventtracker__id"), bytes("" + currentId));
          idMapDb.write(batch);
        }
      }
    }
  }
}
