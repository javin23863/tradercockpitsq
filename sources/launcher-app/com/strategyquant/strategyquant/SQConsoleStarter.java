package com.strategyquant.strategyquant;

import java.lang.reflect.Field;
import sun.misc.Unsafe;

public class SQConsoleStarter {
   public static void main(String[] var0) throws Exception {
      disableWarning();
      SQApp.main(var0, true);
   }

   public static void disableWarning() {
      try {
         Field var0 = Unsafe.class.getDeclaredField("theUnsafe");
         var0.setAccessible(true);
         Unsafe var1 = (Unsafe)var0.get(null);
         Class var2 = Class.forName("jdk.internal.module.IllegalAccessLogger");
         Field var3 = var2.getDeclaredField("logger");
         var1.putObjectVolatile(var2, var1.staticFieldOffset(var3), null);
      } catch (Exception var4) {
      }
   }
}
