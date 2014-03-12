package com.mobicrave.eventtracker.list;

import com.google.inject.Injector;
import com.mobicrave.eventtracker.integration.GuiceTestCase;
import org.junit.Assert;
import org.junit.Test;

import java.util.Properties;

public class DmaIdListTest extends GuiceTestCase {
  @Test
  public void testDmaIdList() throws Exception {
    DmaIdList.Factory dmaIdListFactory = getDmaIdListFactory();
    dmaIdListFactory.setDefaultCapacity(2);
    String filename = getTempDirectory() + "/simple_id_list.ser";
    IdList idList = dmaIdListFactory.build(filename);
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

    iterator = idList.subList(19, 2);
    for (int i = 1; i < ids.length - 2; i++) {
      Assert.assertTrue(iterator.hasNext());
      Assert.assertEquals(ids[i], iterator.next());
    }
    Assert.assertFalse(iterator.hasNext());

    idList.close();

    idList = dmaIdListFactory.build(filename);
    idList.add(ids[ids.length - 1]);

    iterator = idList.iterator();
    for (long id : ids) {
      Assert.assertTrue(iterator.hasNext());
      Assert.assertEquals(id, iterator.next());
    }
    Assert.assertFalse(iterator.hasNext());
  }

  private DmaIdList.Factory getDmaIdListFactory() {
    Injector injector = createInjectorFor(
        new Properties(), new DmaIdListModule());
    return injector.getInstance(DmaIdList.Factory.class);
  }
}
