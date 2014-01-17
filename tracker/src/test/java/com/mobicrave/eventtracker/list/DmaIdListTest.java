package com.mobicrave.eventtracker.list;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;

public class DmaIdListTest {
  @Rule
  public TemporaryFolder folder = new TemporaryFolder();

  @Test
  public void testDmaIdList() throws Exception {
    File directory = folder.newFolder();
    String filename = directory.getCanonicalPath() + "/simple_id_list.ser";
    IdList idList = DmaIdList.build(filename, 2);
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

    idList.close();

    idList = DmaIdList.build(filename, 2);
    idList.add(ids[ids.length - 1]);

    iterator = idList.iterator();
    for (long id : ids) {
      Assert.assertTrue(iterator.hasNext());
      Assert.assertEquals(id, iterator.next());
    }
    Assert.assertFalse(iterator.hasNext());
  }
}
