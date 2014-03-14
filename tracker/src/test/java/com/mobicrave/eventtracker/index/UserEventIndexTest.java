package com.mobicrave.eventtracker.index;

import com.google.inject.Injector;
import com.mobicrave.eventtracker.integration.GuiceTestCase;
import org.junit.Assert;
import org.junit.Test;

import javax.inject.Provider;
import java.nio.ByteBuffer;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

public class UserEventIndexTest extends GuiceTestCase {
  @Test
  public void testAll() throws Exception {
    Provider<UserEventIndex> dmaUserEventIndexProvider = getDmaUserEventIndexProvider();
    UserEventIndex userEventIndex = dmaUserEventIndexProvider.get();
    userEventIndex.addEvent(2, 30);
    userEventIndex.addEvent(0, 10);
    userEventIndex.addEvent(1, 20);
    userEventIndex.addEvent(0, 40);
    userEventIndex.addEvent(1, 50);
    userEventIndex.addEvent(2, 60);
    userEventIndex.addEvent(0, 70);
    userEventIndex.addEvent(1, 80);
    userEventIndex.addEvent(2, 90);
    userEventIndex.addEvent(0, 100);
    userEventIndex.addEvent(1, 110);
    userEventIndex.addEvent(2, 120);

    IdVerificationCallback callback = new IdVerificationCallback(new int[] { 20, 50, 80, 110 });
    userEventIndex.enumerateEventIds(1, userEventIndex.getEventOffset(1, 1), Integer.MAX_VALUE, callback);
    callback.verify();

    callback = new IdVerificationCallback(new int[] { 50, 80, 110 });
    userEventIndex.enumerateEventIds(1, userEventIndex.getEventOffset(1, 50), Integer.MAX_VALUE, callback);
    callback.verify();

    callback = new IdVerificationCallback(new int[] { 20, 50, 80 });
    userEventIndex.enumerateEventIds(1, userEventIndex.getEventOffset(1, 20), 3, callback);
    callback.verify();

    userEventIndex.close();
    userEventIndex = dmaUserEventIndexProvider.get();

    callback = new IdVerificationCallback(new int[] { 20, 50, 80, 110 });
    userEventIndex.enumerateEventIds(1, userEventIndex.getEventOffset(1, 1), Integer.MAX_VALUE, callback);
    callback.verify();

    callback = new IdVerificationCallback(new int[] { 50, 80, 110 });
    userEventIndex.enumerateEventIds(1, userEventIndex.getEventOffset(1, 50), Integer.MAX_VALUE, callback);
    callback.verify();

    callback = new IdVerificationCallback(new int[] { 20, 50, 80 });
    userEventIndex.enumerateEventIds(1, userEventIndex.getEventOffset(1, 20), 3, callback);
    callback.verify();
  }

  private static class IdVerificationCallback implements UserEventIndex.Callback {
    private final int[] expectedIds;
    private int counter;

    public IdVerificationCallback(int[] expectedIds) {
      this.expectedIds = expectedIds;
      this.counter = 0;
    }

    @Override
    public boolean shouldContinueOnEventId(long eventId) {
      Assert.assertEquals(expectedIds[counter++], eventId);
      return true;
    }

    public void verify() {
      Assert.assertEquals(expectedIds.length, counter);
    }
  }

  @Test
  public void testBlock() throws Exception {
    int blockOffset = 10;
    long pointer = 2000;
    int minId = 5;
    ByteBuffer metaDataByteBuffer = ByteBuffer.allocate(UserEventIndex.Block.MetaData.SIZE);
    ByteBuffer blockByteBuffer = ByteBuffer.allocate(1000);
    long prevBlockPointer = 1000;
    long nextBlockPointer = 3000;

    UserEventIndex.Block.MetaData metaData = new UserEventIndex.Block.MetaData(metaDataByteBuffer);
    UserEventIndex.Block block = new UserEventIndex.Block(metaData, blockByteBuffer);
    metaData.setBlockOffset(blockOffset);
    metaData.setPointer(pointer);
    metaData.setMinId(minId);
    metaData.setNextBlockPointer(nextBlockPointer);
    metaData.setPrevBlockPointer(prevBlockPointer);
    metaData.setNumRecords(0);

    long[] records = new long[] { 10, 20, 30 };
    for (long record : records) {
      block.add(record);
    }
    Assert.assertEquals(minId, block.getMetaData().getMinId());
    Assert.assertEquals(pointer, block.getMetaData().getPointer());
    Assert.assertEquals(prevBlockPointer, block.getMetaData().getPrevBlockPointer());
    Assert.assertEquals(nextBlockPointer, block.getMetaData().getNextBlockPointer());
    for (int i = 0; i < records.length; i++) {
      Assert.assertEquals(records[i], block.getRecord(i));
    }
    Assert.assertEquals(0, block.findOffset(10));
    Assert.assertEquals(1, block.findOffset(20));
    Assert.assertEquals(2, block.findOffset(25));
    Assert.assertEquals(2, block.findOffset(30));
    Assert.assertEquals(3, block.findOffset(40));

    block = new UserEventIndex.Block(new UserEventIndex.Block.MetaData(metaDataByteBuffer),
        blockByteBuffer);
    Assert.assertEquals(minId, block.getMetaData().getMinId());
    Assert.assertEquals(pointer, block.getMetaData().getPointer());
    Assert.assertEquals(prevBlockPointer, block.getMetaData().getPrevBlockPointer());
    Assert.assertEquals(nextBlockPointer, block.getMetaData().getNextBlockPointer());
    for (int i = 0; i < records.length; i++) {
      Assert.assertEquals(records[i], block.getRecord(i));
    }
    Assert.assertEquals(0, block.findOffset(10));
    Assert.assertEquals(1, block.findOffset(20));
    Assert.assertEquals(2, block.findOffset(25));
    Assert.assertEquals(2, block.findOffset(30));
    Assert.assertEquals(3, block.findOffset(40));
  }

