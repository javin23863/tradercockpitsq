package com.strategyquant.datalib.data.io.newDataFormat;

import java.io.DataOutputStream;

public class NewDataFormat {
   public static final double OLD_VOLUME_CONSTANT = 100.0;
   public static final double VOLUME_CONSTANT = 100000.0;
   public static final int BLOCK_LENGTH = 1000;
   protected static final int MAGIC_SIZE = 15;
   public static final int BYTE = 0;
   public static final int SHORT = 1;
   public static final int INT = 2;
   public static final int LONG = 3;
   public static final int MINUS = 0;
   public static final int PLUS = 1;
   public static final int ASIS = 2;
   protected boolean start = true;
   protected double decimalsConstant;
   protected double volumeConstant;
   private int decimals;
   private boolean forceOldFormat;

   public NewDataFormat(boolean var1) {
      this.overrideDecimals(6);
      this.volumeConstant = 100000.0;
      this.start = true;
      this.forceOldFormat = var1;
   }

   public void overrideDecimals(int var1) {
      this.decimals = var1;
      this.decimalsConstant = Math.pow(10.0, var1);
   }

   public void setVolumeConstant(double var1) {
      this.volumeConstant = var1;
   }

   protected boolean isForceOldFormat() {
      return this.forceOldFormat;
   }

   public void reset() {
      this.start = true;
   }

   public static int getDataType(long var0) {
      var0 = var0 >= 0L ? var0 : -var0;
      if (var0 < 256L) {
         return 0;
      } else if (var0 < 65536L) {
         return 1;
      } else {
         return var0 < 4294967296L ? 2 : 3;
      }
   }

   public static int getLogicType(long var0) {
      return var0 < 0L ? 0 : 1;
   }

   public static void printBits(byte var0) {
      String var1 = String.format("%8s", Integer.toBinaryString(var0 & 255)).replace(' ', '0');
      System.out.println(var1);
   }

   protected byte[] getConfigBytes(int[] var1, int[] var2) throws Exception {
      byte[] var3 = new byte[var1.length / 2];

      for (int var4 = var1.length - 1; var4 >= 0; var4--) {
         int var5 = var4 / 2;
         boolean var6 = var4 % 2 == 0;
         int var7 = var6 ? var4 + 1 : var4 - 1;
         var3[var5] = (byte)(var3[var5] | var1[var7]);
         var3[var5] = (byte)(var3[var5] << 2);
         var3[var5] = (byte)(var3[var5] | var2[var7]);
         if (!var6) {
            var3[var5] = (byte)(var3[var5] << 2);
         }
      }

      return var3;
   }

   protected void writeValue(DataOutputStream var1, long var2, int var4) throws Exception {
      if (var2 < 0L) {
         var2 = -var2;
      }

      switch (var4) {
         case 0:
            var1.write((byte)var2);
            break;
         case 1:
            var1.writeShort((int)var2);
            break;
         case 2:
            var1.writeInt((int)var2);
            break;
         case 3:
            var1.writeLong(var2);
            break;
         default:
            throw new Exception("Invalid data type of value " + var4);
      }
   }

   protected final long getValue(IRandomAccessReader var1, int var2) throws Exception {
      switch (var2) {
         case 0:
            return var1.readByte() & 0xFF;
         case 1:
            return var1.readShort() & 65535;
         case 2:
            return var1.readInt() & 4294967295L;
         case 3:
            return var1.readLong();
         default:
            throw new Exception("Unknown data type of value " + var2);
      }
   }

   protected int getBitValue(byte[] var1, int var2) {
      if (this.start) {
         this.start = false;
      } else {
         var1[var2] = (byte)(var1[var2] >> 2);
      }

      return var1[var2] & 3;
   }

   public int getDecimals() {
      return this.decimals;
   }
}
