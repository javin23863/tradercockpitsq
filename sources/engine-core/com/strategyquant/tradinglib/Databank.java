package com.strategyquant.tradinglib;

import com.strategyquant.lib.L;
import com.strategyquant.lib.app.MainApp;
import com.strategyquant.lib.constants.SQPaths;
import com.strategyquant.pluginlib.program.IProgram;
import com.strategyquant.pluginlib.program.Program;
import com.strategyquant.tradinglib.databank.DatabankRecords;
import com.strategyquant.tradinglib.databank.DatabankTableView;
import com.strategyquant.tradinglib.databank.DatabankViewsManager;
import com.strategyquant.tradinglib.databank.IDatabankFilter;
import com.strategyquant.tradinglib.databank.IDatabankListener;
import com.strategyquant.tradinglib.databank.IProgressListener;
import com.strategyquant.tradinglib.databank.RecordNotFoundException;
import com.strategyquant.tradinglib.gp.FitnessCollectionData;
import com.strategyquant.tradinglib.project.ProjectEngine;
import com.strategyquant.tradinglib.project.StrategiesSaver;
import com.strategyquant.tradinglib.project.StrategySaver;
import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.concurrent.locks.StampedLock;
import org.jdom2.Element;
import org.json.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Databank implements Serializable {
   public static final Logger Log = LoggerFactory.getLogger(Databank.class);
   public static final String ACTION_RESULT_ADDED = "added";
   public static final String ACTION_RESULT_REMOVED = "removed";
   public static final String ACTION_CLEARED = "cleared";
   public static final String ACTION_UPDATED = "updated";
   public static final String DefaultMainViewName = "Default - Main data";
   public static final String DefaultPortfolioViewName = "Default - Portfolio";
   public static final int TIMER_TICKS_FREQUENCY = 5000;
   private String name;
   private String projectName;
   private String databankFolder;
   private int position;
   private boolean loading = false;
   private boolean loaded = false;
   private double percentLoaded = 0.0;
   private DatabankTableView view;
   private DatabankRecords databankRecords = new DatabankRecords(this);
   private StrategySaver reportSaver = new StrategySaver();
   private ArrayList<IDatabankListener> listeners = new ArrayList<>();
   private IDatabankFilter filter;
   private String syncType;
   private boolean fileBased = false;
   private boolean syncAllowed = true;
   private Thread dbSyncThread = null;
   private StampedLock progressLock = new StampedLock();

   public Databank(String var1, String var2, String var3) {
      this.projectName = var1;
      this.name = var2;
      this.databankFolder = var3 + "/" + this.name;

      try {
         this.view = DatabankViewsManager.getInstance().getView("Default - Main data");
      } catch (Exception var5) {
         Log.debug("Cannot find databank view 'Default - Main data'", var5);
         this.view = DatabankViewsManager.getInstance().createDefaultView("Default - Main data", "main");
      }
   }

   public String getProject() {
      return this.projectName;
   }

   public void addListener(IDatabankListener var1) {
      this.listeners.add(var1);
   }

   public void removeListener(IDatabankListener var1) {
      this.listeners.remove(var1);
   }

   public void loadStrategies(final IProgressListener var1) throws Exception {
      if (this.databankFolder == null) {
         this.loading = false;
         throw new Exception(L.t("Databank folder not set", new Object[0]));
      }

      if (this.loading) {
         throw new Exception(L.t("Loading is already in progress", new Object[0]));
      }

      this.loaded = false;
      final File var2 = new File(this.databankFolder);
      if (var2.exists()) {
         this.loading = true;
         Thread var3 = new Thread() {
            @Override
            public void run() {
               try {
                  File[] var1x = var2.listFiles();
                  int var2x = 0;
                  IProgram var3x = null;

                  try {
                     var3x = Program.get("Loader");
                  } catch (Exception var13) {
                     Databank.Log.error("Loader not initialized", var13);
                  }

                  for (int var4 = 0; var4 < var1x.length; var4++) {
                     File var5 = var1x[var4];

                     try {
                        ResultsGroup var6 = (ResultsGroup)var3x.call("loadFile", new Object[]{var5.getAbsolutePath()});
                        Databank.this.add(Databank.this, var6);
                     } catch (Exception var14) {
                        Databank.Log.error("Cannot load strategy from file '" + var5.getAbsolutePath() + "'. ", var14);
                        if (var1 != null) {
                           var1.onError(Databank.this.percentLoaded, "Cannot load strategy from file '" + var5.getAbsolutePath() + "'. " + var14.getMessage());
                        }
                     } catch (OutOfMemoryError var15) {
                        if (var1 != null) {
                           var1.onError(Databank.this.percentLoaded, "Cannot load strategy from file '" + var5.getAbsolutePath() + ". " + var15.getMessage());
                        }
                        break;
                     }

                     double var10001;
                     if (var1x.length != 0) {
                        var2x++;
                        var10001 = (double)var2x / var1x.length * 100.0;
                     } else {
                        var10001 = 100.0;
                     }

                     Databank.this.setPercentLoaded(var10001);
                     if (var1 != null) {
                        var1.onProgress(Databank.this.percentLoaded);
                     }
                  }

                  Databank.this.refreshGrid();
                  Databank.this.updateBestResults();
               } catch (Exception | Error var16) {
                  Databank.Log
                     .error(
                        "Databank '" + Databank.this.getProject() + "/" + Databank.this.getName() + "', error while loading strategies - " + var16.getMessage(),
                        var16
                     );
               } finally {
                  if (Databank.this.percentLoaded != 100.0) {
                     Databank.this.setPercentLoaded(100.0);
                     if (var1 != null) {
                        var1.onProgress(Databank.this.percentLoaded);
                     }
                  }

                  Databank.this.loading = false;
                  Databank.this.loaded = true;
                  if (var1 != null) {
                     var1.onDone();
                  }
               }
            }
         };
         var3.start();
         if (MainApp.runInConsoleWithoutActiveWebserver()) {
            var3.join();
         }
      } else {
         var2.mkdirs();
         if (var1 != null) {
            this.setPercentLoaded(100.0);
            var1.onProgress(this.percentLoaded);
            var1.onDone();
         }

         this.loading = false;
         this.loaded = true;
      }
   }

   private void setPercentLoaded(double var1) {
      long var3 = this.progressLock.writeLock();

      try {
         this.percentLoaded = var1;
      } finally {
         this.progressLock.unlock(var3);
      }
   }

   protected void add(Databank var1, ResultsGroup var2) throws Exception {
      var1.add(var2, false);
   }

   public void add(StrategyBase var1) throws Exception {
      ResultsGroup var2 = new ResultsGroup(var1.getStrategyName());
      var2.portfolio().addStrategy(var1);
      this.add(var2, true);
   }

   public boolean add(ResultsGroup var1, boolean var2) throws Exception {
      return this.add(var1, var2, true);
   }

   public boolean add(ResultsGroup var1, boolean var2, boolean var3) throws Exception {
      return this.databankRecords.add(var1, var2, var3);
   }

   public void remove(String var1, boolean var2, boolean var3, boolean var4, boolean var5, String var6) throws Exception {
      this.databankRecords.remove(var1, var2, var3, var4, var5, var6);
   }

   public void remove(String var1, boolean var2, boolean var3, boolean var4, boolean var5, boolean var6, String var7) throws Exception {
      this.databankRecords.remove(var1, var2, var3, var4, var5, var6, var7);
   }

   public void removeNoLock(String var1, boolean var2, boolean var3, boolean var4, String var5) throws Exception {
      this.databankRecords.removeNoLock(var1, var2, var3, var4, var5);
   }

   public void removeNoLockNoException(String var1, boolean var2, boolean var3, boolean var4, String var5) throws Exception {
      try {
         this.databankRecords.removeNoLock(var1, var2, var3, var4, var5);
      } catch (RecordNotFoundException var7) {
      }
   }

   public void update(String var1, ResultsGroup var2, boolean var3, String var4) throws Exception {
      this.databankRecords.update(var1, var2, var3, var4);
   }

   public void updateAllResults() throws Exception {
      this.databankRecords.updateAllResults();
   }

   public void updateBestResults() {
      this.databankRecords.updateBestResults();
   }

   public void updateBestResults(ResultsGroup var1) {
      this.databankRecords.updateBestResults(var1);
   }

   public ResultsGroup getLocked(String var1, String var2) throws Exception {
      return this.databankRecords.get(var1, var2);
   }

   public boolean contains(String var1) {
      return this.databankRecords.contains(var1);
   }

   public ArrayList<ResultsGroup> getRecords() {
      return this.databankRecords.getRecords();
   }

   public int getRecordsSizeNoLock() {
      return this.databankRecords.getRecordsSizeNoLock();
   }

   public ArrayList<String> getRecordKeys() {
      return this.databankRecords.getRecordKeys();
   }

   public ArrayList<String> getRecordKeysNoLock() {
      return this.databankRecords.getRecordKeysNoLock();
   }

   public ArrayList<String> getRecordKeysNoLock(String var1) {
      return this.databankRecords.getRecordKeysNoLock(var1);
   }

   public ArrayList<String> getRecordKeys(String var1) {
      return this.databankRecords.getRecordKeys(var1);
   }

   public void onResultChange(ResultsGroup var1) throws Exception {
      this.databankRecords.sendUpdate(var1, true);
   }

   public String getResultsFilePath(ResultsGroup var1) {
      return this.databankFolder + "/" + var1.getName() + ".sqx";
   }

   public void deleteResultsFile(ResultsGroup var1) throws Exception {
      String var2 = this.getResultsFilePath(var1);
      File var3 = new File(var2);
      if (var3.exists()) {
         if (!var3.delete()) {
            throw new Exception(L.t("File '%s' wasn't removed.", new Object[]{var3.getName()}));
         }
      } else {
         throw new Exception(L.t("File '%s' doesn't exist.", new Object[]{var3.getName()}));
      }
   }

   public Element getConfig() {
      Element var1 = new Element("Databank");
      var1.setAttribute("name", this.name);
      var1.setAttribute("view", this.view.name.equals("?") ? "Default - Main data" : this.view.name);
      var1.setAttribute("syncType", this.syncType != null ? this.syncType : getDefaultSyncType());
      var1.setAttribute("position", String.valueOf(this.position));
      return var1;
   }

   public static Element getDefaultConfig() {
      Element var0 = new Element("Databank");
      var0.setAttribute("name", "Results");
      var0.setAttribute("view", "Default - Main data");
      var0.setAttribute("syncType", "Auto-sync never");
      var0.setAttribute("position", "10");
      return var0;
   }

   public void setView(DatabankTableView var1) {
      this.view = var1;
   }

   public DatabankTableView getView() {
      return this.view;
   }

   public ArrayList<Object[]> getBufferData(boolean var1) {
      return this.databankRecords.getBufferData(var1);
   }

   public void removeAll(boolean var1, String var2) throws Exception {
      this.clearRecords(var1, true, var2);
      if (this.isFileBased()) {
         File var3 = new File(this.databankFolder);
         File[] var4 = var3.listFiles();
         boolean var5 = true;

         for (int var6 = 0; var6 < var4.length; var6++) {
            File var7 = var4[var6];
            if (var7.delete()) {
               Log.info("File " + var7.getName() + " deleted.");
            } else {
               Log.error("Unable to delete file " + var7.getName());
               var5 = false;
            }
         }

         if (!var5) {
            throw new Exception(L.t("Some databank files were not deleted.", new Object[0]));
         }
      }
   }

   public void clearRecords(boolean var1, boolean var2, String var3) throws Exception {
      this.databankRecords.clearRecords(var1);
      if (var2) {
         this.synchronizeNow(var3);
      }
   }

   public boolean setViewByName(String var1) {
      try {
         if (var1.equals("Default")) {
            var1 = "Default - Main data";
         }

         DatabankTableView var2 = DatabankViewsManager.getInstance().getView(var1);
         this.view = var2;
         return true;
      } catch (Exception var5) {
         Log.debug("Cannot find databank view '" + var1 + "'");

         try {
            DatabankViewsManager.getInstance().getView("Default - Main data");
         } catch (Exception var4) {
            Log.error("Cannot get default databank view");
         }

         return false;
      }
   }

   public boolean noActionRunning() {
      return this.dbSyncThread == null || !this.dbSyncThread.isAlive();
   }

   public String getName() {
      return this.name;
   }

   public boolean isLoading() {
      return this.loading;
   }

   public boolean isLoaded() {
      return this.loaded;
   }

   public int size() {
      return this.databankRecords.size();
   }

   public double getPercentLoaded() {
      long var1 = this.progressLock.readLock();

      try {
         return this.percentLoaded;
      } finally {
         this.progressLock.unlock(var1);
      }
   }

   public void notifyListeners(String var1) {
      for (int var2 = 0; var2 < this.listeners.size(); var2++) {
         IDatabankListener var3 = this.listeners.get(var2);
         var3.databankChanged(this);
      }
   }

   public void applyFilter(IDatabankFilter var1) throws Exception {
      this.filter = var1;
      var1.init(this);
   }

   public void applyFilter(IDatabankFilter var1, IProgressListener var2) throws Exception {
      this.filter = var1;
      var1.init(this, var2);
   }

   public void removeFilter() {
      if (this.filter != null) {
         this.filter.destroy();
         this.filter = null;
      }
   }

   public IDatabankFilter getFilter() {
      return this.filter;
   }

   public double getWorstFitness() {
      return this.filter != null ? this.filter.getWorstFitness() : 0.0;
   }

   public JSONArray getChanges() {
      return this.databankRecords.getChanges();
   }

   public StrategySaver getReportSaver() {
      return this.reportSaver;
   }

   public String getDatabankFolder() {
      return this.databankFolder;
   }

   public void addExtraDatabankChange(String var1, String var2) {
      this.databankRecords.addExtraChange(var1, var2);
   }

   public ResultsGroup[] getTopResults() {
      return this.databankRecords.getTopResults();
   }

   public FitnessCollectionData getFitnessData() {
      return this.databankRecords.getFitnessData();
   }

   public boolean isFileBased() {
      return this.fileBased;
   }

   public String getSyncType() {
      return this.syncType != null ? this.syncType : getDefaultSyncType();
   }

   public void setSyncType(String var1) {
      if (this.name.equals("Last generation")) {
         this.syncType = "Auto-sync never";
         this.fileBased = false;
      } else {
         this.syncType = var1 != null ? var1 : getDefaultSyncType();
         this.fileBased = this.syncType.equals("Immediately (not recommended, very slow)");

         try {
            final int var2 = DatabankSyncTypes.getTimeout(this.syncType);
            if (this.dbSyncThread != null && this.dbSyncThread.isAlive()) {
               this.dbSyncThread.interrupt();
            }

            if (this.fileBased) {
               this.synchronize(false, String.format("Periodical sync of databank %s/%s - %s", this.projectName, this.name, this.syncType));
               return;
            }

            if (var2 > 0 && !this.isFileBased()) {
               this.dbSyncThread = new Thread() {
                  @Override
                  public void run() {
                     while (true) {
                        try {
                           Thread.sleep(var2);
                        } catch (InterruptedException var2x) {
                           break;
                        }

                        if (MainApp.AppLoaded) {
                           if (!MainApp.isExiting()) {
                              if (!Databank.this.loading && Databank.this.syncAllowed) {
                                 Databank.this.synchronize(
                                    false,
                                    String.format(
                                       "Periodical sync of databank %s/%s - %s", Databank.this.projectName, Databank.this.name, Databank.this.syncType
                                    )
                                 );
                              }
                              continue;
                           }
                           break;
                        }
                     }
                  }
               };
               this.dbSyncThread.start();
            }
         } catch (Exception var3) {
            Log.error("Initializing databank synchronization failed", var3);
         }
      }
   }

   public void synchronizeNow(String var1) {
      this.synchronizeNow(false, var1);
   }

   public void synchronizeNow(boolean var1, final String var2) {
      if (!this.isFileBased()) {
         if (this.dbSyncThread != null && this.dbSyncThread.isAlive()) {
            this.dbSyncThread.interrupt();
         }

         if (MainApp.runInConsoleWithoutActiveWebserver()) {
            this.synchronize(false, var2);
            this.setSyncType(this.syncType);
         } else {
            Thread var3 = new Thread() {
               @Override
               public void run() {
                  Databank.this.synchronize(true, var2);
                  Databank.this.setSyncType(Databank.this.syncType);
                  Databank.Log.debug("Synchronization finished2.");
               }
            };
            var3.start();
            if (var1) {
               try {
                  var3.join();
               } catch (Exception var5) {
                  Log.error("Error while joining databank synchronization thread", var5);
               }
            }
         }
      }
   }

   private void synchronize(boolean var1, String var2) {
      if (!ProjectEngine.loading) {
         StrategiesSaver.syncDatabank(this, var1, var2);
      }
   }

   public static String getDefaultSyncType() {
      return MainApp.settings().get("databankSyncType", "Auto-sync every 1 hour");
   }

   public void refreshGrid() {
      this.addExtraDatabankChange("remove", "refreshGrid");
   }

   public void setProjectName(String var1) {
      this.projectName = var1;
      this.databankFolder = SQPaths.projectsDirPath + "/" + var1 + "/databanks/" + this.name;
   }

   public int getPosition() {
      return this.position;
   }

   public void setPosition(int var1) {
      this.position = var1;
   }

   public boolean isDefault() {
      return isDefault(this.name);
   }

   public static boolean isDefault(String var0) {
      switch (var0) {
         case "Results":
         case "Strategies to improve":
         case "Initial population":
         case "Strategies to optimize":
         case "Last generation":
         case "Existing portfolio":
         case "Simple strategies":
            return true;
         default:
            return false;
      }
   }

   public void rename(String var1) {
      File var2 = new File(this.databankFolder);
      File var3 = var2.getParentFile();
      this.databankFolder = var3.getAbsolutePath() + "/" + var1;
      var2.renameTo(new File(this.databankFolder));
      this.name = var1;
   }

   public void destroyTimers() {
      if (this.dbSyncThread != null && this.dbSyncThread.isAlive()) {
         this.dbSyncThread.interrupt();
      }
   }

   public long getLastChangeTime() {
      return this.databankRecords.getLastChangeTime();
   }

   public void disableSynchronization() {
      this.syncAllowed = false;
   }

   public void enableSynchronization() {
      this.syncAllowed = true;
   }

   public void clearResultsData() {
      this.databankRecords.clearResultsData();
   }

   public void clearResultsData(ArrayList<String> var1) {
      this.databankRecords.clearResultsData(var1);
   }
}
