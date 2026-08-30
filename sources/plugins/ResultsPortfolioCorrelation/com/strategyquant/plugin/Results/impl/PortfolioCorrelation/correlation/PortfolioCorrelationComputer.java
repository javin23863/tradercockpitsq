package com.strategyquant.plugin.Results.impl.PortfolioCorrelation.correlation;

import com.strategyquant.lib.L;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.TimePeriod;
import com.strategyquant.lib.TimePeriods;
import com.strategyquant.lib.memory.CpuInfo;
import com.strategyquant.lib.utils.JsonCreator;
import com.strategyquant.plugin.Results.impl.PortfolioCorrelation.PortfolioCorrelationInterruptException;
import com.strategyquant.plugin.Results.impl.PortfolioCorrelation.PortfolioCorrelationResultsSender;
import com.strategyquant.tradinglib.CorrelationLib;
import com.strategyquant.tradinglib.CorrelationType;
import com.strategyquant.tradinglib.CustomCellFormat;
import com.strategyquant.tradinglib.Order;
import com.strategyquant.tradinglib.OrdersList;
import com.strategyquant.tradinglib.ResultsGroup;
import com.strategyquant.tradinglib.correlation.CorrelationPeriodTypes;
import com.strategyquant.tradinglib.correlation.CorrelationPeriods;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap.Entry;
import it.unimi.dsi.fastutil.objects.ObjectBidirectionalIterator;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PortfolioCorrelationComputer {
   private static final Logger Log = LoggerFactory.getLogger(PortfolioCorrelationComputer.class);
   private HashMap<String, CorrelationResult> results = new HashMap<>();
   private boolean allowNegativeCorrelation;
   boolean addEmptyPeriods;
   private CustomCellFormat cellFormat = new CustomCellFormat();
   private CorrelationType type;
   private int period;
   private List<String> symbolList;
   private CorrelationPeriods periods;
   public boolean stopped;

   public JSONObject compute(ResultsGroup var1, int var2, CorrelationType var3, boolean var4, boolean var5, PortfolioCorrelationResultsSender var6) throws Exception {
      this.allowNegativeCorrelation = var4;
      this.addEmptyPeriods = var5;
      this.period = var2;
      this.type = var3;
      this.stopped = false;
      int var7 = CpuInfo.getAvailableProcessors();
      ExecutorService var8 = Executors.newFixedThreadPool(var7);

      try {
         this.results.clear();
         this.periods = this.precomputePeriods(var1);
         ArrayList var10 = new ArrayList();
         this.symbolList = var1.getResultKeys();

         for (String var12 : this.symbolList) {
            if (this.stopped) {
               throw new PortfolioCorrelationInterruptException();
            }

            if (!var12.equals("Portfolio") && !var12.startsWith("WF:") && !var12.startsWith("CrossCheck_")) {
               for (String var14 : this.symbolList) {
                  if (this.stopped) {
                     throw new PortfolioCorrelationInterruptException();
                  }

                  if (!var14.equals("Portfolio") && !var14.startsWith("WF:") && !var14.startsWith("CrossCheck_") && !var12.equals(var14)) {
                     String var9 = this.key(var12, var14);
                     if (!this.results.containsKey(var9)) {
                        this.results.put(var9, null);
                        var10.add(var8.submit(new CorrelationComputerTask(var1, var12, var14, var2, var3, var4, var5, this.periods)));
                     }
                  }
               }
            }
         }

         int var21 = 0;

         for (Future var16 : var10) {
            if (this.stopped) {
               throw new PortfolioCorrelationInterruptException();
            }

            CorrelationResult var20 = (CorrelationResult)var16.get();
            var21++;
            double var22 = (double)var21 / this.results.size() * 100.0 / 2.0;
            if (var20 != null) {
               var6.progress((int)var22);
               String var19 = this.key(var20.symbol1, var20.symbol2);
               this.results.put(var19, var20);
            }
         }

         return this.printResults(var2, var3);
      } catch (PortfolioCorrelationInterruptException var17) {
         var8.shutdownNow();
         throw var17;
      } catch (Exception var18) {
         Log.error("Error while computing portfolio correlation.", var18);
         throw var18;
      }
   }

   private CorrelationPeriods precomputePeriods(ResultsGroup var1) throws Exception {
      CorrelationPeriods var2 = new CorrelationPeriods();
      TimePeriod var3 = CorrelationLib.getPeriod(var1.orders());
      TimePeriods var4 = CorrelationLib.generatePeriods(this.period, var3.from, var3.to);

      for (String var7 : var1.getResultKeys()) {
         if (this.stopped) {
            throw new PortfolioCorrelationInterruptException();
         }

         if (!var7.equals("Portfolio") && !var7.startsWith("WF:") && !var7.startsWith("CrossCheck_")) {
            OrdersList var8 = this.createOrdersFromStrategy(var1, var7);
            TimePeriods var9 = var4.clone();
            this.type.computePeriods(var8, this.period, var9);
            var2.put(var7, var9);
         }
      }

      return var2;
   }

   private OrdersList createOrdersFromStrategy(ResultsGroup var1, String var2) throws Exception {
      OrdersList var3 = new OrdersList("orders");
      OrdersList var4 = var1.orders();

      for (int var5 = 0; var5 < var4.size(); var5++) {
         Order var6 = var4.get(var5);
         if (var6.isRealOrder() && var6.SetupName.equals(var2)) {
            var3.add(var6);
         }
      }

      return var3;
   }

   private JSONObject printResults(int var1, CorrelationType var2) throws Exception {
      this.cellFormat.reset();
      JSONObject var3 = new JSONObject();
      Object var4 = null;
      JSONArray var6 = new JSONArray();
      var6.put("");
      JSONArray var7 = new JSONArray();

      for (int var8 = 0; var8 < this.symbolList.size(); var8++) {
         String var9 = this.symbolList.get(var8);
         if (!var9.equals("Portfolio") && !var9.startsWith("WF:") && !var9.startsWith("CrossCheck_")) {
            var6.put(var9);
            JSONArray var10 = new JSONArray();
            var10.put(var9);

            for (String var12 : this.symbolList) {
               if (!var12.equals("Portfolio") && !var12.startsWith("WF:") && !var12.startsWith("CrossCheck_")) {
                  if (var9.equals(var12)) {
                     var10.put("");
                  } else {
                     var4 = this.key(var9, var12);
                     CorrelationResult var5 = this.results.get(var4);
                     if (var5 != null) {
                        this.cellFormat.customStyle = "background-color: " + this.getColor(var5.value) + ";";
                        this.cellFormat.customStyle = this.cellFormat.customStyle + "color: " + this.getTextColor(var5.value) + ";";
                        var10.put(this.cellFormat.getHtmlContent(SQUtils.d2(var5.value)));
                     }
                  }
               }
            }

            JSONObject var14 = new JSONObject();
            var14.put("id", "r" + var8);
            var14.put("data", var10);
            var7.put(var14);
         }
      }

      var3.put("type", var2.getName());
      var3.put("period", CorrelationPeriodTypes.toString(var1));
      var3.put("columns", var6);
      var3.put("dataType", var2.getDataType() == 5 ? "Profit/Loss" : "# of trades");
      var3.put("rows", var7);
      return var3;
   }

   private String key(String var1, String var2) {
      return var1.compareTo(var2) > 0 ? var1 + "-" + var2 : var2 + "-" + var1;
   }

   public String list(String var1, String var2, int var3, int var4) {
      JsonCreator var5 = new JsonCreator();
      var5.beginObject();
      var5.put("rows");
      this.cellFormat.reset();
      var5.beginArray();
      TimePeriods var11 = (TimePeriods)this.periods.get(var1);
      TimePeriods var12 = (TimePeriods)this.periods.get(var2);
      if (!this.addEmptyPeriods) {
         TimePeriods var13 = new TimePeriods();
         TimePeriods var14 = new TimePeriods();
         this.filterEmptyPeriods(var11, var12, var13, var14);
         var11 = var13;
         var12 = var14;
      }

      int var18 = -1;
      ObjectBidirectionalIterator var19 = var11.long2ObjectEntrySet().iterator();

      while (var19.hasNext()) {
         Entry var15 = (Entry)var19.next();
         if (++var18 >= var3) {
            if (var18 >= var3 + var4) {
               break;
            }

            if (var18 != var3) {
               var5.separator();
            }

            long var16 = var15.getLongKey();
            double var6 = ((TimePeriod)var11.get(var16)).value;
            double var8 = ((TimePeriod)var12.get(var16)).value;
            String var10 = ((TimePeriod)var11.get(var16)).name;
            var5.beginObject();
            var5.put("data");
            var5.beginArray();
            var5.setValue(var18 + 1, true);
            var5.setValue(var10, true);
            this.cellFormat.customStyle = "color: " + this.getColor2(var6) + ";";
            var5.setValue(this.cellFormat.getHtmlContent(this.type.getDataType() == 5 ? SQUtils.d2(var6) : (int)var6), true);
            this.cellFormat.customStyle = "color: " + this.getColor2(var8) + ";";
            var5.setValue(this.cellFormat.getHtmlContent(this.type.getDataType() == 5 ? SQUtils.d2(var8) : (int)var8), false);
            var5.endArray(true);
            var5.put("id", "r" + var18, false);
            var5.endObject(false);
         }
      }

      var5.endArray(true);
      var5.put("pos", var3, true);
      var5.put("total_count", var11.size(), true);
      var5.put("success", "ok", false);
      var5.endObject(false);
      return var5.toJson();
   }

   private void filterEmptyPeriods(TimePeriods var1, TimePeriods var2, TimePeriods var3, TimePeriods var4) {
      ObjectBidirectionalIterator var5 = var1.long2ObjectEntrySet().iterator();

      while (var5.hasNext()) {
         Entry var6 = (Entry)var5.next();
         long var7 = var6.getLongKey();
         if (this.addEmptyPeriods || ((TimePeriod)var1.get(var7)).value != 0.0 || ((TimePeriod)var2.get(var7)).value != 0.0) {
            var3.put(var6.getLongKey(), (TimePeriod)var1.get(var7));
            var4.put(var6.getLongKey(), (TimePeriod)var2.get(var7));
         }
      }
   }

   public void saveToCsv(String var1) throws Exception {
      try {
         if (!var1.toLowerCase().endsWith(".csv")) {
            var1 = var1 + ".csv";
         }

         Object var2 = null;
         PrintWriter var4 = new PrintWriter(var1);
         var4.print("\"\";");

         for (int var5 = 0; var5 < this.symbolList.size(); var5++) {
            String var6 = this.symbolList.get(var5);
            if (!var6.equals("Portfolio") && !var6.startsWith("WF:") && !var6.startsWith("CrossCheck_")) {
               var4.print("\"" + var6 + "\"");
               if (var5 != this.symbolList.size() - 1) {
                  var4.print(";");
               }
            }
         }

         var4.println();

         for (int var11 = 0; var11 < this.symbolList.size(); var11++) {
            String var12 = this.symbolList.get(var11);
            if (!var12.equals("Portfolio") && !var12.startsWith("WF:") && !var12.startsWith("CrossCheck_")) {
               var4.print("\"" + var12 + "\";");

               for (int var7 = 0; var7 < this.symbolList.size(); var7++) {
                  String var8 = this.symbolList.get(var7);
                  if (!var8.equals("Portfolio") && !var8.startsWith("WF:") && !var8.startsWith("CrossCheck_")) {
                     if (var12.equals(var8)) {
                        var4.print("\"\";");
                     } else {
                        var2 = this.key(var12, var8);
                        CorrelationResult var3 = this.results.get(var2);
                        var4.print("\"" + SQUtils.d2(var3.value) + "\"");
                        if (var7 != this.symbolList.size() - 1) {
                           var4.print(";");
                        }
                     }
                  }
               }

               var4.println();
            }
         }

         var4.flush();
         var4.close();
      } catch (Exception var9) {
         Log.error("Error while saving Correlation matrix to csv.", var9);
         throw new Exception(L.t("Cannot save Correlation matrix to csv - %s", new Object[]{var9.getMessage()}));
      }
   }

   private String getColor(double var1) {
      try {
         if (var1 < 0.0) {
            if (this.allowNegativeCorrelation) {
               return "#00ff00";
            }

            var1 = Math.abs(var1);
         }

         int var3 = (int)(var1 * 100.0);
         int var4 = 255 * var3 / 100;
         int var5 = 255 * (100 - var3) / 100;
         byte var6 = 0;
         return String.format("#%02x%02x%02x", var4, var5, Integer.valueOf(var6));
      } catch (Exception var7) {
         Log.error("Exc.", var7);
         return "#FFFFFF";
      }
   }

   private String getTextColor(double var1) {
      try {
         if (var1 < 0.0) {
            if (this.allowNegativeCorrelation) {
               return "#0000ff";
            }

            var1 = Math.abs(var1);
         }

         int var3 = var1 < 0.3 ? 0 : 255;
         int var4 = var1 < 0.3 ? 0 : 255;
         short var5 = 255;
         return String.format("#%02x%02x%02x", var3, var4, Integer.valueOf(var5));
      } catch (Exception var6) {
         Log.error("Exc.", var6);
         return "#FFFFFF";
      }
   }

   private String getColor2(double var1) {
      if (var1 < 0.0) {
         return "#ff0000";
      } else {
         return var1 > 0.0 ? "#00ff00" : "#000000";
      }
   }

   public void stop() {
      this.stopped = true;
   }

   public void clear() {
      this.results.clear();
      if (this.periods != null) {
         this.periods = null;
      }
   }
}