  @Test
  public void testIndexEntry() throws Exception {
    int numRecords = 1;
    long minId = 10L;
    int numPointers = 3;
    long[] pointers = new long[] { 10L, -1, -1 };
    long[] minIds = new long[] { 1L, -1, -1 };
    UserEventIndex.IndexEntry indexEntry = new UserEventIndex.IndexEntry(
        new AtomicInteger(numRecords), minId, pointers, minIds);
    UserEventIndex.IndexEntry.Schema schema = new UserEventIndex.IndexEntry.Schema(
        numPointers);

    Assert.assertEquals(minId, indexEntry.getMinId());
    Assert.assertEquals(numRecords, indexEntry.getNumRecords());
    indexEntry.incrementNumRecord();
    Assert.assertEquals(numRecords + 1, indexEntry.getNumRecords());
    Assert.assertEquals(10L, indexEntry.getPointer(0));
    Assert.assertEquals(-1, indexEntry.getPointer(1));
    Assert.assertEquals(1L, indexEntry.getMinIdInIndex(0));
    Assert.assertEquals(-1, indexEntry.getMinIdInIndex(1));

    for (int i = 1; i < 5; i++) {
      UserEventIndex.Block.MetaData metaData = new UserEventIndex.Block.MetaData(
          ByteBuffer.allocate(UserEventIndex.Block.MetaData.SIZE));
      metaData.setBlockOffset(i);
      metaData.setPointer(i * 100);
      metaData.setMinId(i * 10);
      indexEntry.shiftBlock(new UserEventIndex.Block(metaData, null));
    }
    Assert.assertEquals(400L, indexEntry.getPointer(0));
    Assert.assertEquals(300L, indexEntry.getPointer(1));
    Assert.assertEquals(200L, indexEntry.getPointer(2));
    Assert.assertEquals(40L, indexEntry.getMinIdInIndex(0));
    Assert.assertEquals(30L, indexEntry.getMinIdInIndex(1));
    Assert.assertEquals(20L, indexEntry.getMinIdInIndex(2));

    indexEntry = schema.fromBytes(schema.toBytes(indexEntry));
    Assert.assertEquals(minId, indexEntry.getMinId());
    Assert.assertEquals(numRecords + 1, indexEntry.getNumRecords());
    Assert.assertEquals(400L, indexEntry.getPointer(0));
    Assert.assertEquals(300L, indexEntry.getPointer(1));
    Assert.assertEquals(200L, indexEntry.getPointer(2));
    Assert.assertEquals(40L, indexEntry.getMinIdInIndex(0));
    Assert.assertEquals(30L, indexEntry.getMinIdInIndex(1));
    Assert.assertEquals(20L, indexEntry.getMinIdInIndex(2));
  }

  private Provider<UserEventIndex> getDmaUserEventIndexProvider() {
    Properties prop = new Properties();
    prop.put("eventtracker.directory", getTempDirectory());
    prop.put("eventtracker.usereventindex.numPointersPerIndexEntry", "2");
    prop.put("eventtracker.usereventindex.numIndexEntryPerFile", "2");
    prop.put("eventtracker.usereventindex.indexEntryFileCacheSize", "2");
    prop.put("eventtracker.usereventindex.numRecordsPerBlock", "2");
    prop.put("eventtracker.usereventindex.numBlocksPerFile", "2");
    prop.put("eventtracker.usereventindex.blockCacheSize", "2");

    Injector injector = createInjectorFor(
        prop, new UserEventIndexModule());
    return injector.getProvider(UserEventIndex.class);
  }
}
