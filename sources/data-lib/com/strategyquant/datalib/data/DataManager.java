package com.strategyquant.datalib.data;

import com.strategyquant.datalib.ChartDef;
import com.strategyquant.datalib.DataInfo;
import com.strategyquant.datalib.InstrumentInfo;
import com.strategyquant.datalib.TickEvent;
import com.strategyquant.datalib.TimeframeManager;
import com.strategyquant.datalib.bartype.BarType;
import com.strategyquant.datalib.bartype.BarTypeFactory;
import com.strategyquant.datalib.bartype.BarTypeStatus;
import com.strategyquant.datalib.consts.Precisions;
import com.strategyquant.datalib.data.io.BinaryDataLoader;
import com.strategyquant.datalib.data.io.IDataLoader;
import com.strategyquant.datalib.data.io.VersatileData;
import com.strategyquant.datalib.data.io.newDataFormat.DataBinWriterNew;
import com.strategyquant.datalib.instrument.InstrumentManager;
import com.strategyquant.datalib.session.Session;
import com.strategyquant.datalib.session.SessionManager;
import com.strategyquant.datalib.session.SessionStatus;
import com.strategyquant.datalib.ticksimulator.DefaultTickSimulator;
import com.strategyquant.lib.HistoryDataNotAvailableExeption;
import com.strategyquant.lib.HistoryOHLCData;
import com.strategyquant.lib.IStopPauseStatus;
import com.strategyquant.lib.SQTime;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.TaskStoppedException;
import com.strategyquant.lib.app.MainApp;
import com.strategyquant.lib.constants.SQPaths;
import com.strategyquant.lib.historyData.HistoryDataChecker;
import com.strategyquant.lib.historyData.ICryptable;
import com.strategyquant.lib.hw.OperatingSystem;
import com.strategyquant.lib.time.SQTimeOld;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DataManager extends DataDb {
   public static final String SpreadTypePoints = "points";
   public static final String SpreadTypePips = "pips";
   public static final String SpreadTypeReal = "real";
   private static final int BATCH_SIZE = 150;
   public static final String FUTURES_FOLDER = "sq_futures";
   public static final String EQUITY_FOLDER = "sq_equity";
   public static final Logger Log = LoggerFactory.getLogger(DataManager.class);
   private static DataManager instance = null;
   private ArrayList<DataInfo> cachedDataList = new ArrayList<>();
   private HashMap<String, DataInfo> dataInfoCache = new HashMap<>();
   private HashMap<String, Boolean> connectionCache = new HashMap<>();
   private DataComparator dataComparator = new DataComparator();
   private DataInfoCache dataInfoCacheAll = new DataInfoCache();
   private long dataLastTimeUpdated = -1L;
   private List<SymbolInfo> updatedInfos = new LinkedList<>();
   private HashMap<String, DataInfo> customDataInfoCache = new HashMap<>();

   public static void init(String var0) throws Exception {
      if (instance != null) {
         throw new Exception("DataManager.init() called more than once!");
      }

      instance = new DataManager(var0);
      instance.moveDataFromBaseFolder("History");
      if (MainApp.getProduct() != "BACKTESTNODE") {
         checkSymbolFiles();
      }
   }

   private DataManager(String var1) {
      super(var1);
   }

   public static DataManager get() {
      return instance;
   }

   @Override
   public void initDatabase() {
      try {
         Class.forName("org.sqlite.JDBC");
         if (!this.tableExists("DATA")) {
            String var1 = "CREATE TABLE DATA (ID INTEGER PRIMARY KEY AUTOINCREMENT,SOURCEDATA_ID INTEGER DEFAULT 0,CONNECTION \tVARCHAR(50)\tNOT NULL, SYMBOL\t\t\tVARCHAR(50)\tNOT NULL, INSTRUMENT \tVARCHAR(20) NOT NULL, TIMEFRAME \t\tVARCHAR(50), TIMEZONE \t\tVARCHAR(100), FILENAME \t\tVARCHAR(100), DATEFROM     \tLONG, DATETO \t\tLONG, DATATYPE \t\tINT, ROWS \t\t\tINT DEFAULT 0, DECIMALS\t\tINT, SOURCE\t\t\tINT)";
            this.sqlCommand(var1);
            Log.info("Data table created successfully");
         }

         if (!this.columnExists("DATA", "SECONDS_RECORDS")) {
            String var4 = "ALTER TABLE DATA ADD SECONDS_RECORDS LONG DEFAULT 0";
            this.sqlCommand(var4);
         }

         if (!this.columnExists("DATA", "USYMBOL")) {
            this.sqlCommand("ALTER TABLE DATA ADD USYMBOL VARCHAR(50)");
            this.sqlCommand("ALTER TABLE DATA ADD USYMBOLNAME VARCHAR(256)");
         }

         if (!this.columnExists("DATA", "REMOVE_WEEKENDS")) {
            String var5 = "ALTER TABLE DATA ADD REMOVE_WEEKENDS INT DEFAULT 0";
            this.sqlCommand(var5);
         }

         if (!this.columnExists("DATA", "SHOW")) {
            String var6 = "ALTER TABLE DATA ADD SHOW INT DEFAULT 1";
            this.sqlCommand(var6);
         }

         if (!this.columnExists("DATA", "BASKET_ID")) {
            String var7 = "ALTER TABLE DATA ADD BASKET_ID INT DEFAULT -1";
            this.sqlCommand(var7);
         }

         if (!this.columnExists("DATA", "BROKER_ID")) {
            String var8 = "ALTER TABLE DATA ADD BROKER_ID INT DEFAULT -1";
            this.sqlCommand(var8);
            String var2 = "UPDATE DATA SET BROKER_ID=(SELECT BROKER_ID FROM INSTRUMENTS WHERE INSTRUMENTS.INSTRUMENT=DATA.INSTRUMENT)";
            this.sqlCommand(var2);
         }
      } catch (Exception var3) {
         Log.error("DB error: Cannot create Data table.", var3);
      }
   }

   private static void checkSymbolFiles() {
      try {
         ArrayList var0 = list();

         for (int var1 = 0; var1 < var0.size(); var1++) {
            DataInfo var2 = (DataInfo)var0.get(var1);
            if (!isGroupAlias(var2.symbol)) {
               String var3 = get()._getDataFileName(var2.connection, var2.symbol, var2.timeframe, "No Session");
               File var4 = new File(var3);
               if (var2.rows > 0 && !Files.exists(var4.toPath())) {
                  updateDataInBatch(var2.connection, var2.symbol, 0L, 0L, 0, 0L, var2.barTimeType, var2.timeframe, null);
                  Log.debug("No data files found for symbol '" + var2.symbol + "' (connection '" + var2.connection + "')");
               }
            }
         }

         flushUpdatedData();
      } catch (Exception var5) {
         Log.error("Cannot check symbol files. ", var5);
      }
   }

   private void moveDataFromBaseFolder(String var1) {
      File var2 = new File(this._getDirectory() + "/" + var1);
      if (Files.exists(var2.toPath())) {
         File[] var3 = var2.listFiles();
         ArrayList var4 = new ArrayList();
         if (var3.length != 0) {
            for (File var8 : var3) {
               if (!var8.isDirectory() && var8.getName().endsWith(".dat")) {
                  var4.add(var8);
               }
            }

            for (; var4.size() != 0; var4.remove(0)) {
               File var10 = (File)var4.get(0);

               try {
                  this.moveFileFromBaseFolder(var1, var10);
               } catch (Exception var9) {
                  var9.printStackTrace();
               }
            }
         }
      }
   }

   private void moveFileFromBaseFolder(String var1, File var2) throws Exception {
      String var3 = var2.getName();
      int var5 = var3.lastIndexOf(".");
      if (var5 != -1) {
         String var4 = var3.substring(0, var5);
      }

      int var6 = var3.lastIndexOf("_");
      if (var5 != -1) {
         String var7 = var3.substring(0, var6);
         FileUtils.moveFile(var2, new File(this._getDirectory() + "/" + var1 + "/" + var7 + "/" + var3));
      } else {
         throw new Exception("Data file name doesn't have correct format!");
      }
   }

   private static void deleteFolderExcept(File var0, File... var1) {
      if (Files.exists(var0.toPath())) {
         File[] var2 = var0.listFiles();
         if (var2 != null) {
            for (File var6 : var2) {
               boolean var7 = false;
               if (var1 != null) {
                  for (File var11 : var1) {
                     if (var6.getAbsolutePath().equals(var11.getAbsolutePath())) {
                        var7 = true;
                        break;
                     }
                  }
               }

               if (!var7) {
                  var6.delete();
               }
            }
         }

         var0.delete();
      }
   }

   public static ArrayList<DataInfo> list() throws Exception {
      return get()._list();
   }

   public static ArrayList<DataInfo> listSafe() throws Exception {
      return get()._listSafe();
   }

   public static ArrayList<DataInfo> listForSource(int var0) throws Exception {
      return get()._listForSource(var0);
   }

   public static ArrayList<DataInfo> listCloned(int var0) throws Exception {
      return get()._listCloned(var0);
   }

   public static DataInfo getDataInfo(String var0, String var1) {
      String var2 = var0 + var1;
      return get().customDataInfoCache.containsKey(var2) ? get().customDataInfoCache.get(var2) : get()._getDataInfo(var0, var1);
   }

   public static DataInfo getDataInfo(String var0, String var1, boolean var2) {
      String var3 = var0 + var1;
      if (get().customDataInfoCache.containsKey(var3)) {
         return get().customDataInfoCache.get(var3);
      } else {
         return var2 ? get().dataInfoCacheAll.get(var3) : get()._getDataInfo(var0, var1);
      }
   }

   public static DataInfo getDataInfo(String var0, int var1) {
      return get()._getDataInfo(var0, var1);
   }

   public static boolean checkDataExists(String var0, String var1) {
      return get()._checkDataExists(var0, var1);
   }

   public static void addData(String var0, String var1, String var2, int var3, int var4) throws Exception {
      get()._addData(var0, var1, var2, var3, var4, null, null, -1, -1);
   }

   public static void addData(String var0, String var1, String var2, int var3, int var4, int var5, int var6) throws Exception {
      get()._addData(var0, var1, var2, var3, var4, null, null, var5, var6);
   }

   public static void addData(String var0, String var1, String var2, int var3, int var4, String var5, String var6, int var7, int var8) throws Exception {
      get()._addData(var0, var1, var2, var3, var4, var5, var6, var7, var8);
   }

   public static void addData(String var0, String var1, String var2, int var3, int var4, String var5, String var6) throws Exception {
      get()._addData(var0, var1, var2, var3, var4, var5, var6, -1, -1);
   }

   public static void addDataInBatch(String var0, List<DataInfo> var1, BatchProgressController var2) throws Exception {
      get()._addDataInBatch(var0, var1, var2);
   }

   public static boolean checkConnectionExists(String var0) {
      return get()._checkConnectionExists(var0);
   }

   public static ArrayList<String> getSymbolsForInstrument(String var0) throws Exception {
      return get()._getSymbolsForInstrument(var0);
   }

   public static void clearData(String var0, String var1) {
      String var2 = null;
      DataInfo var3 = getDataInfo(var0, var1);
      int var4 = var3.barTimeType;
      if (var3.source != 1) {
         var2 = var3.timeframe;
      }

      if (var3.source == 5) {
         String var5 = getUnadjustedDataFileName(var0, var1, var2, "No Session");
         File var6 = new File(var5);

         try {
            Files.deleteIfExists(var6.toPath());
         } catch (IOException var8) {
            Log.error("Unable to delete file " + var5 + ".", var8);
         }
      }

      removeDataFilesWithFolder(var0, var1);
      updateData(var0, var1, 0L, 0L, 0, 0L, var4, var2, null);
   }

   public static void clearDataInBatch(String var0, String var1) {
      String var2 = null;
      if (!isLimited(var1)) {
         DataInfo var3 = getDataInfo(var0, var1);
         int var4 = var3.barTimeType;
         if (var3.source != 1) {
            var2 = var3.timeframe;
         }

         if (var3.source == 5) {
            String var5 = getUnadjustedDataFileName(var0, var1, var2, "No Session");
            File var6 = new File(var5);

            try {
               Files.deleteIfExists(var6.toPath());
            } catch (IOException var8) {
               Log.error("Unable to delete file " + var5 + ".", var8);
            }
         }

         removeDataFilesWithFolder(var0, var1);
         updateDataInBatch(var0, var1, 0L, 0L, 0, 0L, var4, var2, null);
      }
   }

   public static void deleteData(String var0, String var1) {
      get()._deleteData(var0, var1);
   }

   public static void deleteForGroup(String var0, String var1, int var2) {
      get()._deleteForGroup(var0, var1, var2);
   }

   private void _deleteForGroup(String var1, String var2, int var3) {
      try {
         String var4 = "DELETE FROM DATA WHERE BASKET_ID=" + var3;
         this.sqlCommand(var4);
         DataInfo var5 = this.dataInfoCache.remove(this.getCacheKey(var1, var2));
         if (var5 != null) {
            this.cachedDataList.remove(var5);
         }

         this.dataLastTimeUpdated = System.currentTimeMillis();
      } catch (Exception var6) {
         Log.error("Data DB error:", var6);
      }
   }

   public static void deleteDataInBatch(String[] var0, String[] var1, BatchProgressController var2) throws ClassNotFoundException {
      get()._deleteDataInBatch(var0, var1, var2);
   }

   public static void updateInstrument(String var0, String var1, String var2) {
      get()._updateInstrument(var0, var1, var2);
   }

   public static void updateInstrumentSwap(String var0, String var1, String var2, String var3) {
      get()._updateInstrumentSwap(var0, var1, var2, var3);
   }

   public static void updateSecondsRecords(String var0, String var1, long var2) {
      get()._updateSecondsRecords(var0, var1, var2);
   }

   public static void updateData(String var0, String var1, long var2, long var4, int var6, long var7, int var9, String var10, String var11) {
      updateData(var0, var1, var2, var4, var6, var7, var9, var10, var11, -1, false);
   }

   public static void updateDataInBatch(String var0, String var1, long var2, long var4, int var6, long var7, int var9, String var10, String var11) {
      updateDataInBatch(var0, var1, var2, var4, var6, var7, var9, var10, var11, -1, false);
   }

   public static void updateData(
      String var0, String var1, long var2, long var4, int var6, long var7, int var9, String var10, String var11, int var12, boolean var13
   ) {
      get()._updateData(var0, var1, var2, var4, var6, var7, var9, var10, var11, var12, var13);
   }

   public static void updateDataInBatch(
      String var0, String var1, long var2, long var4, int var6, long var7, int var9, String var10, String var11, int var12, boolean var13
   ) {
      get()._updateDataInBatch(var0, var1, var2, var4, var6, var7, var9, var10, var11, var12, var13);
   }

   public static void flushUpdatedData() {
      get()._flushUpdatedData();
   }

   public static void updateTimeframe(String var0, String var1, String var2) {
      get()._updateTimeframe(var0, var1, var2);
   }

   public static void updateBroker(String var0, String var1, int var2) {
      get()._updateBroker(var0, var1, var2);
   }

   public static void renameData(String var0, String var1, String var2) {
      get()._renameData(var0, var1, var2);
   }

   public static String renameUnderlayingData(String var0, String var1, String var2) {
      return get()._renameUnderlayingData(var0, var1, var2);
   }

   public static void showData(String var0, String var1, boolean var2) {
      get()._showData(var0, var1, var2);
   }

   public static void updateUnderlyingSymbolName(String var0, String var1, String var2) {
      get()._updateUnderlyingSymbolName(var0, var1, var2);
   }

   public static String getDataFileName(String var0, String var1, String var2, String var3) {
      return get()._getDataFileName(var0, var1, var2, var3);
   }

   public static String getUnadjustedDataFileName(String var0, String var1, String var2, String var3) {
      return get()._getUnadjustedDataFileName(var0, var1, var2, var3);
   }

   public static String getDataFileName(String var0, String var1, String var2, String var3, DataInfo var4) {
      return get()._getDataFileName(var0, var1, var2, var3, var4);
   }

   public static String getUnadjustedDataFileName(String var0, String var1, String var2, String var3, DataInfo var4) {
      return get()._getUnadjustedDataFileName(var0, var1, var2, var3, var4);
   }

   public static synchronized IDataLoader getDataLoader(ChartDef var0, int var1, IStopPauseStatus var2) throws Exception {
      return get()._getDataLoader(var0, var1, var2);
   }

   public static synchronized IDataLoader getDataLoader(ChartDef var0, int var1) throws Exception {
      return get()._getDataLoader(var0, var1, null);
   }

   private synchronized ArrayList<DataInfo> _listSafe() throws Exception {
      return new ArrayList<>(this._list());
   }

   private synchronized ArrayList<DataInfo> _list() throws Exception {
      if (!this.cachedDataList.isEmpty()) {
         return this.cachedDataList;
      }

      ArrayList var1 = new ArrayList();
      Connection var2 = null;
      Statement var3 = null;
      ResultSet var4 = null;
      var2 = this.getConnection();
      var3 = var2.createStatement();
      var4 = var3.executeQuery("SELECT count(*) FROM DATA");
      if (var4.next()) {
         int var5 = var4.getInt(1);
         var1.ensureCapacity(var5);
         this.cachedDataList.ensureCapacity(var5);
      }

      var3 = var2.createStatement();
      var4 = var3.executeQuery("SELECT * FROM DATA");

      while (var4.next()) {
         DataInfo var14 = new DataInfo();
         var14.id = var4.getInt("ID");
         var14.connection = var4.getString("CONNECTION");
         var14.symbol = var4.getString("SYMBOL");
         var14.instrument = var4.getString("INSTRUMENT");
         var14.timeframe = var4.getString("TIMEFRAME");
         var14.timezone = var4.getString("TIMEZONE");
         var14.filename = var4.getString("FILENAME");
         var14.dateFrom = var4.getLong("DATEFROM");
         var14.dateTo = var4.getLong("DATETO");
         var14.rows = var4.getInt("ROWS");
         var14.secondsRecords = var4.getLong("SECONDS_RECORDS");
         var14.barTimeType = var4.getInt("DATATYPE");
         var14.decimals = var4.getInt("DECIMALS");
         var14.source = var4.getInt("SOURCE");
         var14.sourceDataId = var4.getInt("SOURCEDATA_ID");
         var14.uSymbol = var4.getString("USYMBOL");
         var14.uSymbolName = var4.getString("USYMBOLNAME");
         var14.show = var4.getBoolean("SHOW");
         var14.basketId = var4.getInt("BASKET_ID");
         var14.brokerId = var4.getInt("BROKER_ID");
         var14.fileHash = this.getFileHash(var14);
         var14.timeframe = var14.timeframe != null && !var14.timeframe.equals("null") ? var14.timeframe : null;

         try {
            var14.symbolInfo = InstrumentManager.getInstrumentInfo(var14.instrument);
         } catch (Exception var8) {
            Log.error("Error while loading symbolInfo. ", var8);
         }

         var1.add(var14);
         this.cachedDataList.add(var14);
         String var6 = var4.getString("CONNECTION");
         String var7 = var6 + var14.symbol;
         this.dataInfoCache.put(var7, var14);
      }

      this.close(var4);
      this.close(var3);
      this.close(var2);
      this.cachedDataList.sort(this.dataComparator);
      var1.sort(this.dataComparator);
      return var1;
   }

   private synchronized ArrayList<DataInfo> _listForSource(int var1) throws Exception {
      ArrayList var2 = new ArrayList();
      Connection var3 = null;
      Statement var4 = null;
      ResultSet var5 = null;
      var3 = this.getConnection();
      var4 = var3.createStatement();
      var5 = var4.executeQuery("SELECT * FROM DATA where source=" + var1);

      while (var5.next()) {
         DataInfo var6 = new DataInfo();
         var6.id = var5.getInt("ID");
         var6.connection = var5.getString("CONNECTION");
         var6.symbol = var5.getString("SYMBOL");
         var6.instrument = var5.getString("INSTRUMENT");
         var6.timeframe = var5.getString("TIMEFRAME");
         var6.timezone = var5.getString("TIMEZONE");
         var6.filename = var5.getString("FILENAME");
         var6.dateFrom = var5.getLong("DATEFROM");
         var6.dateTo = var5.getLong("DATETO");
         var6.rows = var5.getInt("ROWS");
         var6.secondsRecords = var5.getLong("SECONDS_RECORDS");
         var6.barTimeType = var5.getInt("DATATYPE");
         var6.decimals = var5.getInt("DECIMALS");
         var6.source = var5.getInt("SOURCE");
         var6.sourceDataId = var5.getInt("SOURCEDATA_ID");
         var6.uSymbol = var5.getString("USYMBOL");
         var6.uSymbolName = var5.getString("USYMBOLNAME");
         var6.show = var5.getBoolean("SHOW");
         var6.basketId = var5.getInt("BASKET_ID");
         var6.brokerId = var5.getInt("BROKER_ID");
         var6.fileHash = this.getFileHash(var6);
         var6.timeframe = var6.timeframe != null && !var6.timeframe.equals("null") ? var6.timeframe : null;

         try {
            var6.symbolInfo = InstrumentManager.getInstrumentInfo(var6.instrument);
         } catch (Exception var8) {
            Log.error("Error while loading symbolInfo. ", var8);
         }

         var2.add(var6);
      }

      this.close(var5);
      this.close(var4);
      this.close(var3);
      return var2;
   }

   private synchronized ArrayList<DataInfo> _listCloned(int var1) throws Exception {
      ArrayList var2 = new ArrayList();
      Connection var3 = null;
      Statement var4 = null;
      ResultSet var5 = null;
      var3 = this.getConnection();
      var4 = var3.createStatement();
      var5 = var4.executeQuery("SELECT * FROM DATA WHERE SOURCEDATA_ID=" + var1);

      while (var5.next()) {
         DataInfo var6 = new DataInfo();
         var6.id = var5.getInt("ID");
         var6.connection = var5.getString("CONNECTION");
         var6.symbol = var5.getString("SYMBOL");
         var6.instrument = var5.getString("INSTRUMENT");
         var6.timeframe = var5.getString("TIMEFRAME");
         var6.timezone = var5.getString("TIMEZONE");
         var6.filename = var5.getString("FILENAME");
         var6.dateFrom = var5.getLong("DATEFROM");
         var6.dateTo = var5.getLong("DATETO");
         var6.rows = var5.getInt("ROWS");
         var6.secondsRecords = var5.getLong("SECONDS_RECORDS");
         var6.barTimeType = var5.getInt("DATATYPE");
         var6.decimals = var5.getInt("DECIMALS");
         var6.source = var5.getInt("SOURCE");
         var6.sourceDataId = var5.getInt("SOURCEDATA_ID");
         var6.uSymbol = var5.getString("USYMBOL");
         var6.uSymbolName = var5.getString("USYMBOLNAME");
         var6.removeWeekends = var5.getBoolean("REMOVE_WEEKENDS");
         var6.show = var5.getBoolean("SHOW");
         var6.basketId = var5.getInt("BASKET_ID");
         var6.brokerId = var5.getInt("BROKER_ID");
         var6.fileHash = this.getFileHash(var6);
         var6.timeframe = var6.timeframe != null && !var6.timeframe.equals("null") ? var6.timeframe : null;

         try {
            var6.symbolInfo = InstrumentManager.getInstrumentInfo(var6.instrument);
         } catch (Exception var8) {
            Log.error("Error while loading symbolInfo. ", var8);
         }

         var2.add(var6);
      }

      this.close(var5);
      this.close(var4);
      this.close(var3);
      return var2;
   }

   private String _getDataFileName(String var1, String var2, String var3, String var4) {
      String var5 = getSymbolFolderName(this._getDirectory(), var1, var2);
      if (var4 != null && !var4.equals("No Session")) {
         String var7 = var2 + "_" + var3 + "_" + var4 + ".dat";
         return var5 + "/" + fixFilename(var7);
      } else {
         String var6 = var2 + "_" + var3 + ".dat";
         return var5 + "/" + fixFilename(var6);
      }
   }

   private String _getUnadjustedDataFileName(String var1, String var2, String var3, String var4) {
      String var5 = getSymbolFolderName(this._getDirectory(), var1, var2);
      if (var4 != null && !var4.equals("No Session")) {
         String var7 = var2 + "unadjusted_" + var3 + "_" + var4 + ".dat";
         return var5 + "/" + fixFilename(var7);
      } else {
         String var6 = var2 + "unadjusted_" + var3 + ".dat";
         return var5 + "/" + fixFilename(var6);
      }
   }

   private String _getDataFileName(String var1, String var2, String var3, String var4, DataInfo var5) {
      String var6 = getSymbolFolderName(this._getDirectory(), var1, var2, var5);
      if (var4 != null && !var4.equals("No Session")) {
         String var8 = var2 + "_" + var3 + "_" + var4 + ".dat";
         return var6 + "/" + fixFilename(var8);
      } else {
         String var7 = var2 + "_" + var3 + ".dat";
         return var6 + "/" + fixFilename(var7);
      }
   }

   private String _getUnadjustedDataFileName(String var1, String var2, String var3, String var4, DataInfo var5) {
      String var6 = getSymbolFolderName(this._getDirectory(), var1, var2, var5);
      if (var4 != null && !var4.equals("No Session")) {
         String var8 = var2 + "unadjusted_" + var3 + "_" + var4 + ".dat";
         return var6 + "/" + fixFilename(var8);
      } else {
         String var7 = var2 + "unadjusted_" + var3 + ".dat";
         return var6 + "/" + fixFilename(var7);
      }
   }

   public static String fixFilename(String var0) {
      String var1 = var0.replaceAll("[^a-zA-Z0-9\\.\\-@#\\(\\)]", "_");
      if (var1.equalsIgnoreCase("CON")
         || var1.startsWith("CON.")
         || var1.equalsIgnoreCase("PRN")
         || var1.startsWith("PRN.")
         || var1.equalsIgnoreCase("AUX")
         || var1.startsWith("AUX.")) {
         var1 = "_" + var1;
      }

      return var1;
   }

   public boolean _isFilenameValid(String var1) {
      File var2 = new File(var1);

      try {
         var2.getCanonicalPath();
         return true;
      } catch (IOException var4) {
         return false;
      }
   }

   public void _updateInstrumentSwap(String var1, String var2, String var3, String var4) {
      try {
         DataInfo var5 = this.getRecordFromCache(var1, var2);
         InstrumentInfo var6 = InstrumentManager.getInstrumentInfo(var3);
         var6.swap = var4;
         InstrumentManager.updateInstrument(var6);
         var5.instrument = var3;
         var5.symbolInfo = InstrumentManager.getInstrumentInfo(var3);
         this.updateCachedDataList(var5);
         this._clearHistoryFolder(var1, var2);
      } catch (Exception var7) {
         Log.error("Data DB error:", var7);
      }
   }

   public void _updateInstrument(String var1, String var2, String var3) {
      try {
         String var4 = "UPDATE DATA SET INSTRUMENT='" + var3 + "'";
         var4 = var4 + " WHERE CONNECTION='" + var1 + "' AND SYMBOL='" + var2 + "'";
         this.sqlCommand(var4);
         DataInfo var5 = this.getRecordFromCache(var1, var2);
         var5.instrument = var3;
         var5.symbolInfo = InstrumentManager.getInstrumentInfo(var3);
         this.updateCachedDataList(var5);
         this._clearHistoryFolder(var1, var2);
      } catch (Exception var6) {
         Log.error("Data DB error:", var6);
      }
   }

   public void _updateSecondsRecords(String var1, String var2, long var3) {
      try {
         String var5 = "UPDATE DATA SET SECONDS_RECORDS=" + var3 + "";
         var5 = var5 + " WHERE CONNECTION='" + var1 + "' AND SYMBOL='" + var2 + "'";
         this.sqlCommand(var5);
         DataInfo var6 = this.getRecordFromCache(var1, var2);
         var6.secondsRecords = var3;
         this.updateCachedDataList(var6);
         this._clearHistoryFolder(var1, var2);
      } catch (Exception var7) {
         Log.error("Data DB error:", var7);
      }
   }

   private DataInfo getRecordFromCache(String var1, String var2) {
      String var3 = this.getCacheKey(var1, var2);
      return this.dataInfoCache.get(var3);
   }

   private void updateCachedDataList(DataInfo var1) {
      for (int var2 = 0; var2 < this.cachedDataList.size(); var2++) {
         if (this.cachedDataList.get(var2).symbol.equals(var1.symbol)) {
            this.cachedDataList.set(var2, var1);
         }
      }

      this.dataInfoCache.put(this.getCacheKey(var1.connection, var1.symbol), var1);
      this.dataLastTimeUpdated = System.currentTimeMillis();
   }

   private void updateCachedDataList(DataInfo var1, String var2) {
      for (int var3 = 0; var3 < this.cachedDataList.size(); var3++) {
         if (this.cachedDataList.get(var3).symbol.equals(var2)) {
            this.cachedDataList.set(var3, var1);
         }
      }

      this.dataInfoCache.put(this.getCacheKey(var1.connection, var1.symbol), var1);
      this.dataLastTimeUpdated = System.currentTimeMillis();
   }

   public void removeRecordFromCacheForInstrument(String var1) {
      ArrayList var2 = new ArrayList();

      for (String var4 : this.dataInfoCache.keySet()) {
         DataInfo var5 = this.dataInfoCache.get(var4);
         if (var5 != null && var5.instrument != null && var5.instrument.equals(var1)) {
            var2.add(var4);
         }
      }

      for (int var6 = 0; var6 < var2.size(); var6++) {
         this.dataInfoCache.remove(var2.get(var6));
      }
   }

   private synchronized void _updateDataInBatch(
      String var1, String var2, long var3, long var5, int var7, long var8, int var10, String var11, String var12, int var13, boolean var14
   ) {
      SymbolInfo var15 = new SymbolInfo();
      var15.connection = var1;
      var15.symbol = var2;
      var15.dateFrom = var3;
      var15.dateTo = var5;
      var15.rows = var7;
      var15.secondsRecords = var8;
      var15.timeframe = var11;
      var15.barType = var10;
      var15.timezone = var12;
      var15.sourceDataId = var13;
      var15.removeWeekends = var14;
      this.updatedInfos.add(var15);
      DataInfo var16 = this.getRecordFromCache(var1, var2);
      var16.dateFrom = var3;
      var16.dateTo = var5;
      var16.rows = var7;
      var16.secondsRecords = var8;
      var16.timeframe = var11;
      var16.barTimeType = var10;
      var16.fileHash = this.getFileHash(var16);
      if (var12 != null) {
         var16.timezone = var12;
      }

      if (var13 > 0) {
         var16.sourceDataId = var13;
         var16.removeWeekends = var14;
      }

      if (this.updatedInfos.size() % 150 == 0) {
         this._flushUpdatedData();
      }
   }

   private synchronized void _flushUpdatedData() {
      if (!this.updatedInfos.isEmpty()) {
         try {
            Connection var1 = this.getConnection();
            var1.setAutoCommit(false);

            for (SymbolInfo var3 : this.updatedInfos) {
               String var4 = "UPDATE DATA SET DATEFROM='"
                  + var3.dateFrom
                  + "',DATETO='"
                  + var3.dateTo
                  + "',ROWS="
                  + var3.rows
                  + ",SECONDS_RECORDS="
                  + var3.secondsRecords
                  + ",DATATYPE="
                  + var3.barType
                  + ",TIMEFRAME='"
                  + var3.timeframe
                  + "'";
               if (var3.timezone != null) {
                  var4 = var4 + ",TIMEZONE='" + var3.timezone + "'";
               }

               if (var3.sourceDataId > 0) {
                  var4 = var4 + ",SOURCEDATA_ID=" + var3.sourceDataId + ", REMOVE_WEEKENDS=" + (var3.removeWeekends ? 1 : 0);
               }

               var4 = var4 + " WHERE CONNECTION='" + var3.connection + "' AND SYMBOL='" + var3.symbol + "'";
               Statement var5 = var1.createStatement();
               var5.executeUpdate(var4);
               var5.close();
               this._clearHistoryFolder(var3.connection, var3.symbol);
            }

            var1.commit();
            var1.close();
         } catch (Exception var6) {
            Log.error("Data DB error:", var6);
         }

         this.dataLastTimeUpdated = System.currentTimeMillis();
         this.updatedInfos.clear();
      }
   }

   public void _updateData(String var1, String var2, long var3, long var5, int var7, long var8, int var10, String var11, String var12, int var13, boolean var14) {
      try {
         DataInfo var15 = this.getRecordFromCache(var1, var2);
         String var16 = "UPDATE DATA SET DATEFROM='"
            + var3
            + "',DATETO='"
            + var5
            + "',ROWS="
            + var7
            + ",SECONDS_RECORDS="
            + var8
            + ",DATATYPE="
            + var10
            + ",TIMEFRAME='"
            + var11
            + "'";
         if (var12 != null) {
            var16 = var16 + ",TIMEZONE='" + var12 + "'";
            var15.timezone = var12;
         }

         if (var13 > 0) {
            var16 = var16 + ",SOURCEDATA_ID=" + var13 + ", REMOVE_WEEKENDS=" + (var14 ? 1 : 0);
            var15.sourceDataId = var13;
            var15.removeWeekends = var14;
         }

         var16 = var16 + " WHERE CONNECTION='" + var1 + "' AND SYMBOL='" + var2 + "'";
         this.sqlCommand(var16);
         var15.dateFrom = var3;
         var15.dateTo = var5;
         var15.rows = var7;
         var15.secondsRecords = var8;
         var15.timeframe = var11;
         var15.barTimeType = var10;
         var15.fileHash = this.getFileHash(var15);
         this.updateCachedDataList(var15);
         this._clearHistoryFolder(var1, var2);
      } catch (Exception var17) {
         Log.error("Data DB error:", var17);
      }
   }

   private void _updateBroker(String var1, String var2, int var3) {
      try {
         String var4 = "UPDATE DATA SET BROKER_ID='" + var3 + "'";
         var4 = var4 + " WHERE CONNECTION='" + var1 + "' AND SYMBOL='" + var2 + "'";
         this.sqlCommand(var4);
         String var5 = var1 + var2;
         DataInfo var6 = this.dataInfoCache.get(var5);
         var6.brokerId = var3;
         this.updateCachedDataList(var6);
         this._clearHistoryFolder(var1, var2);
      } catch (Exception var7) {
         Log.error("Data DB error:", var7);
      }
   }

   private void _updateTimeframe(String var1, String var2, String var3) {
      try {
         String var4 = "UPDATE DATA SET TIMEFRAME='" + var3 + "'";
         var4 = var4 + " WHERE CONNECTION='" + var1 + "' AND SYMBOL='" + var2 + "'";
         this.sqlCommand(var4);
         String var5 = var1 + var2;
         DataInfo var6 = this.dataInfoCache.get(var5);
         var6.timeframe = var3;
         this.updateCachedDataList(var6);
         this._clearHistoryFolder(var1, var2);
      } catch (Exception var7) {
         Log.error("Data DB error:", var7);
      }
   }

   private void _updateTimezone(String var1, String var2, String var3) {
      try {
         String var4 = "UPDATE DATA SET TIMEZONE='" + var3 + "'";
         var4 = var4 + " WHERE CONNECTION='" + var1 + "' AND SYMBOL='" + var2 + "'";
         this.sqlCommand(var4);
         String var5 = var1 + var2;
         DataInfo var6 = this.dataInfoCache.get(var5);
         var6.timezone = var3;
         this.updateCachedDataList(var6);
         this._clearHistoryFolder(var1, var2);
      } catch (Exception var7) {
         Log.error("Data DB error:", var7);
      }
   }

   private void _clearHistoryFolder(String var1, String var2) {
      String var3 = getSymbolFolderName(this._getDirectory(), var1, var2);
      File var4 = new File(var3);
      if (Files.exists(var4.toPath())) {
         DataInfo var5 = this._getDataInfo(var1, var2);
         if (var5 != null) {
            File var6 = new File(this._getDataFileName(var1, var2, var5.timeframe, "No Session"));
            File var7 = new File(this._getUnadjustedDataFileName(var1, var2, var5.timeframe, "No Session"));
            File var8 = new File(getTempFileName(var6.getAbsolutePath()));
            deleteFolderExcept(var4, var6, var7, var8);
         }
      }
   }

   public synchronized void _renameData(String var1, String var2, String var3) {
      try {
         if (var2.equals(var3)) {
            throw new RuntimeException("Rename failed - original and new name are the same.");
         }

         boolean var4 = !var2.equals(var3) && var2.equalsIgnoreCase(var3);
         if (!var4 || !new OperatingSystem().isWindows()) {
            renameDataFiles(var1, var2, var3);
         }

         String var5 = "UPDATE DATA SET SYMBOL='" + var3 + "' WHERE CONNECTION='" + var1 + "' AND SYMBOL='" + var2 + "'";
         this.sqlCommand(var5);
         DataInfo var6 = this.getRecordFromCache(var1, var2);
         var6.symbol = var3;
         String var7 = this.getCacheKey(var1, var2);
         this.dataInfoCache.remove(var7);
         String var8 = this.getCacheKey(var1, var3);
         this.dataInfoCache.put(var8, var6);
         this.updateCachedDataList(var6, var2);
      } catch (Exception var9) {
         Log.error("Data DB error:", var9);
      }
   }

   public synchronized String _renameUnderlayingData(String var1, String var2, String var3) {
      try {
         if (var2.equals(var3)) {
            throw new RuntimeException("Rename failed - original and new name are the same.");
         }

         String var4 = "UPDATE DATA SET USYMBOL='" + var3 + "' WHERE CONNECTION='" + var1 + "' AND SYMBOL='" + var2 + "'";
         DataInfo var5 = this.getRecordFromCache(var1, var2);
         var5.uSymbol = var3;
         this.sqlCommand(var4);
         String var6 = var3;

         while (checkDataExists(var1, var6)) {
            var6 = generateName(var6);
         }

         this._renameData(var1, var2, var6);
         return var6;
      } catch (Exception var7) {
         Log.error("Data DB error:", var7);
         return null;
      }
   }

   private String getCacheKey(String var1, String var2) {
      return var1 + var2;
   }

   private synchronized void _deleteData(String var1, String var2) {
      try {
         String var3 = "DELETE FROM DATA WHERE CONNECTION='" + var1 + "' AND SYMBOL='" + var2 + "'";
         this.sqlCommand(var3);
         removeDataFilesWithFolder(var1, var2);
         DataInfo var4 = this.dataInfoCache.remove(this.getCacheKey(var1, var2));
         if (var4 != null) {
            this.cachedDataList.remove(var4);
         }

         this.dataLastTimeUpdated = System.currentTimeMillis();
         this._clearHistoryFolder(var1, var2);
      } catch (Exception var5) {
         Log.error("Data DB error:", var5);
      }
   }

   private synchronized void _deleteDataInBatch(String[] var1, String[] var2, BatchProgressController var3) throws ClassNotFoundException {
      Class.forName("org.sqlite.JDBC");
      Connection var4 = null;

      try {
         var4 = this.getConnection();
         var4.setAutoCommit(false);
         int var5 = var1.length;
         int var6 = 0;

         for (int var7 = 0; var7 < var1.length; var7++) {
            String var8 = var1[var7];
            String var9 = var2[var7];
            String var10 = "DELETE FROM DATA WHERE CONNECTION='" + var8 + "' AND SYMBOL='" + var9 + "'";
            Statement var11 = var4.createStatement();

            try {
               var11.executeUpdate(var10);
            } catch (Throwable var22) {
               if (var11 != null) {
                  try {
                     var11.close();
                  } catch (Throwable var20) {
                     var22.addSuppressed(var20);
                  }
               }

               throw var22;
            }

            if (var11 != null) {
               var11.close();
            }

            try {
               removeDataFilesWithFolder(var8, var9);
               this._clearHistoryFolder(var8, var9);
            } catch (Exception var21) {
               Log.error("Error while deleting data on filesystem for symbol: " + var9, var21);
            }

            DataInfo var25 = this.dataInfoCache.remove(this.getCacheKey(var8, var9));
            if (var25 != null) {
               this.cachedDataList.remove(var25);
            }

            var6++;
            if (var3 != null) {
               if (var3.isCancel()) {
                  break;
               }

               var3.updateProgress(var6, var5, var9);
            }
         }

         this.dataLastTimeUpdated = System.currentTimeMillis();
         var4.commit();
      } catch (Exception var23) {
         Log.error("Data DB error:", var23);
      } finally {
         this.close(var4);
      }

      if (var3 != null) {
         var3.finished();
      }
   }

   public static boolean isLimited(String var0) {
      return var0.endsWith("_limited") || var0.endsWith("_limited.D");
   }

   private synchronized boolean _checkConnectionExists(String var1) {
      if (this.connectionCache.containsKey(var1)) {
         return this.connectionCache.get(var1);
      }

      Connection var2 = null;

      try {
         var2 = this.getConnection();
         boolean var3 = this.sqlCheckRecordExists(var2, "SELECT * FROM DATA WHERE CONNECTION='" + var1 + "'");
         this.connectionCache.put(var1, var3);
         return var3;
      } catch (Exception var8) {
         Log.error("DB Exception", var8);
         return false;
      } finally {
         this.close(var2);
      }
   }

   private synchronized DataInfo _getDataInfo(String var1, String var2) {
      String var3 = var1 + var2;
      if (this.dataInfoCache.containsKey(var3)) {
         return this.dataInfoCache.get(var3);
      }

      Connection var4 = null;
      Statement var5 = null;
      ResultSet var6 = null;

      try {
         var4 = this.getConnection();
         if (!this._checkDataExists(var4, var1, var2)) {
            return null;
         }

         String var7 = "SELECT * FROM DATA WHERE SYMBOL='" + var2 + "'";
         if (var1 != null) {
            var7 = var7 + "AND CONNECTION='" + var1 + "'";
         }

         var5 = var4.createStatement();
         var6 = var5.executeQuery(var7);
         if (var6.next()) {
            DataInfo var8 = this._dbResultSetToDataInfo(var6);
            this.dataInfoCache.put(var3, var8);
            return var8;
         }
      } catch (Exception var13) {
         Log.error("DB Exception", var13);
         return null;
      } finally {
         this.close(var6);
         this.close(var5);
         this.close(var4);
      }

      return null;
   }

   private DataInfo _dbResultSetToDataInfo(ResultSet var1) throws Exception {
      DataInfo var2 = new DataInfo();
      var2.id = var1.getInt("ID");
      var2.sourceDataId = var1.getInt("SOURCEDATA_ID");
      var2.connection = var1.getString("CONNECTION");
      var2.symbol = var1.getString("SYMBOL");
      var2.instrument = var1.getString("INSTRUMENT");
      var2.timeframe = var1.getString("TIMEFRAME");
      var2.timezone = var1.getString("TIMEZONE");
      var2.filename = var1.getString("FILENAME");
      var2.dateFrom = var1.getLong("DATEFROM");
      var2.dateTo = var1.getLong("DATETO");
      var2.rows = var1.getInt("ROWS");
      var2.secondsRecords = var1.getLong("SECONDS_RECORDS");
      var2.barTimeType = var1.getInt("DATATYPE");
      var2.decimals = var1.getInt("DECIMALS");
      var2.source = var1.getInt("SOURCE");
      var2.timeframe = var2.timeframe != null && !var2.timeframe.equals("null") ? var2.timeframe : null;
      var2.symbolInfo = InstrumentManager.getInstrumentInfo(var2.instrument);
      var2.uSymbol = var1.getString("USYMBOL");
      var2.uSymbolName = var1.getString("USYMBOLNAME");
      var2.removeWeekends = var1.getBoolean("REMOVE_WEEKENDS");
      var2.show = var1.getBoolean("SHOW");
      var2.basketId = var1.getInt("BASKET_ID");
      var2.brokerId = var1.getInt("BROKER_ID");
      var2.fileHash = this.getFileHash(var2);
      return var2;
   }

   private long getFileHash(DataInfo var1) {
      if (var1 == null) {
         return 0L;
      }

      boolean var2 = var1.source == 6;
      boolean var3 = var1.source == 5;
      String var4 = fixFilename(var1.symbol);
      String var5 = var1.connection;
      if (var2) {
         var5 = var1.connection + "/" + "sq_futures" + "/" + var4.charAt(0);
      } else if (var3) {
         var5 = var1.connection + "/" + "sq_equity" + "/" + var4.charAt(0);
      }

      String var6 = this._getDirectory() + "/" + var5 + "/" + var4;
      String var7 = var1.symbol + "_" + var1.timeframe + ".dat";
      var7 = var6 + "/" + fixFilename(var7);
      File var8 = new File(var7);
      return var8 != null && Files.exists(var8.toPath()) ? var8.lastModified() + var8.length() : 0L;
   }

   public synchronized void refreshDataInfoCache() throws Exception {
      if (this.dataInfoCacheAll.lastTimeUpdated <= this.dataLastTimeUpdated) {
         this.dataInfoCacheAll.clear();
         Connection var1 = null;
         Statement var2 = null;
         ResultSet var3 = null;

         try {
            String var4 = "SELECT * FROM DATA";
            var1 = this.getConnection();
            var2 = var1.createStatement();
            var3 = var2.executeQuery(var4);

            while (var3.next()) {
               DataInfo var5 = this._dbResultSetToDataInfo(var3);
               String var6 = var5.connection + var5.symbol;
               this.dataInfoCacheAll.put(var6, var5);
            }

            this.dataInfoCacheAll.lastTimeUpdated = System.currentTimeMillis();
         } catch (Exception var10) {
            Log.error("DB Exception", var10);
            throw new Exception("DB Exception, failed to load data info - " + var10.getMessage(), var10);
         } finally {
            this.close(var3);
            this.close(var2);
            this.close(var1);
         }
      }
   }

   private synchronized DataInfo _getDataInfo(String var1, int var2) {
      Connection var3 = null;
      Statement var4 = null;
      ResultSet var5 = null;

      try {
         var3 = this.getConnection();
         DataInfo var6 = new DataInfo();
         String var7 = "SELECT * FROM DATA WHERE ID=" + var2;
         if (var1 != null) {
            var7 = var7 + " AND CONNECTION='" + var1 + "'";
         }

         var4 = var3.createStatement();
         var5 = var4.executeQuery(var7);
         if (var5.next()) {
            var6.id = var5.getInt("ID");
            var6.sourceDataId = var5.getInt("SOURCEDATA_ID");
            var6.connection = var5.getString("CONNECTION");
            var6.symbol = var5.getString("SYMBOL");
            var6.instrument = var5.getString("INSTRUMENT");
            var6.timeframe = var5.getString("TIMEFRAME");
            var6.timezone = var5.getString("TIMEZONE");
            var6.filename = var5.getString("FILENAME");
            var6.dateFrom = var5.getLong("DATEFROM");
            var6.dateTo = var5.getLong("DATETO");
            var6.rows = var5.getInt("ROWS");
            var6.secondsRecords = var5.getLong("SECONDS_RECORDS");
            var6.barTimeType = var5.getInt("DATATYPE");
            var6.decimals = var5.getInt("DECIMALS");
            var6.source = var5.getInt("SOURCE");
            var6.timeframe = var6.timeframe != null && !var6.timeframe.equals("null") ? var6.timeframe : null;
            var6.symbolInfo = InstrumentManager.getInstrumentInfo(var6.instrument);
            var6.uSymbol = var5.getString("USYMBOL");
            var6.uSymbolName = var5.getString("USYMBOLNAME");
            var6.removeWeekends = var5.getBoolean("REMOVE_WEEKENDS");
            var6.show = var5.getBoolean("SHOW");
            var6.basketId = var5.getInt("BASKET_ID");
            var6.brokerId = var5.getInt("BROKER_ID");
            var6.fileHash = this.getFileHash(var6);
            return var6;
         }
      } catch (Exception var12) {
         Log.error("DB Exception", var12);
         return null;
      } finally {
         this.close(var5);
         this.close(var4);
         this.close(var3);
      }

      return null;
   }

   private synchronized boolean _checkDataExists(String var1, String var2) {
      String var3 = var1 + var2;
      if (this.dataInfoCache.containsKey(var3)) {
         return true;
      }

      Connection var4 = null;

      try {
         var4 = this.getConnection();
         return this._checkDataExists(var4, var1, var2);
      } catch (Exception var10) {
         Log.error("DB Exception", var10);
         return false;
      } finally {
         this.close(var4);
      }
   }

   private boolean _checkDataExists(Connection var1, String var2, String var3) {
      String var4 = "SELECT * FROM DATA WHERE SYMBOL='" + var3 + "'";
      if (var2 != null) {
         var4 = var4 + " AND CONNECTION='" + var2 + "'";
      }

      return this.sqlCheckRecordExists(var1, var4);
   }

   public void _addDataInBatch(String var1, List<DataInfo> var2, BatchProgressController var3) throws Exception {
      Connection var4 = null;
      Class.forName("org.sqlite.JDBC");

      try {
         var4 = this.getConnection();
         var4.setAutoCommit(false);
         int var5 = 0;
         int var6 = var2.size();

         for (DataInfo var8 : var2) {
            this.checkSymbolValid(var8.symbol, -1);
            int var9 = InstrumentManager.getInstrumentInfo(var8.instrument).decimals;
            var8.decimals = var9;
            String var10 = "INSERT INTO DATA (CONNECTION, TIMEFRAME, TIMEZONE, SYMBOL, INSTRUMENT, DATATYPE, DECIMALS, SOURCE) VALUES ('"
               + var1
               + "','"
               + var8.timeframe
               + "','"
               + var8.timezone
               + "','"
               + var8.symbol
               + "','"
               + var8.instrument
               + "',"
               + var8.barTimeType
               + ","
               + var9
               + ","
               + var8.source
               + ")";
            if (var8.uSymbol != null) {
               var8.uSymbolName = var8.uSymbolName.replace("'", "''");
               var10 = "INSERT INTO DATA (CONNECTION, TIMEFRAME, TIMEZONE, SYMBOL, INSTRUMENT, DATATYPE, DECIMALS, SOURCE, USYMBOL, USYMBOLNAME) VALUES ('"
                  + var1
                  + "','"
                  + var8.timeframe
                  + "','"
                  + var8.timezone
                  + "','"
                  + var8.symbol
                  + "','"
                  + var8.instrument
                  + "',"
                  + var8.barTimeType
                  + ","
                  + var9
                  + ","
                  + var8.source
                  + ",'"
                  + var8.uSymbol
                  + "','"
                  + var8.uSymbolName
                  + "')";
            }

            int var11 = -1;
            Statement var12 = var4.createStatement();

            try {
               var12.executeUpdate(var10);
               ResultSet var13 = var12.getGeneratedKeys();
               if (var13.next()) {
                  var11 = var13.getInt(1);
               }
            } catch (Throwable var21) {
               if (var12 != null) {
                  try {
                     var12.close();
                  } catch (Throwable var20) {
                     var21.addSuppressed(var20);
                  }
               }

               throw var21;
            }

            if (var12 != null) {
               var12.close();
            }

            var8.id = var11;
            File var24 = new File(this._getDirectory() + "/" + var1);
            if (!Files.exists(var24.toPath())) {
               Files.createDirectory(var24.toPath());
            }

            var8.show = true;
            var8.fileHash = this.getFileHash(var8);
            this.dataInfoCache.put(var1 + var8.symbol, var8);
            this.cachedDataList.add(var8);
            var5++;
            if (var3 != null) {
               if (var3.isCancel()) {
                  var3.finished();
                  break;
               }

               var3.updateProgress(var5, var6, var8.symbol);
               if (var5 == var6) {
                  var3.finished();
               }
            }
         }

         this.dataLastTimeUpdated = System.currentTimeMillis();
         var4.commit();
      } catch (Exception var22) {
         Log.error("DB Exception", var22);
         throw var22;
      } finally {
         this.close(var4);
      }
   }

   private void checkSymbolValid(String var1, int var2) throws Exception {
      if (var2 == -1 || !var1.startsWith("[") || !var1.endsWith("]")) {
         if (!var1.matches("[\\^\\$a-zA-Z0-9_@.:()-]*")) {
            throw new Exception(String.format("Symbol name '%s' cannot contain any special characters except for ._@$^()", var1));
         }
      }
   }

   public void _addData(String var1, String var2, String var3, int var4, int var5, String var6, String var7, int var8, int var9) throws Exception {
      Connection var10 = null;

      try {
         var10 = this.getConnection();
         this.checkSymbolValid(var2, var8);
         if (!this._checkDataExists(var10, var1, var2)) {
            if (!InstrumentManager.checkInstrumentExists(var3)) {
               throw new DataException(3, "Instrument '" + var3 + "' doesn't exists in instruments database !");
            }

            int var11 = InstrumentManager.getInstrumentInfo(var3).decimals;
            String var12 = "INSERT INTO DATA (CONNECTION, SYMBOL, INSTRUMENT, DATATYPE, DECIMALS, SOURCE, BASKET_ID, BROKER_ID) VALUES ('"
               + var1
               + "','"
               + var2
               + "','"
               + var3
               + "',"
               + var4
               + ","
               + var11
               + ","
               + var5
               + ","
               + var8
               + ","
               + var9
               + ")";
            if (var6 != null) {
               var7 = var7.replace("'", "''");
               var12 = "INSERT INTO DATA (CONNECTION, SYMBOL, INSTRUMENT, DATATYPE, DECIMALS, SOURCE, USYMBOL, USYMBOLNAME, BROKER_ID) VALUES ('"
                  + var1
                  + "','"
                  + var2
                  + "','"
                  + var3
                  + "',"
                  + var4
                  + ","
                  + var11
                  + ","
                  + var5
                  + ",'"
                  + var6
                  + "','"
                  + var7
                  + "',"
                  + var9
                  + ")";
            }

            int var13 = this.sqlInsertReturnAutoId(var10, var12);
            File var14 = new File(this._getDirectory() + "/" + var1);
            if (!Files.exists(var14.toPath())) {
               Files.createDirectory(var14.toPath());
            }

            DataInfo var15 = new DataInfo();
            var15.id = var13;
            var15.connection = var1;
            var15.symbol = var2;
            var15.instrument = var3;
            var15.source = var5;
            var15.decimals = var11;
            var15.barTimeType = var4;
            var15.uSymbol = var6;
            var15.uSymbolName = var7;
            var15.show = true;
            var15.basketId = var8;
            var15.brokerId = var9;

            try {
               var15.symbolInfo = InstrumentManager.getInstrumentInfo(var15.instrument);
            } catch (Exception var21) {
               Log.error("Error while loading symbolInfo. ", var21);
            }

            this.dataInfoCache.put(var1 + var2, var15);
            this.cachedDataList.add(var15);
            this.dataLastTimeUpdated = System.currentTimeMillis();
            return;
         }
      } catch (Exception var22) {
         Log.error("DB Exception - error while inserting data " + var2, var22);
         throw var22;
      } finally {
         this.close(var10);
      }
   }

   public ArrayList<String> _getSymbolsForInstrument(String var1) throws Exception {
      Log.debug("_getSymbolsForInstrument calling DB");
      ArrayList var2 = new ArrayList();
      Connection var3 = null;
      Statement var4 = null;
      ResultSet var5 = null;

      try {
         String var6 = "SELECT SYMBOL FROM DATA WHERE INSTRUMENT='" + var1 + "'";
         var3 = this.getConnection();
         var4 = var3.createStatement();
         var5 = var4.executeQuery(var6);

         while (var5.next()) {
            var2.add(var5.getString(1));
         }
      } catch (Exception var11) {
         Log.error("DB Exception", var11);
         return null;
      } finally {
         this.close(var5);
         this.close(var4);
         this.close(var3);
      }

      return var2;
   }

   public IDataLoader _getDataLoader(ChartDef var1, int var2, IStopPauseStatus var3) throws Exception {
      IDataLoader var4 = this._getSimpleDataLoader(var1.getSymbol());
      if (var4 != null) {
         return var4;
      }

      String var5 = var1.getConnectionName();
      String var6 = var1.getSymbol();
      DataInfo var7 = this._getDataInfo(var5, var6);
      String var8 = this._getTimeframeToLoad(var7.timeframe, var1.getTimeframe(), var2);
      BarType var9 = BarTypeFactory.getBarType(var8, var7.barTimeType);
      String var10 = this._getDataFileName(var5, var6, var8, var1.getSession());
      if (!this._dataFileExists(var10)) {
         this._computeData(var8, var7, var1.getSession(), var3);
         if (!this._dataFileExists(var10)) {
            throw new DataException(2, "Data file '" + var10 + "' doesn't exist after computation! ");
         }
      }

      int var11 = var9.isTickBar() ? 2 : 1;
      return new BinaryDataLoader(var10, var1, var11, var7);
   }

   public static void renameDataFiles(String var0, String var1, String var2) throws Exception {
      DataInfo var3 = getDataInfo("History", var1);
      boolean var4 = var3.source == 6 || var3.source == 5;
      if (var4) {
         for (String var9 : TimeframeManager.getTimeframes()) {
            get();
            String var5 = getDataFileName(var0, var1, var9, null);
            String var6 = getDataFileName(var0, var2, var9, null, var3);
            File var7 = new File(var5);
            if (Files.exists(var7.toPath())) {
               try {
                  FileUtils.copyFile(new File(var5), new File(var6));
               } catch (Exception var16) {
                  throw new Exception("Failed to copy data file '" + var5 + "' to '" + var6 + "'.");
               }
            }

            var5 = getUnadjustedDataFileName(var0, var1, var9, null);
            var6 = getUnadjustedDataFileName(var0, var2, var9, null, var3);
            var7 = new File(var5);
            if (Files.exists(var7.toPath())) {
               try {
                  FileUtils.copyFile(new File(var5), new File(var6));
               } catch (Exception var15) {
                  throw new Exception("Failed to copy unadjusted data file '" + var5 + "' to '" + var6 + "'.");
               }
            }
         }
      } else {
         for (String var25 : TimeframeManager.getTimeframes()) {
            for (Session var11 : SessionManager.getSessions()) {
               String var18 = getDataFileName(var0, var1, var25, var11.getSessionName());
               String var20 = getDataFileName(var0, var2, var25, var11.getSessionName(), var3);
               File var22 = new File(var18);
               if (Files.exists(var22.toPath())) {
                  try {
                     FileUtils.copyFile(new File(var18), new File(var20));
                  } catch (Exception var14) {
                     throw new Exception("Failed to copy data file '" + var18 + "' to '" + var20 + "'.");
                  }
               }
            }
         }
      }

      File var23 = new File(getSymbolFolderName(SQPaths.dataDirPath, var0, var1));

      try {
         FileUtils.deleteDirectory(var23);
      } catch (Exception var13) {
         throw new Exception("Failed to delete orig symbol directory '" + var23.getAbsolutePath() + "'");
      }
   }

   public static void removeDataFiles(String var0, String var1) {
      for (String var3 : TimeframeManager.getTimeframes()) {
         for (Session var5 : SessionManager.getSessions()) {
            String var6 = getDataFileName(var0, var1, var3, var5.getSessionName());
            File var7 = new File(var6);

            try {
               Files.deleteIfExists(var7.toPath());
            } catch (IOException var12) {
               Log.error("Unable to delete file " + var6 + ".", var12);
            }

            String var8 = getUnadjustedDataFileName(var0, var1, var3, var5.getSessionName());
            File var9 = new File(var8);

            try {
               Files.deleteIfExists(var9.toPath());
            } catch (IOException var11) {
               Log.error("Unable to delete file " + var8 + ".", var11);
            }
         }
      }
   }

   public static void removeDataFilesWithFolder(String var0, String var1) {
      File var2 = new File(getSymbolFolderName(SQPaths.dataDirPath, var0, var1));

      try {
         deleteFolderExcept(var2);
      } catch (Exception var4) {
         Log.error("Unable to delete file " + var2.getAbsolutePath() + ".", var4);
      }
   }

   public static String getSymbolFolderName(String var0, String var1, String var2) {
      DataInfo var3 = getDataInfo(var1, var2);
      if (var3 == null) {
         Log.error("Info of symbol {} was not found for connection {}", var2, var1);
         throw new IllegalArgumentException("Symbol was not found: " + var2);
      }

      boolean var4 = var3.source == 6;
      boolean var5 = var3.source == 5;
      String var6 = fixFilename(var2);
      String var7 = var1;
      if (var4) {
         var7 = var1 + "/" + "sq_futures" + "/" + var6.charAt(0);
      } else if (var5) {
         var7 = var1 + "/" + "sq_equity" + "/" + var6.charAt(0);
      }

      return var0 + "/" + var7 + "/" + var6;
   }

   public static String getSymbolFolderName(String var0, String var1, String var2, DataInfo var3) {
      boolean var4 = var3.source == 6;
      boolean var5 = var3.source == 5;
      String var6 = fixFilename(var2);
      String var7 = var1;
      if (var4) {
         var7 = var1 + "/" + "sq_futures" + "/" + var6.charAt(0);
      } else if (var5) {
         var7 = var1 + "/" + "sq_equity" + "/" + var6.charAt(0);
      }

      return var0 + "/" + var7 + "/" + var6;
   }

   private boolean _dataFileExists(String var1) {
      File var2 = new File(var1);
      boolean var3 = Files.exists(var2.toPath());
      Log.debug("Checking if data file exists: " + var1 + ". Exists : " + (var3 ? "Yes" : "No"));
      return var3;
   }

   String _getTimeframeToLoad(String var1, String var2, int var3) throws Exception {
      String var4 = null;
      if (var1 == null) {
         throw new Exception("Selected symbol has no base timeframe");
      }

      BarType var5 = BarTypeFactory.getBarType(var2, 1);
      BarType var6 = BarTypeFactory.getBarType(var1, 1);
      String var7 = var5.checkCanBeComputedFrom(var1);
      if (var7 != null) {
         throw new DataException(2, var7);
      }

      if (var3 == 3 || var3 == 4) {
         String var8 = var5.getTickTF();
         BarType var9 = BarTypeFactory.getBarType(var8, 1);
         var7 = var9.checkCanBeComputedFrom(var1);
         if (var7 == null) {
            var4 = var8;
         } else {
            var3 = 2;
         }
      }

      if (var4 == null && var3 == 2) {
         String var12 = var5.getBaseTF();
         BarType var13 = BarTypeFactory.getBarType(var12, 1);
         var7 = var13.checkCanBeComputedFrom(var1);
         if (var7 == null) {
            var4 = var12;
         } else {
            var3 = 1;
         }
      }

      if (var4 == null && var3 == 1) {
         var4 = var2;
      }

      if (var4 == null && var3 == 5) {
         var4 = var1.equals("TICK") ? "TICK" : var2;
      }

      Log.debug(
         "Requested timeframe: " + var2 + ", imported data timeframe: " + var1 + ", precision: " + Precisions.toString(var3) + ", real used timeframe: " + var4
      );
      return var4;
   }

   private IDataLoader _getSimpleDataLoader(String var1) {
      if (!var1.contains("simpletest1:") && var1.contains("simpletest2:")) {
      }

      return null;
   }

   void _computeData(String var1, DataInfo var2, String var3) throws Exception {
      this._computeData(var1, var2, var3, null);
   }

   void _computeData(String var1, DataInfo var2, String var3, IStopPauseStatus var4) throws Exception {
      BarType var5 = BarTypeFactory.getBarType(var1, var2.barTimeType);
      Session var6 = SessionManager.getSession(var3);
      if (var6 == null) {
         var6 = SessionManager.getSession("No Session");
      }

      BinaryDataLoader var7 = this._getSourceLoader(var2, var3);
      if (var7 != null) {
         if (var1.equals(var2.timeframe)) {
            String var8 = var2.connection;
            String var9 = var2.symbol;
            String var10 = var2.timeframe;
            String var11 = this._getDataFileName(var8, var9, var10, var3);
            if (this._dataFileExists(var11)) {
               return;
            }
         }

         var7.open();
         boolean var12 = var7.isCrypted();
         DataBinWriterNew var13 = this._getTargetWriter(var5, var1, var2, var3, var12);
         var13.open();
         boolean var14 = false;
         if (var5.isTickBar()) {
            var14 = this.processTickDataTarget(var7, var13, var5, var6, var4);
         } else {
            var14 = this.processOHLCDataTarget(var7, var13, var5, var6, var4);
         }

         var13.close();
         var7.close();
         if (!var14) {
            File var16 = new File(var13.getFileName());
            if (!var16.delete()) {
               Log.error("Cannot delete data file " + var16.getAbsolutePath());
            }

            throw new TaskStoppedException();
         }
      }
   }

   private boolean processOHLCDataTarget(BinaryDataLoader var1, DataBinWriterNew var2, BarType var3, Session var4, IStopPauseStatus var5) throws Exception {
      VersatileData var6 = new VersatileData();
      TickEvent var7 = new TickEvent();
      VersatileData var8 = new VersatileData();
      BarTypeStatus var9 = new BarTypeStatus();
      String var10 = var3.getTimeframe();
      var9.convertingToHigherTF = true;
      var3 = var3.clone(var3.getTimeframe());
      DefaultTickSimulator var11 = new DefaultTickSimulator();
      SessionStatus var12 = new SessionStatus();
      var4.clearSessionTempData();
      boolean var13 = false;
      long var14 = 0L;
      long var16 = Long.MIN_VALUE;
      int var18 = 0;

      while (var1.hasNextTick()) {
         if (var5 != null && var5.isStopped()) {
            return false;
         }

         var14++;
         var1.getNextTick(var6);
         var11.init(var6);

         while (var11.getNextTick(var7)) {
            if (var10.equals("D1") || var10.equals("Weekly") || var10.equals("Monthly")) {
               var4.fixD1DataTime(var7);
            }

            var4.checkTimeIsInSession(var7.getTime(), var12, var10);
            if (!var12.isInSession) {
               var9.status = 0;
            } else {
               var7.setSessionStartTime(var12.sessionStartTime);
               var7.setSessionEndTime(var12.sessionEndTime);
               var16 = var12.sessionEndTime;
               var3.processTick(var7, var9, 1);
            }

            if (var9.status == 0) {
               if (var13) {
                  if (var8.time > var16 && var16 > Long.MIN_VALUE) {
                     var8.time = var16;
                  }

                  this.writeData(var2, var8);
                  var13 = false;
               }
            } else if (var9.status == 1) {
               var18++;
               this.writeData(var2, var8);
               this.initBarData(var8, var9.barTime, var7.getBid(), var7.getVolume());
               var13 = true;
            } else {
               this.updateBarData(var8, var7.getBid(), var7.getVolume());
            }
         }
      }

      if (var13) {
         this.writeData(var2, var8);
      }

      Log.debug("Processed {} ticks, created {} bars", var14, var18);
      return true;
   }

   private void writeData(DataBinWriterNew var1, VersatileData var2) throws Exception {
      if (var2.time != Long.MIN_VALUE) {
         var2.volume = var2.volume > 1.0 ? SQUtils.roundLong(var2.volume).longValue() : 1.0;
         var1.writeData(var2);
         var2.time = Long.MIN_VALUE;
      }
   }

   private void updateBarData(VersatileData var1, double var2, double var4) {
      if (var2 > var1.high) {
         var1.high = var2;
      }

      if (var2 < var1.low) {
         var1.low = var2;
      }

      var1.close = var2;
      var1.volume += var4;
   }

   private void initBarData(VersatileData var1, long var2, double var4, double var6) {
      var1.time = var2;
      var1.open = var4;
      var1.high = var4;
      var1.low = var4;
      var1.close = var4;
      var1.volume = var6;
   }

   private boolean processTickDataTarget(BinaryDataLoader var1, DataBinWriterNew var2, BarType var3, Session var4, IStopPauseStatus var5) throws IOException, Exception {
      VersatileData var6 = new VersatileData();

      while (var1.hasNextTick()) {
         if (var5 != null && var5.isStopped()) {
            return false;
         }

         var1.getNextTick(var6);
         var2.writeData(var6);
      }

      return true;
   }

   private DataBinWriterNew _getTargetWriter(BarType var1, String var2, DataInfo var3, String var4, boolean var5) {
      DataBinWriterNew var6;
      if (var1.isTickBar()) {
         var6 = var5
            ? DataBinWriterNew.getCryptedInstance(2, this.getDbPath(), var3.symbolInfo)
            : DataBinWriterNew.getInstance(2, this.getDbPath(), var3.symbolInfo);
      } else {
         var6 = var5
            ? DataBinWriterNew.getCryptedInstance(1, this.getDbPath(), var3.symbolInfo)
            : DataBinWriterNew.getInstance(1, this.getDbPath(), var3.symbolInfo);
      }

      var6.setFileName(getDataFileName(var3.connection, var3.symbol, var2, var4));
      return var6;
   }

   private BinaryDataLoader _getSourceLoader(DataInfo var1, String var2) throws DataException {
      String var3 = var1.connection;
      String var4 = var1.symbol;
      String var5 = var1.timeframe;
      String var6 = this._getDataFileName(var3, var4, var5, var2);
      if (!this._dataFileExists(var6)) {
         var6 = this._getDataFileName(var3, var4, var5, "No Session");
         Log.debug("No data file for " + var3 + " / " + var4 + " / " + var5 + " / " + var2);
      }

      if (MainApp.getProduct() == "BACKTESTNODE" && !this._dataFileExists(var6)) {
         Log.info("Data not found, copying data file from NFS disk " + var6 + "...");
         String var7 = var6;
         Log.debug("Trying to get source data file from NFS disk...");
         String var8 = System.getProperty("NFSDataPath");
         if (var8 == null) {
            throw new DataException(2, "Unable to load data for " + var3 + " / " + var4 + " / " + var5 + " / " + var2 + " - NFSDataPath property not set");
         }

         String var9 = SQPaths.dataDirPath.replace("\\", "/");
         var7 = var7.replace("\\", "/").replace(var9, var8);
         if (!this._dataFileExists(var7)) {
            throw new DataException(2, "Source data file not found on NFS disk (" + var7 + ") - DataPath: " + var9 + ", NFSDataPath: " + var8);
         }

         try {
            long var10 = System.currentTimeMillis();
            Log.debug("Copying data file from NFS disk " + var7 + " to " + var6 + "...");
            File var12 = new File(var6);
            var12.getParentFile().mkdirs();
            File var13 = new File(var7);
            Files.copy(var13.toPath(), var12.toPath());
            long var14 = System.currentTimeMillis() - var10;
            Log.debug("Copying data took " + var14 + "ms");
            if (var13.length() != var12.length()) {
               throw new Exception(
                  String.format("Error while copying data file from NFS disk. NFS file size %d <> %d copied file size", var13.length(), var12.length())
               );
            }

            Log.debug("Copying data success, file size " + var12.length());
         } catch (Exception var16) {
            Log.error("Unable to copy file " + var7 + " to " + var6, var16);
            throw new DataException(2, "Unable to copy file " + var7 + " from NFS disk");
         }
      }

      if (!this._dataFileExists(var6)) {
         throw new DataException(2, "There is no source data file for " + var3 + " / " + var4 + " / " + var5 + " / " + var2);
      }

      BarType var18 = BarTypeFactory.getBarType(var5, var1.barTimeType);
      int var19 = var18.isTickBar() ? 2 : 1;
      ChartDef var20 = new ChartDef(var1.connection, var1.symbol, var1.timeframe, 0L, SQTimeOld.toLong(2100, 1, 1), 0.0, var2);
      return new BinaryDataLoader(var6, var20, var19, var1);
   }

   public static void exportTick(String var0, long var1, long var3, String var5) throws Exception {
      new DataExporter().exportTick(var0, var1, var3, var5);
   }

   public static void exportM1(String var0, long var1, long var3, String var5) throws Exception {
      new DataExporter().exportM1(var0, var1, var3, var5);
   }

   private void updateUnderlyingSymbol(String var1, String var2, String var3) {
      try {
         String var4 = "UPDATE DATA SET USYMBOL='" + var3 + "', USYMBOLNAME='" + var3 + "'";
         var4 = var4 + " WHERE CONNECTION='" + var1 + "' AND SYMBOL='" + var2 + "'";
         this.sqlCommand(var4);
         DataInfo var5 = this.getRecordFromCache(var1, var2);
         var5.uSymbol = var3;
         this.updateCachedDataList(var5);
      } catch (Exception var6) {
         Log.error("Data DB error:", var6);
      }
   }

   private void _updateUnderlyingSymbolName(String var1, String var2, String var3) {
      try {
         var3 = var3.replace("'", "''");
         String var4 = "UPDATE DATA SET USYMBOLNAME='" + var3 + "'";
         var4 = var4 + " WHERE CONNECTION='" + var1 + "' AND SYMBOL='" + var2 + "'";
         this.sqlCommand(var4);
         DataInfo var5 = this.getRecordFromCache(var1, var2);
         var5.uSymbolName = var3;
         this.updateCachedDataList(var5);
      } catch (Exception var6) {
         Log.error("Data DB error:", var6);
      }
   }

   private void _showData(String var1, String var2, boolean var3) {
      try {
         String var4 = "UPDATE DATA SET SHOW=" + (var3 ? 1 : 0);
         var4 = var4 + " WHERE CONNECTION='" + var1 + "' AND SYMBOL='" + var2 + "'";
         this.sqlCommand(var4);
         DataInfo var5 = this.getRecordFromCache(var1, var2);
         var5.show = var3;
         this.updateCachedDataList(var5);
      } catch (Exception var6) {
         Log.error("Data DB error:", var6);
      }
   }

   public static void checkData() {
      try {
         ArrayList var0 = get()._list();

         for (int var1 = 0; var1 < var0.size(); var1++) {
            DataInfo var2 = (DataInfo)var0.get(var1);
            if (var2.rows > 0 && var2.secondsRecords == 0L) {
               updateSecondsRecords(var2);
            }

            if (var2.source == 2 && var2.uSymbol == null) {
               get().updateUnderlyingSymbol(var2.connection, var2.symbol, var2.instrument);
            }
         }
      } catch (Exception var3) {
         Log.error("Error while checking data. Exc.", var3);
      }
   }

   public static void updateSecondsRecords(DataInfo var0) throws Exception {
      long var1 = 0L;
      if (var0.timeframe.equals("TICK")) {
         var1 = var0.totalDays * 86400;
      } else {
         var1 = var0.rows * 60;
      }

      updateSecondsRecords(var0.connection, var0.symbol, var1);
   }

   public static void checkDataExport(ICryptable var0, int var1) throws Exception {
      HistoryDataChecker.checkDataExport(var0, var1);
   }

   public static String generateName(String var0) {
      int var1 = var0.lastIndexOf("(");
      int var2 = var0.length() - 1;
      int var3 = 0;

      try {
         String var4 = var0.substring(var1 + 1, var2);
         var3 = Integer.parseInt(var4);
      } catch (Exception var5) {
      }

      if (var3 > 0) {
         return var0.substring(0, var1) + "(" + (var3 + 1) + ")";
      }

      var1 = var1 < 0 ? var0.length() : var1;
      return var0.substring(0, var1) + "(2)";
   }

   public static String getTempFileName(String var0) {
      int var1 = var0.lastIndexOf(".");
      String var2 = var0.substring(0, var0.lastIndexOf("."));
      String var3 = var0.substring(var1);
      return var2 + "_temp" + var3;
   }

   public static boolean createGroupAlias(String var0, int var1) throws Exception {
      DataInfo var2 = getDataInfo("History", var0);
      if (var2 == null) {
         if (!InstrumentManager.checkInstrumentExists(var0)) {
            InstrumentManager.addInstrument(var0, var0, 1.0, 0.01, 0.01, 0.0, 0.0, "<Method type=\"None\" use=\"true\"><Params/></Method>", (byte)1);
         }

         addData("History", var0, var0, 1, 5, var1, -1);
         updateData("History", var0, 0L, 0L, 1000000, 0L, 1, "D1", "Etc/UCT");
         return true;
      } else {
         if (var2.rows == 0) {
            updateData("History", var0, 0L, 0L, 1000000, 0L, 1, "D1", "Etc/UCT");
         }

         return false;
      }
   }

   public static boolean isGroupAlias(String var0) {
      return var0 == null ? false : var0.startsWith("[") && var0.endsWith("]");
   }

   public static void deleteGroupAlias(int var0, String var1) {
      try {
         deleteForGroup("History", var1, var0);
         InstrumentManager.removeInstrument(var1);
      } catch (Exception var3) {
         Log.error("Data DB error:", var3);
      }
   }

   public static HistoryOHLCData getHistoryData(String var0, String var1, long var2, long var4, String var6) throws HistoryDataNotAvailableExeption {
      return get()._getHistoryData(var0, var1, var2, var4, var6);
   }

   private synchronized HistoryOHLCData _getHistoryData(String var1, String var2, long var3, long var5, String var7) throws HistoryDataNotAvailableExeption {
      HistoryOHLCData var8 = new HistoryOHLCData(var1, var2, var3, var5, var7);
      DataInfo var9 = this._getDataInfo("History", var1);
      if (var9 == null) {
         throw new HistoryDataNotAvailableExeption(String.format("Symbol '%s' is not recognized!", var1));
      }

      try {
         TimeframeManager.getMillis(var2);
      } catch (Exception var14) {
         throw new HistoryDataNotAvailableExeption(String.format("Timeframe '%s' for symbol '%s' does not exist!", var2, var1));
      }

      try {
         ChartDef var10 = new ChartDef("History", var1, var2, var3, var5, 2.5, var7);
         IDataLoader var11 = this._getDataLoader(var10, 1, null);
         var11.open();
         if (var11.isCrypted()) {
            throw new HistoryDataNotAvailableExeption(String.format("Data for symbol '%s' are protected and cannot be loaded!", var1));
         }

         List var12 = this._loadHistoryData(var11, var3, var5);
         this._fillHistoryData(var8, var12);
         return var8;
      } catch (Exception var13) {
         Log.error("Error while loading history data", var13);
         throw new HistoryDataNotAvailableExeption("Error while loading data.", var13);
      }
   }

   private void _fillHistoryData(HistoryOHLCData var1, List<VersatileData> var2) {
      var1.Open = new float[var2.size()];
      var1.Close = new float[var2.size()];
      var1.High = new float[var2.size()];
      var1.Low = new float[var2.size()];
      var1.Volume = new float[var2.size()];
      var1.Time = new long[var2.size()];

      for (int var3 = 0; var3 < var2.size(); var3++) {
         VersatileData var4 = (VersatileData)var2.get(var3);
         var1.Open[var3] = (float)var4.open;
         var1.Close[var3] = (float)var4.close;
         var1.High[var3] = (float)var4.high;
         var1.Low[var3] = (float)var4.low;
         var1.Volume[var3] = (float)var4.volume;
         var1.Time[var3] = var4.time;
      }
   }

   private List<VersatileData> _loadHistoryData(IDataLoader var1, long var2, long var4) throws Exception {
      LinkedList var6 = new LinkedList();

      while (var1.hasNextTick()) {
         VersatileData var7 = new VersatileData();
         var1.getNextTick(var7);
         if (var4 != -1L && var7.time > var4) {
            break;
         }

         if (var2 == -1L || var7.time >= var2) {
            var6.add(var7);
         }
      }

      return var6;
   }

   public static DataInfo addCustomData(String var0, String var1, int var2, String var3) throws Exception {
      String var4 = "SingleAssetCloud";
      if (!InstrumentManager.checkInstrumentExists(var4)) {
         InstrumentManager.addInstrument(var4, var4, 1.0, 0.01, 0.01, 0.0, 0.0, "<Method type=\"None\" use=\"true\"><Params/></Method>", (byte)1);
      }

      InstrumentInfo var5 = InstrumentManager.getInstrumentInfo(var4);
      DataInfo var6 = new DataInfo();
      var6.id = -1;
      var6.symbol = var1;
      var6.connection = var0;
      var6.instrument = var4;
      var6.basketId = var2;
      var6.symbolInfo = var5;
      var6.originalSymbol = var3;
      var6.rows = 100000;
      var6.dateFrom = SQTime.toLong(1980, 1, 1);
      var6.dateTo = System.currentTimeMillis();
      String var7 = var0 + var1;
      get().customDataInfoCache.put(var7, var6);
      return var6;
   }

   public static void cleanHigherTFFiles(String var0) {
   }
}
