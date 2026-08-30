package com.strategyquant.datalib.data.io.newDataFormat;

import com.strategyquant.datalib.data.io.VersatileData;
import java.io.DataOutputStream;
import java.nio.ByteBuffer;

public class OhlcDataWriter extends NewDataFormatWritter {
   private long prevTime;
   private long prevOpen;
   private long prevHigh;
   private long prevLow;
   private long prevClose;
   private long prevVolume;

   public OhlcDataWriter() {
      this(false);
   }

   public OhlcDataWriter(boolean var1) {
      super(var1);
   }

   public void writeData(DataOutputStream var1, VersatileData var2) throws Exception {
      long var3 = Math.round(var2.open * this.decimalsConstant);
      long var5 = Math.round(var2.high * this.decimalsConstant);
      long var7 = Math.round(var2.low * this.decimalsConstant);
      long var9 = Math.round(var2.close * this.decimalsConstant);
      long var11 = Math.round(var2.volume * this.volumeConstant);
      if (var11 < 0L) {
         var11 = Long.MAX_VALUE;
      }

      if (this.shouldWriteFullData()) {
         this.start = false;
         if (!this.isForceOldFormat()) {
            this.writeMagicChain(var1);
         }

         int[] var13 = new int[]{2, 2, 2, 2, 2, 2};
         int[] var14 = new int[]{3, 3, 3, 3, 3, 3};
         var1.write(this.getConfigBytes(var13, var14));
         var1.writeLong(var2.time);
         var1.writeLong(var3);
         var1.writeLong(var5);
         var1.writeLong(var7);
         var1.writeLong(var9);
         var1.writeLong(var11);
      } else {
         long var27 = var2.time - this.prevTime;
         long var15 = var3 - this.prevOpen;
         long var17 = var5 - this.prevHigh;
         long var19 = var7 - this.prevLow;
         long var21 = var9 - this.prevClose;
         long var23 = var11 - this.prevVolume;
         int[] var25 = new int[]{getLogicType(var27), getLogicType(var15), getLogicType(var17), getLogicType(var19), getLogicType(var21), getLogicType(var23)};
         int[] var26 = new int[]{getDataType(var27), getDataType(var15), getDataType(var17), getDataType(var19), getDataType(var21), getDataType(var23)};
         var1.write(this.getConfigBytes(var25, var26));
         this.writeValue(var1, var27, var26[0]);
         this.writeValue(var1, var15, var26[1]);
         this.writeValue(var1, var17, var26[2]);
         this.writeValue(var1, var19, var26[3]);
         this.writeValue(var1, var21, var26[4]);
         this.writeValue(var1, var23, var26[5]);
      }

      this.prevTime = var2.time;
      this.prevOpen = var3;
      this.prevHigh = var5;
      this.prevLow = var7;
      this.prevClose = var9;
      this.prevVolume = var11;
      this.nextSaved();
   }

   public void putData(ByteBuffer var1, VersatileData var2) throws Exception {
      long var3 = Math.round(var2.open * this.decimalsConstant);
      long var5 = Math.round(var2.high * this.decimalsConstant);
      long var7 = Math.round(var2.low * this.decimalsConstant);
      long var9 = Math.round(var2.close * this.decimalsConstant);
      long var11 = Math.round(var2.volume * this.volumeConstant);
      if (this.shouldWriteFullData()) {
         this.start = false;
         if (!this.isForceOldFormat()) {
            this.writeMagicChain(var1);
         }

         int[] var13 = new int[]{2, 2, 2, 2, 2, 2};
         int[] var14 = new int[]{3, 3, 3, 3, 3, 3};
         var1.put(this.getConfigBytes(var13, var14));
         var1.putLong(var2.time);
         var1.putLong(var3);
         var1.putLong(var5);
         var1.putLong(var7);
         var1.putLong(var9);
         var1.putLong(var11);
      } else {
         long var27 = var2.time - this.prevTime;
         long var15 = var3 - this.prevOpen;
         long var17 = var5 - this.prevHigh;
         long var19 = var7 - this.prevLow;
         long var21 = var9 - this.prevClose;
         long var23 = var11 - this.prevVolume;
         int[] var25 = new int[]{getLogicType(var27), getLogicType(var15), getLogicType(var17), getLogicType(var19), getLogicType(var21), getLogicType(var23)};
         int[] var26 = new int[]{getDataType(var27), getDataType(var15), getDataType(var17), getDataType(var19), getDataType(var21), getDataType(var23)};
         var1.put(this.getConfigBytes(var25, var26));
         this.putValue(var1, var27, var26[0]);
         this.putValue(var1, var15, var26[1]);
         this.putValue(var1, var17, var26[2]);
         this.putValue(var1, var19, var26[3]);
         this.putValue(var1, var21, var26[4]);
         this.putValue(var1, var23, var26[5]);
      }

      this.prevTime = var2.time;
      this.prevOpen = var3;
      this.prevHigh = var5;
      this.prevLow = var7;
      this.prevClose = var9;
      this.prevVolume = var11;
      this.nextSaved();
   }

