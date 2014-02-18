package com.mobicrave.eventtracker.list;

import com.mobicrave.eventtracker.base.Schema;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.nio.ByteBuffer;

public class DmaListTest {
  @Rule
  public TemporaryFolder folder = new TemporaryFolder();

  @Test
  public void testAll() throws Exception {
    int numRecordsPerFile = 2;
    int cacheSize = 1;
    String directory = folder.newFolder().getCanonicalPath();
    DmaList<MetaData> list = DmaList.build(MetaData.getSchema(), directory, numRecordsPerFile, cacheSize);
    MetaData[] metaDatas = new MetaData[] {
        new MetaData(1, new byte[] { 0b00000000, 0, 0, 0, 0b00101010, 0, 0, 0, 0b00110011, 0, 0, 0, 0b00000001 } ),
        new MetaData(3, new byte[] { 0b00000010, 0, 0, 0, 0b00101101, 0, 0, 0, 0b01001100, 0, 0, 0, 0b00100001 } ),
        new MetaData(5, new byte[] { 0b00000100, 0, 0, 0, 0b00100101, 0, 0, 0, 0b01010101, 0, 0, 0, 0b01000001 } ),
        new MetaData(7, new byte[] { 0b01000100, 0, 0, 0, 0b01101110, 0, 0, 0, 0b00011110, 0, 0, 0, 0b01000101 } )
    };

    for (int i = 0; i < metaDatas.length - 1; i++) {
      list.add(metaDatas[i]);
    }
    for (int i = 0; i < metaDatas.length - 1; i++) {
      Assert.assertEquals(metaDatas[i].getUserId(), list.get(i).getUserId());
      Assert.assertArrayEquals(metaDatas[i].getLocation(), list.get(i).getLocation());
    }

    list.close();
    list = DmaList.build(MetaData.getSchema(), directory, numRecordsPerFile, cacheSize);
    list.add(metaDatas[3]);
    for (int i = 0; i < metaDatas.length; i++) {
      Assert.assertEquals(metaDatas[i].getUserId(), list.get(i).getUserId());
      Assert.assertArrayEquals(metaDatas[i].getLocation(), list.get(i).getLocation());
    }
  }

  private static class MetaData {
    private final long userId;
    private final byte[] location;

    public MetaData(long userId, byte[] location) {
      this.userId = userId;
      this.location = location;
    }

    public long getUserId() {
      return userId;
    }

    public byte[] getLocation() {
      return location;
    }

    public static Schema<MetaData> getSchema() {
      return new MetaDataSchema();
    }

    private static class MetaDataSchema implements Schema<MetaData> {
      @Override
      public int getObjectSize() {
        return 8 + 13 + 4;
      }

      @Override
      public byte[] toBytes(MetaData metaData) {
        ByteBuffer byteBuffer = ByteBuffer.allocate(getObjectSize());
        byteBuffer.putLong(metaData.userId)
            .put(metaData.location);
        return byteBuffer.array();
      }

      @Override
      public MetaData fromBytes(byte[] bytes) {
        ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
        long userId = byteBuffer.getLong();
        byte[] location = new byte[13];
        byteBuffer.get(location);
        return new MetaData(userId, location);
      }
    }
  }
}
