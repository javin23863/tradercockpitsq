package com.strategyquant.tradinglib.results.stats;

import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.SettingsMap;
import com.strategyquant.lib.snippets.NonexistingCustomClassException;
import com.strategyquant.lib.utils.ISQCloneable;
import com.strategyquant.tradinglib.DatabankColumn;
import com.strategyquant.tradinglib.Order;
import com.strategyquant.tradinglib.OrdersList;
import com.strategyquant.tradinglib.Result;
import com.strategyquant.tradinglib.SQStats;
import com.strategyquant.tradinglib.StatsTypeCombination;
import com.strategyquant.tradinglib.databank.DatabankColumns;
import com.strategyquant.tradinglib.results.SymbolsMap;
import com.strategyquant.tradinglib.results.stats.comparator.OrderComparatorByCloseTime;
import com.strategyquant.tradinglib.results.stats.computer.IOrderComputer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.ListIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StatsComputer implements ISQCloneable {
   public static final Logger Log = LoggerFactory.getLogger("StatsComputer");
   private static final String ConstantAll = "*";
   private static final ArrayList<DatabankColumn> globalStatValues = new ArrayList<>();
   private static final ArrayList<IOrderComputer> globalOrderComputers = new ArrayList<>();
   private ArrayList<DatabankColumn> statValues;
   private static boolean dependenciesComputed = false;
   private boolean filtered = false;
   private OrdersList originalOrdersList;
   private OrdersList filteredOL;
   private SettingsMap rgSpecialValues = null;
   private final Comparator<Order> comparatorByCloseTime = new OrderComparatorByCloseTime();

   public StatsComputer() {
      this.statValues = globalStatValues;
   }

   public StatsComputer(ArrayList<DatabankColumn> var1) {
      this.statValues = var1;
   }

   public static void clearGlobalStatsValues() {
      globalStatValues.clear();
   }

   public static void registerDatabankColumn(DatabankColumn var0) {
      globalStatValues.add(var0);
   }

   public static void registerOrderComputer(IOrderComputer var0) {
      globalOrderComputers.add(var0);
   }

   public static void createDependencyGraph() throws Exception {
      createDependencyGraph(globalStatValues);
      dependenciesComputed = true;
   }

   public static void createDependencyGraph(ArrayList<DatabankColumn> var0) throws Exception {
      ArrayList var1 = new ArrayList();
      var1.addAll(var0);
      ArrayList var2 = new ArrayList();

      for (int var3 = 0; var3 < var0.size(); var3++) {
         boolean var4 = false;
         ListIterator var5 = var1.listIterator();

         while (var5.hasNext()) {
            DatabankColumn var6 = (DatabankColumn)var5.next();
            if (checkColumnDependsOnExistingColumn(var6, var2)) {
               var2.add(var6);
               var5.remove();
               var4 = true;
            }
         }

         if (var1.size() == 0) {
            break;
         }

         if (!var4) {
            throw new Exception(
               "Unsolved dependency detected. Missing values required by some other snippet:\n\nThe problem could be either circular dependency (A depends on B, B depends on C, C depends on A)\nor missing / uncompiled stats value snippet. Check if all the StatValues snippets were successfuly compiled."
            );
         }
      }

      var0.clear();
      var0.addAll(var2);
   }

   private static boolean checkColumnDependsOnExistingColumn(DatabankColumn var0, ArrayList<DatabankColumn> var1) {
      String[] var2 = var0.getDependencies();
      if (var2 == null) {
         return true;
      }

      for (String var6 : var2) {
         boolean var7 = false;

         for (int var8 = 0; var8 < var1.size(); var8++) {
            if (var6.equals(((DatabankColumn)var1.get(var8)).getClass().getSimpleName())) {
               var7 = true;
               break;
            }
         }

         if (!var7) {
            return false;
         }
      }

      return true;
   }

   public static ArrayList<DatabankColumn> getAllDependentStatValues(ArrayList<DatabankColumn> var0) throws NonexistingCustomClassException {
      while (true) {
         ArrayList var1 = null;

         for (int var2 = 0; var2 < var0.size(); var2++) {
            DatabankColumn var3 = (DatabankColumn)var0.get(var2);
            if (!checkColumnDependsOnExistingColumn(var3, var0)) {
               if (var1 == null) {
                  var1 = new ArrayList();
               }

               var1.addAll(getDependOnColumns(var3));
            }
         }

         if (var1 == null) {
            return var0;
         }

         var0.addAll(var1);
      }
   }

   private static ArrayList<DatabankColumn> getDependOnColumns(DatabankColumn var0) throws NonexistingCustomClassException {
      String[] var1 = var0.getDependencies();
      if (var1 == null) {
         return null;
      }

      ArrayList var2 = new ArrayList();

      for (String var6 : var1) {
         var2.add((DatabankColumn)DatabankColumns.get().findClassByName(var6));
      }

      return var2;
   }

   public void initializeWithOrders(OrdersList var1) {
      this.initializeWithOrders(var1, false);
   }

   public void initializeWithOrders(OrdersList var1, boolean var2) {
      this.originalOrdersList = var1;
      this.filteredOL = this.originalOrdersList;
      this.filtered = var2;
   }

   public void filterBy(String var1, byte var2, byte var3) {
      this.filteredOL = this.originalOrdersList.filter(var1, var2, var3);
      this.filtered = true;
   }

   public void sortAndComputeOrderValues(SettingsMap var1, SymbolsMap var2) throws Exception {
      if (!this.filtered) {
         throw new Exception("You have to call StatsComputer.filterBy() before calling sortAndComputeOrderValues()");
      }

      this.filteredOL.sort(this.comparatorByCloseTime);

      for (int var3 = 0; var3 < globalOrderComputers.size(); var3++) {
         globalOrderComputers.get(var3).compute(this.filteredOL, var1, var2);
      }
   }

   public SQStats computeStats(Result var1, StatsTypeCombination var2, SettingsMap var3) throws Exception {
      if (!this.filtered) {
         throw new Exception("You have to call StatsComputer.filterBy() before calling computeStats()");
      }

      int var4 = var2.getKey();

      try {
         SQStats var5 = new SQStats();
         SQStats var6 = null;
         SQStats var7 = null;
         if (var1 != null) {
            if (var2.getPLType() != 10) {
               SQStats var8 = var1.stats((byte)1, (byte)10, var2.getSampleType());
               var5.copyValuesFrom(var8);
            }

            if (var2.getDirection() == 0) {
               var6 = var1.stats((byte)1, var2.getPLType(), var2.getSampleType());
               var7 = var1.stats((byte)-1, var2.getPLType(), var2.getSampleType());
            }
         }

         long var11 = System.currentTimeMillis();
         this.computeStatValues(var5, var2, this.filteredOL, var3, var6, var7, var1);
         return var5;
      } catch (Exception var10) {
         Log.error("Failed to compute stats for combination: " + var2.getKeyAsStr() + ", message: " + var10.getMessage(), var10);
         throw new Exception("Failed to compute stats for combination: " + var2.getKeyAsStr() + ", message: " + var10.getMessage(), var10);
      }
   }

   private void computeStatValues(SQStats var1, StatsTypeCombination var2, OrdersList var3, SettingsMap var4, SQStats var5, SQStats var6, Result var7) throws Exception {
      for (int var8 = 0; var8 < this.statValues.size(); var8++) {
         DatabankColumn var9 = this.statValues.get(var8);
         String var10 = var9.getClassName();
         boolean var11 = true;
         byte[] var12 = var9.getPLTypeRestrictions();
         if (var12 != null && !SQUtils.isInByteArray(var2.getPLType(), var12)) {
            var11 = false;
         }

         if (var11) {
            byte[] var13 = var9.getDirectionRestrictions();
            if (var13 != null && !SQUtils.isInByteArray(var2.getDirection(), var13)) {
            }
         }

         if (var11) {
            try {
               double var16 = var9.compute(var1, var2, var3, var4, var5, var6, var7, this.rgSpecialValues);
               var1.set(var10, var16);
            } catch (Exception var15) {
               Log.error("Exception computing databank column {}", var10, var15);
               var1.set(var10, -1);
            }
         } else if (!var1.containsKey(var10)) {
            var1.set(var10, 0);
         }
      }
   }

   public OrdersList getFilteredOrders() throws Exception {
      if (!this.filtered) {
         throw new Exception("You have to call StatsComputer.filterBy() before calling getFilteredOrders()");
      } else {
         return this.filteredOL;
      }
   }

   public int checkStrategyProblems(String var1, int var2) {
      if (this.filteredOL.size() == 0) {
         return var2;
      }

      double var3 = 0.0;
      double var5 = 0.0;
      double var7 = 0.0;
      int var9 = 0;

      for (int var10 = 0; var10 < this.filteredOL.size(); var10++) {
         Order var11 = this.filteredOL.get(var10);
         if (!var11.isBalanceOrder()) {
            double var12 = var11.PL;
            if (var12 > var3) {
               var7 = var5;
               var5 = var3;
               var3 = var12;
            } else if (var12 > var5) {
               var7 = var5;
               var5 = var12;
            } else if (var12 > var7) {
               var7 = var12;
            }

            var9++;
         }
      }

      if (var9 > 2 && var3 > 2.0 * (var5 + var7)) {
         var2 |= 512;
      }

      return var2;
   }

   public void setRGSpecialValues(SettingsMap var1) {
      this.rgSpecialValues = var1;
   }

   public StatsComputer getClone() {
      StatsComputer var1 = new StatsComputer();
      var1.statValues = this.statValues;
      return var1;
   }

   public SQStats computeZeroStats() {
      SQStats var1 = new SQStats();

      for (int var2 = 0; var2 < this.statValues.size(); var2++) {
         DatabankColumn var3 = this.statValues.get(var2);
         String var4 = var3.getClassName();
         var1.set(var4, 0);
      }

      return var1;
   }
}
