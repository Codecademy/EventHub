package com.codecademy.eventhub.storage;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.inject.Injector;
import com.codecademy.eventhub.storage.filter.And;
import com.codecademy.eventhub.storage.filter.ExactMatch;
import com.codecademy.eventhub.storage.filter.Filter;
import com.codecademy.eventhub.integration.GuiceTestCase;
import com.codecademy.eventhub.model.User;
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
    Assert.assertEquals(2, userStorage.getNumRecords());
  }

  @Test
  public void testAll() throws Exception {
    Provider<BloomFilteredUserStorage> bloomFilteredUserStorageProvider = getBloomFilteredUserStorageProvider();
    BloomFilteredUserStorage userStorage = bloomFilteredUserStorageProvider.get();
    String[] externalIds = new String[] { "x", "y", "z" };
    @SuppressWarnings("unchecked")
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
    List<Filter> matchedFilters = Lists.newArrayList(
        And.of(new ExactMatch("foo1", "bar1"), new ExactMatch("foo2", "bar2")),
        new ExactMatch("foo2", "bar2"),
        new ExactMatch("foo3", "bar3"));
    List<Filter> unmatchedFilters = Lists.newArrayList(
        And.of(new ExactMatch("foo1", "bar1"), new ExactMatch("foo2", "bar2"), new ExactMatch("foo3", "bar3")),
        new ExactMatch("foo2", "bar1"),
        new ExactMatch("foo1", "bar1"));
    for (int i = 0; i < externalIds.length - 1; i++) {
      Assert.assertTrue(matchedFilters.get(i).accept(userStorage.getFilterVisitor(i)));
      Assert.assertFalse(unmatchedFilters.get(i).accept(userStorage.getFilterVisitor(i)));
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
      Assert.assertTrue(matchedFilters.get(i).accept(userStorage.getFilterVisitor(i)));
      Assert.assertFalse(unmatchedFilters.get(i).accept(userStorage.getFilterVisitor(i)));
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
    prop.put("eventhub.directory", getTempDirectory());
    prop.put("eventhub.journaluserstorage.numMetaDataPerFile", "1");
    prop.put("eventhub.journaluserstorage.metaDataFileCacheSize", "1");
    prop.put("eventhub.journaluserstorage.journalFileSize", "1024");
    prop.put("eventhub.journaluserstorage.journalWriteBatchSize", "1024");
    prop.put("eventhub.cacheduserstorage.recordCacheSize", "1");
    prop.put("eventhub.bloomfiltereduserstorage.bloomFilterSize", "64");
    prop.put("eventhub.bloomfiltereduserstorage.numHashes", "1");
    prop.put("eventhub.bloomfiltereduserstorage.numMetaDataPerFile", "1");
    prop.put("eventhub.bloomfiltereduserstorage.metaDataFileCacheSize", "1");

    Injector injector = createInjectorFor(
        prop, new UserStorageModule());
    return injector.getProvider(BloomFilteredUserStorage.class);
  }
}
