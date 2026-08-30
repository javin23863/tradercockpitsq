package com.strategyquant.tradinglib.robustnesstests;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.jar.JarOutputStream;

public class RobustnessOrdersValues {
   private int[] arr;
   private int size;

   public RobustnessOrdersValues(int var1) {
      this.size = var1;
      this.arr = new int[this.size];
   }

   public RobustnessOrdersValues(InputStream var1) throws IOException {
      DataInputStream var2 = new DataInputStream(var1);
      this.size = var2.readInt();
      if (this.size > 0) {
         this.arr = new int[this.size];
         byte[] var3 = new byte[this.size * 4];
         var3 = var2.readAllBytes();

         for (int var4 = 0; var4 < this.size; var4++) {
            this.arr[var4] = this.convertByteArrayToInt(new byte[]{var3[var4 * 4], var3[var4 * 4 + 1], var3[var4 * 4 + 2], var3[var4 * 4 + 3]});
         }
      }

      var2.close();
   }

   private int convertByteArrayToInt(byte[] var1) {
      return var1 != null && var1.length == 4 ? (0xFF & var1[0]) << 24 | (0xFF & var1[1]) << 16 | (0xFF & var1[2]) << 8 | (0xFF & var1[3]) << 0 : 0;
   }

   public int size() {
      return this.size;
   }

   public int[] arr() {
      return this.arr;
   }

   public void set(int var1, double var2) {
      if (var1 < 0) {
         throw new IllegalArgumentException("Index must be bigget than zero.");
      }

      if (var1 > this.arr.length - 1) {
         throw new IllegalArgumentException("Index is bigger than array size, have you initialized it properly?");
      }

      this.arr[var1] = (int)(var2 * 100.0);
   }

   public void saveOrderValuesToFile(JarOutputStream var1) throws IOException {
      DataOutputStream var2 = new DataOutputStream(var1);
      var2.writeInt(this.size);
      if (this.size > 0) {
         byte[] var3 = new byte[this.size * 4];

         for (int var4 = 0; var4 < this.size; var4++) {
            System.arraycopy(this.convertIntToByteArray(this.arr[var4]), 0, var3, var4 * 4, 4);
         }

         var2.write(var3);
      }
   }

   private byte[] convertIntToByteArray(int var1) {
      return new byte[]{(byte)(var1 >> 24 & 0xFF), (byte)(var1 >> 16 & 0xFF), (byte)(var1 >> 8 & 0xFF), (byte)(var1 >> 0 & 0xFF)};
   }

   public RobustnessOrdersValues clone() {
      RobustnessOrdersValues var1 = new RobustnessOrdersValues(this.size());

      for (int var2 = 0; var2 < this.arr.length; var2++) {
         var1.arr[var2] = this.arr[var2];
      }

      return var1;
   }
}
