package com.strategyquant.datalib.data.io.newDataFormat;

import java.io.IOException;
import java.nio.ByteBuffer;

public class RandomAccessReaderArray implements IRandomAccessReader {
   private long size;
   private ByteBuffer buffer;

   public RandomAccessReaderArray(byte[] var1) {
      this.size = var1.length;
      this.buffer = ByteBuffer.wrap(var1);
   }

   @Override
   public boolean dataRemaining() {
      try {
         return this.getPosition() < this.size;
      } catch (Exception var2) {
         throw new RuntimeException("Exc.", var2);
      }
   }

   @Override
   public byte readByte() throws IOException {
      return this.buffer.get();
   }

   @Override
   public void readBytes(byte[] var1) throws IOException {
      this.buffer.get(var1);
   }

   @Override
   public short readShort() throws IOException {
      return this.buffer.getShort();
   }

   @Override
   public int readInt() throws IOException {
      return this.buffer.getInt();
   }

   @Override
   public long readLong() throws IOException {
      return this.buffer.getLong();
   }

   @Override
   public String readUTF() throws IOException {
      this.readByte();
      byte var1 = this.readByte();
      byte[] var2 = new byte[var1];
      this.readBytes(var2);
      return new String(var2);
   }

   @Override
   public double readDouble() throws IOException {
      return this.buffer.getDouble();
   }

   @Override
   public float readFloat() throws IOException {
      return this.buffer.getFloat();
   }

   @Override
   public void seek(long var1) throws IOException {
      this.buffer.position((int)var1);
   }

   @Override
   public long getPosition() throws IOException {
      return this.buffer.position();
   }

   @Override
   public long getLength() throws IOException {
      return this.size;
   }
}
