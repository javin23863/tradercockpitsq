package com.strategyquant.tradinglib.wfo;

import com.strategyquant.datalib.ChartDef;
import com.strategyquant.lib.L;
import com.strategyquant.lib.SQTime;
import com.strategyquant.lib.SettingsMap;
import com.strategyquant.lib.app.MainApp;
import com.strategyquant.tradinglib.Order;
import com.strategyquant.tradinglib.OrderCloseTypes;
import com.strategyquant.tradinglib.OrdersList;
import com.strategyquant.tradinglib.Result;
import com.strategyquant.tradinglib.ResultsGroup;
import com.strategyquant.tradinglib.SQStats;
import com.strategyquant.tradinglib.StatsTypeCombination;
import com.strategyquant.tradinglib.engine.ChartSetups;
import com.strategyquant.tradinglib.optimization.VariantComparatorByName;
import com.strategyquant.tradinglib.results.SymbolsMap;
import com.strategyquant.tradinglib.results.stats.StatsComputer;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WFOSimulationEngine {
   public static final Logger Log = LoggerFactory.getLogger("WFOSimulationEngine");
   public static final String DefaultSetupName = "Setup 1";
   private ArrayList<WFVariant> variants = new ArrayList<>();
   private Int2ObjectOpenHashMap<SimulationOrdersList> simulationsMap = new Int2ObjectOpenHashMap();
   private Comparator<WFVariant> variantComparatorByName = new VariantComparatorByName();
   public boolean periodTypeBars = false;

   public WFOSimulationEngine() throws Exception {
      this(false);
   }

   public WFOSimulationEngine(boolean var1) throws Exception {
      this.periodTypeBars = var1;
   }

   public synchronized void saveTestToFile(ResultsGroup var1) throws Exception {
      String var2 = var1.specialValues().getString("OptimizationParameters");
      if (var2 == null) {
         throw new Exception(L.t("No params set in the result!", new Object[0]));
      }

      short[] var3 = (short[])var1.specialValues().get("OptimizationParametersArray");
      if (var3 == null) {
         throw new Exception(L.t("No indexes set in the result!", new Object[0]));
      }

      String var4 = this.convertParamsToFilename(var2);
      this.saveOrdersToFile(var2, var4, var3, var1);
   }

   private void saveOrdersToTxtFile(String var1, String var2, short[] var3, ResultsGroup var4) throws Exception {
      String var5 = MainApp.getDataPath() + "tests/tmp/wfosim/" + var2 + ".txt";
      BufferedWriter var6 = new BufferedWriter(new FileWriter(var5, StandardCharsets.UTF_8));
      double var8 = 0.0;

      for (int var10 = 0; var10 < var4.orders().size(); var10++) {
         Order var7 = var4.orders().get(var10);
         var8 += var7.PL;
         var6.write(this.orderToString(var7, var8));
         var6.write("\n");
      }

      var6.close();
   }

   public String orderToString(Order var1, double var2) {
      StringBuilder var4 = new StringBuilder();
      var4.append(var1.Order);
      var4.append(" - ");
      var4.append(var1.Ticket);
      var4.append(" - ");
      var4.append(SQTime.toDateMinuteString(var1.OriginalOpenTime));
      var4.append("/");
      var4.append(SQTime.toDateMinuteString(var1.OpenTime));
      var4.append(" : ");
      var4.append(SQTime.toDateMinuteString(var1.CloseTime));
      var4.append(" : ");
      var4.append(OrderCloseTypes.toString(var1.CloseType));
      var4.append(", PL: ");
      var4.append(var1.PL);
      var4.append(", Running PL: ");
      var4.append(var2);
      return var4.toString();
   }

   private String convertParamsToFilename(String var1) {
      StringBuilder var2 = new StringBuilder("p");
      String[] var3 = var1.split(",");

      for (int var4 = 0; var4 < var3.length; var4++) {
         if (var3[var4].length() > 1) {
            String var5 = var3[var4].replace("=", "_");
            if (var5.endsWith(".0")) {
               var5 = var5.replace(".0", "");
            }

            var2.append("_");
            var2.append(var5);
         }
      }

      return var2.toString();
   }

   private void saveOrdersToFile(String var1, String var2, short[] var3, ResultsGroup var4) throws Exception {
      SimulationOrdersList var5 = new SimulationOrdersList(var4.orders().size());
      new Order();

      for (int var6 = 0; var6 < var4.orders().size(); var6++) {
         Order var7 = var4.orders().get(var6);
         var5.add(
            var7.OpenTime,
            var7.CloseTime,
            var7.Type,
            var7.CloseType,
            var7.PL,
            var7.Size,
            var7.OpenPrice,
            var7.ClosePrice,
            var7.BarsInTrade,
            var7.Symbol,
            var7.MAE,
            var7.MFE,
            var7.CommSwap,
            var7.StopLoss
         );
      }

      int var8 = var5.getHash();
      if (!this.simulationsMap.containsKey(var8)) {
         this.simulationsMap.put(var8, var5);
      }

      this.variants.add(new WFVariant(var1, var3, var2, var8));
      if (Log.isDebugEnabled()) {
         Log.debug("WF Saved orders: {}", var4.orders().size());
      }
   }

   private synchronized OrdersList loadOrders(int var1, long var2, long var4, String var6) throws Exception {
      if (!this.simulationsMap.containsKey(var1)) {
         throw new Exception(L.t("Simulation: %d not found!", new Object[]{var1}));
      }

      SimulationOrdersList var7 = (SimulationOrdersList)this.simulationsMap.get(var1);
      OrdersList var8 = new OrdersList("loadOrders", var7.openTime.length);

      for (int var9 = 0; var9 < var7.openTime.length; var9++) {
         long var10;
         if (this.periodTypeBars) {
            var10 = var7.closeTime[var9];
         } else {
            var10 = var7.openTime[var9];
         }

         if (var10 >= var4) {
            break;
         }

         if (var10 >= var2) {
            Order var12 = new Order();
            var12.OpenTime = var7.openTime[var9];
            var12.CloseTime = var7.closeTime[var9];
            var12.Type = var7.type[var9];
            var12.CloseType = var7.closeType[var9];
            var12.PL = var7.pL[var9];
            var12.Size = var7.size[var9];
            var12.OpenPrice = var7.openPrice[var9];
            var12.ClosePrice = var7.closePrice[var9];
            var12.BarsInTrade = var7.barsInTrade[var9];
            var12.Symbol = var7.symbol[var9];
            var12.MAE = var7.mae[var9];
            var12.MFE = var7.mfe[var9];
            var12.SampleType = 10;
            var12.SetupName = var6;
            var12.CommSwap = var7.commswap[var9];
            var12.StopLoss = var7.stoploss[var9];
            if (!this.periodTypeBars || var12.CloseType != 4) {
               var8.add(var12);
            }
         }
      }

      return var8;
   }

   public ArrayList<WFVariant> getVariants() {
      return this.variants;
   }

   public ResultsGroup simulateTestCandidate(StatsComputer var1, WFVariant var2, long var3, long var5, SettingsMap var7) throws Exception {
      if (var2 == null) {
         Log.error("Variant is null!!!");
         return null;
      }

      String var8 = "Setup 1";
      OrdersList var9 = this.loadOrders(var2.simulationOLHash, var3, var5, var8);
      ResultsGroup var10 = new ResultsGroup("Variant " + var2.paramsFile);
      var10.addSubresult(var8, var7);
      ChartSetups var11 = (ChartSetups)var7.get("ChartSetups");

      for (ChartDef var13 : var11.getMainSetup().getCharts()) {
         var10.symbols().add(var13.getSymbol(), var13.getSymbolInfo().instrument, var13.getSymbolInfo());
         var10.addResultSymbol(var8, var13.getSymbol());
      }

      SettingsMap var15 = var10.portfolio().getSettings();
      var15.setIfNotExists("MoneyManagement.InitialCapital", 20000.0);
      var10.orders().replaceWithList(var9);
      Result var16 = var10.subResult(var8);
      SettingsMap var14 = var16.getSettings();
      var14.set("PortfolioDataStart", var3);
      var14.set("PortfolioDataEnd", var5);
      computeStatsRequiredForFitness(var1, var8, var16, var9, var10.symbols());
      var10.specialValues().set("OptimizationParameters", var2.params);
      var10.specialValues().set("OptimizationParametersArray", var2.indexes);
      return var10;
   }

   public static void computeStatsRequiredForFitness(StatsComputer var0, String var1, Result var2, OrdersList var3, SymbolsMap var4) throws Exception {
      SettingsMap var5 = var2.getSettings();
      var5.set("AddCommissionSwapToPL", true);
      var0.initializeWithOrders(var3);
      StatsTypeCombination var6 = new StatsTypeCombination((byte)1, (byte)10, (byte)127);
      var0.filterBy(var1, (byte)1, (byte)127);
      var0.sortAndComputeOrderValues(var5, var4);
      computeStats(var2, var0, var6.set((byte)1, (byte)10, (byte)127), var5);
      computeStats(var2, var0, var6.set((byte)1, (byte)20, (byte)127), var5);
      computeStats(var2, var0, var6.set((byte)1, (byte)30, (byte)127), var5);
      var0.filterBy(var1, (byte)-1, (byte)127);
      var0.sortAndComputeOrderValues(var5, var4);
      computeStats(var2, var0, var6.set((byte)-1, (byte)10, (byte)127), var5);
      computeStats(var2, var0, var6.set((byte)-1, (byte)20, (byte)127), var5);
      computeStats(var2, var0, var6.set((byte)-1, (byte)30, (byte)127), var5);
      var0.filterBy(var1, (byte)0, (byte)127);
      var0.sortAndComputeOrderValues(var5, var4);
      computeStats(var2, var0, var6.set((byte)0, (byte)10, (byte)127), var5);
      computeStats(var2, var0, var6.set((byte)0, (byte)20, (byte)127), var5);
      computeStats(var2, var0, var6.set((byte)0, (byte)30, (byte)127), var5);
   }

   private static void computeStats(Result var0, StatsComputer var1, StatsTypeCombination var2, SettingsMap var3) throws Exception {
      SQStats var4 = var1.computeStats(var0, var2, var3);
      int var5 = var2.getKey();
      var0.set(var5, var4);
      var2.set(var2.getDirection(), var2.getPLType(), (byte)10);
      var5 = var2.getKey();
      var0.set(var5, var4);
      var2.set(var2.getDirection(), var2.getPLType(), (byte)20);
      var5 = var2.getKey();
      var0.set(var5, var4);
   }

   public void sortVariants() {
      Collections.sort(this.variants, this.variantComparatorByName);
   }

   public boolean isPeriodTypeBars() {
      return this.periodTypeBars;
   }
}
