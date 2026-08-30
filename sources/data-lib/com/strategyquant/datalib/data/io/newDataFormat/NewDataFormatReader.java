package com.strategyquant.datalib.data.io.newDataFormat;

import java.io.IOException;
import java.nio.ByteBuffer;

public abstract class NewDataFormatReader extends NewDataFormat {
   private byte[] loadStartChainTmp = null;
   private long loadedCnt = 0L;
   private long dataStartPosition = 0L;
   private boolean newFormat;

   public NewDataFormatReader(boolean var1) {
      super(var1);
   }

   public void setDataStartPosition(long var1) {
      this.dataStartPosition = var1;
   }

   protected void checkFormat(IRandomAccessReader var1) throws IOException {
      var1.seek(this.dataStartPosition);
      this.newFormat = this.readMagicChain(var1, true) != -1;
      var1.seek(this.dataStartPosition);
   }

   protected void setNewFormat(boolean var1) {
      this.newFormat = var1;
   }

   protected boolean isNewFormat() {
      return this.newFormat;
   }

   public void seek(IRandomAccessReader var1, long var2) throws Exception {
      var1.seek(this.dataStartPosition);
      int var4 = (int)(var2 / 1000L);
      if (var4 > 0) {
         long var5 = this.gotoChain(var1, var4);
         var1.seek(var5);
      }

      long var7 = var2 - var4 * 1000;
      this.loadedCnt = var4 * 1000;
      this.start = false;
      this.newFormat = true;
      this.readAndTrashNRecords(var7, var1);
   }

   protected boolean shouldLoadStartChain() {
      return this.loadedCnt % 1000L == 0L;
   }

   protected void nextLoaded() {
      this.loadedCnt++;
   }

   protected int readMagicChain(IRandomAccessReader var1, boolean var2) throws IOException {
      if (this.loadStartChainTmp == null) {
         this.loadStartChainTmp = new byte[15];
      }

      var1.readBytes(this.loadStartChainTmp);
      int var3 = 0;
      if (var2) {
         for (byte var7 : this.loadStartChainTmp) {
            if (var7 != var3) {
               return -1;
            }

            var3++;
         }
      }

      long var8 = var1.getPosition();
      return var1.readInt();
   }

   private long gotoChain(IRandomAccessReader var1, int var2) throws IOException {
      long var3 = var1.getLength();
      int var5 = this.getMinimalChainSize();
      int var6 = this.getMaximalChainSize();
      long var7 = (long)var5 * var2;
      var1.seek(var7);

      while (true) {
         long var9 = this.findNextChain(var1, var7, var3);
         if (var9 == -1L) {
            throw new IOException("Seek parameter is out of range");
         }

         var1.seek(var9 + 15L);
         int var11 = this.getBlockIndex(var1, var9 + 15L);
         if (var11 == var2) {
            return var9;
         }

         int var12 = var2 - var11;
         int var13 = var12 > 0 ? var12 * var5 : var12 * var6;
         var7 = var9 + var13;
         var1.seek(var7);
      }
   }

   private int getMinimalChainSize() {
      return 1000 * this.getMinimalRecordSize() + 15;
   }

   private int getMaximalChainSize() {
      return 1000 * this.getMaximalRecordSize() + 15;
   }

   protected int getMinimalRecordSize() {
      return 1;
   }

   protected int getMaximalRecordSize() {
      return 1;
   }

   private long findNextChain(IRandomAccessReader var1, long var2, long var4) throws IOException {
      int var6 = 2048;
      int var7 = 0;
      long var8 = 0L;

      while (true) {
         long var10 = var4 - var2;
         if (var10 < 0L) {
            return -1L;
         }

         if (var10 < var6) {
            var6 = (int)var10;
         }

         byte[] var12 = new byte[var6];
         var1.readBytes(var12);

         for (int var13 = 0; var13 < var6; var13++) {
            byte var14 = var12[var13];
            if (var14 != var7 && var14 != 0) {
               var7 = 0;
            } else {
               if (var14 == 0) {
                  var8 = var2 + var13;
                  var7 = 0;
               }

               var7++;
            }

            if (var7 == 14) {
               return var8;
            }
         }

         if (var6 != 2048) {
            return -1L;
         }

         var2 += var6;
      }
   }

   private int getBlockIndex(IRandomAccessReader var1, long var2) throws IOException {
      var1.seek(var2 - 20L);
      byte[] var4 = new byte[30];
      var1.readBytes(var4);
      var1.seek(var2);
      byte[] var5 = new byte[4];
      var1.readBytes(var5);
      ByteBuffer var6 = ByteBuffer.wrap(var5);
      return var6.getInt();
   }

   protected void readAndTrashNRecords(long var1, IRandomAccessReader var3) throws Exception {
   }
}
