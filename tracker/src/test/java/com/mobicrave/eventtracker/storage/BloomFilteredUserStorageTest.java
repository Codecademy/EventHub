package com.mobicrave.eventtracker.storage;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.inject.Injector;
import com.mobicrave.eventtracker.Criterion;
import com.mobicrave.eventtracker.integration.GuiceTestCase;
import com.mobicrave.eventtracker.model.User;
import org.junit.Assert;
import org.junit.Test;

import javax.inject.Provider;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class BloomFilteredUserStorageTest extends GuiceTestCase {
  @Test
  public void testEnsureUser() throws Exception {
    Provider<BloomFilteredUserStorage> bloomFilteredUserStorageProvider = getBloomFilteredUserStorageProvider();
    BloomFilteredUserStorage userStorage = bloomFilteredUserStorageProvider.get();
    Assert.assertEquals(0, userStorage.ensureUser("foo"));
    Assert.assertEquals(0, userStorage.ensureUser("foo"));
    Assert.assertEquals(1, userStorage.ensureUser("bar"));
    Assert.assertEquals(0, userStorage.ensureUser("foo"));
    Assert.assertEquals(0, userStorage.ensureUser("foo"));
    Assert.assertEquals(1, userStorage.ensureUser("bar"));
  }

  @Test
  public void testAll() throws Exception {
    Provider<BloomFilteredUserStorage> bloomFilteredUserStorageProvider = getBloomFilteredUserStorageProvider();
    BloomFilteredUserStorage userStorage = bloomFilteredUserStorageProvider.get();
    String[] externalIds = new String[] { "x", "y", "z" };
    Map<String, String>[] properties = (Map<String, String>[]) new Map[] {
        ImmutableMap.<String, String>builder().put("foo1", "bar1").put("foo2", "bar2").build(),
        ImmutableMap.<String, String>builder().put("foo2", "bar2").put("foo3", "bar3").build(),
        ImmutableMap.<String, String>builder().put("foo3", "bar3").build()
    };

    for (int i = 0; i < externalIds.length - 1; i++) {
      userStorage.ensureUser(externalIds[i]);
      userStorage.ensureUser(externalIds[i]); // Make sure ensureUser is idempotent
      userStorage.updateUser(new User.Builder(externalIds[i], properties[i]).build());
    }
    for (int i = 0; i < externalIds.length - 1; i++) {
      userStorage.ensureUser(externalIds[i]); // Make sure ensureUser is idempotent
    }

    Assert.assertEquals(-1, userStorage.getId("NOT EXIST"));
    List[] matchedCriteria = new List[] {
        Lists.newArrayList(new Criterion("foo1", "bar1"), new Criterion("foo2", "bar2")),
        Lists.newArrayList(new Criterion("foo2", "bar2")),
        Lists.newArrayList(new Criterion("foo3", "bar3"))
    };
    List[] unmatchedCriteria = new List[] {
        Lists.newArrayList(new Criterion("foo1", "bar1"), new Criterion("foo2", "bar2"),
            new Criterion("foo3", "bar3")),
        Lists.newArrayList(new Criterion("foo2", "bar1")),
        Lists.newArrayList(new Criterion("foo1", "bar1"))
    };
    for (int i = 0; i < externalIds.length - 1; i++) {
      Assert.assertTrue(userStorage.satisfy(i, matchedCriteria[i]));
      Assert.assertFalse(userStorage.satisfy(i, unmatchedCriteria[i]));
      Assert.assertEquals(externalIds[i], userStorage.getUser(i).getExternalId());
      Assert.assertEquals(i, userStorage.getId(externalIds[i]));
      for (Map.Entry<String, String> entry : properties[i].entrySet()) {
        Assert.assertEquals(entry.getValue(), userStorage.getUser(i).get(entry.getKey()));
      }
    }
    userStorage.close();

    userStorage = bloomFilteredUserStorageProvider.get();
    userStorage.ensureUser(externalIds[externalIds.length - 1]);
    userStorage.updateUser(
        new User.Builder(externalIds[externalIds.length - 1], properties[externalIds.length - 1])
            .build());
    for (int i = 0; i < externalIds.length; i++) {
      Assert.assertTrue(userStorage.satisfy(i, matchedCriteria[i]));
      Assert.assertFalse(userStorage.satisfy(i, unmatchedCriteria[i]));
      Assert.assertEquals(externalIds[i], userStorage.getUser(i).getExternalId());
      Assert.assertEquals(i, userStorage.getId(externalIds[i]));
      for (Map.Entry<String, String> entry : properties[i].entrySet()) {
        Assert.assertEquals(entry.getValue(), userStorage.getUser(i).get(entry.getKey()));
      }
    }

    userStorage.alias("foo", userStorage.getId(externalIds[0]));
    Assert.assertEquals(externalIds[0], userStorage.getUser(0).getExternalId());
    Assert.assertEquals(userStorage.getId(externalIds[0]), userStorage.getId("foo"));

    userStorage.updateUser(new User.Builder(externalIds[0], properties[1]).build());
    for (Map.Entry<String, String> entry : properties[1].entrySet()) {
      Assert.assertEquals(entry.getValue(), userStorage.getUser(0).get(entry.getKey()));
    }
  }

  private Provider<BloomFilteredUserStorage> getBloomFilteredUserStorageProvider() {
    Properties prop = new Properties();
    prop.put("eventtracker.directory", getTempDirectory());
    prop.put("eventtracker.journaluserstorage.numMetaDataPerFile", "1");
    prop.put("eventtracker.journaluserstorage.metaDataFileCacheSize", "1");
    prop.put("eventtracker.journaluserstorage.journalFileSize", "1024");
    prop.put("eventtracker.journaluserstorage.journalWriteBatchSize", "1024");
    prop.put("eventtracker.cacheduserstorage.recordCacheSize", "1");
    prop.put("eventtracker.bloomfiltereduserstorage.bloomFilterSize", "64");
    prop.put("eventtracker.bloomfiltereduserstorage.numHashes", "1");
    prop.put("eventtracker.bloomfiltereduserstorage.numMetaDataPerFile", "1");
    prop.put("eventtracker.bloomfiltereduserstorage.metaDataFileCacheSize", "1");

    Injector injector = createInjectorFor(
        prop, new UserStorageModule());
    return injector.getProvider(BloomFilteredUserStorage.class);
  }
}
