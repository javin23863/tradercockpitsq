package com.strategyquant.tradinglib.backtestrunner;

import com.strategyquant.lib.L;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class DurationStats {
   public static final String MainTest = L.t("Main test", new Object[]{true});
   private HashMap<String, Double> map = new HashMap<>();
   private ArrayList<String> keysInOrder = new ArrayList<>();

   public void addDuration(String var1, long var2) {
      double var4 = var2 / 1000.0;
      this.keysInOrder.add(var1);
      this.map.put(var1, var4);
   }

   public HashMap<String, Double> getMap() {
      return this.map;
   }

   public List<String> getKeys() {
      return this.keysInOrder;
   }
}