   public void putData(RandomAccessReaderOffheap var1, VersatileData var2) throws Exception {
      long var3 = Math.round(var2.open * this.decimalsConstant);
      long var5 = Math.round(var2.high * this.decimalsConstant);
      long var7 = Math.round(var2.low * this.decimalsConstant);
      long var9 = Math.round(var2.close * this.decimalsConstant);
      long var11 = Math.round(var2.volume * this.volumeConstant);
      if (this.shouldWriteFullData()) {
         this.start = false;
         if (!this.isForceOldFormat()) {
            this.writeMagicChain(var1);
         }

         int[] var13 = new int[]{2, 2, 2, 2, 2, 2};
         int[] var14 = new int[]{3, 3, 3, 3, 3, 3};
         var1.put(this.getConfigBytes(var13, var14));
         var1.putLong(var2.time);
         var1.putLong(var3);
         var1.putLong(var5);
         var1.putLong(var7);
         var1.putLong(var9);
         var1.putLong(var11);
      } else {
         long var27 = var2.time - this.prevTime;
         long var15 = var3 - this.prevOpen;
         long var17 = var5 - this.prevHigh;
         long var19 = var7 - this.prevLow;
         long var21 = var9 - this.prevClose;
         long var23 = var11 - this.prevVolume;
         int[] var25 = new int[]{getLogicType(var27), getLogicType(var15), getLogicType(var17), getLogicType(var19), getLogicType(var21), getLogicType(var23)};
         int[] var26 = new int[]{getDataType(var27), getDataType(var15), getDataType(var17), getDataType(var19), getDataType(var21), getDataType(var23)};
         var1.put(this.getConfigBytes(var25, var26));
         this.putValue(var1, var27, var26[0]);
         this.putValue(var1, var15, var26[1]);
         this.putValue(var1, var17, var26[2]);
         this.putValue(var1, var19, var26[3]);
         this.putValue(var1, var21, var26[4]);
         this.putValue(var1, var23, var26[5]);
      }

      this.prevTime = var2.time;
      this.prevOpen = var3;
      this.prevHigh = var5;
      this.prevLow = var7;
      this.prevClose = var9;
      this.prevVolume = var11;
      this.nextSaved();
   }

   private void putValue(RandomAccessReaderOffheap var1, long var2, int var4) throws Exception {
      if (var2 < 0L) {
         var2 = -var2;
      }

      switch (var4) {
         case 0:
            var1.put((byte)var2);
            break;
         case 1:
            var1.putShort((short)var2);
            break;
         case 2:
            var1.putInt((int)var2);
            break;
         case 3:
            var1.putLong(var2);
            break;
         default:
            throw new Exception("Invalid data type of value " + var4);
      }
   }

   private void putValue(ByteBuffer var1, long var2, int var4) throws Exception {
      if (var2 < 0L) {
         var2 = -var2;
      }

      switch (var4) {
         case 0:
            var1.put((byte)var2);
            break;
         case 1:
            var1.putShort((short)var2);
            break;
         case 2:
            var1.putInt((int)var2);
            break;
         case 3:
            var1.putLong(var2);
            break;
         default:
            throw new Exception("Invalid data type of value " + var4);
      }
   }
}
