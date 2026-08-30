package com.strategyquant.tradinglib.util;

import com.strategyquant.lib.L;
import com.strategyquant.lib.SQTime;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.tradinglib.ResultsGroup;
import com.strategyquant.tradinglib.WalkForwardPeriod;
import com.strategyquant.tradinglib.WalkForwardResult;
import com.strategyquant.tradinglib.fitnessfunction.IFitnessFunction;
import com.strategyquant.tradinglib.optimization.FitnessMetricComparator;
import com.strategyquant.tradinglib.optimization.MetricForFitness;
import com.strategyquant.tradinglib.optimization.OptimizationSettings;
import com.strategyquant.tradinglib.optimization.OrdersIndexComparator;
import com.strategyquant.tradinglib.optimization.StandardFitnessComparator;
import com.strategyquant.tradinglib.optimization.WalkForwardMatrixResult;
import com.strategyquant.tradinglib.project.ProjectConfigHelper;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

public class RngUtils {
   private static final OrdersIndexComparator comparatorIndex = new OrdersIndexComparator();
   private static final StandardFitnessComparator comparatorFitness = new StandardFitnessComparator();

   public static void sortCandidatesByIndexFitness(ArrayList<ResultsGroup> var0, IFitnessFunction var1) {
      for (int var2 = 0; var2 < var0.size(); var2++) {
         ResultsGroup var3 = (ResultsGroup)var0.get(var2);
         var3.specialValues().set("OptIndexFitness", 0.0);
      }

      ArrayList var5 = var1.getMetricsForFitness();

      for (int var6 = 0; var6 < var5.size(); var6++) {
         MetricForFitness var4 = (MetricForFitness)var5.get(var6);
         setIndexByMetric(var0, var4, var1);
      }

      var0.sort(comparatorIndex);
   }

   public static void setIndexByMetric(ArrayList<ResultsGroup> var0, MetricForFitness var1, IFitnessFunction var2) {
      FitnessMetricComparator var3 = new FitnessMetricComparator(var1, var2);
      var0.sort(var3);

      try {
         for (int var4 = 0; var4 < var0.size(); var4++) {
            ResultsGroup var5 = (ResultsGroup)var0.get(var4);
            double var6 = var5.specialValues().getDouble("OptIndexFitness");
            double var8 = var6 + var1.weight * (var4 + 1.0);
            var5.specialValues().set("OptIndexFitness", var8);
         }
      } catch (Exception var10) {
         var10.printStackTrace();
      }
   }

   public static void sortCandidatesByStandardFitness(ArrayList<ResultsGroup> var0, IFitnessFunction var1) throws Exception {
      for (int var2 = 0; var2 < var0.size(); var2++) {
         ResultsGroup var3 = (ResultsGroup)var0.get(var2);
         var3.specialValues().set("OptIndexFitness", 0.0);
      }

      for (int var4 = 0; var4 < var0.size(); var4++) {
         ResultsGroup var5 = (ResultsGroup)var0.get(var4);
         computeFitness(var5, var1);
      }

      var0.sort(comparatorFitness);
   }

   private static void computeFitness(ResultsGroup var0, IFitnessFunction var1) throws Exception {
      double var2 = var1.computeFitness(var0, (byte)0, (byte)10);
      var0.portfolio().setFitness((byte)10, var2);
      var0.portfolio().setFitness((byte)20, var2);
      var0.portfolio().setFitness((byte)127, var2);
   }

   public static void printCandidates(String var0, ArrayList<ResultsGroup> var1, String var2) {
      System.out.println("==========================================");
      System.out.println(var0);
      System.out.println("==========================================");

      try {
         for (int var3 = 0; var3 < var1.size(); var3++) {
            ResultsGroup var4 = (ResultsGroup)var1.get(var3);
            double var5 = var4.subResult(var4.getMainResultKey()).statsOrNull((byte)0, (byte)10, (byte)10).getDouble(var2);
            double var7 = var4.specialValues().getDouble("OptIndexFitness");
            System.out.println("#" + var3 + " : " + var4.getName() + " : " + var5 + " : fitness: " + var7);
         }
      } catch (Exception var9) {
         var9.printStackTrace();
      }

      System.out.println("------------------------------------------");
   }

   public static String getString(HashMap<String, Object> var0, String var1, String var2) {
      String var3 = (String)var0.get(var1);
      return var3 != null && !var3.isEmpty() ? var3 : var2;
   }

   public static int getInt(HashMap<String, Object> var0, String var1, int var2) {
      String var3 = (String)var0.get(var1);
      return var3 != null && !var3.isEmpty() ? Integer.parseInt(var3) : var2;
   }

   public static boolean getBool(HashMap<String, Object> var0, String var1, boolean var2) {
      String var3 = (String)var0.get(var1);
      return var3 != null && !var3.isEmpty() ? Boolean.parseBoolean(var3) : var2;
   }

   public static String getFitnessNameFromOptimizationType(int var0) throws Exception {
      switch (var0) {
         case 1:
            return "fitnessSimple";
         case 2:
            return "fitnessWF";
         case 3:
            return "fitnessCA";
         default:
            throw new Exception(L.t("Cannot recognize Optimization type from settings!", new Object[0]));
      }
   }

