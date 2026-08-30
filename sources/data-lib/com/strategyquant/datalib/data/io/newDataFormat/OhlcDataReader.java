package com.strategyquant.datalib.data.io.newDataFormat;

import com.strategyquant.datalib.data.io.VersatileData;
import java.io.EOFException;

public class OhlcDataReader extends NewDataFormatReader {
   private long prevTime;
   private long prevOpen;
   private long prevHigh;
   private long prevLow;
   private long prevClose;
   private long prevVolume;
   private byte[] configBytes = new byte[3];

   public OhlcDataReader() {
      this(false);
   }

   public OhlcDataReader(boolean var1) {
      super(var1);
   }

   @Override
   protected int getMinimalRecordSize() {
      return 7;
   }

   @Override
   protected int getMaximalRecordSize() {
      return 42;
   }

   @Override
   protected void readAndTrashNRecords(long var1, IRandomAccessReader var3) throws Exception {
      VersatileData var4 = new VersatileData();

      for (int var5 = 0; var5 < var1; var5++) {
         this.readData(var3, var4);
      }
   }

   public void readData(IRandomAccessReader var1, VersatileData var2) throws Exception {
      if (!var1.dataRemaining()) {
         throw new EOFException();
      }

      if (this.start) {
         if (this.isForceOldFormat()) {
            this.setNewFormat(false);
         } else {
            this.checkFormat(var1);
         }
      }

      if (this.isNewFormat() && this.shouldLoadStartChain()) {
         this.readMagicChain(var1, false);
      }

      var1.readBytes(this.configBytes);
      this.start = true;
      int var3 = this.getBitValue(this.configBytes, 2);
      int var4 = this.getBitValue(this.configBytes, 2);
      int var5 = this.getBitValue(this.configBytes, 2);
      int var6 = this.getBitValue(this.configBytes, 2);
      this.start = true;
      int var7 = this.getBitValue(this.configBytes, 1);
      int var8 = this.getBitValue(this.configBytes, 1);
      int var9 = this.getBitValue(this.configBytes, 1);
      int var10 = this.getBitValue(this.configBytes, 1);
      this.start = true;
      int var11 = this.getBitValue(this.configBytes, 0);
      int var12 = this.getBitValue(this.configBytes, 0);
      int var13 = this.getBitValue(this.configBytes, 0);
      int var14 = this.getBitValue(this.configBytes, 0);
      var2.time = this.getTime(var1, var13, var14);
      var2.open = this.getOpen(var1, var11, var12);
      var2.high = this.getHigh(var1, var9, var10);
      var2.low = this.getLow(var1, var7, var8);
      var2.close = this.getClose(var1, var5, var6);
      var2.volume = this.getVolume(var1, var3, var4);
      this.nextLoaded();
   }

   private long getTime(IRandomAccessReader var1, int var2, int var3) throws Exception {
      long var4 = this.getValue(var1, var2);
      switch (var3) {
         case 0:
            this.prevTime -= var4;
            return this.prevTime;
         case 1:
            this.prevTime += var4;
            return this.prevTime;
         case 2:
            this.prevTime = var4;
            return var4;
         default:
            throw new Exception("Unknown logic type of value " + var3);
      }
   }

   private double getOpen(IRandomAccessReader var1, int var2, int var3) throws Exception {
      long var4 = this.getValue(var1, var2);
      switch (var3) {
         case 0:
            this.prevOpen -= var4;
            return this.prevOpen / this.decimalsConstant;
         case 1:
            this.prevOpen += var4;
            return this.prevOpen / this.decimalsConstant;
         case 2:
            this.prevOpen = var4;
            return var4 / this.decimalsConstant;
         default:
            throw new Exception("Unknown logic type of value " + var3);
      }
   }

   private double getHigh(IRandomAccessReader var1, int var2, int var3) throws Exception {
      long var4 = this.getValue(var1, var2);
      switch (var3) {
         case 0:
            this.prevHigh -= var4;
            return this.prevHigh / this.decimalsConstant;
         case 1:
            this.prevHigh += var4;
            return this.prevHigh / this.decimalsConstant;
         case 2:
            this.prevHigh = var4;
            return var4 / this.decimalsConstant;
         default:
            throw new Exception("Unknown logic type of value " + var3);
      }
   }

   private double getLow(IRandomAccessReader var1, int var2, int var3) throws Exception {
      long var4 = this.getValue(var1, var2);
      switch (var3) {
         case 0:
            this.prevLow -= var4;
            return this.prevLow / this.decimalsConstant;
         case 1:
            this.prevLow += var4;
            return this.prevLow / this.decimalsConstant;
         case 2:
            this.prevLow = var4;
            return var4 / this.decimalsConstant;
         default:
            throw new Exception("Unknown logic type of value " + var3);
      }
   }

   private double getClose(IRandomAccessReader var1, int var2, int var3) throws Exception {
      long var4 = this.getValue(var1, var2);
      switch (var3) {
         case 0:
            this.prevClose -= var4;
            return this.prevClose / this.decimalsConstant;
         case 1:
            this.prevClose += var4;
            return this.prevClose / this.decimalsConstant;
         case 2:
            this.prevClose = var4;
            return var4 / this.decimalsConstant;
         default:
            throw new Exception("Unknown logic type of value " + var3);
      }
   }

   private double getVolume(IRandomAccessReader var1, int var2, int var3) throws Exception {
      long var4 = this.getValue(var1, var2);
      switch (var3) {
         case 0:
            this.prevVolume -= var4;
            return this.prevVolume / this.volumeConstant;
         case 1:
            this.prevVolume += var4;
            return this.prevVolume / this.volumeConstant;
         case 2:
            this.prevVolume = var4;
            return var4 / this.volumeConstant;
         default:
            throw new Exception("Unknown logic type of value " + var3);
      }
   }
}
