package com.mobicrave.eventtracker.list;

import org.junit.Assert;
import org.junit.Test;

public class MemIdListTest {
  @Test
  public void testMemIdList() throws Exception {
    IdList idList = new MemIdList(new long[2], 0);
    long[] ids = new long[] { 10, 20, 30, 40, 50 };

    IdList.Iterator iterator = idList.iterator();
    for (int i = 0; i < ids.length - 1; i++) {
      idList.add(ids[i]);
    }
    Assert.assertFalse(iterator.hasNext());
    iterator = idList.iterator();
    for (int i = 0; i < ids.length - 1; i++) {
      Assert.assertTrue(iterator.hasNext());
      Assert.assertEquals(ids[i], iterator.next());
    }
    Assert.assertFalse(iterator.hasNext());

    iterator = idList.subList(19, 40);
    for (int i = 1; i < ids.length - 2; i++) {
      Assert.assertTrue(iterator.hasNext());
      Assert.assertEquals(ids[i], iterator.next());
    }
    Assert.assertFalse(iterator.hasNext());
  }
}
