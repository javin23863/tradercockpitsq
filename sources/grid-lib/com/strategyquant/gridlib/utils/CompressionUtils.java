package com.strategyquant.gridlib.utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

public class CompressionUtils {
   public static byte[] compress(byte[] var0) throws IOException {
      Deflater var1 = new Deflater();
      var1.setLevel(9);
      var1.setInput(var0);
      ByteArrayOutputStream var2 = new ByteArrayOutputStream(var0.length);

      byte[] var5;
      try {
         var1.finish();
         byte[] var3 = new byte[1024];

         while (!var1.finished()) {
            int var4 = var1.deflate(var3);
            var2.write(var3, 0, var4);
         }

         var2.close();
         byte[] var8 = var2.toByteArray();
         var5 = var8;
      } catch (Throwable var7) {
         try {
            var2.close();
         } catch (Throwable var6) {
            var7.addSuppressed(var6);
         }

         throw var7;
      }

      var2.close();
      return var5;
   }

   public static byte[] decompress(byte[] var0) throws IOException, DataFormatException {
      Inflater var1 = new Inflater();
      var1.setInput(var0);
      ByteArrayOutputStream var2 = new ByteArrayOutputStream(var0.length);

      byte[] var5;
      try {
         byte[] var3 = new byte[10240];

         while (!var1.finished()) {
            int var4 = var1.inflate(var3);
            var2.write(var3, 0, var4);
         }

         var2.close();
         byte[] var8 = var2.toByteArray();
         var5 = var8;
      } catch (Throwable var7) {
         try {
            var2.close();
         } catch (Throwable var6) {
            var7.addSuppressed(var6);
         }

         throw var7;
      }

      var2.close();
      return var5;
   }
}
