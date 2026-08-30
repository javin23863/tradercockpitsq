package com.strategyquant.tradinglib.databank;

import com.strategyquant.lib.L;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.memory.MemoryUsageChecker;
import com.strategyquant.tradinglib.CustomCellFormat;
import com.strategyquant.tradinglib.Databank;
import com.strategyquant.tradinglib.DatabankColumn;
import com.strategyquant.tradinglib.ResultsGroup;
import com.strategyquant.tradinglib.WalkForwardResult;
import com.strategyquant.tradinglib.gp.FitnessCollectionData;
import com.strategyquant.tradinglib.miniequitychart.MiniEquityChartComputer;
import com.strategyquant.tradinglib.optimization.OptimizationProfile;
import com.strategyquant.tradinglib.optimization.WalkForwardMatrixResult;
import com.strategyquant.tradinglib.project.SQProject;
import com.strategyquant.tradinglib.project.websocket.ProjectFeeds;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.locks.StampedLock;
import org.json.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DatabankRecords {
   public static final int MaxColumns = 500;
   public static final Logger Log = LoggerFactory.getLogger(DatabankRecords.class);
   private static final int CHANGES_BUFFER_SIZE_INCREMENT = 20;
   private static final int DATABANK_FIRSTLOAD_COUNT = 1000;
   private Databank databank;
   private HashMap<String, Integer> indexes = new HashMap<>();
   private ArrayList<ResultsGroup> records = new ArrayList<>();
   private boolean containsRefreshChange = false;
   private StampedLock recordsLock = new StampedLock();
   private StampedLock changesLock = new StampedLock();
   private FitnessCollectionData databankFitness = new FitnessCollectionData(0);
   private TopStrategiesData topStrategiesData = new TopStrategiesData();
   private DatabankChange[] changes = new DatabankChange[100];
   private Object[] rowDataTemp = new Object[500];
   private int changesLastIndex = -1;
   private long lastChangeTime = 0L;

   public DatabankRecords(Databank var1) {
      this.databank = var1;
      this.databankFitness.reset();
      this.topStrategiesData.reset();

      for (int var2 = 0; var2 < this.changes.length; var2++) {
         this.changes[var2] = new DatabankChange();
      }

      this.changesLastIndex = -1;
   }

   public boolean add(ResultsGroup var1, boolean var2, boolean var3) throws Exception {
      if (this.databank.isLoading() || SQProject.isMemoryProtectionUsed()) {
         MemoryUsageChecker.checkAvailableMemory();
      }

      IDatabankFilter var4 = this.databank.getFilter();
      return !this.databank.isLoading() && var4 != null ? this.checkAndAddRecord(var4, var1, var2, var3) : this.addRecord(var1, var2, var3);
   }

   private boolean checkAndAddRecord(IDatabankFilter var1, ResultsGroup var2, boolean var3, boolean var4) {
      boolean var5 = var1.checkShouldAdd(var2);
      if (!var5) {
         return false;
      }

      long var6 = this.recordsLock.writeLock();
      boolean var9 = false;

      String var8;
      try {
         boolean var10 = var1.onAdd(var2);
         if (!var10) {
            return false;
         }

         var8 = this.findUniqueName(var2);
         var2.setDatabank(this.databank);
         var2.setName(var8);
         this.records.add(var2);
         this.indexes.put(var8, this.records.size() - 1);
         if (!this.databank.isLoading()) {
            var2.trySaveToFile();
         }

         var9 = true;
      } finally {
         this.recordsLock.unlock(var6);
      }

      if (var9) {
         this.computeMiniEquityCharts(var2);
         boolean var16 = var2.specialValues().containsKey("DontComputeOP");
         if (!var16) {
            OptimizationProfile var11 = var2.getOptimizationProfile();
            if (var11 != null) {
               var11.compute(var4, true);
            }
         }

         if (this.contains(var8)) {
            this.addChange("add", var8, var2, var3);
         }

         this.databank.notifyListeners("added");
         this.databank.refreshGrid();
      }

      return var9;
   }

   private boolean addRecord(ResultsGroup var1, boolean var2, boolean var3) {
      long var4 = this.recordsLock.writeLock();
      boolean var7 = false;

      String var6;
      try {
         var6 = this.findUniqueName(var1);
         var1.setDatabank(this.databank);
         var1.setName(var6);
         this.records.add(var1);
         this.indexes.put(var6, this.records.size() - 1);
         if (!this.databank.isLoading()) {
            var1.trySaveToFile();
         }

         var7 = true;
      } finally {
         this.recordsLock.unlock(var4);
      }

      if (var7) {
         this.computeMiniEquityCharts(var1);
         boolean var8 = var1.specialValues().containsKey("DontComputeOP");
         if (!var8) {
            OptimizationProfile var9 = var1.getOptimizationProfile();
            if (var9 != null) {
               var9.compute(var3, true);
            }
         }

         if (this.contains(var6)) {
            this.addChange("add", var6, var1, var2);
         }

         this.databank.notifyListeners("added");
         this.databank.refreshGrid();
      }

      return var7;
   }

   private void computeMiniEquityCharts(ResultsGroup var1) {
      try {
         List var2 = var1.getResultKeys();
         String var3 = var1.getMainResultKey();

         for (int var4 = 0; var4 < var2.size(); var4++) {
            String var5 = (String)var2.get(var4);
            if (var5.equals(var3) || var5.equals("Portfolio") || var5.contains("Walk-Forward")) {
               MiniEquityChartComputer.computeForResult(var1, var5);
            }
         }
      } catch (Exception var6) {
         Log.error("Cannot compute mini equity chart values for strategy '" + var1.getName() + "'", var6);
      }
   }

   private String findUniqueName(ResultsGroup var1) {
      String var2 = var1.getName();
      if (!this.indexes.containsKey(var2)) {
         return var2;
      }

      int var4 = 1;

      while (true) {
         StringBuilder var5 = new StringBuilder(var2);
         var5.append("(");
         var5.append(var4);
         var5.append(")");
         String var3 = var5.toString();
         if (!this.indexes.containsKey(var3)) {
            return var3;
         }

         var4++;
      }
   }

   public void remove(String var1, boolean var2, boolean var3, boolean var4, boolean var5, String var6) throws Exception {
      this.remove(var1, var2, var3, true, var4, var5, var6);
   }

   public void remove(String var1, boolean var2, boolean var3, boolean var4, boolean var5, boolean var6, String var7) throws Exception {
      long var8 = this.recordsLock.writeLock();

      try {
         this.removeNoLock(var1, var2, var5, var6, var7);
         IDatabankFilter var10 = this.databank.getFilter();
         if (var10 != null) {
            var10.onRemove(var1);
         }
      } finally {
         this.recordsLock.unlock(var8);
      }

      if (var3) {
         this.databank.notifyListeners("removed");
      }
   }

   public void removeNoLock(String var1, boolean var2, boolean var3, boolean var4, String var5) throws Exception {
      this.removeNoLock(var1, var2, true, var3, var4, var5);
   }

   public void removeNoLock(String var1, boolean var2, boolean var3, boolean var4, boolean var5, String var6) throws Exception {
      if (this.databank.isLoading()) {
         throw new Exception(L.t("Cannot remove results while loading", new Object[0]));
      }

      if (this.indexes.containsKey(var1)) {
         if (Log.isDebugEnabled()) {
            Log.debug(String.format("Report with the name '%s' was removed from databank '%s'.", var1, this.databank.getName()));
         }

         int var7 = this.indexes.remove(var1);
         ResultsGroup var8 = this.records.remove(var7);
         if (var3) {
            this.addChange("remove", var1, null, var4);
         }

         for (int var9 = var7; var9 < this.records.size(); var9++) {
            String var10 = this.records.get(var9).getName();
            this.indexes.put(var10, var9);
         }

         if (var2) {
            var8.destroy(var5);
         }

         if (this.databank.isFileBased()) {
            var8.lockRecord(var6);

            try {
               String var14 = this.databank.getResultsFilePath(var8);
               File var15 = new File(var14);
               if (var15.exists()) {
                  if (!var15.delete()) {
                     Log.error("Unable to delete file " + var14);
                     throw new Exception(L.t("Unable to delete %s.sqx file", new Object[]{var8.getName()}));
                  }

                  if (Log.isDebugEnabled()) {
                     Log.debug("File " + var14 + " deleted.");
                  }
               }
            } finally {
               var8.releaseLock(var6);
            }
         }
      }
   }

   public void update(String var1, ResultsGroup var2, boolean var3, String var4) throws Exception {
      ResultsGroup var5 = null;

      try {
         var5 = this.databank.getLocked(var1, var4);
         if (var5 != var2) {
            var5.updateResults(var2, true);
            boolean var6 = var2.specialValues().containsKey("DontComputeOP");
            if (!var6) {
               OptimizationProfile var7 = var5.getOptimizationProfile();
               if (var7 != null) {
                  var7.compute(true, true);
               }
            }
         }
      } finally {
         if (var5 != null) {
            var5.releaseLock(var4);
         }
      }

      long var16 = this.recordsLock.writeLock();

      try {
         var2.trySaveToFile();
         this.sendUpdateNoLock(var2, var3);
      } finally {
         this.recordsLock.unlock(var16);
      }

      this.databank.notifyListeners("updated");
   }

   public void updateAllResults() throws Exception {
      long var1 = this.recordsLock.writeLock();

      try {
         for (int var3 = 0; var3 < this.records.size(); var3++) {
            ResultsGroup var4 = this.records.get(var3);
            this.addChange("update", var4.getName(), var4, true);
         }
      } finally {
         this.recordsLock.unlock(var1);
      }

      this.updateBestResults();
      if (this.databank.getFilter() != null) {
         this.databank.getFilter().init(this.databank);
      }

      this.databank.notifyListeners("updated");
   }

   public void clearRecords(boolean var1) {
      long var2 = this.recordsLock.writeLock();

      try {
         this.topStrategiesData.reset();
         this.databankFitness.reset();

         for (int var4 = 0; var4 < this.records.size(); var4++) {
            ResultsGroup var5 = this.records.get(var4);
            if (var1) {
               var5.clear();
            }

            if (this.databank.getFilter() != null) {
               this.databank.getFilter().onRemove(var5.getName());
            }
         }

         this.records.clear();
         this.indexes.clear();
      } finally {
         this.recordsLock.unlock(var2);
      }

      this.databank.notifyListeners("cleared");
      this.addChange("clearAll", null, null, true);
   }

   public ResultsGroup get(String var1, String var2) throws Exception {
      ResultsGroup var3 = null;
      long var4 = this.recordsLock.tryOptimisticRead();
      if (this.recordsLock.validate(var4)) {
         if (this.indexes.containsKey(var1)) {
            var3 = this.records.get(this.indexes.get(var1));
         }
      } else {
         var4 = this.recordsLock.readLock();

         try {
            if (this.indexes.containsKey(var1)) {
               var3 = this.records.get(this.indexes.get(var1));
            }
         } finally {
            this.recordsLock.unlockRead(var4);
         }
      }

      if (var3 == null) {
         throw new RecordNotFoundException(
            "RecordNotFoundException - " + L.t("Report with the name '%s' was not found in databank '%s'(2).", new Object[]{var1, this.databank.getName()})
         );
      } else {
         return var3.lockRecord(var2);
      }
   }

   public ArrayList<ResultsGroup> getRecords() {
      long var1 = this.recordsLock.readLock();

      try {
         return this.records;
      } finally {
         this.recordsLock.unlock(var1);
      }
   }

   public int getRecordsSizeNoLock() {
      return this.records.size();
   }

   public ArrayList<String> getRecordKeys() {
      long var1 = this.recordsLock.readLock();

      try {
         return new ArrayList<>(this.indexes.keySet());
      } finally {
         this.recordsLock.unlock(var1);
      }
   }

   public ArrayList<String> getRecordKeys(String var1) {
      long var2 = this.recordsLock.readLock();

      try {
         ArrayList var4 = new ArrayList();

         for (String var6 : this.indexes.keySet()) {
            if (var6.startsWith(var1) && var6.contains(" - Optimiz")) {
               var4.add(var6);
            }
         }

         return var4;
      } finally {
         this.recordsLock.unlock(var2);
      }
   }

   public ArrayList<String> getRecordKeysNoLock() {
      return new ArrayList<>(this.indexes.keySet());
   }

   public ArrayList<String> getRecordKeysNoLock(String var1) {
      ArrayList var2 = new ArrayList();

      for (String var4 : this.indexes.keySet()) {
         if (var4.startsWith(var1) && var4.contains(" - Optimiz")) {
            var2.add(var4);
         }
      }

      return var2;
   }

   public int size() {
      long var1 = this.recordsLock.tryOptimisticRead();
      int var3 = this.records.size();
      if (!this.recordsLock.validate(var1)) {
         var1 = this.recordsLock.readLock();

         try {
            var3 = this.records.size();
         } finally {
            this.recordsLock.unlockRead(var1);
         }
      }

      return var3;
   }

   public ArrayList<Object[]> getBufferData(boolean var1) {
      ArrayList var2 = new ArrayList();
      long var3 = this.recordsLock.readLock();

      try {
         for (int var5 = 0; var5 < this.records.size(); var5++) {
            ResultsGroup var6 = this.records.get(var5);
            var2.add(this.getRecordData(var6, true));
            if (var1 && var5 >= 999) {
               break;
            }
         }
      } finally {
         this.recordsLock.unlockRead(var3);
      }

      return var2;
   }

   private synchronized Object[] getRecordData(ResultsGroup var1, boolean var2) {
      DatabankTableView var3 = this.databank.getView();
      int var4 = var3.columns.size();
      int var5 = 1;
      boolean var6 = !this.databank.getProject().equals("Builder") && !this.databank.getProject().equals("PortfolioMaster");
      boolean var7 = this.databank.getProject().equals("Optimizer");
      if (var6) {
         var5++;
      }

      if (var7) {
         var5 += 2;
      }

      int var8 = var4 + var5 + 1;
      if (var8 > this.rowDataTemp.length) {
         this.rowDataTemp = new Object[var8];
      } else {
         for (int var9 = var8; var9 < this.rowDataTemp.length; var9++) {
            this.rowDataTemp[var9] = null;
         }
      }

      this.rowDataTemp[0] = var1.getName();
      if (var6) {
         try {
            DatabankColumn var19 = (DatabankColumn)DatabankColumns.get().findClassByName("FiltersResult");
            String var10 = var19.getValue(var1, null, (byte)0, (byte)10, (byte)127);
            if (!var10.equals("N/A")) {
               CustomCellFormat var11 = var19.getCustomFormat((byte)0, (byte)10, (byte)127, var10, var1);
               var10 = var11.getHtmlContent(var10);
            }

            this.rowDataTemp[1] = var10;
         } catch (Exception var18) {
            this.rowDataTemp[1] = "N/A";
         }
      }

      if (var7) {
         String var20 = null;
         String var23 = null;

         try {
            WalkForwardMatrixResult var26 = (WalkForwardMatrixResult)var1.mainResult().get("WalkForwardResult");
            WalkForwardResult var12 = var26.getWFResult(var1.getBestWFResultKey(), true);
            var20 = var12.testParams;
            var23 = var12.toString(var26.periodType);
         } catch (Exception var17) {
         }

         try {
            var20 = var1.getParams();
         } catch (Exception var16) {
         }

         this.rowDataTemp[var6 ? 2 : 1] = var20 != null ? var20 : "N/A";
         this.rowDataTemp[var6 ? 3 : 2] = var23 != null ? var23 : "N/A";
      }

      for (int var21 = 0; var21 < var3.columns.size(); var21++) {
         DatabankTableColumnEntry var24 = var3.columns.get(var21);
         String var27 = var24.getValue(var1);

         try {
            if (!var27.equals("N/A")) {
               if (var24.tableColumn.getType().equals("Integer")) {
                  try {
                     double var28 = Double.parseDouble(var27);
                     if (!Double.isNaN(var28)) {
                        var27 = Integer.toString((int)var28);
                     }
                  } catch (Exception var14) {
                  }
               }

               CustomCellFormat var29 = var24.tableColumn.getCustomFormat(var24.direction, var24.plType, var24.sampleType, var27, var1);
               var27 = var29.getHtmlContent(var27);
            }

            this.rowDataTemp[var21 + var5] = var27;
         } catch (Exception var15) {
            this.rowDataTemp[var21 + var5] = "N/A";
         }

         this.rowDataTemp[var4 + var5] = var1.specialValues().getInt("StrategyProblems", 0);
      }

      if (!var2) {
         return this.rowDataTemp;
      }

      Object[] var22 = new Object[var8];

      for (int var25 = 0; var25 < var8; var25++) {
         var22[var25] = this.rowDataTemp[var25];
      }

      return var22;
   }

   private void addChange(String var1, String var2, ResultsGroup var3, boolean var4) {
      this.lastChangeTime = System.currentTimeMillis();
      if (ProjectFeeds.isSubscribed(this.databank.getProject(), "databanks-channel")) {
         long var5 = this.changesLock.writeLock();

         try {
            Object[] var7 = var3 != null ? this.getRecordData(var3, false) : null;
            String var8 = this.records.size() == 0 ? "DatabankEmpty" : (var4 ? "refreshGrid" : null);
            this.setNextChange(var1, var2, var7, var8);
         } finally {
            this.changesLock.unlock(var5);
         }
      }
   }

   public void addExtraChange(String var1, String var2) {
      if (var2 != null) {
         if (ProjectFeeds.isSubscribed(this.databank.getProject(), "databanks-channel")) {
            long var3 = this.changesLock.writeLock();

            try {
               boolean var5 = var2.equals("refreshGrid");
               if (!var5 || !this.containsRefreshChange) {
                  this.setNextChange(var1, null, null, var2);
               }
            } finally {
               this.changesLock.unlock(var3);
            }
         }
      }
   }

   private void setNextChange(String var1, String var2, Object[] var3, String var4) {
      if (this.changesLastIndex >= this.changes.length - 1) {
         Log.debug("Maximum changes capacity reached");
         int var5 = this.changes.length;
         int var6 = var5 + 20;
         this.changes = Arrays.copyOf(this.changes, var6);

         for (int var7 = var5; var7 < var6; var7++) {
            this.changes[var7] = new DatabankChange();
         }
      }

      this.changesLastIndex++;
      DatabankChange var8 = this.changes[this.changesLastIndex];
      var8.update(var1, var2, var3);
      var8.setExtraValue(var4);
      this.containsRefreshChange = this.containsRefreshChange || var4 != null && var4.equals("refreshGrid");
   }

   public JSONArray getChanges() {
      long var1 = this.changesLock.writeLock();

      try {
         if (this.changesLastIndex < 0) {
            return null;
         }

         JSONArray var3 = new JSONArray();
         boolean var4 = false;
         String var5 = null;

         for (int var6 = 0; var6 <= this.changesLastIndex; var6++) {
            DatabankChange var7 = this.changes[var6];
            if (var7.type == "remove" && var4 && var7.id != null && var5 != null && var7.id.equals(var5)) {
               var3.remove(var3.length() - 1);
               var4 = false;
            } else {
               if (var7.type == "add") {
                  var4 = true;
                  var5 = var7.id;
               } else {
                  var4 = false;
               }

               var3.put(var7.toJSON());
            }
         }

         this.changesLastIndex = -1;
         this.containsRefreshChange = false;
         return var3;
      } finally {
         this.changesLock.unlock(var1);
      }
   }

   public void sendUpdate(ResultsGroup var1, boolean var2) {
      long var3 = this.recordsLock.readLock();

      try {
         this.sendUpdateNoLock(var1, var2);
      } finally {
         this.recordsLock.unlock(var3);
      }
   }

   private void sendUpdateNoLock(ResultsGroup var1, boolean var2) {
      String var3 = var1.getName();
      this.addChange("update", var3, var1, var2);
      if (this.databank.getFilter() != null) {
         this.databank.getFilter().onUpdate(var1);
      }
   }

   public boolean contains(String var1) {
      return this.indexes.containsKey(var1);
   }

   public ResultsGroup[] getTopResults() {
      long var1 = this.recordsLock.readLock();

      try {
         return this.topStrategiesData.getTopResults();
      } finally {
         this.recordsLock.unlock(var1);
      }
   }

   public FitnessCollectionData getFitnessData() {
      return this.databankFitness;
   }

   public void updateBestResults(ResultsGroup var1) {
      this.topStrategiesData.updateWith(var1);
      this.topStrategiesData.updateDatabankFitnesses(this.databankFitness);
   }

   private String fitnessesToString(ResultsGroup var1) {
      StringBuilder var2 = new StringBuilder();
      var2.append("BST IS:");
      var2.append(this.getFitness(var1, (byte)10));
      var2.append(", ISV:");
      var2.append(this.getFitness(var1, (byte)40));
      var2.append(", IST:");
      var2.append(this.getFitness(var1, (byte)11));
      var2.append(", OOS:");
      var2.append(this.getFitness(var1, (byte)20));
      var2.append(", FS:");
      var2.append(this.getFitness(var1, (byte)127));
      var2.append("\n");
      var2.append("-----------------------------------");
      return var2.toString();
   }

   private double getFitness(ResultsGroup var1, byte var2) {
      try {
         double var3 = SQUtils.round2(var1.portfolio().getFitness(var2));
         return var3 > 0.0 ? var3 : 0.0;
      } catch (Exception var5) {
         return 0.0;
      }
   }

   public void updateBestResults() {
      long var1 = this.recordsLock.readLock();

      try {
         this.topStrategiesData.reset();
         this.databankFitness.reset();

         for (int var3 = 0; var3 < this.records.size(); var3++) {
            ResultsGroup var4 = this.records.get(var3);
            this.topStrategiesData.updateWith(var4);
         }

         this.topStrategiesData.updateDatabankFitnesses(this.databankFitness);
      } finally {
         this.recordsLock.unlock(var1);
      }
   }

   public long getLastChangeTime() {
      return this.lastChangeTime;
   }

   public void clearResultsData() {
      this.clearResultsData(null);
   }

   public void clearResultsData(ArrayList<String> var1) {
      ArrayList var2 = null;
      if (var1 != null) {
         var2 = new ArrayList(var1);
      }

      long var3 = this.recordsLock.readLock();

      try {
         this.topStrategiesData.reset();
         this.databankFitness.reset();

         for (int var5 = 0; var5 < this.records.size(); var5++) {
            ResultsGroup var6 = this.records.get(var5);
            String var7 = var6.getName();
            if (var2 == null || var2.contains(var7)) {
               var6.clear();
               if (var2 != null) {
                  var2.remove(var7);
               }

               this.addChange("reset", var7, null, true);
            }
         }
      } finally {
         this.recordsLock.unlock(var3);
      }

      if (var2 == null) {
         this.addChange("reset", null, null, true);
      }
   }
}