   public static String printStrategiesToLog(ArrayList<ResultsGroup> var0, String var1) throws Exception {
      IFitnessFunction var2 = null;
      var2 = ProjectConfigHelper.getFitnessFunction(null);
      sortCandidatesByStandardFitness(var0, var2);
      StringBuffer var3 = new StringBuffer();
      var3.append("List of all evaluated strategies, sorted by their fitness (output as CSV format, tab-delimited) ---------------------------------\n");
      ArrayList var4 = printStrategyResultsHeader(var3, false, var2);

      for (int var5 = 0; var5 < var0.size(); var5++) {
         ResultsGroup var6 = (ResultsGroup)var0.get(var5);
         printStrategyResultToLog(var3, var6, false, var4);
      }

      var3.append("------------------------------------------------------------------\n");
      return var3.toString();
   }

   public static ArrayList<String> printStrategyResultsHeader(StringBuffer var0, boolean var1, IFitnessFunction var2) {
      ArrayList var3 = new ArrayList();
      if (var2 != null) {
         ArrayList var4 = var2.getMetricsForFitness();

         for (int var5 = 0; var5 < var4.size(); var5++) {
            MetricForFitness var6 = (MetricForFitness)var4.get(var5);
            var3.add(var6.metric);
         }
      }

      addIfNotExist(var3, "NumberOfTrades");
      addIfNotExist(var3, "NetProfit");
      var0.append("Strategy name\t");
      var0.append("Parameters\t");
      var0.append("Fitness\t");
      var0.append("FitnessIndex\t");

      for (int var7 = 0; var7 < var3.size(); var7++) {
         var0.append((String)var3.get(var7));
         var0.append("\t");
      }

      var0.append("\n");
      return var3;
   }

   private static void printStrategyResultToLog(StringBuffer var0, ResultsGroup var1, boolean var2, ArrayList<String> var3) throws Exception {
      var0.append(var1.getName());
      var0.append("\t");
      var0.append(getParameters(var1));
      var0.append("\t");
      double var4 = var1.portfolio().getFitness((byte)10);
      var0.append(String.format("%.6f", var4));
      var0.append("\t");
      double var6 = var1.specialValues().getDouble("OptIndexFitness", 0.0);
      var0.append(String.format("%.2f", var6));
      var0.append("\t");

      for (int var8 = 0; var8 < var3.size(); var8++) {
         String var9 = (String)var3.get(var8);
         double var10 = var1.portfolio().stats((byte)0, (byte)10, (byte)127).getDouble(var9);
         var0.append(String.format("%.2f", var10));
         var0.append("\t");
      }

      var0.append("\n");
   }

   private static String getParameters(ResultsGroup var0) {
      try {
         WalkForwardMatrixResult var1 = (WalkForwardMatrixResult)var0.mainResult().get("WalkForwardResult");
         WalkForwardResult var2 = var1.getWFResult(var0.getBestWFResultKey(), true);
         return var2.testParams;
      } catch (Exception var3) {
         return var0.getParams();
      }
   }

   private static void addIfNotExist(ArrayList<String> var0, String var1) {
      for (int var2 = 0; var2 < var0.size(); var2++) {
         if (((String)var0.get(var2)).equals(var1)) {
            return;
         }
      }

      var0.add(var1);
   }

   public static void debugWFLogCandidate(StringBuffer var0, String var1, ResultsGroup var2, ArrayList<String> var3) throws Exception {
      var0.append(var1);
      var0.append("\t");
      var0.append(var2.specialValues().getString("OptimizationParameters"));
      var0.append("\t");
      double var4 = var2.portfolio().getFitness((byte)10);
      var0.append(String.format("%.6f", var4));
      var0.append("\t");
      double var6 = var2.specialValues().getDouble("OptIndexFitness", 0.0);
      var0.append(String.format("%.2f", var6));
      var0.append("\t");

      for (int var8 = 0; var8 < var3.size(); var8++) {
         String var9 = (String)var3.get(var8);
         double var10 = var2.portfolio().stats((byte)0, (byte)10, (byte)127).getDouble(var9);
         var0.append(String.format("%.2f", var10));
         var0.append("\t");
      }

      var0.append("\n");
   }

   public static void debugWFLogWriteFile(OptimizationSettings var0, String var1, String var2, StringBuffer var3, WalkForwardPeriod var4) {
      String var5 = getDebugFilePath(var0, var1, var2, var4);
      File var6 = new File(var5);
      if (var6.exists()) {
         boolean var7 = true;
      }

      if (var5 != null) {
         SQUtils.stringToFile(new File(var5), var3.toString());
      }
   }

   private static String getDebugFilePath(OptimizationSettings var0, String var1, String var2, WalkForwardPeriod var3) {
      StringBuffer var4 = new StringBuffer(var0.debugWFFitnessPath);
      var4.append("/");
      if (var0.type == 2) {
         var4.append("WF_");
      } else {
         var4.append("CA_");
      }

      var4.append(SQTime.toDateString(var3.runFrom));
      var4.append("_");
      var4.append(SQTime.toDateString(var3.runTo));
      var4.append("_");
      var4.append(var1);
      var2 = var2.replace(":", "");
      var4.append(var2);
      var4.append(".csv");
      return var4.toString();
   }
}
