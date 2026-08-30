package com.strategyquant.tradinglib;

import com.strategyquant.pluginlib.SQPluginManager;
import com.strategyquant.tradinglib.crosscheck.ICrossCheck;
import java.util.ArrayList;

public class ResultTypes {
   public static final String Main = "main";
   public static final String Portfolio = "portfolio";
   public static ArrayList<ICrossCheck> allCrossChecks;
   public static int[] crossCheckHashes;

   public static String typeToString(String var0) {
      switch (var0) {
         case "main":
            return "Main data";
         case "portfolio":
            return "Portfolio";
         default:
            ICrossCheck var3 = getCrossCheckMethod(var0);
            return var3 != null ? var3.getName() : "?";
      }
   }

   public static boolean isCrossCheck(String var0) {
      ICrossCheck var1 = getCrossCheckMethod(var0);
      return var1 != null;
   }

   public static ICrossCheck findCrossCheckByType(String var0) {
      return getCrossCheckMethod(var0);
   }

   public static boolean isValid(String var0) {
      switch (var0) {
         case "main":
         case "portfolio":
            return true;
         default:
            ICrossCheck var3 = getCrossCheckMethod(var0);
            return var3 != null;
      }
   }

   private static ICrossCheck getCrossCheckMethod(String var0) {
      checkCrossCheckHashesLoaded();
      int var1 = var0.hashCode();

      for (int var2 = 0; var2 < crossCheckHashes.length; var2++) {
         if (var1 == crossCheckHashes[var2]) {
            return allCrossChecks.get(var2);
         }
      }

      return null;
   }

   private static void checkCrossCheckHashesLoaded() {
      if (crossCheckHashes == null) {
         allCrossChecks = SQPluginManager.getPlugins(ICrossCheck.class);
         crossCheckHashes = new int[allCrossChecks.size()];

         for (int var0 = 0; var0 < allCrossChecks.size(); var0++) {
            ICrossCheck var1 = allCrossChecks.get(var0);
            crossCheckHashes[var0] = var1.getSettingName().hashCode();
         }
      }
   }
}
