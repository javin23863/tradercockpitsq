package com.strategyquant.datalib.data.io.newDataFormat;

import com.strategyquant.lib.memory.OffHeapMemory;
import java.io.IOException;

public class RandomAccessReaderOffheap implements IRandomAccessReader {
   private long address = -2L;
   private long index;
   private long size;

   public RandomAccessReaderOffheap(long var1, long var3) throws Exception {
      this.address = var1;
      this.size = var3;
      this.index = 0L;
   }

   @Override
   public final boolean dataRemaining() {
      return this.index < this.size;
   }

   @Override
   public final byte readByte() throws IOException {
      this.checkOffHeap();
      byte var1 = OffHeapMemory.getByte(this.address + this.index);
      this.index++;
      return var1;
   }

   @Override
   public final void readBytes(byte[] var1) throws IOException {
      this.checkOffHeap();

      for (int var2 = 0; var2 < var1.length; var2++) {
         var1[var2] = OffHeapMemory.getByte(this.address + this.index);
         this.index++;
      }
   }

   @Override
   public final short readShort() throws IOException {
      this.checkOffHeap();
      short var1 = OffHeapMemory.getShort(this.address + this.index);
      this.index += 2L;
      return var1;
   }

   @Override
   public final int readInt() throws IOException {
      this.checkOffHeap();
      int var1 = OffHeapMemory.getInt(this.address + this.index);
      this.index += 4L;
      return var1;
   }

   @Override
   public final long readLong() throws IOException {
      this.checkOffHeap();
      long var1 = OffHeapMemory.getLong(this.address + this.index);
      this.index += 8L;
      return var1;
   }

   @Override
   public final String readUTF() throws IOException {
      this.readByte();
      byte var1 = this.readByte();
      byte[] var2 = new byte[var1];
      this.readBytes(var2);
      return new String(var2);
   }

   public final void put(byte var1) {
      this.checkOffHeap();
      OffHeapMemory.putByte(this.address + this.index, var1);
      this.index++;
   }

   public final void putLong(long var1) {
      this.checkOffHeap();
      OffHeapMemory.putLong(this.address + this.index, var1);
      this.index += 8L;
   }

   public final void put(byte[] var1) {
      this.checkOffHeap();

      for (int var2 = 0; var2 < var1.length; var2++) {
         this.put(var1[var2]);
      }
   }

   public final void putInt(int var1) {
      this.checkOffHeap();
      OffHeapMemory.putInt(this.address + this.index, var1);
      this.index += 4L;
   }

   public final void putShort(short var1) {
      this.checkOffHeap();
      OffHeapMemory.putShort(this.address + this.index, var1);
      this.index += 2L;
   }

   public void checkOffHeap() {
      if (this.address < 0L) {
         OffHeapMemory.throwMemoryException(this.address);
      }
   }

   @Override
   public double readDouble() throws IOException {
      this.checkOffHeap();
      double var1 = OffHeapMemory.getDouble(this.address + this.index);
      this.index += 8L;
      return var1;
   }

   @Override
   public float readFloat() throws IOException {
      this.checkOffHeap();
      float var1 = OffHeapMemory.getFloat(this.address + this.index);
      this.index += 4L;
      return var1;
   }

   @Override
   public void seek(long var1) throws IOException {
      this.index = var1;
   }

   @Override
   public long getLength() throws IOException {
      return this.size;
   }

   @Override
   public long getPosition() throws IOException {
      return this.index;
   }
}
