package com.strategyquant.tradinglib.optimization;

import com.strategyquant.lib.L;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.XMLUtil;
import com.strategyquant.lib.app.MainApp;
import com.strategyquant.lib.settings.IXMLAble;
import com.strategyquant.tradinglib.BarChart;
import com.strategyquant.tradinglib.ResultsGroup;
import com.strategyquant.tradinglib.SQStats;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.List;
import org.jdom2.Element;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OptimizationProfile implements IXMLAble {
   public static final Logger Log = LoggerFactory.getLogger("OptimizationResults");
   private ObjectArrayList<OptimizationTestResult> optimizations = new ObjectArrayList();
   private IntOpenHashSet optimizationKeys = new IntOpenHashSet();
   private boolean updated = false;
   public int countTotalOptimizations;
   public int countProfitableOptimizations;
   public int countLosingOptimizations;
   public int lastCount = 0;
   public int countZeroOptimizations;
   public double profitableOptPct;
   public double avgProfit;
   public int uniformDistrChanges;
   public double topProfit;
   public double stdev;
   private double stdevComputed;
   private OptimizationTestResult originalResult;
   private String dismissalMessage = null;
   private int dismissalReason;
   private static final int Passed = 1;
   private static final int Failed = 0;
   private static final int NotEvaluated = -1;
   private static final Object OPTPROF_FILE_FORMAT = null;
   public int profitableOptCheckResult;
   public int avgProfitCheckResult;
   public int uniformDistrCheckResult;
   public int topProfitCheckResult;
   private SysParamPermutationsCharts sysParamPermutationsCharts = null;
   private SysParamPermutationsTable sysParamPermutationsTable = null;
   private JSONArray availableParams = null;
   private JSONObject profitableOptimizationsJSON = null;
   private JSONObject performanceDistributionChart = null;
   private JSONObject checksObjJSON = null;
   private int checksLevelHash;

   public void addResult(ResultsGroup var1) throws Exception {
      String var2 = var1.specialValues().getString("OptimizationParameters");
      if (var2 == null) {
         throw new Exception(L.t("No params set in the result!", new Object[0]));
      }

      synchronized (this.optimizationKeys) {
         if (!this.optimizationKeys.contains(var2.hashCode())) {
            OptimizationTestResult var4 = new OptimizationTestResult(var1, var2, this);
            this.optimizations.add(var4);
            this.optimizationKeys.add(var2.hashCode());
            this.updated = true;
         }
      }
   }

   public void addOriginalResult(ResultsGroup var1) throws Exception {
      String var2 = var1.specialValues().getString("OptimizationParameters");
      if (var2 == null) {
         throw new Exception(L.t("No params set in the result!", new Object[0]));
      }

      this.originalResult = new OptimizationTestResult(var1, var2, this);
      this.updated = true;
   }

   public OptimizationTestResult getOriginalResult() {
      return this.originalResult;
   }

   private void recomputeCounts() {
      if (this.lastCount != this.optimizations.size() && this.optimizations.size() != 0) {
         if (this.profitableOptPct == 0.0 && this.avgProfit == 0.0 && this.countTotalOptimizations != 0) {
         }

         this.lastCount = this.optimizations.size();
         this.countTotalOptimizations = this.optimizations.size();
         this.countProfitableOptimizations = 0;
         this.countLosingOptimizations = 0;
         this.countZeroOptimizations = 0;
         double var1 = 0.0;
         double var3 = Double.MIN_VALUE;
         this.profitableOptPct = 0.0;
         this.avgProfit = 0.0;
         this.uniformDistrChanges = 0;
         this.topProfit = -Double.MAX_VALUE;
         DoubleArrayList var5 = new DoubleArrayList();

         for (int var6 = 0; var6 < this.optimizations.size(); var6++) {
            OptimizationTestResult var7 = (OptimizationTestResult)this.optimizations.get(var6);
            SQStats var8 = var7.stats;
            double var9 = var8.getDouble("NetProfit");
            var5.add(var9);
            var1 += var9;
            if (var9 > this.topProfit) {
               this.topProfit = var9;
            }

            if (var3 != Double.MIN_VALUE && (var3 < 0.0 && var9 > 0.0 || var3 > 0.0 && var9 < 0.0)) {
               this.uniformDistrChanges++;
            }

            if (var9 >= 0.0) {
               this.countProfitableOptimizations++;
            } else if (var9 == 0.0) {
               this.countZeroOptimizations++;
            } else {
               this.countLosingOptimizations++;
            }

            var3 = var9;
         }

         this.profitableOptPct = (double)this.countProfitableOptimizations / this.optimizations.size() * 100.0;
         this.avgProfit = var1 / this.optimizations.size();
         this.stdev = this.computeStdev(this.avgProfit, var5);
      }
   }

   public ObjectArrayList<OptimizationTestResult> getOptimizations() {
      return this.optimizations;
   }

   public int getDismissalReason() {
      return this.dismissalReason;
   }

   public String getDismissalMessage() {
      return this.dismissalMessage;
   }

   public boolean evaluateChecks(OptProfileChecksLevels var1) {
      this.dismissalMessage = null;
      this.dismissalReason = 0;
      this.profitableOptCheckResult = 1;
      this.avgProfitCheckResult = 1;
      this.uniformDistrCheckResult = 1;
      this.topProfitCheckResult = 1;
      this.recomputeCounts();
      if (var1.evalProfitOptCheck) {
         if (this.profitableOptPct <= var1.profitableOptPct) {
            this.profitableOptCheckResult = 0;
            this.dismissalReason = 10008;
            this.dismissalMessage = L.t(
               "%% of Profitable Optimizations (%d %%) > %d %%", new Object[]{SQUtils.round(this.profitableOptPct), var1.profitableOptPct}
            );
         }
      } else {
         this.profitableOptCheckResult = -1;
      }

      if (var1.evalAvgProfitCheck) {
         if (this.avgProfit <= var1.avgProfit) {
            this.avgProfitCheckResult = 0;
            if (this.dismissalMessage == null) {
               this.dismissalReason = 10009;
               this.dismissalMessage = L.t("Average profit ($ %s) > $ %s", new Object[]{SQUtils.d2(this.avgProfit), SQUtils.d2(var1.avgProfit)});
            }
         }
      } else {
         this.avgProfitCheckResult = -1;
      }

      if (var1.evalUniformDistrCheck) {
         if (this.uniformDistrChanges >= var1.uniformDistrChanges) {
            this.uniformDistrCheckResult = 0;
            if (this.dismissalMessage == null) {
               this.dismissalReason = 10010;
               this.dismissalMessage = L.t("Uniform distribution changes (%d) < %d", new Object[]{this.uniformDistrChanges, var1.uniformDistrChanges});
            }
         }
      } else {
         this.uniformDistrCheckResult = -1;
      }

      this.stdevComputed = this.stdev * var1.stdevAvgProfit + this.avgProfit;
      if (var1.evalTopProfitCheck) {
         if (this.topProfit >= this.stdevComputed) {
            this.topProfitCheckResult = 0;
            if (this.dismissalMessage == null) {
               this.dismissalReason = 10011;
               this.dismissalMessage = L.t("Best Optimization profit ($ %s) < $ %s", new Object[]{SQUtils.d2(this.topProfit), SQUtils.d2(this.stdev)});
            }
         }
      } else {
         this.topProfitCheckResult = -1;
      }

      return this.dismissalMessage == null;
   }

   private double computeStdev(double var1, DoubleArrayList var3) {
      if (var3.size() <= 0) {
         return 0.0;
      }

      double var4 = 0.0;

      for (int var10 = 0; var10 < var3.size(); var10++) {
         double var8 = var3.getDouble(var10);
         var4 += Math.pow(1.0 * var8 - var1, 2.0);
      }

      return Math.sqrt(var4 / var3.size());
   }

   public Element getXML() {
      Element var1 = new Element("OptimizationProfile");
      XMLUtil.trySetAttr(var1, "version", "114");
      if (this.originalResult != null) {
         Element var2 = new Element("Original");
         var2.addContent(this.originalResult.getXML());
         var1.addContent(var2);
      }

      for (int var3 = 0; var3 < this.optimizations.size(); var3++) {
         var1.addContent(((OptimizationTestResult)this.optimizations.get(var3)).getXML());
      }

      return var1;
   }

   public void setFromXML(Element var1) throws Exception {
      Element var2 = var1;
      List var3 = var2.getChildren();

      for (int var4 = 0; var4 < var3.size(); var4++) {
         Element var5 = (Element)var3.get(var4);
         if (var5.getName().equals("Original")) {
            OptimizationTestResult var6 = new OptimizationTestResult();
            var6.setFromXML((Element)var5.getChildren().get(0));
            this.originalResult = var6;
         } else {
            OptimizationTestResult var7 = new OptimizationTestResult();
            var7.setFromXML(var5);
            this.optimizations.add(var7);
         }
      }
   }

   public void clear() {
   }

   public void serialize(ObjectOutput var1) throws IOException {
      var1.writeInt(2);
      String var2 = MainApp.settings().get("dontStoreOP3DChartsData");
      boolean var3 = var2 == null ? false : !Boolean.parseBoolean(var2);
      var1.writeBoolean(var3);
      if (var3) {
         if (this.originalResult != null) {
            var1.writeBoolean(true);
            this.originalResult.serialize(var1);
         } else {
            var1.writeBoolean(false);
         }

         var1.writeInt(this.optimizations.size());

         for (int var4 = 0; var4 < this.optimizations.size(); var4++) {
            ((OptimizationTestResult)this.optimizations.get(var4)).serialize(var1);
         }
      }

      this.getSysParamPermutationsTable().serialize(var1);
      this.getSysParamPermutationsCharts().serialize(var1);
      JSONArray var6 = this.getAvailableParams();
      var1.writeUTF(var6.toString());
      JSONObject var5 = this.printProfitableOptimizations();
      var1.writeUTF(var5.toString());
      var1.writeInt(this.countTotalOptimizations);
      var1.writeInt(this.countProfitableOptimizations);
      var1.writeInt(this.countLosingOptimizations);
      var1.writeInt(this.lastCount);
      var1.writeInt(this.countZeroOptimizations);
      var1.writeDouble(this.profitableOptPct);
      var1.writeDouble(this.avgProfit);
      var1.writeDouble(this.uniformDistrChanges);
      var1.writeDouble(this.topProfit);
      var1.writeDouble(this.stdev);
      var1.writeDouble(this.stdevComputed);
      var5 = this.printPerformanceDistributionChart();
      var1.writeUTF(var5.toString());
      var1.flush();
   }

   public void deserialize(ObjectInput var1) throws IOException {
      if (var1.available() > 0) {
         int var2 = var1.readInt();
         String var3 = MainApp.settings().get("dontStoreOP3DChartsData");
         boolean var4 = var3 == null ? false : !Boolean.parseBoolean(var3);
         if (var2 == 1) {
            this.readFormat1(var1);
            if (!var4) {
               this.getSysParamPermutationsTable();
               this.getSysParamPermutationsCharts();
               this.getAvailableParams();
               this.printProfitableOptimizations();
               this.printPerformanceDistributionChart();
            }
         } else {
            if (var2 != 2) {
               throw new IllegalArgumentException("Incorrect format " + var2);
            }

            this.readFormat2(var1);
         }

         if (!var4) {
            this.optimizations.clear();
            this.optimizationKeys.clear();
         }
      }
   }

   private void readFormat2(ObjectInput var1) throws IOException {
      boolean var2 = var1.readBoolean();
      if (var2) {
         this.readFormat1(var1);
      }

      this.sysParamPermutationsTable = new SysParamPermutationsTable(var1);
      this.sysParamPermutationsCharts = new SysParamPermutationsCharts(var1);
      this.availableParams = new JSONArray(var1.readUTF());
      this.profitableOptimizationsJSON = new JSONObject(var1.readUTF());
      this.countTotalOptimizations = var1.readInt();
      this.countProfitableOptimizations = var1.readInt();
      this.countLosingOptimizations = var1.readInt();
      this.lastCount = var1.readInt();
      this.countZeroOptimizations = var1.readInt();
      this.profitableOptPct = var1.readDouble();
      this.avgProfit = var1.readDouble();
      this.uniformDistrChanges = (int)var1.readDouble();
      this.topProfit = var1.readDouble();
      this.stdev = var1.readDouble();
      this.stdevComputed = var1.readDouble();
      this.performanceDistributionChart = new JSONObject(var1.readUTF());
   }

   private void readFormat1(ObjectInput var1) throws IOException {
      if (var1.readBoolean()) {
         OptimizationTestResult var2 = new OptimizationTestResult();
         var2.deserialize(var1);
         this.originalResult = var2;
      }

      int var5 = var1.readInt();

      for (int var3 = 0; var3 < var5; var3++) {
         OptimizationTestResult var4 = new OptimizationTestResult();
         var4.deserialize(var1);
         this.optimizations.add(var4);
      }
   }

   public SysParamPermutationsTable getSysParamPermutationsTable() {
      if (this.sysParamPermutationsTable == null) {
         this.sysParamPermutationsTable = new SysParamPermutationsTable(this);
      }

      return this.sysParamPermutationsTable;
   }

   public SysParamPermutationsCharts getSysParamPermutationsCharts() {
      if (this.sysParamPermutationsCharts == null) {
         this.sysParamPermutationsCharts = new SysParamPermutationsCharts(this);
      }

      return this.sysParamPermutationsCharts;
   }

   public JSONArray getAvailableParams() {
      if (this.availableParams == null) {
         if (this.optimizations.size() == 0) {
            return new JSONArray();
         }

         String var1 = ((OptimizationTestResult)this.optimizations.get(0)).params;
         this.availableParams = new JSONArray();
         String[] var2 = var1.split(",");

         for (int var3 = 0; var3 < var2.length; var3++) {
            if (var2[var3].contains("=")) {
               String[] var4 = var2[var3].split("=");
               String var5 = var4[0].trim();
               this.availableParams.put(var5);
            }
         }
      }

      return this.availableParams;
   }

   public JSONObject printProfitableOptimizations() {
      if (this.profitableOptimizationsJSON == null) {
         JSONObject var1 = new JSONObject();
         this.recomputeCounts();
         double var2 = this.countTotalOptimizations;
         double var4 = this.countProfitableOptimizations;
         double var6 = this.countLosingOptimizations;
         double var8 = this.countZeroOptimizations;
         double var10 = SQUtils.round2(var4 / var2 * 100.0);
         double var12 = SQUtils.round2(var6 / var2 * 100.0);
         double var14 = SQUtils.round2(var8 / var2 * 100.0);
         var1.put("total", new JSONObject().put("count", var2).put("pct", 100));
         var1.put("profitable", new JSONObject().put("count", var4).put("pct", var10));
         var1.put("losing", new JSONObject().put("count", var6).put("pct", var12));
         var1.put("zero", new JSONObject().put("count", var8).put("pct", var14));
         this.profitableOptimizationsJSON = var1;
      }

      return this.profitableOptimizationsJSON;
   }

   public JSONObject printChecksResults(OptProfileChecksLevels var1) {
      if (this.checksObjJSON == null) {
         JSONObject var2 = new JSONObject();
         var2.put("profitableOptPct", SQUtils.round2(this.profitableOptPct));
         var2.put("avgProfit", SQUtils.round2(this.avgProfit));
         var2.put("uniformDistrChanges", this.uniformDistrChanges);
         var2.put("topProfit", SQUtils.round2(this.topProfit));
         var2.put("stdev", SQUtils.round2(this.stdev));
         this.checksObjJSON = var2;
      }

      if (var1 != null) {
         this.evaluateChecks(var1);
         this.checksObjJSON.put("profitableOptCheckResult", this.profitableOptCheckResult);
         this.checksObjJSON.put("avgProfitCheckResult", this.avgProfitCheckResult);
         this.checksObjJSON.put("uniformDistrCheckResult", this.uniformDistrCheckResult);
         this.checksObjJSON.put("topProfitCheckResult", this.topProfitCheckResult);
      }

      return this.checksObjJSON;
   }

   public JSONObject printPerformanceDistributionChart() {
      if (this.performanceDistributionChart == null) {
         this.performanceDistributionChart = new JSONObject();
         BarChart var1 = new BarChart();
         var1.maxXTicksLimit = 10;
         var1.yAxisTitle = L.t("Net profit", new Object[0]);
         var1.xAxisTitle = L.t("Optimization run", new Object[0]);
         var1.tooltips = false;
         var1.displayXAxesLabels = false;
         var1.disableEvents = true;
         ObjectArrayList var2 = this.getOptimizations();
         int[] var3 = new int[var2.size()];

         for (int var4 = 0; var4 < var3.length; var4++) {
            OptimizationTestResult var5 = (OptimizationTestResult)var2.get(var4);
            var3[var4] = (int)var5.stats.getDouble("NetProfit");
         }

         var3 = this.optimalize(var3, (int)this.avgProfit);
         JSONArray var9 = new JSONArray();

         for (int var10 = 0; var10 < var3.length; var10++) {
            var1.addValue("p", var10, var3[var10]);
            var9.put((int)this.avgProfit);
         }

         JSONObject var11 = new JSONObject();
         var11.put("type", "line");
         var11.put("borderColor", "#E8383C");
         var11.put("borderWidth", "2");
         var11.put("fill", "false");
         var11.put("data", var9);
         this.performanceDistributionChart = var1.toJSON();
         JSONArray var6 = this.performanceDistributionChart.getJSONObject("data").getJSONArray("datasets");
         if (var6.length() > 0) {
            JSONObject var7 = this.performanceDistributionChart.getJSONObject("data").getJSONArray("datasets").getJSONObject(0);
            this.performanceDistributionChart.getJSONObject("data").getJSONArray("datasets").put(0, var11).put(var7);
         }
      }

      return this.performanceDistributionChart;
   }

   private int[] optimalize(int[] var1, int var2) {
      short var3 = 500;
      if (var1.length <= var3 * 2) {
         return var1;
      }

      int[] var4 = new int[var3];
      int[] var5 = new int[var3];

      for (int var6 = 0; var6 < var3; var6++) {
         var4[var6] = Integer.MAX_VALUE;
         var5[var6] = Integer.MAX_VALUE;
      }

      int var12 = var1.length / var3;

      for (int var9 = 0; var9 < var1.length; var9++) {
         int var8 = Math.round(var9 / var12);
         int var7 = var1[var9];
         if (var8 >= var3) {
            var8 = var3 - 1;
         }

         if (var4[var8] == Integer.MAX_VALUE || var4[var8] > var7) {
            var4[var8] = var7;
         }

         if (var5[var8] == Integer.MAX_VALUE || var5[var8] < var7) {
            var5[var8] = var7;
         }
      }

      int[] var13 = new int[var3 * 2];
      int var10 = 0;

      for (int var11 = 0; var11 < var3; var11++) {
         var13[var10++] = var4[var11];
         var13[var10++] = var5[var11];
      }

      return var13;
   }

   public void compute(boolean var1, boolean var2) {
      String var3 = MainApp.settings().get("dontStoreOP3DChartsData");
      boolean var4 = true;
      if (var2) {
         if (this.optimizations == null || this.optimizations.size() == 0) {
            return;
         }

         this.recomputeCounts();
         this.sysParamPermutationsTable = null;
         this.getSysParamPermutationsTable();
         this.sysParamPermutationsCharts = null;
         this.getSysParamPermutationsCharts();
         this.getAvailableParams();
         this.profitableOptimizationsJSON = null;
         this.printProfitableOptimizations();
         this.performanceDistributionChart = null;
         this.printPerformanceDistributionChart();
         this.checksObjJSON = null;
         this.printChecksResults(null);
      }
   }

   public void reset() {
      this.lastCount = 0;
      this.profitableOptPct = 0.0;
      this.avgProfit = 0.0;
      this.countTotalOptimizations = 0;
      this.sysParamPermutationsTable = null;
      this.sysParamPermutationsCharts = null;
      this.availableParams = null;
      this.profitableOptimizationsJSON = null;
      this.performanceDistributionChart = null;
   }

   public void clearData() {
      String var1 = MainApp.settings().get("dontStoreOP3DChartsData");
      boolean var2 = var1 == null ? false : !Boolean.parseBoolean(var1);
      if (!var2) {
         if (this.optimizations == null || this.optimizations.size() == 0) {
            return;
         }

         this.getSysParamPermutationsTable();
         this.getSysParamPermutationsCharts();
         this.getAvailableParams();
         this.profitableOptimizationsJSON = null;
         this.printProfitableOptimizations();
         this.performanceDistributionChart = null;
         this.printPerformanceDistributionChart();
         this.checksObjJSON = null;
         this.printChecksResults(null);
         this.optimizations.clear();
         this.optimizationKeys.clear();
      }
   }
}
