package com.strategyquant.tradinglib.tasks;

import java.util.HashMap;

public class HelpTexts {
   private static HashMap<String, String> helps = new HashMap<>();

   public static void add(String var0, String var1) {
      helps.put(var0, var1);
   }

   public static String get(String var0) {
      return helps.get(var0);
   }

   public static HashMap<String, String> getAll() {
      return helps;
   }
}
