package com.mobicrave.eventtracker.storage;

import com.google.common.collect.ImmutableMap;
import com.mobicrave.eventtracker.model.User;
import com.mobicrave.eventtracker.storage.MemUserStorage;
import com.mobicrave.eventtracker.storage.UserStorage;
import org.junit.Assert;
import org.junit.Test;

import java.util.Map;

public class MemUserStorageTest {
  @Test
  public void testAll() throws Exception {
    UserStorage userStorage = MemUserStorage.build();
    String[] externalIds = new String[] { "x", "y", "z" };
    Map<String, String>[] properties = new Map[] {
        ImmutableMap.<String, String>builder().put("foo1", "bar1").build(),
        ImmutableMap.<String, String>builder().put("foo2", "bar2").build(),
        ImmutableMap.<String, String>builder().put("foo3", "bar3").build()
    };

    for (int i = 0; i < externalIds.length; i++) {
      userStorage.addUser(new User.Builder(externalIds[i], properties[i]).build());
    }

    for (int i = 0; i < externalIds.length; i++) {
      Assert.assertEquals(i, userStorage.getId(externalIds[i]));
      Assert.assertEquals(externalIds[i], userStorage.getUser(i).getExternalId());
      Assert.assertEquals(properties[i], userStorage.getUser(i).getProperties());
    }
  }
}
