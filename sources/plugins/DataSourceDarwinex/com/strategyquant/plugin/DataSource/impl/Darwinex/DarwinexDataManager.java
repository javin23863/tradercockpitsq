package com.strategyquant.plugin.DataSource.impl.Darwinex;

import com.strategyquant.datalib.InstrumentInfo;
import com.strategyquant.datalib.SymbolData;
import com.strategyquant.datalib.broker.BrokerDto;
import com.strategyquant.datalib.broker.BrokerManager;
import com.strategyquant.datalib.data.DataManager;
import com.strategyquant.datalib.instrument.InstrumentManager;
import com.strategyquant.lib.L;
import com.strategyquant.lib.SQTime;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.app.MainApp;
import com.strategyquant.lib.constants.SQPaths;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DarwinexDataManager {
   public static final Logger Log = LoggerFactory.getLogger(DarwinexDataManager.class);
   public static final String DOWNLOAD_DARWINEX_SYMBOL_JOB = "DownloadDarwinexSymbolJob_";
   private static final Set<String> FREE_SYMBOLS = Set.of(
      "EURUSD",
      "GBPUSD",
      "USDJPY",
      "USDCHF",
      "AUDUSD",
      "NZDUSD",
      "EURJPY",
      "GBPJPY",
      "CADJPY",
      "INDEXY",
      "NDXm",
      "WS30",
      "SP500",
      "SPXm",
      "XTIUSD",
      "XAUUSD",
      "GDAXIm"
   );
   private static final String dataListPath = SQPaths.pluginsDirPath + "/DataSourceDarwinex/darwinex.csv";
   private static final String dataListLineDelimiter = "\n";
   private static final String dataListValueDelimiter = ";";
   private static final String dataListDateFormat = "dd.MM.yyyy";
   private ArrayList<SymbolData> availableData = new ArrayList<>();
   private Map<String, SymbolData> availableDataMap = new HashMap<>();
   private static ReentrantLock lock = new ReentrantLock();
   private static DarwinexDataManager instance;

   public static synchronized DarwinexDataManager get() {
      if (instance == null) {
         try {
            lock.lock();
            if (instance == null) {
               instance = new DarwinexDataManager();
            }
         } finally {
            lock.unlock();
         }
      }

      return instance;
   }

   private DarwinexDataManager() {
      this.loadSettings();
      this.loadAvailableData();
   }

   public boolean canFreeDownloadFromCdn(String var1) {
      return FREE_SYMBOLS.contains(var1);
   }

   private void loadSettings() {
      boolean var1 = false;
      if (var1) {
         MainApp.settings().save();
      }
   }

   private void loadAvailableData() {
      try {
         this.availableData.clear();
         this.availableDataMap.clear();
         File var1 = new File(dataListPath);
         String[] var2 = SQUtils.fileToString(var1).split("\n");
         int var3 = 0;

         for (String var7 : var2) {
            String[] var8 = var7.split(";");
            var3++;
            if (var8.length == 8) {
               try {
                  SymbolData var9 = new SymbolData();
                  var9.symbol = var8[0].trim();
                  var9.name = var9.symbol;
                  var9.dateFrom = SQTime.parseToMilis(var8[1].trim(), "dd.MM.yyyy");
                  var9.decimals = Integer.parseInt(var8[2].trim());
                  var9.tickValue = Double.parseDouble(var8[3].trim());
                  var9.defaultSpread = Double.parseDouble(var8[4].trim());
                  var9.tickSize = Double.parseDouble(var8[5].trim());
                  var9.tickStep = Double.parseDouble(var8[6].trim());
                  var9.instrumentType = Byte.parseByte(var8[7].trim());
                  this.availableData.add(var9);
                  this.availableDataMap.put(var9.symbol, var9);
               } catch (Exception var10) {
                  Log.error("Cannot read Darwinex data file line " + var3 + ". " + var10.getMessage());
               }
            }
         }
      } catch (Exception var11) {
         Log.error("Cannot read Darwinex data list file. ", var11);
      }
   }

   public ArrayList<SymbolData> getAvailableDataList() {
      lock.lock();

      try {
         return get().availableData;
      } finally {
         lock.unlock();
      }
   }

   public SymbolData getAvailableDataInfo(String var1) {
      lock.lock();

      try {
         return this.availableDataMap.get(var1);
      } finally {
         lock.unlock();
      }
   }

   public String addData(String var1, String var2, int var3, String var4) throws Exception {
      BrokerDto var5 = BrokerManager.getInstance().getBroker(var3);
      if (var5 == null) {
         throw new RuntimeException("Specified broker was not found");
      }

      String var6 = var1 + var2;
      String var7 = var4 == null ? var1 : var4;
      lock.lock();

      try {
         SymbolData var8 = this.getAvailableDataInfo(var1);
         if (var8 == null) {
            throw new Exception(L.t("No info found for symbol %s. Check Darwinex config file %s.", new Object[]{dataListPath}));
         }

         while (DataManager.checkDataExists("History", var6)) {
            var6 = this.generateName(var6);
         }

         if (!InstrumentManager.checkInstrumentExists(var7)) {
            if (var4 != null) {
               throw new RuntimeException(L.t("Instrument %s for broker %s doesn't exists. Symbol can't be added.", new Object[]{var7, var5.getName()}));
            }

            InstrumentManager.addInstrument(
               var7,
               "Darwinex instrument",
               var8.tickValue,
               var8.tickSize,
               var8.tickStep,
               var8.defaultSpread,
               var8.defaultSlippage,
               "<Method type=\"None\" use=\"true\"><Params/></Method>",
               var8.instrumentType,
               null,
               null,
               null,
               null,
               1.0,
               0.0
            );
         } else if (var3 == -1) {
            InstrumentInfo var9 = InstrumentManager.getInstrumentInfo(var7);
            if (var9.tickSize != var8.tickSize || var9.tickStep != var8.tickStep) {
               while (InstrumentManager.checkInstrumentExists(var7)) {
                  var7 = this.generateName(var7);
               }

               InstrumentManager.addInstrument(
                  var7,
                  var3,
                  "Darwinex instrument",
                  var8.tickValue,
                  var8.tickSize,
                  var8.tickStep,
                  var8.defaultSpread,
                  var8.defaultSlippage,
                  0.0,
                  "<Method type=\"None\" use=\"true\"><Params/></Method>",
                  var8.instrumentType,
                  null,
                  null,
                  null,
                  null,
                  1.0,
                  0.0
               );
            }
         }

         DataManager.addData("History", var6, var7, 1, 4, var1, var8.name, -1, var3);
      } finally {
         lock.unlock();
      }

      return var6;
   }

   private String generateName(String var1) {
      int var2 = var1.lastIndexOf("(");
      int var3 = var1.length() - 1;
      int var4 = 0;

      try {
         String var5 = var1.substring(var2 + 1, var3);
         var4 = Integer.parseInt(var5);
      } catch (Exception var6) {
      }

      if (var4 > 0) {
         return var1.substring(0, var2) + "(" + (var4 + 1) + ")";
      }

      var2 = var2 < 0 ? var1.length() : var2;
      return var1.substring(0, var2) + "(2)";
   }
}
