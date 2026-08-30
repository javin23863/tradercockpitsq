package com.strategyquant.datalib.data.io;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

public class FileChannelReader {
   private static final int BUFFER_SIZE = 65536;
   private ByteBuffer readerBuffer;
   private FileChannel readerFileChannel;
   private int dataSize = getDataSize();

   public FileChannelReader(FileChannel var1, int var2) {
      this.readerBuffer = ByteBuffer.allocate(65536);
      this.readerFileChannel = var1;
      this.readerBuffer.clear();
      this.readerBuffer.flip();
   }

   public static int getDataSize() {
      return 40;
   }

   private void ensureData(int var1) throws IOException {
      if (this.readerBuffer.remaining() < var1) {
         this.readerBuffer.compact();
         if (this.readerFileChannel.read(this.readerBuffer) <= 0) {
            throw new IOException("Unexpected end-of-stream");
         }

         this.readerBuffer.flip();
      }
   }

   public long position() throws IOException {
      return this.readerFileChannel.position() - this.readerBuffer.remaining();
   }

   public void position(long var1) throws IOException {
      this.readerFileChannel.position(var1);
      this.readerBuffer.clear();
      this.readerBuffer.flip();
   }

   public VersatileData getData(VersatileData var1) throws IOException {
      this.ensureData(this.dataSize);
      var1.time = this.readerBuffer.getLong();
      var1.ask = this.readerBuffer.getDouble();
      var1.bid = this.readerBuffer.getDouble();
      var1.volume = this.readerBuffer.getDouble();
      return var1;
   }
}
