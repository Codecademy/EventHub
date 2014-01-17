package com.mobicrave.eventtracker.storage;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.mobicrave.eventtracker.Criterion;
import com.mobicrave.eventtracker.model.User;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;
import java.util.Map;

public class MemUserStorageTest {
  @Test
  public void testAll() throws Exception {
    UserStorage userStorage = MemUserStorage.build();
    String[] externalIds = new String[] { "x", "y", "z" };
    Map<String, String>[] properties = (Map<String, String>[]) new Map[] {
        ImmutableMap.<String, String>builder().put("foo1", "bar1").put("foo2", "bar2").build(),
        ImmutableMap.<String, String>builder().put("foo2", "bar2").put("foo3", "bar3").build(),
        ImmutableMap.<String, String>builder().put("foo3", "bar3").build()
    };

    for (int i = 0; i < externalIds.length; i++) {
      userStorage.addUser(new User.Builder(externalIds[i], properties[i]).build());
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
    for (int i = 0; i < externalIds.length; i++) {
      Assert.assertTrue(userStorage.satisfy(i, matchedCriteria[i]));
      Assert.assertFalse(userStorage.satisfy(i, unmatchedCriteria[i]));
      Assert.assertEquals(i, userStorage.getId(externalIds[i]));
      Assert.assertEquals(externalIds[i], userStorage.getUser(i).getExternalId());
      Assert.assertEquals(properties[i], userStorage.getUser(i).getProperties());
    }
  }
}
