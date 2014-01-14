package com.mobicrave.eventtracker;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class MemMappedListTest {
  @Rule
  public TemporaryFolder folder = new TemporaryFolder();

  @Test
  public void testAll() throws Exception {
    int defaultCapacity = 2;
    String filename = folder.newFolder().getCanonicalPath() + "/test.mem";
    MemMappedList<Event.MetaData> list = MemMappedList.build(Event.MetaData.getSchema(),
        filename, defaultCapacity);
    Event.MetaData[] metaDatas = new Event.MetaData[] {
        new Event.MetaData(1, 2, new byte[] { 0b00000000, 0, 0, 0, 0b00101010, 0, 0, 0, 0b00110011, 0, 0, 0, 0b00000001 } ),
        new Event.MetaData(3, 4, new byte[] { 0b00000010, 0, 0, 0, 0b00101101, 0, 0, 0, 0b01001100, 0, 0, 0, 0b00100001 } ),
        new Event.MetaData(5, 6, new byte[] { 0b00000100, 0, 0, 0, 0b00100101, 0, 0, 0, 0b01010101, 0, 0, 0, 0b01000001 } ),
        new Event.MetaData(7, 8, new byte[] { 0b01000100, 0, 0, 0, 0b01101110, 0, 0, 0, 0b00011110, 0, 0, 0, 0b01000101 } )
    };

    for (int i = 0; i < metaDatas.length - 1; i++) {
      list.add(metaDatas[i]);
    }
    for (int i = 0; i < metaDatas.length - 1; i++) {
      Assert.assertEquals(metaDatas[i].getUserId(), list.get(i).getUserId());
      Assert.assertEquals(metaDatas[i].getEventTypeId(), list.get(i).getEventTypeId());
      Assert.assertArrayEquals(metaDatas[i].getLocation(), list.get(i).getLocation());
    }

    list.close();
    list = MemMappedList.build(Event.MetaData.getSchema(), filename, defaultCapacity);
    list.add(metaDatas[3]);
    for (int i = 0; i < metaDatas.length; i++) {
      Assert.assertEquals(metaDatas[i].getUserId(), list.get(i).getUserId());
      Assert.assertEquals(metaDatas[i].getEventTypeId(), list.get(i).getEventTypeId());
      Assert.assertArrayEquals(metaDatas[i].getLocation(), list.get(i).getLocation());
    }
  }
}
