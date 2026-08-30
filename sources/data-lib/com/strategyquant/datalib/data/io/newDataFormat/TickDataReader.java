package com.strategyquant.datalib.data.io.newDataFormat;

import com.strategyquant.datalib.data.io.VersatileData;
import java.io.EOFException;

public class TickDataReader extends NewDataFormatReader {
   private long prevTime;
   private long prevAsk;
   private long prevBid;
   private long prevVolume;
   private byte[] configBytes = new byte[2];

   public TickDataReader() {
      this(false);
   }

   public TickDataReader(boolean var1) {
      super(var1);
   }

   @Override
   protected int getMinimalRecordSize() {
      return 6;
   }

   @Override
   protected int getMaximalRecordSize() {
      return 34;
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
      int var3 = this.getBitValue(this.configBytes, 1);
      int var4 = this.getBitValue(this.configBytes, 1);
      int var5 = this.getBitValue(this.configBytes, 1);
      int var6 = this.getBitValue(this.configBytes, 1);
      this.start = true;
      int var7 = this.getBitValue(this.configBytes, 0);
      int var8 = this.getBitValue(this.configBytes, 0);
      int var9 = this.getBitValue(this.configBytes, 0);
      int var10 = this.getBitValue(this.configBytes, 0);
      var2.time = this.getTime(var1, var9, var10);
      var2.ask = this.getAsk(var1, var7, var8);
      var2.bid = this.getBid(var1, var5, var6);
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

   private double getAsk(IRandomAccessReader var1, int var2, int var3) throws Exception {
      long var4 = this.getValue(var1, var2);
      switch (var3) {
         case 0:
            this.prevAsk -= var4;
            return this.prevAsk / this.decimalsConstant;
         case 1:
            this.prevAsk += var4;
            return this.prevAsk / this.decimalsConstant;
         case 2:
            this.prevAsk = var4;
            return var4 / this.decimalsConstant;
         default:
            throw new Exception("Unknown logic type of value " + var3);
      }
   }

   private double getBid(IRandomAccessReader var1, int var2, int var3) throws Exception {
      long var4 = this.getValue(var1, var2);
      switch (var3) {
         case 0:
            this.prevBid -= var4;
            return this.prevBid / this.decimalsConstant;
         case 1:
            this.prevBid += var4;
            return this.prevBid / this.decimalsConstant;
         case 2:
            this.prevBid = var4;
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
