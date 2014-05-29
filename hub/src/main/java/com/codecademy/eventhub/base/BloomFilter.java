package com.codecademy.eventhub.base;

import com.google.common.hash.Hashing;

import java.nio.ByteBuffer;
import java.util.BitSet;

public class BloomFilter {
  private BitSet bitSet;
  private int hashCount;

  public BloomFilter(int hashCount, BitSet bitSet) {
    this.hashCount = hashCount;
    this.bitSet = bitSet;
  }

  public boolean isPresent(String key) {
    for (int bucketIndex : getHashBuckets(key.getBytes(), hashCount, bitSet.size())) {
      if (!bitSet.get(bucketIndex)) {
        return false;
      }
    }
    return true;
  }

  public void add(String key) {
    for (int bucketIndex : getHashBuckets(key.getBytes(), hashCount, bitSet.size())) {
      bitSet.set(bucketIndex);
    }
  }

  public BitSet getBitSet() {
    return bitSet;
  }

  @Override
  public String toString() {
    return bitSet.toString();
  }

  private static int[] getHashBuckets(byte[] bytes, int hashCount, int max) {
    int[] result = new int[hashCount];
    int hash1 = Hashing.murmur3_128().hashBytes(bytes).asInt();
    int hash2 = Hashing.murmur3_128(hash1).hashBytes(bytes).asInt();
    for (int i = 0; i < hashCount; i++) {
      result[i] = Math.abs((hash1 + i * hash2) % max);
    }
    return result;
  }

  public static BloomFilter build(int numHashes, int bloomFilterSize) {
    BitSet bitSet = new BitSet(bloomFilterSize * 8);
    // sacrifice 1 bit to ensure the size of the bitset to BLOOM_FILTER_SIZE
    bitSet.set(bloomFilterSize * 8 - 1);
    return new BloomFilter(numHashes, bitSet);
  }

  public static class Schema implements com.codecademy.eventhub.base.Schema<BloomFilter> {
    private final int numHashes;
    private final int bloomFilterSize;

    public Schema(int numHashes, int bloomFilterSize) {
      this.numHashes = numHashes;
      this.bloomFilterSize = bloomFilterSize;
    }

    @Override
    public int getObjectSize() {
      return bloomFilterSize;
    }

    @Override
    public byte[] toBytes(BloomFilter bloomFilter) {
      ByteBuffer byteBuffer = ByteBuffer.allocate(getObjectSize());
      byteBuffer.put(bloomFilter.getBitSet().toByteArray());
      return byteBuffer.array();
    }

    @Override
    public BloomFilter fromBytes(byte[] bytes) {
      ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
      byte[] bloomFilter = new byte[bloomFilterSize];
      byteBuffer.get(bloomFilter);
      return new BloomFilter(numHashes, BitSet.valueOf(bloomFilter));
    }
  }
}
