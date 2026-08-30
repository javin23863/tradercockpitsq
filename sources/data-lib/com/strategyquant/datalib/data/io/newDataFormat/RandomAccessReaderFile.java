package com.strategyquant.datalib.data.io.newDataFormat;

import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

public class RandomAccessReaderFile implements IRandomAccessReader {
   private int bufferSize = 65536;
   private ByteBuffer buffer;
   private FileChannel fileChannel;
   private RandomAccessFile file;
   private String filePath;
   private long index = 0L;

   public RandomAccessReaderFile(String var1) throws Exception {
      this.filePath = var1;
      File var2 = new File(var1);
      if (!var2.exists()) {
         throw new Exception("File '" + var1 + "' doesn't exist");
      }

      if (var2.length() == 0L) {
         throw new Exception("File '" + var1 + "' is empty");
      }
   }

   public void openFile() throws IOException {
      this.file = new RandomAccessFile(this.filePath, "r");
      this.file.seek(0L);
      this.buffer = ByteBuffer.allocateDirect(this.bufferSize);
      this.fileChannel = this.file.getChannel();
      this.fileChannel.read(this.buffer);
      this.buffer.flip();
   }

   public void closeFile() throws IOException {
      this.fileChannel.close();
      this.file.close();
   }

   private void ensureData(int var1, boolean var2) throws IOException {
      if (this.buffer.remaining() < var1) {
         this.buffer.compact();
         int var3 = this.fileChannel.read(this.buffer);
         this.buffer.flip();
         if (var3 <= 0) {
            throw new EOFException();
         }
      }

      if (var2) {
         this.index += var1;
      }
   }

   @Override
   public boolean dataRemaining() {
      int var1 = this.buffer.remaining();
      if (var1 > 0) {
         return true;
      }

      try {
         this.ensureData(1, false);
      } catch (IOException var3) {
         return false;
      }

      return this.buffer.remaining() > 0;
   }

   @Override
   public byte readByte() throws IOException {
      this.ensureData(1, true);
      return this.buffer.get();
   }

   @Override
   public void readBytes(byte[] var1) throws IOException {
      this.ensureData(var1.length, true);
      this.buffer.get(var1);
   }

   @Override
   public short readShort() throws IOException {
      this.ensureData(2, true);
      return this.buffer.getShort();
   }

   @Override
   public int readInt() throws IOException {
      this.ensureData(4, true);
      return this.buffer.getInt();
   }

   @Override
   public long readLong() throws IOException {
      this.ensureData(8, true);
      return this.buffer.getLong();
   }

   @Override
   public double readDouble() throws IOException {
      this.ensureData(8, true);
      return this.buffer.getDouble();
   }

   @Override
   public String readUTF() throws IOException {
      this.ensureData(2, true);
      this.buffer.get();
      byte var1 = this.buffer.get();
      byte[] var2 = new byte[var1];
      this.ensureData(var1, true);
      this.buffer.get(var2);
      return new String(var2);
   }

   @Override
   public float readFloat() throws IOException {
      this.ensureData(4, true);
      return this.buffer.getFloat();
   }

   @Override
   public void seek(long var1) throws IOException {
      this.index = var1;
      this.file.seek(var1);
      this.buffer.clear();
      this.fileChannel.read(this.buffer);
      this.buffer.flip();
   }

   @Override
   public long getLength() throws IOException {
      return this.file.length();
   }

   @Override
   public long getPosition() throws IOException {
      return this.index;
   }
}
