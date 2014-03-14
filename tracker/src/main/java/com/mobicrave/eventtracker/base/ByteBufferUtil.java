package com.mobicrave.eventtracker.base;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public class ByteBufferUtil {
  public static MappedByteBuffer createNewBuffer(String filename, int fileSize) {
    try (RandomAccessFile raf = new RandomAccessFile(filename, "rw")) {
      return raf.getChannel().map(FileChannel.MapMode.READ_WRITE, 0, fileSize);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static MappedByteBuffer expandBuffer(String filename, MappedByteBuffer buffer,
      long newSize) {
    buffer.force();
    int oldPosition = buffer.position();
    try (RandomAccessFile raf = new RandomAccessFile(filename, "rw")) {
      raf.setLength(newSize);
      buffer = raf.getChannel().map(FileChannel.MapMode.READ_WRITE, 0, raf.length());
      buffer.position(oldPosition);
      return buffer;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static int binarySearchOffset(ByteBuffer buffer, int startOffset, int endOffset,
      long id, int recordSize) {
    if (startOffset == endOffset) {
      return endOffset;
    }
    int offset = (startOffset + endOffset) >>> 1;
    long value = buffer.getLong(offset * recordSize);
    if (value == id) {
      return offset;
    } else if (value < id) {
      return binarySearchOffset(buffer, offset + 1, endOffset, id, recordSize);
    } else {
      return binarySearchOffset(buffer, startOffset, offset, id, recordSize);
    }
  }
}
