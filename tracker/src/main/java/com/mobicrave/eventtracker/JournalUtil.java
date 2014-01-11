package com.mobicrave.eventtracker;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import org.fusesource.hawtjournal.api.Journal;
import org.fusesource.hawtjournal.api.Location;

import java.io.File;
import java.io.IOException;

public class JournalUtil {
  public static byte[] locationToBytes(Location location) throws IOException {
    ByteArrayDataOutput dos = ByteStreams.newDataOutput();
    location.writeExternal(dos);
    return dos.toByteArray();
  }

  public static Journal createJournal(String dirPath) {
    Journal journal = new Journal();
    File directory = new File(dirPath);
    directory.mkdirs();
    journal.setDirectory(directory);
    journal.setMaxFileLength(1024 * 1024 * 1024);
    journal.setMaxWriteBatchSize(1024 * 10);

    try {
      journal.open();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return journal;
  }
}
