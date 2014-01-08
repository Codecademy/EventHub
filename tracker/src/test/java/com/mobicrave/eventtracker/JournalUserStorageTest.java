package com.mobicrave.eventtracker;

import com.google.common.collect.ImmutableMap;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.util.Map;

public class JournalUserStorageTest {
  @Rule
  public TemporaryFolder folder = new TemporaryFolder();

  @Test
  public void testAll() throws Exception {
    String dataDir = folder.newFolder("journal-user-storage-test").getCanonicalPath() + "/";
    JournalUserStorage userStorage = JournalUserStorage.build(dataDir);
    String[] externalIds = new String[] { "x", "y", "z" };
    Map<String, String>[] properties = new Map[] {
        ImmutableMap.<String, String>builder().put("foo1", "bar1").build(),
        ImmutableMap.<String, String>builder().put("foo2", "bar2").build(),
        ImmutableMap.<String, String>builder().put("foo3", "bar3").build()
    };

    for (int i = 0; i < externalIds.length - 1; i++) {
      userStorage.addUser(new User.Builder(externalIds[i], properties[i]).build());
    }

    for (int i = 0; i < externalIds.length - 1; i++) {
      Assert.assertEquals(externalIds[i], userStorage.getUser(i).getExternalId());
      Assert.assertEquals(i, userStorage.getId(externalIds[i]));
      Assert.assertEquals(properties[i], userStorage.getUser(i).getProperties());
    }
    userStorage.close();

    userStorage = JournalUserStorage.build(dataDir);
    userStorage.addUser(
        new User.Builder(externalIds[externalIds.length - 1], properties[externalIds.length - 1])
            .build());
    for (int i = 0; i < externalIds.length; i++) {
      Assert.assertEquals(externalIds[i], userStorage.getUser(i).getExternalId());
      Assert.assertEquals(i, userStorage.getId(externalIds[i]));
      Assert.assertEquals(properties[i], userStorage.getUser(i).getProperties());
    }
  }}
