package com.strategyquant.gridlib.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

public class BeanUtils {
   public static byte[] serialize(Object var0) throws IOException {
      ByteArrayOutputStream var1 = new ByteArrayOutputStream();

      byte[] var3;
      try {
         ObjectOutputStream var2 = null;
         var2 = new ObjectOutputStream(var1);
         var2.writeObject(var0);
         var2.flush();
         var3 = var1.toByteArray();
      } catch (Throwable var5) {
         try {
            var1.close();
         } catch (Throwable var4) {
            var5.addSuppressed(var4);
         }

         throw var5;
      }

      var1.close();
      return var3;
   }

   public static Serializable deserializeObject(byte[] var0) throws IOException, ClassNotFoundException {
      try {
         ByteArrayInputStream var1 = new ByteArrayInputStream(var0);

         Serializable var4;
         try {
            ObjectInputStream var2 = new ObjectInputStream(var1);

            try {
               Object var3 = var2.readObject();
               var4 = (Serializable)var3;
            } catch (Throwable var7) {
               try {
                  var2.close();
               } catch (Throwable var6) {
                  var7.addSuppressed(var6);
               }

               throw var7;
            }

            var2.close();
         } catch (Throwable var8) {
            try {
               var1.close();
            } catch (Throwable var5) {
               var8.addSuppressed(var5);
            }

            throw var8;
         }

         var1.close();
         return var4;
      } catch (Exception var9) {
         var9.printStackTrace();
         throw var9;
      }
   }
}
