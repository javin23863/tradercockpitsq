package com.strategyquant.tradinglib.mt5api;

import com.strategyquant.datalib.DataInfo;
import com.strategyquant.lib.SQTime;
import com.strategyquant.tradinglib.python.PythonRunner;
import java.io.File;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;
import org.joda.time.format.DateTimeFormat;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Mt5ApiManager {
   private static final Logger Log = LoggerFactory.getLogger(Mt5ApiManager.class);
   private final ReentrantLock lock = new ReentrantLock(true);
   private static Mt5ApiManager instance = new Mt5ApiManager();
   private final PythonRunner pyRunner = new PythonRunner();

   public static Mt5ApiManager get() {
      return instance;
   }

   private Mt5ApiManager() {
   }

   public boolean isAvailable() {
      return !this.lock.isLocked();
   }

   private String runPython(PythonRunner.ProgressCallback var1, String... var2) throws Exception {
      this.lock.lock();

      try {
         final AtomicReference var3 = new AtomicReference(null);
         final AtomicReference var4 = new AtomicReference(null);
         PythonRunner.ProgressCallbackAdapter var5 = new PythonRunner.ProgressCallbackAdapter() {
            @Override
            public void onMessage(String var1) {
               Mt5ApiManager.Log.debug(var1);
            }

            @Override
            public void onError(String var1) {
               Mt5ApiManager.Log.error("MT5 error: " + var1);
               var4.set(var1);
            }

            @Override
            public void onResult(String var1) {
               var3.set(var1);
            }
         };
         PythonRunner.ProgressCallback var6 = var1 != null ? var1 : var5;
         this.pyRunner.run("mt5api.py", var6, var2);
         this.sleep();
         if (var3.get() == null && var4.get() != null) {
            throw new RuntimeException((String)var4.get());
         } else {
            return (String)var3.get();
         }
      } finally {
         this.lock.unlock();
      }
   }

   private String runPython(String... var1) throws Exception {
      return this.runPython(null, var1);
   }

   public Map<String, Object> getPriceSymbolInfoJson(Mt5ApiManager.Source var1, String var2, String var3) throws Exception {
      List var4 = this.buildParams(var1, var2, "symbol_price_info", "--symbol", var3);
      return this.parseJsonToMap(this.runPython(var4.toArray(new String[0])));
   }

   public Map<String, Object> getSymbolInfoJson(Mt5ApiManager.Source var1, String var2, String var3) throws Exception {
      List var4 = this.buildParams(var1, var2, "symbol_info", "--symbol", var3);
      return this.parseJsonToMap(this.runPython(var4.toArray(new String[0])));
   }

   public File downloadData(Mt5ApiManager.Source var1, String var2, DataInfo var3, long var4, long var6, PythonRunner.ProgressCallback var8) throws Exception {
      File var9 = this.pyRunner.getTmpFile("", ".csv");
      String var10 = var3.timeframe.equals("TICK") ? "ticks" : "ohlc";
      LinkedList var11 = new LinkedList<>(List.of("fetch", "--symbol", var3.uSymbol, "--type", var10, "--output", var9.getAbsolutePath()));
      if (var4 != -1L) {
         var11.addAll(List.of("--from-date", SQTime.toString(var4, DateTimeFormat.forPattern("yyyy-MM-dd"))));
      }

      if (var6 != -1L) {
         var11.addAll(List.of("--to-date", SQTime.toString(var6, DateTimeFormat.forPattern("yyyy-MM-dd"))));
      }

      this.addRunningParameters(var1, var2, var11);
      this.runPython(var8, var11.toArray(new String[0]));
      return var9;
   }

   public List<JSONObject> getSymbolListJson(Mt5ApiManager.Source var1, String var2) throws Exception {
      List var3 = this.buildParams(var1, var2, "symbol_list");
      String var4 = this.runPython(var3.toArray(new String[0]));
      if (var4 == null) {
         return new LinkedList<>();
      }

      JSONArray var5 = new JSONArray(var4);
      LinkedList var6 = new LinkedList();

      for (int var7 = 0; var7 < var5.length(); var7++) {
         JSONObject var8 = var5.getJSONObject(var7);
         String var9 = var8.getString("path");
         int var10 = var9.lastIndexOf(92);
         String var11 = var10 >= 0 ? var9.substring(0, var10) : var9;
         JSONObject var12 = new JSONObject();
         var12.put("name", var8.getString("name"));
         var12.put("description", var8.getString("description"));
         var12.put("category", var8.getString("category"));
         var12.put("path", var11.replace('\\', '-'));
         var6.add(var12);
      }

      return var6;
   }

   private List<String> buildParams(Mt5ApiManager.Source var1, String var2, String... var3) {
      LinkedList var4 = new LinkedList<>(List.of(var3));
      this.addRunningParameters(var1, var2, var4);
      return var4;
   }

   private Map<String, Object> parseJsonToMap(String var1) {
      JSONObject var2 = new JSONObject(var1);
      HashMap var3 = new HashMap();

      for (String var5 : var2.keySet()) {
         var3.put(var5, var2.get(var5));
      }

      return var3;
   }

   private void addRunningParameters(Mt5ApiManager.Source var1, String var2, List<String> var3) {
      if (var1 == Mt5ApiManager.Source.portable) {
         if (var2 == null) {
            throw new RuntimeException("You have to select MT5 installation folder");
         }

         File var4 = new File(var2, "terminal64.exe");
         if (!var4.exists()) {
            throw new RuntimeException("Selected folder doesn't contains terminal64.exe");
         }

         var3.add("--mt5-path");
         var3.add(var4.getAbsolutePath());
         var3.add("--portable");
      }
   }

   private void sleep() {
      try {
         Thread.sleep(500L);
      } catch (InterruptedException var2) {
         Thread.currentThread().interrupt();
      }
   }

   public void cancel() {
      if (this.pyRunner != null) {
         this.pyRunner.cancel();
      }
   }

   public double getTickSize(Map<String, Object> var1) {
      return ((Number)var1.get("tick_size")).doubleValue();
   }

   public double getTickStep(Map<String, Object> var1) {
      return ((Number)var1.get("tick_step")).doubleValue();
   }

   public double getPointValue(Map<String, Object> var1) {
      return ((Number)var1.get("point_value")).doubleValue();
   }

   public double getDefaultSpread(Map<String, Object> var1) {
      return ((Number)var1.get("spread")).doubleValue();
   }

   public String getDescription(Map<String, Object> var1) {
      return (String)var1.get("description");
   }

   public enum Source {
      installed,
      portable;
   }
}
