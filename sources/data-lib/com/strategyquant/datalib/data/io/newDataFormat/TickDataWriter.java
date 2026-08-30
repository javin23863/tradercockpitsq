package com.strategyquant.datalib.data.io.newDataFormat;

import com.strategyquant.datalib.data.io.VersatileData;
import java.io.DataOutputStream;
import java.nio.ByteBuffer;

public class TickDataWriter extends NewDataFormatWritter {
   private long prevTime;
   private long prevAsk;
   private long prevBid;
   private long prevVolume;
   int[] logicTypes = new int[]{2, 2, 2, 2};
   int[] dataTypes = new int[]{3, 3, 3, 3};

   public TickDataWriter(boolean var1) {
      super(var1);
   }

   public TickDataWriter() {
      this(false);
   }

   public void writeData(DataOutputStream var1, VersatileData var2) throws Exception {
      long var3 = Math.round(var2.ask * this.decimalsConstant);
      long var5 = Math.round(var2.bid * this.decimalsConstant);
      if (var5 > var3 && var3 != 0L) {
         long var7 = var3;
         var3 = var5;
         var5 = var7;
      }

      long var17 = Math.round(var2.volume * this.volumeConstant);
      if (var17 < 0L) {
         var17 = Long.MAX_VALUE;
      }

      if (this.shouldWriteFullData()) {
         this.start = false;
         if (!this.isForceOldFormat()) {
            this.writeMagicChain(var1);
         }

         this.fillLogicTypes(2, 2, 2, 2);
         this.fillDataTypes(3, 3, 3, 3);
         var1.write(this.getConfigBytes(this.logicTypes, this.dataTypes));
         var1.writeLong(var2.time);
         var1.writeLong(var3);
         var1.writeLong(var5);
         var1.writeLong(var17);
      } else {
         long var9 = var2.time - this.prevTime;
         long var11 = var3 - this.prevAsk;
         long var13 = var5 - this.prevBid;
         long var15 = var17 - this.prevVolume;
         this.fillLogicTypes(getLogicType(var9), getLogicType(var11), getLogicType(var13), getLogicType(var15));
         this.fillDataTypes(getDataType(var9), getDataType(var11), getDataType(var13), getDataType(var15));
         var1.write(this.getConfigBytes(this.logicTypes, this.dataTypes));
         this.writeValue(var1, var9, this.dataTypes[0]);
         this.writeValue(var1, var11, this.dataTypes[1]);
         this.writeValue(var1, var13, this.dataTypes[2]);
         this.writeValue(var1, var15, this.dataTypes[3]);
      }

      this.prevTime = var2.time;
      this.prevAsk = var3;
      this.prevBid = var5;
      this.prevVolume = var17;
      this.nextSaved();
   }

   private void fillDataTypes(int var1, int var2, int var3, int var4) {
      this.dataTypes[0] = var1;
      this.dataTypes[1] = var2;
      this.dataTypes[2] = var3;
      this.dataTypes[3] = var4;
   }

   private void fillLogicTypes(int var1, int var2, int var3, int var4) {
      this.logicTypes[0] = var1;
      this.logicTypes[1] = var2;
      this.logicTypes[2] = var3;
      this.logicTypes[3] = var4;
   }

   public void putData(ByteBuffer var1, VersatileData var2) throws Exception {
      long var3 = Math.round(var2.ask * this.decimalsConstant);
      long var5 = Math.round(var2.bid * this.decimalsConstant);
      long var7 = Math.round(var2.volume * this.volumeConstant);
      if (this.shouldWriteFullData()) {
         this.start = false;
         if (!this.isForceOldFormat()) {
            this.writeMagicChain(var1);
         }

         this.fillLogicTypes(2, 2, 2, 2);
         this.fillDataTypes(3, 3, 3, 3);
         var1.put(this.getConfigBytes(this.logicTypes, this.dataTypes));
         var1.putLong(var2.time);
         var1.putLong(var3);
         var1.putLong(var5);
         var1.putLong(var7);
      } else {
         long var9 = var2.time - this.prevTime;
         long var11 = var3 - this.prevAsk;
         long var13 = var5 - this.prevBid;
         long var15 = var7 - this.prevVolume;
         this.fillLogicTypes(getLogicType(var9), getLogicType(var11), getLogicType(var13), getLogicType(var15));
         this.fillDataTypes(getDataType(var9), getDataType(var11), getDataType(var13), getDataType(var15));
         var1.put(this.getConfigBytes(this.logicTypes, this.dataTypes));
         this.putValue(var1, var9, this.dataTypes[0]);
         this.putValue(var1, var11, this.dataTypes[1]);
         this.putValue(var1, var13, this.dataTypes[2]);
         this.putValue(var1, var15, this.dataTypes[3]);
      }

      this.prevTime = var2.time;
      this.prevAsk = var3;
      this.prevBid = var5;
      this.prevVolume = var7;
      this.nextSaved();
   }

   public void putData(RandomAccessReaderOffheap var1, VersatileData var2) throws Exception {
      long var3 = Math.round(var2.ask * this.decimalsConstant);
      long var5 = Math.round(var2.bid * this.decimalsConstant);
      long var7 = Math.round(var2.volume * this.volumeConstant);
      if (this.shouldWriteFullData()) {
         this.start = false;
         if (!this.isForceOldFormat()) {
            this.writeMagicChain(var1);
         }

         this.fillLogicTypes(2, 2, 2, 2);
         this.fillDataTypes(3, 3, 3, 3);
         var1.put(this.getConfigBytes(this.logicTypes, this.dataTypes));
         var1.putLong(var2.time);
         var1.putLong(var3);
         var1.putLong(var5);
         var1.putLong(var7);
      } else {
         long var9 = var2.time - this.prevTime;
         long var11 = var3 - this.prevAsk;
         long var13 = var5 - this.prevBid;
         long var15 = var7 - this.prevVolume;
         this.fillLogicTypes(getLogicType(var9), getLogicType(var11), getLogicType(var13), getLogicType(var15));
         this.fillDataTypes(getDataType(var9), getDataType(var11), getDataType(var13), getDataType(var15));
         var1.put(this.getConfigBytes(this.logicTypes, this.dataTypes));
         this.putValue(var1, var9, this.dataTypes[0]);
         this.putValue(var1, var11, this.dataTypes[1]);
         this.putValue(var1, var13, this.dataTypes[2]);
         this.putValue(var1, var15, this.dataTypes[3]);
      }

      this.prevTime = var2.time;
      this.prevAsk = var3;
      this.prevBid = var5;
      this.prevVolume = var7;
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
