package com.strategyquant.datalib.data.io.newDataFormat;

import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

public abstract class NewDataFormatWritter extends NewDataFormat {
   private long savedCnt = 0L;
   public static final byte[] BLOCK_START_MAGIC = new byte[15];

   public NewDataFormatWritter(boolean var1) {
      super(var1);
   }

   protected void writeMagicChain(DataOutputStream var1) throws IOException {
      var1.write(BLOCK_START_MAGIC, 0, BLOCK_START_MAGIC.length);
      int var2 = (int)((this.savedCnt + 1L) / 1000L);
      var1.writeInt(var2);
   }

   protected void writeMagicChain(ByteBuffer var1) {
      var1.put(BLOCK_START_MAGIC);
   }

   protected void writeMagicChain(RandomAccessReaderOffheap var1) {
      var1.put(BLOCK_START_MAGIC);
   }

   protected boolean shouldSaveStartChain() {
      return this.savedCnt % 1000L == 0L;
   }

   protected void nextSaved() {
      this.savedCnt++;
   }

   protected boolean shouldWriteFullData() {
      return this.start || !this.isForceOldFormat() && this.shouldSaveStartChain();
   }

   public long getSavedCount() {
      return this.savedCnt;
   }

   static {
      for (int var0 = 0; var0 < BLOCK_START_MAGIC.length; var0++) {
         BLOCK_START_MAGIC[var0] = (byte)var0;
      }
   }
}
