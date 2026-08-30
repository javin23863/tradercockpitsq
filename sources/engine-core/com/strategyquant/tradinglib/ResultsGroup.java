package com.strategyquant.tradinglib;

import com.strategyquant.datalib.InstrumentInfo;
import com.strategyquant.lib.L;
import com.strategyquant.lib.SQTime;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.SettingsMap;
import com.strategyquant.lib.TimePeriods;
import com.strategyquant.lib.XMLUtil;
import com.strategyquant.lib.app.MainApp;
import com.strategyquant.lib.settings.IXMLAble;
import com.strategyquant.lib.utils.IUniqueNameChecker;
import com.strategyquant.pluginlib.program.IProgram;
import com.strategyquant.pluginlib.program.Program;
import com.strategyquant.tradinglib.miniequitychart.MiniEquityChartComputer;
import com.strategyquant.tradinglib.optimization.OptimizationProfile;
import com.strategyquant.tradinglib.optimization.WalkForwardMatrixResult;
import com.strategyquant.tradinglib.project.SQProject;
import com.strategyquant.tradinglib.results.ResultsMap;
import com.strategyquant.tradinglib.results.SpecialValues;
import com.strategyquant.tradinglib.results.StrategyFingerprint;
import com.strategyquant.tradinglib.results.SymbolsMap;
import com.strategyquant.tradinglib.results.stats.comparator.OrderComparatorByCloseTime;
import com.strategyquant.tradinglib.results.stats.comparator.OrderComparatorByOpenTime;
import com.strategyquant.tradinglib.strategy.OutOfSample;
import it.unimi.dsi.fastutil.longs.Long2FloatRBTreeMap;
import it.unimi.dsi.fastutil.longs.Long2FloatSortedMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongBidirectionalIterator;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.StampedLock;
import org.jdom2.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResultsGroup implements IXMLAble, Serializable {
   public static final Logger Log = LoggerFactory.getLogger("ResultsGroup");
   public static final String Portfolio = "Portfolio";
   public static final String MergedPortfolio = "Merged portfolio";
   public static final String WFResult = "WF:";
   public static final String AdditionalMarket = "AdditionalMarket";
   private String resultName;
   private ResultsMap resultsMap = new ResultsMap(this);
   private SymbolsMap symbolsMap = new SymbolsMap();
   private SettingsMap specialValues = new SettingsMap();
   private Databank databank;
   private String resultFileName = null;
   private OrdersList ordersList = null;
   private String lastSettingsXml = null;
   private StampedLock resultsGroupLock = new StampedLock();
   private volatile boolean destroyOnLockRelease = false;
   public boolean updated = true;
   private static boolean storeChartData = false;
   private OptimizationProfile optimizationProfile;
   private String lockingTaskInfo = null;
   private String bestWFResultKey = null;
   private OutOfSample oosSettings;
   private List<ResultsGroup> mergedResults = null;
   public TimePeriods correlationPeriods = null;

   public ResultsGroup(String var1) {
      var1 = this.correctName(var1);
      this.resultName = var1;
      this.ordersList = new OrdersList(var1);
   }

   public static void init() {
      storeChartData = Boolean.parseBoolean(MainApp.settings().get("storeChartData", "false"));
   }

   public String getName() {
      return this.resultName;
   }

   public void setName(String var1) {
      this.resultName = this.correctName(var1);
   }

   private String correctName(String var1) {
      return var1 != null ? var1.replace(",", ".") : null;
   }

   private void ensureResultsMap() throws Exception {
      if (this.resultsMap == null) {
         throw new Exception("ResultsMap is empty, this ResultsGroup was destroyed before! " + this + " - " + this.getName());
      }
   }

   public void addResultSymbol(String var1, String var2) throws Exception {
   }

   public Result subResult(String var1) throws Exception {
      this.ensureResultsMap();
      return this.resultsMap.get(var1);
   }

   public String getMainResultKey() throws Exception {
      this.ensureResultsMap();
      return this.resultsMap.getMainResultKey();
   }

   public Result mainResult() throws Exception {
      this.ensureResultsMap();
      String var1 = this.resultsMap.getMainResultKey();
      return this.subResult(var1);
   }

   public Result portfolio() throws Exception {
      this.ensureResultsMap();
      return this.resultsMap.get(null);
   }

   public void addSubresult(String var1, SettingsMap var2) throws Exception {
      this.ensureResultsMap();
      this.resultsMap.addResult(var1, var2);
   }

   public void addSubresult(String var1, SettingsMap var2, Result var3) throws Exception {
      this.ensureResultsMap();
      this.resultsMap.addResult(var1, var2, var3);
   }

   public void removeSubresult(String var1, boolean var2) throws Exception {
      this.ensureResultsMap();
      this.resultsMap.removeResult(var1, var2);
   }

   public List<String> getResultKeys() throws Exception {
      this.ensureResultsMap();
      return this.resultsMap.keys();
   }

   public List<String> additionalMarketKeys() throws Exception {
      this.ensureResultsMap();
      return this.resultsMap.additionalMarketKeys();
   }

   public boolean hasResult(String var1) throws Exception {
      this.ensureResultsMap();
      return this.resultsMap.hasResult(var1);
   }

   public SymbolsMap symbols() {
      return this.symbolsMap;
   }

   public void setDatabank(Databank var1) {
      this.databank = var1;
   }

   public void updateResults(ResultsGroup var1, boolean var2) throws Exception {
      this.resultsMap = var1.resultsMap;
      this.ordersList.clear();
      this.ordersList = var1.ordersList;
      this.lastSettingsXml = var1.lastSettingsXml;
      this.symbolsMap = var1.symbolsMap;
      String var3 = this.specialValues().getString(SpecialValues.Note, "");
      this.specialValues = var1.specialValues;
      this.specialValues().setString(SpecialValues.Note, var3);
      this.optimizationProfile = var1.optimizationProfile;
      this.updated = true;
   }

   public OrdersList orders() throws Exception {
      return this.ordersList;
   }

   public String getFilePath() {
      return this.databank != null ? this.databank.getResultsFilePath(this) : this.resultFileName;
   }

   public Databank getDatabank() {
      return this.databank;
   }

   public String generateUniqueResultKeyName(String var1) throws Exception {
      return SQUtils.generateUniqueName(var1, new IUniqueNameChecker() {
         public boolean checkNameExist(String var1) throws Exception {
            return ResultsGroup.this.hasResult(var1);
         }
      });
   }

   public static ResultsGroup merge(ArrayList<ResultsGroup> var0, String[] var1, SQProject var2) throws Exception {
      return merge(var0, var1, var2, null, null);
   }

   public static ResultsGroup merge(
      ArrayList<ResultsGroup> var0, String[] var1, SQProject var2, Long2ObjectOpenHashMap<String> var3, ObjectArrayList<String> var4
   ) throws Exception {
      ResultsGroup var5 = new ResultsGroup("Merged portfolio");
      int var6 = var0.size();

      for (int var7 = 0; var7 < var6; var7++) {
         ResultsGroup var8 = (ResultsGroup)var0.get(var7);
         var5.mergeWith(var8, var1, var3, var4);
         if (var2 != null) {
            var2.getProgress().update("Create Portfolio", -1.0, null, L.t(String.format("Merging strategies %d / %d", var7 + 1, var6), new Object[]{true}));
         }
      }

      return var5;
   }

   public static ResultsGroup mergeWF(ArrayList<ResultsGroup> var0, String[] var1, SQProject var2) throws Exception {
      ResultsGroup var3 = new ResultsGroup("Merged portfolio");
      int var4 = var0.size();

      for (int var5 = 0; var5 < var4; var5++) {
         ResultsGroup var6 = (ResultsGroup)var0.get(var5);
         var3.mergeWith(var6, var1, true);
         if (var2 != null) {
            var2.getProgress().update("Create Portfolio", -1.0, null, L.t(String.format("Merging strategies %d / %d", var5 + 1, var4), new Object[]{true}));
         }
      }

      return var3;
   }

   public static ResultsGroup merge(ResultsGroup... var0) throws Exception {
      ResultsGroup var1 = new ResultsGroup("Merged portfolio");

      for (int var2 = 0; var2 < var0.length; var2++) {
         ResultsGroup var3 = var0[var2];
         var1.mergeWith(var3, null);
      }

      return var1;
   }

   public void mergeWith(ResultsGroup var1) throws Exception {
      this.mergeWith(var1, null);
   }

   public void mergeWith(ResultsGroup var1, String[] var2) throws Exception {
      this.mergeWith(var1, var2, false, null, null);
   }

   public void mergeWith(ResultsGroup var1, String[] var2, Long2ObjectOpenHashMap<String> var3, ObjectArrayList<String> var4) throws Exception {
      this.mergeWith(var1, var2, false, var3, var4);
   }

   public void mergeWith(ResultsGroup var1, String[] var2, boolean var3) throws Exception {
      this.mergeWith(var1, var2, var3, null, null);
   }

   public void mergeWith(ResultsGroup var1, String[] var2, boolean var3, Long2ObjectOpenHashMap<String> var4, ObjectArrayList<String> var5) throws Exception {
      OrdersList var6 = this.orders();
      Element var7 = null;
      int var8 = 0;
      ResultsGroup var9 = null;
      Object var10 = null;
      double var11 = 0.0;
      var8 |= var1.specialValues().getInt("StrategyProblems", 0);
      if (this.getResultKeys().size() == 0) {
         var9 = var1;
         ChartSetup var13 = (ChartSetup)var9.portfolio().getSettings().get("BacktestChart");
         if (var13 == null) {
            Object var10000 = null;
         } else {
            var13.getClone();
         }
      } else {
         var10 = (ChartSetup)this.portfolio().getSettings().get("BacktestChart");
         if (var10 != null) {
            ChartSetup var28 = (ChartSetup)var1.portfolio().getSettings().get("BacktestChart");
         }
      }

      if (var7 == null) {
         var7 = var1.portfolio().getStrategyXml();
         if (var7 != null) {
            var7 = var7.clone();
         }
      }

      boolean var29 = var1.isHodlStrategy();
      List var14 = var1.getResultKeys();

      for (String var17 : this.getKeysToMerge(var1, var14, var3, var2)) {
         Result var18 = var1.subResult(var17);
         String var19;
         if (var17.startsWith("Main:")) {
            var19 = var17.replace("Main", var1.getName());
         } else {
            var19 = var17;
         }

         if (var3 && var17.startsWith("WF:")) {
            var19 = var19.replace("WF:", "WF_");
         }

         if (var1.resultsMap != null && var1.resultsMap.size() == 1) {
            String var20 = var1.getName();
            if (var20.equals("CrossCheck_SequentialOptimization")) {
               var19 = "CrossCheck_SequentialOptimization";
            } else {
               int var21 = var19.lastIndexOf(":");
               String var22 = null;
               if (var21 > 0) {
                  var22 = var19.substring(var21 + 1);
               }

               if (var20 != null) {
                  var19 = var20;
                  if (var22 != null) {
                     var19 = var19 + ": " + var22;
                  }
               }
            }
         }

         if (this.hasResult(var19)) {
            var19 = SQUtils.generateUniqueName(var17, new IUniqueNameChecker() {
               public boolean checkNameExist(String var1) throws Exception {
                  return ResultsGroup.this.hasResult(var1);
               }
            });
         }

         SettingsMap var32 = var18.getSettings().clone();
         Result var34 = new Result(var19, this, var32);
         var34.setString(SpecialValues.Symbol, var1.mainResult().getString(SpecialValues.Symbol));
         var34.setString(SpecialValues.Timeframe, var1.mainResult().getString(SpecialValues.Timeframe));
         this.addSubresult(var19, var32, var34);
         OrdersList var35 = var1.orders().filterWithClone(var17, (byte)0, (byte)127);

         for (int var23 = 0; var23 < var35.size(); var23++) {
            Order var24 = var35.get(var23);
            if (var17.hashCode() != var19.hashCode() && var24.SetupName.equals(var17) || var1.isStockpickerStrategy()) {
               var24.SetupName = var19;
            }

            if (var3 && var17.startsWith("WF:")) {
               var24.SampleType = 10;
            }

            if (var4 != null) {
               var4.put(var24.OrderId, var24.SetupName);
            }

            if (var5 != null) {
               var5.add(var24.SetupName);
            }

            var6.add(var24);
         }

         this.symbols().add(var1.symbolsMap);
         var34.setStockChartPath(null);
         Long2FloatRBTreeMap var37 = var18.getWorstDailyEquity();
         if (var37 != null) {
            var34.setWorstDailyEquity(var37.clone());
         }

         var34.copyStats(var18);
      }

      var11 += var1.specialValues().getDouble(SpecialValues.BacktestDuration, 0.0);
      if (!this.specialValues().containsKey(SpecialValues.DateGenerated)) {
         this.specialValues().set(SpecialValues.DateGenerated, SQTime.getLocalCurrentTimeInMs());
      }

      this.specialValues().set(SpecialValues.DateLastModified, SQTime.getLocalCurrentTimeInMs());
      if (var9 != null) {
         this.setLastSettings(var9.getLastSettings());
         this.specialValues().setString(SpecialValues.Symbol, "Merged portfolio");
         this.specialValues().set(SpecialValues.HistoryFrom, var9.specialValues().getLong(SpecialValues.HistoryFrom));
         this.specialValues().set(SpecialValues.HistoryTo, var9.specialValues().getLong(SpecialValues.HistoryTo));
         this.specialValues().setString(SpecialValues.Timeframe, var9.specialValues().getString(SpecialValues.Timeframe));
         this.specialValues().set("StrategyProblems", var8);
         this.specialValues().set(SpecialValues.BacktestDuration, SQUtils.round(var11, 2));
         this.specialValues().set(SpecialValues.LastModified, System.currentTimeMillis());
         this.specialValues().set(SpecialValues.Complexity, var9.specialValues().getInt(SpecialValues.Complexity));
         this.specialValues().set(SpecialValues.ParametersStability, var9.specialValues().get(SpecialValues.ParametersStability));
         if (var7 != null) {
            var7.setAttribute("mergedResult", "false");
            this.portfolio().addStrategyXml(var7);
         }

         long var30 = var9.specialValues().getLong(SpecialValues.HistoryFrom, Long.MAX_VALUE);
         long var31 = var9.specialValues().getLong(SpecialValues.HistoryTo, Long.MIN_VALUE);
         long var33 = this.specialValues().getLong(SpecialValues.PortfolioHistoryFrom, Long.MAX_VALUE);
         long var36 = this.specialValues().getLong(SpecialValues.PortfolioHistoryTo, Long.MIN_VALUE);
         if (var30 < var33) {
            this.specialValues().set(SpecialValues.PortfolioHistoryFrom, var30);
         }

         if (var31 > var36) {
            this.specialValues().set(SpecialValues.PortfolioHistoryTo, var31);
         }
      }

      if (this.getOptimizationProfile() == null) {
         this.setOptimizationProfile(var1.getOptimizationProfile());
      }

      this.updated = true;
   }

   private ArrayList<String> getKeysToMerge(ResultsGroup var1, List<String> var2, boolean var3, String[] var4) throws Exception {
      ArrayList var5 = new ArrayList();
      if (var3) {
         for (String var7 : var2) {
            if (var7.startsWith("WF:")) {
               var5.add(var7);
               break;
            }
         }

         if (var5.size() > 0) {
            return var5;
         }
      }

      Iterator var10 = var2.iterator();

      while (true) {
         String var11;
         while (true) {
            if (!var10.hasNext()) {
               return var5;
            }

            var11 = (String)var10.next();
            if (!var11.equals("Portfolio") && (var3 || !var11.startsWith("WF"))) {
               if (var4 == null) {
                  break;
               }

               boolean var8 = false;

               for (int var9 = 0; var9 < var4.length; var9++) {
                  if (var11.contains(var4[var9])) {
                     var8 = true;
                     break;
                  }
               }

               if (!var8) {
                  break;
               }
            }
         }

         Result var12 = var1.subResult(var11);
         if (!var12.isSpecial()) {
            var5.add(var11);
         }
      }
   }

   public boolean isPortfolio() throws Exception {
      this.ensureResultsMap();
      return this.resultsMap.isPortfolio();
   }

   public SettingsMap specialValues() {
      return this.specialValues;
   }

   public Element getXML() {
      Element var1 = new Element("ResultsGroup");
      XMLUtil.trySetAttr(var1, "ResultName", this.resultName);
      if (this.resultsMap != null) {
         Element var2 = this.resultsMap.getXML();
         var1.addContent(var2);
      }

      if (this.symbolsMap != null) {
         Element var4 = this.symbolsMap.getXML();
         var1.addContent(var4);
      }

      if (this.specialValues != null) {
         Element var5 = new Element("SpecialValuesMap");
         Element var3 = this.specialValues.getXML();
         var5.addContent(var3);
         var1.addContent(var5);
      }

      return var1;
   }

   public void setFromXML(Element var1) throws Exception {
      Element var2 = var1.getChild("ResultsMap");
      Element var3 = var1.getChild("SymbolsMap");
      if (var2 != null) {
         this.resultsMap = new ResultsMap(this);
         this.resultsMap.setFromXML(var2);
      }

      if (var3 != null) {
         this.symbolsMap = new SymbolsMap();
         this.symbolsMap.setFromXML(var3);
      }

      Element var4 = var1.getChild("SpecialValuesMap");
      if (var4 != null) {
         this.specialValues = new SettingsMap();
         Element var5 = var4.getChild("SettingsMap");
         List var6 = var5.getChildren();
         String var7 = this.getMainResultKey();
         var7 = MiniEquityChartComputer.correctResultKey(var7);

         for (int var8 = 0; var8 < var6.size(); var8++) {
            Element var9 = (Element)var6.get(var8);
            String var10 = SQUtils.decodeXmlKey(var9.getName());
            if (var10.startsWith("MEC_") && var10.endsWith(var7)) {
               var10 = var10.replace(var7, "Main");
            }

            Object var11 = null;
            if (var9.getContentSize() > 0) {
               var11 = XMLUtil.elementToValue(var9);
            }

            this.specialValues.set(var10, var11);
         }
      }
   }

   public void computeAllStats() throws Exception {
      this.ensureResultsMap();
      this.resultsMap.computeAllStats(this.specialValues(), this.getOOS());
      StrategyFingerprint var1 = new StrategyFingerprint(this);
      this.specialValues().set(SpecialValues.Fingerprint, var1);
   }

   public ResultsGroup clone() {
      return this.clone(false);
   }

   public ResultsGroup clone(boolean var1) {
      ResultsGroup var2 = new ResultsGroup(this.resultName);

      try {
         var2.symbolsMap = this.symbolsMap.getClone();
      } catch (Exception var5) {
         Log.error("Cannot clone SymbolsMap", var5);
      }

      var2.resultsMap = this.resultsMap.clone(var2);
      var2.specialValues = this.specialValues.clone();
      var2.databank = this.databank;
      var2.updated = true;
      var2.lastSettingsXml = this.lastSettingsXml;

      try {
         if (this.ordersList != null) {
            var2.ordersList = var1 ? this.ordersList.cloneDeep() : this.ordersList.clone();
         }
      } catch (Exception var4) {
         Log.error("Exception when cloning ResultsGroup: ", var4);
      }

      var2.optimizationProfile = this.optimizationProfile;
      if (this.mergedResults != null) {
         var2.mergedResults = new ArrayList<>();

         for (int var3 = 0; var3 < this.mergedResults.size(); var3++) {
            var2.mergedResults.add(this.mergedResults.get(var3).clone());
         }
      }

      return var2;
   }

   public Element getStrategyXml() throws Exception {
      return this.portfolio().getStrategyXml();
   }

   public void setStrategyXml(Element var1) throws Exception {
      this.portfolio().addStrategyXml(var1);
   }

   public void setResultFileName(String var1) {
      this.resultFileName = var1;
   }

   public String getParams() {
      try {
         return (String)this.specialValues().get("OptimizationParameters");
      } catch (Exception var2) {
         Log.error("Error while obtaining opt params. Exc.", var2);
         return "N/A";
      }
   }

   public String getOriginalWFResultKey() throws Exception {
      for (String var2 : this.getResultKeys()) {
         if (!var2.startsWith("WF:") && !var2.equals("Portfolio")) {
            return var2;
         }
      }

      return null;
   }

   public int getChartCount() {
      try {
         Element var1 = this.portfolio().getStrategyXml();
         Element var2 = var1.getChild("Strategy").getChild("Datas");
         return var2.getChildren("data").size() - 1;
      } catch (Exception var3) {
         Log.error("Chart count of ResultsGroup '" + this.getName() + "' cannot be resolved. ", var3);
         return -1;
      }
   }

   public void setLastSettings(String var1) throws Exception {
      this.lastSettingsXml = var1;
   }

   public String getLastSettings() throws Exception {
      return this.lastSettingsXml;
   }

   public void createPortfolioResult() throws Exception {
      this.createPortfolioResult("single", null);
   }

   public void createPortfolioResult(String var1, SQProject var2) throws Exception {
      Result var3 = null;
      String var4 = null;
      Long2FloatRBTreeMap var5 = this.recomputeMergedDailyEquity();
      int var6 = 0;
      double var7 = 0.0;
      long var9 = Long.MAX_VALUE;
      long var11 = Long.MIN_VALUE;

      for (String var14 : this.getResultKeys()) {
         Result var15 = this.subResult(var14);
         if (!var15.isSpecial()) {
            var6++;
            if (var4 == null) {
               var4 = var15.getString(SpecialValues.Timeframe, "N/A");
            } else if (!var4.equals(var15.getString(SpecialValues.Timeframe, "N/A"))) {
               var4 = L.t("multiple", new Object[]{true});
            }

            if (var3 == null) {
               var3 = var15;
            }

            SettingsMap var16 = var15.getSettings();
            if (var16 != null) {
               var7 += var16.getDouble("MoneyManagement.InitialCapital", 10000.0);
            }

            long var17 = this.specialValues().getLong(SpecialValues.PortfolioHistoryFrom, Long.MAX_VALUE);
            long var19 = this.specialValues().getLong(SpecialValues.PortfolioHistoryTo, Long.MIN_VALUE);
            if (var17 < var9) {
               var9 = var17;
            }

            if (var19 > var11) {
               var11 = var19;
            }
         }
      }

      if (var6 <= 1) {
         throw new Exception("ResultGroup doesn't contain more Results, cannot create a portfolio!");
      }

      if (var3 == null) {
         throw new Exception("No Result in ResultGroup to create portfolio!");
      }

      SettingsMap var21 = var3.getSettings();
      if (var1.equals("sum")) {
         var21 = var21.clone();
         var21.set("MoneyManagement.InitialCapital", var7);
      }

      var21.set("PortfolioDataStart", var9);
      var21.set("PortfolioDataEnd", var11);
      Result var22 = new Result("Portfolio", this, var21);
      this.removeSubresult("Portfolio", true);
      this.addSubresult("Portfolio", var21, var22);
      this.specialValues().setString(SpecialValues.Symbol, "Portfolio");
      this.specialValues().setString(SpecialValues.Timeframe, var4);
      OrdersList var23 = this.orders();
      var23.sort(new OrderComparatorByOpenTime());
      if (var3 != null && var3.getStrategyXml() != null) {
         var22.addStrategyXml(var3.getStrategyXml());
      }

      if (var5 != null) {
         var22.setWorstDailyEquity(var5);
      }

      if (var2 != null) {
         var2.getProgress().update("Create Portfolio", -1.0, null, L.t("Computing stats of portfolio", new Object[0]));
      }

      var22.computeAllStats(this.specialValues(), this.getOOS());
      this.updated = true;
   }

   public void createPortfolioResult(SQProject var1, float var2, MoneyManagementMethod var3) throws Exception {
      Result var4 = null;
      String var5 = null;
      Long2FloatRBTreeMap var6 = this.recomputeMergedDailyEquity();
      int var7 = 0;
      long var8 = Long.MAX_VALUE;
      long var10 = Long.MIN_VALUE;

      for (String var13 : this.getResultKeys()) {
         Result var14 = this.subResult(var13);
         if (!var14.isSpecial()) {
            var7++;
            if (var5 == null) {
               var5 = var14.getString(SpecialValues.Timeframe, "N/A");
            } else if (!var5.equals(var14.getString(SpecialValues.Timeframe, "N/A"))) {
               var5 = L.t("multiple", new Object[]{true});
            }

            if (var4 == null) {
               var4 = var14;
            }

            long var15 = this.specialValues().getLong(SpecialValues.PortfolioHistoryFrom, Long.MAX_VALUE);
            long var17 = this.specialValues().getLong(SpecialValues.PortfolioHistoryTo, Long.MIN_VALUE);
            if (var15 < var8) {
               var8 = var15;
            }

            if (var17 > var10) {
               var10 = var17;
            }
         }
      }

      if (var7 <= 1) {
         throw new Exception("ResultGroup doesn't contain more Results, cannot create a portfolio!");
      }

      if (var4 == null) {
         throw new Exception("No Result in ResultGroup to create portfolio!");
      }

      SettingsMap var19 = var4.getSettings();
      var19.set("MoneyManagement.InitialCapital", var2);
      var19.set("PortfolioDataStart", var8);
      var19.set("PortfolioDataEnd", var10);
      Result var20 = new Result("Portfolio", this, var19);
      this.removeSubresult("Portfolio", true);
      this.addSubresult("Portfolio", var19, var20);
      this.specialValues().setString(SpecialValues.Symbol, "Portfolio");
      this.specialValues().setString(SpecialValues.Timeframe, var5);
      OrdersList var21 = this.orders();
      var21.sort(new OrderComparatorByOpenTime());
      if (var4 != null && var4.getStrategyXml() != null) {
         var20.addStrategyXml(var4.getStrategyXml());
      }

      var21.sort(new OrderComparatorByOpenTime());
      if (var3 != null) {
         StrategyBase var22 = StrategyBase.createXmlStrategy(var4.getStrategyXml());
         this.calculateOrderValueSizeByMM(var21, var19, var6, var22, var2, var3);
      }

      if (var6 != null) {
         var20.setWorstDailyEquity(var6);
      }

      if (var1 != null) {
         var1.getProgress().update("Create Portfolio", -1.0, null, L.t("Computing stats of portfolio", new Object[0]));
      }

      var20.computeAllStats(this.specialValues(), this.getOOS());
      this.updated = true;
   }

   private void calculateOrderValueSizeByMM(
      OrdersList var1, SettingsMap var2, Long2FloatRBTreeMap var3, StrategyBase var4, float var5, MoneyManagementMethod var6
   ) throws Exception {
      float var7 = var5;
      float var8 = var7;
      float var9 = 0.0F;
      float var10 = 1.0F;
      float var11 = 0.0F;
      float var12 = 0.0F;
      float var13 = 0.0F;

      for (int var14 = 0; var14 < var1.size(); var14++) {
         Order var15 = var1.get(var14);

         try {
            List var16 = XMLUtil.stringToElement(this.getLastSettings()).getChild("Resources").getChild("Symbols").getChildren();
            Element var17 = ((Element)var16.get(0)).getChild("InstrumentInfo");
            InstrumentInfo var18 = new InstrumentInfo();
            var18.setFromXML(var17);
            var4.accountBalance = var7;
            var4.accountEquity = var7;
            var4.Symbol = var15.Symbol;
            var4.Instrument = var18.instrument;
            float var19 = var15.Size;
            if (var19 == 0.0F) {
               var19 = 1.0F;
            }

            var15.Ticket = var14 + 1;
            var15.Size = (float)var6.computeTradeSize(var4, var15.Symbol, var15.Type, var15.OpenPrice, var15.StopLoss, var18.tickSize, var18.pointValue);
            var15.PL = var15.PipsPL * var15.Size * (float)(var18.pointValue * var18.tickSize);
            float var20 = var15.Size / var19;
            var15.CommSwap *= var20;
            var15.MAE *= var20;
            var15.MFE *= var20;
            var15.PL = var15.PL + var15.CommSwap;
            var15.PL = var15.PL - var15.SlippageInMoney;
            var7 += var15.PL;
            var15.AccountBalance = var7;
            var15.AccountBalanceTemp = var7;
            if (var7 > var8) {
               var8 = var7;
            }

            if (var8 - var7 > var9) {
               var9 = var8 - var7;
            }

            var15.DD = var9;
            if (var7 != 0.0F) {
               var15.PctDD = Math.abs(var9 / var7);
               var15.PctPL = 100.0F * var15.PL / var7;
            } else {
               var15.PctPL = 0.0F;
            }

            if (var15.PL > 0.0F && var15.PctPL < 0.0F) {
               var15.PctPL = -var15.PctPL;
            }

            if (var15.PL < 0.0F && var15.PctPL > 0.0F) {
               var15.PctPL = -var15.PctPL;
            }

            var13 += var15.PctPL;
            var15.PctAccountBalance = var13;
            float var21 = 1.0F + var15.PctPL / 100.0F;
            float var22 = var21 * var10;
            var10 = var22;
            var12 = (var22 - 1.0F) * 100.0F;
            var15.PctPL_TWR = var12 - var11;
            var11 = var12;
            var15.Size = (float)SQUtils.round2(var15.Size);
            var15.MAE = (float)SQUtils.round2(var15.MAE);
            var15.MFE = (float)SQUtils.round2(var15.MFE);
            var15.CommSwap = (float)SQUtils.round2(var15.CommSwap);
            var15.AccountBalance = (float)SQUtils.round2(var15.AccountBalance);
            var15.AccountBalanceTemp = (float)SQUtils.round2(var15.AccountBalanceTemp);
            var15.PL = (float)SQUtils.round2(var15.PL);
            var15.PctPL_TWR = (float)SQUtils.round2(var15.PctPL_TWR);
            var15.PipsPL = (float)SQUtils.round2(var15.PipsPL);
            var15.PctDD = (float)SQUtils.round2(var15.PctDD);
            var15.PctPL = (float)SQUtils.round2(var15.PctPL);
            var15.DD = (float)SQUtils.round2(var15.DD);
            var15.PctAccountBalance = (float)SQUtils.round2(var15.PctAccountBalance);
         } catch (Exception var23) {
            throw new Exception("Failed to calculate initial buy amount from MoneyManagement method - " + var23.getMessage(), var23);
         }
      }

      if (var3 != null) {
         this.recalculateSubresultDailyEquity(this.portfolio());
      }
   }

   public Long2FloatRBTreeMap recomputeMergedDailyEquity() throws Exception {
      ArrayList var1 = null;

      for (String var3 : this.getResultKeys()) {
         if (!var3.contains("Portfolio")) {
            Result var4 = this.subResult(var3);
            if (!var4.isSpecial()) {
               Long2FloatRBTreeMap var5 = var4.getWorstDailyEquity();
               if (var5 != null) {
                  if (var1 == null) {
                     var1 = new ArrayList();
                  }

                  var1.add(var5);
               }
            }
         }
      }

      if (var1 == null) {
         return null;
      } else {
         return var1.size() == 1 ? new Long2FloatRBTreeMap((Long2FloatSortedMap)var1.get(0)) : this.mergeDailyEquity(var1);
      }
   }

   private Long2FloatRBTreeMap mergeDailyEquity(List<Long2FloatRBTreeMap> var1) {
      Long2FloatRBTreeMap var2 = new Long2FloatRBTreeMap();

      for (Long2FloatRBTreeMap var4 : var1) {
         var4.forEach((var1x, var2x) -> var2.put(var1x, 0.0F));
      }

      float[] var11 = new float[var1.size()];

      for (int var12 = 0; var12 < var11.length; var12++) {
         var11[var12] = 0.0F;
      }

      LongBidirectionalIterator var13 = var2.keySet().iterator();

      while (var13.hasNext()) {
         long var5 = (Long)var13.next();
         float var7 = 0.0F;

         for (int var8 = 0; var8 < var1.size(); var8++) {
            Long2FloatRBTreeMap var9 = (Long2FloatRBTreeMap)var1.get(var8);
            float var10 = Float.MAX_VALUE;
            if (var9.containsKey(var5)) {
               var10 = var9.get(var5);
            }

            if (var10 != Float.MAX_VALUE) {
               var11[var8] = var9.get(var5);
            }

            var7 += var11[var8];
         }

         var2.put(var5, var7);
      }

      return var2;
   }

   public void moveResultFrom(String var1, ResultsGroup var2, boolean var3) throws Exception {
      Result var4 = var2.subResult(var1);
      if (var4 == null) {
         throw new Exception("Result'" + var1 + "' doesn't exist in source ResultsGroup!");
      }

      if (!this.hasResult(var1)) {
         this.moveOrdersFrom(var1, var2);
         if (var3) {
            var4.setSpecial(var3);
         }

         this.addSubresult(var1, var4.getSettings(), var4);
         this.updated = true;
      }
   }

   private void moveOrdersFrom(String var1, ResultsGroup var2) throws Exception {
      if (!this.hasResult(var1)) {
         OrdersList var3 = var2.orders().filter(var1, (byte)0, (byte)127);
         this.orders().addAll(var3);
      }
   }

   public void removeUnsavableSettings() {
      this.resultsMap.removeUnsavableSettings();
   }

   public double getFitness() {
      return this.getFitness((byte)10);
   }

   public double getFitness(byte var1) {
      double var2 = 0.0;

      try {
         var2 = this.portfolio().getFitness(var1);
      } catch (Exception var5) {
         Log.error("Error while getting fitness value of ResultsGroup '" + this.resultName + "'.", var5);
      }

      return var2 > 0.0 ? var2 : 0.0;
   }

   public StrategyFingerprint getFingerprint() {
      Object var1 = this.specialValues().get(SpecialValues.Fingerprint, null);
      return var1 == null ? null : (StrategyFingerprint)var1;
   }

   public ResultsGroup lockRecord(String var1) throws Exception {
      return this.lockRecord(15, var1);
   }

   public ResultsGroup lockRecord(int var1, String var2) throws Exception {
      try {
         if (this.resultsGroupLock.tryWriteLock(var1, TimeUnit.SECONDS) == 0L) {
            Log.error("Strategy result lock from {} not acquired, lock held by: {}!", var2, this.lockingTaskInfo);
            throw new Exception("Lock not acquired ");
         } else {
            this.lockingTaskInfo = var2;
            return this;
         }
      } catch (Exception var4) {
         Log.error("[2] Strategy result lock from {} not acquired, lock held by: {}!", var2, this.lockingTaskInfo);
         Log.error("Cannot lock orders", var4);
         throw var4;
      }
   }

   public void releaseLock(String var1) {
      if (this.resultsGroupLock.tryUnlockWrite()) {
         this.lockingTaskInfo = null;
      } else {
         Log.debug("Nothing to unlock, record not locked, at: {}", var1);
      }
   }

   public void clear() {
      this.resultsMap.clear(true, true);
      this.ordersList.clear();
      String var1 = this.specialValues().getString(SpecialValues.Note, "");
      boolean var2 = this.specialValues().containsKey(SpecialValues.IsMergedPortfolio);
      long var3 = this.specialValues().getLong(SpecialValues.DateGenerated, -1L);
      this.specialValues().clear();
      this.specialValues().setString(SpecialValues.Note, var1);
      if (var2) {
         this.specialValues().set(SpecialValues.IsMergedPortfolio, 1);
      }

      this.specialValues().set(SpecialValues.DateGenerated, var3);
      if (this.optimizationProfile != null) {
         this.optimizationProfile.clear();
         this.optimizationProfile = null;
      }

      this.updated = true;
   }

   public void clearTempData() {
      this.resultsMap.clearTempData();
      if (this.optimizationProfile != null) {
         this.optimizationProfile.clear();
         this.optimizationProfile = null;
      }

      this.updated = true;
   }

   public void destroy(boolean var1) {
      this.resultsMap.clear(false, var1);
      this.ordersList.clear();
      this.specialValues().clear();
      if (this.optimizationProfile != null) {
         this.optimizationProfile.clear();
         this.optimizationProfile = null;
      }

      if (this.mergedResults != null) {
         this.mergedResults.clear();
         this.mergedResults = null;
      }

      this.updated = true;
   }

   public boolean isFromAlgoWizard() {
      return this.specialValues().get("backtestID") != null;
   }

   public void trySaveToFile() {
      if (this.databank != null && this.databank.isFileBased()) {
         String var1 = this.databank.getResultsFilePath(this);

         try {
            IProgram var2 = Program.get("Saver");
            var2.call("saveFile", new Object[]{this, var1});
            this.resultFileName = var1;
         } catch (Exception var3) {
            Log.error("Saving ResultsGroup failed", var3);
         }
      }
   }

   public boolean storesChartData() {
      return storeChartData;
   }

   public void setOptimizationProfile(OptimizationProfile var1) {
      this.optimizationProfile = var1;
   }

   public OptimizationProfile getOptimizationProfile() {
      return this.optimizationProfile;
   }

   public String getBestWFResultKey() {
      try {
         WalkForwardMatrixResult var1 = (WalkForwardMatrixResult)this.mainResult().get("WalkForwardResult");
         if (var1 != null && this.bestWFResultKey == null) {
            this.bestWFResultKey = var1.computeBestWF(this);
         }
      } catch (Exception var2) {
         Log.error("Error while computing best WF result. Exc.", var2);
      }

      return this.bestWFResultKey;
   }

   public OutOfSample getOOS() {
      return this.oosSettings;
   }

   public void setOOSSettings(OutOfSample var1) {
      this.oosSettings = var1;
   }

   public List<ResultsGroup> getMergedResults() {
      return this.mergedResults;
   }

   public void setMergedResults(List<ResultsGroup> var1) {
      if (this.mergedResults == null) {
         this.mergedResults = new ArrayList<>();
      } else {
         this.mergedResults.clear();
      }

      for (int var2 = 0; var2 < var1.size(); var2++) {
         ResultsGroup var3 = ((ResultsGroup)var1.get(var2)).clone();
         this.mergedResults.add(var3);
      }

      this.specialValues.set(SpecialValues.IsMergedPortfolio, 1);
   }

   public boolean isStockpickerStrategy() {
      return this.specialValues().getBoolean(SpecialValues.StockpickerStrategy, false);
   }

   public void setNote(String var1) {
      this.specialValues().setString(SpecialValues.Note, var1);
      this.updated = true;
   }

   public String getNote() {
      return this.specialValues().getString(SpecialValues.Note, null);
   }

   public String getNote(String var1) {
      String var2 = this.specialValues().getString(SpecialValues.Note, null);
      return var2 == null ? var1 : var2;
   }

   public boolean isHodlStrategy() {
      String var1 = this.specialValues().getString(SpecialValues.PortfolioComposerHodlSymbol);
      return var1 != null;
   }

   public void recalculateDailyEquity() throws Exception {
      for (String var2 : this.getResultKeys()) {
         Result var3 = this.subResult(var2);
         if (!var3.isSpecial()) {
            this.recalculateSubresultDailyEquity(var3);
         }
      }
   }

   private void recalculateSubresultDailyEquity(Result var1) throws Exception {
      Long2FloatRBTreeMap var2 = var1.getWorstDailyEquity();
      if (var2 != null) {
         var2.clear();
         OrdersList var3 = this.orders().filter(var1.getResultKey(), (byte)0, (byte)127);
         var3.sort(new OrderComparatorByCloseTime());
         float var4 = 0.0F;
         float var5 = 0.0F;
         float var6 = var5;
         long var7 = 0L;
         long var9 = 0L;

         for (int var11 = 0; var11 < var3.size(); var11++) {
            Order var12 = var3.get(var11);
            LocalDateTime var13 = LocalDateTime.ofInstant(Instant.ofEpochMilli(var12.CloseTime), ZoneOffset.UTC);
            LocalDateTime var14 = var13.toLocalDate().atStartOfDay().plusDays(1L);
            var7 = var14.toInstant(ZoneOffset.UTC).toEpochMilli();
            if (var11 > 0) {
               LocalDateTime var15 = LocalDateTime.ofInstant(Instant.ofEpochMilli(var9), ZoneOffset.UTC);

               while (var15.isBefore(var14)) {
                  var15 = var15.plusDays(1L);
                  long var16 = var15.toInstant(ZoneOffset.UTC).toEpochMilli();
                  var2.put(var16, var6);
               }
            }

            var5 += var12.PL;
            var4 = var5 - var12.MAE;
            if (!var2.containsKey(var7) || var4 < var2.get(var7)) {
               var2.put(var7, var4);
            }

            var6 = var4;
            var9 = var7;
         }
      }
   }
}
