/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.cassandra.utils;

import java.nio.ByteBuffer;
import java.util.BitSet;

public class BloomFilter {
  private static final MurmurHash hasher = new MurmurHash();

  private BitSet bitSet;
  private int hashCount;

  public BloomFilter(int hashCount, BitSet bitSet) {
    this.hashCount = hashCount;
    this.bitSet = bitSet;
  }

  public boolean isPresent(String key) {
    for (int bucketIndex : getHashBuckets(
        ByteBuffer.wrap(key.getBytes()), hashCount, bitSet.size())) {
      if (!bitSet.get(bucketIndex)) {
        return false;
      }
    }
    return true;
  }

  public void add(String key) {
    for (int bucketIndex : getHashBuckets(
        ByteBuffer.wrap(key.getBytes()), hashCount, bitSet.size())) {
      bitSet.set(bucketIndex);
    }
  }

  public BitSet getBitSet() {
    return bitSet;
  }

  public String toString() {
    return bitSet.toString();
  }

  private static int[] getHashBuckets(ByteBuffer b, int hashCount, int max) {
    int[] result = new int[hashCount];
    int hash1 = hasher.hash(b.array(), b.position()+b.arrayOffset(), b.remaining(), 0);
    int hash2 = hasher.hash(b.array(), b.position()+b.arrayOffset(), b.remaining(), hash1);
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
}