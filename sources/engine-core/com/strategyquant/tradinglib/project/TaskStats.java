package com.strategyquant.tradinglib.project;

import java.util.HashMap;
import java.util.Map.Entry;
import org.json.JSONArray;

public class TaskStats {
   private static int MAX_ORDERS = 999999;
   public HashMap<String, TaskStat> stats = new HashMap<>();

   private TaskStat getStat(String var1) {
      return this.getStat(var1, MAX_ORDERS);
   }

   private TaskStat getStat(String var1, int var2) {
      TaskStat var3 = this.stats.get(var1);
      if (var3 == null) {
         var3 = new TaskStat();
         var3.action = var1;
         var3.count = 0L;
         var3.order = var2;
         this.stats.put(var1, var3);
      }

      return var3;
   }

   public void increaseStat(String var1, String var2, long var3) {
      this.increaseStat(var1, var2, var3, MAX_ORDERS);
   }

   public void increaseStat(String var1, String var2, long var3, int var5) {
      this.increaseStat(var1, var2, var3, var5, "");
   }

   public void increaseStat(String var1, String var2, long var3, int var5, String var6) {
      TaskStat var7 = this.getStat(var2, var5);
      var7.action = var2;
      var7.count += var3;
      var7.display = var1;
      var7.percentageCompareAction = var6;
   }

   public void setStat(String var1, String var2, long var3, String var5) {
      this.setStat(var1, var2, var3, MAX_ORDERS, var5);
   }

   public void setStat(String var1, String var2, long var3, int var5, String var6) {
      TaskStat var7 = this.getStat(var2, var5);
      var7.action = var2;
      var7.count = var3;
      var7.display = var1;
      var7.percentageCompareAction = var6;
   }

   public JSONArray getStatsJSONArray() {
      JSONArray var1 = new JSONArray();

      for (Entry var3 : this.stats.entrySet()) {
         String var4 = (String)var3.getKey();
         TaskStat var5 = (TaskStat)var3.getValue();
         var1.put(var5.getJSONObject());
      }

      return var1;
   }
}
