package com.strategyquant.datalib.data.io.newDataFormat;

import com.strategyquant.datalib.data.io.FuturesVersatileData;
import java.io.DataOutputStream;

public class FuturesDataWriter extends NewDataFormatWritter {
   private long prevTime;
   private long prevOpen;
   private long prevHigh;
   private long prevLow;
   private long prevClose;
   private long prevVolume;
   private long prevOpenInterest;

   public FuturesDataWriter() {
      this(false);
   }

   public FuturesDataWriter(boolean var1) {
      super(var1);
   }

   public void writeData(DataOutputStream var1, FuturesVersatileData var2) throws Exception {
      long var3 = Math.round(var2.open * this.decimalsConstant);
      long var5 = Math.round(var2.high * this.decimalsConstant);
      long var7 = Math.round(var2.low * this.decimalsConstant);
      long var9 = Math.round(var2.close * this.decimalsConstant);
      long var11 = Math.round(var2.volume * this.volumeConstant);
      long var13 = Math.round(var2.openInterest * this.volumeConstant);
      if (var11 < 0L) {
         var11 = Long.MAX_VALUE;
      }

      if (var13 < 0L) {
         var13 = Long.MAX_VALUE;
      }

      if (this.shouldWriteFullData()) {
         this.start = false;
         if (!this.isForceOldFormat()) {
            this.writeMagicChain(var1);
         }

         int[] var15 = new int[]{2, 2, 2, 2, 2, 2, 2, 2};
         int[] var16 = new int[]{3, 3, 3, 3, 3, 3, 3, 0};
         var1.write(this.getConfigBytes(var15, var16));
         var1.writeLong(var2.time);
         var1.writeLong(var3);
         var1.writeLong(var5);
         var1.writeLong(var7);
         var1.writeLong(var9);
         var1.writeLong(var11);
         var1.writeLong(var13);
      } else {
         long var31 = var2.time - this.prevTime;
         long var17 = var3 - this.prevOpen;
         long var19 = var5 - this.prevHigh;
         long var21 = var7 - this.prevLow;
         long var23 = var9 - this.prevClose;
         long var25 = var11 - this.prevVolume;
         long var27 = var13 - this.prevOpenInterest;
         int[] var29 = new int[]{
            getLogicType(var31),
            getLogicType(var17),
            getLogicType(var19),
            getLogicType(var21),
            getLogicType(var23),
            getLogicType(var25),
            getLogicType(var27),
            2
         };
         int[] var30 = new int[]{
            getDataType(var31), getDataType(var17), getDataType(var19), getDataType(var21), getDataType(var23), getDataType(var25), getDataType(var27), 0
         };
         var1.write(this.getConfigBytes(var29, var30));
         this.writeValue(var1, var31, var30[0]);
         this.writeValue(var1, var17, var30[1]);
         this.writeValue(var1, var19, var30[2]);
         this.writeValue(var1, var21, var30[3]);
         this.writeValue(var1, var23, var30[4]);
         this.writeValue(var1, var25, var30[5]);
         this.writeValue(var1, var27, var30[6]);
      }

      this.prevTime = var2.time;
      this.prevOpen = var3;
      this.prevHigh = var5;
      this.prevLow = var7;
      this.prevClose = var9;
      this.prevVolume = var11;
      this.prevOpenInterest = var13;
      this.nextSaved();
   }
}
