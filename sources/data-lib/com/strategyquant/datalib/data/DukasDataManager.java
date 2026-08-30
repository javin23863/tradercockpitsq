package com.strategyquant.datalib.data;

import com.strategyquant.datalib.DataInfo;
import com.strategyquant.datalib.InstrumentInfo;
import com.strategyquant.datalib.SymbolData;
import com.strategyquant.datalib.broker.BrokerDto;
import com.strategyquant.datalib.broker.BrokerManager;
import com.strategyquant.datalib.historyData.dto.TickerDto;
import com.strategyquant.datalib.historyData.dto.TickerKind;
import com.strategyquant.datalib.instrument.InstrumentManager;
import com.strategyquant.lib.L;
import com.strategyquant.lib.SQTime;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.app.MainApp;
import com.strategyquant.lib.constants.SQPaths;
import java.io.File;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DukasDataManager {
   public static final Logger Log = LoggerFactory.getLogger(DukasDataManager.class);
   public static Set<String> FREE_M1_SYMBOLS = Set.of(
      "EURUSD", "GBPUSD", "USDJPY", "USDCHF", "AUDUSD", "NZDUSD", "EURJPY", "GBPJPY", "CADJPY", "INDEXY", "US500", "USATECH", "USA30", "WTI", "BRENT", "XAUUSD"
   );
   private static final String dataListPath = SQPaths.pluginsDirPath + "/DataSourceDukascopy/dukascopy.csv";
   private static final String dataListLineDelimiter = "\n";
   private static final String dataListValueDelimiter = ";";
   private static final String dataListDateFormat = "dd.MM.yyyy";
   private ArrayList<SymbolData> availableData = new ArrayList<>();
   private Map<String, SymbolData> availableDataMap = new HashMap<>();
   private static ReentrantLock lock = new ReentrantLock();
   private static DukasDataManager instance;

   public static synchronized DukasDataManager get() {
      if (instance == null) {
         try {
            lock.lock();
            if (instance == null) {
               instance = new DukasDataManager();
            }
         } finally {
            lock.unlock();
         }
      }

      return instance;
   }

   private DukasDataManager() {
      this.loadSettings();
      this.loadAvailableData();
   }

   public boolean canFreeDownloadFromCdn(DataInfo var1) {
      return FREE_M1_SYMBOLS.contains(var1.uSymbolName) && var1.timeframe.equals("M1");
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
            if (var8.length == 12) {
               try {
                  SymbolData var9 = new SymbolData();
                  var9.symbol = var8[0].trim();
                  var9.name = var8[1].trim();
                  var9.category = var8[2].trim();
                  var9.subcategory = var8[3].trim();
                  var9.dateFrom = SQTime.parseToMilis(var8[4].trim(), "dd.MM.yyyy");
                  var9.dateFromM1 = SQTime.parseToMilis(var8[5].trim(), "dd.MM.yyyy");
                  var9.decimals = Integer.parseInt(var8[6].trim());
                  var9.tickValue = Double.parseDouble(var8[7].trim());
                  var9.defaultSpread = Double.parseDouble(var8[8].trim());
                  var9.tickSize = Double.parseDouble(var8[9].trim());
                  var9.tickStep = Double.parseDouble(var8[10].trim());
                  var9.instrumentType = Byte.parseByte(var8[11].trim());
                  this.availableData.add(var9);
                  this.availableDataMap.put(var9.symbol, var9);
               } catch (Exception var10) {
                  Log.error("Cannot read dukascopy data file line " + var3 + ". " + var10.getMessage());
               }
            }
         }
      } catch (Exception var11) {
         Log.error("Cannot read dukascopy data list file. ", var11);
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

   public String addData(TickerDto var1, String var2, String var3, TickerKind var4, String var5, byte var6, double var7, double var9, double var11, int var13) throws Exception {
      String var14 = var1.getTicker();
      String var15 = var14 + var2;
      lock.lock();

      try {
         while (DataManager.checkDataExists("History", var15)) {
            var15 = DataManager.generateName(var15);
         }

         double var16 = 0.0;
         double var18 = 0.0;
         String var20 = "History data instrument";
         if (!InstrumentManager.checkInstrumentExists(var5)) {
            InstrumentManager.addInstrument(
               var5, var20, var11, var9, var7, var16, var18, "<Method type=\"None\" use=\"true\"><Params/></Method>", var6, null, null, null, null, 1.0, 0.0
            );
         } else {
            InstrumentInfo var21 = InstrumentManager.getInstrumentInfo(var5);
            if (var21.dataType != var6) {
               InstrumentManager.updateDataType(var5, var6);
            }

            if (var21.tickSize != var9 || var21.tickStep != var7) {
               while (InstrumentManager.checkInstrumentExists(var5)) {
                  var5 = DataManager.generateName(var5);
               }

               InstrumentManager.addInstrument(
                  var5,
                  var20,
                  var11,
                  var9,
                  var7,
                  var16,
                  var18,
                  "<Method type=\"None\" use=\"true\"><Params/></Method>",
                  var6,
                  var1.getMarketName(),
                  null,
                  null,
                  null,
                  1.0,
                  0.0
               );
            }
         }

         int var26 = var4 == TickerKind.FUTURES ? 6 : 5;
         String var22 = var1.getName() != null ? var1.getName() : var1.getTicker();
         DataManager.addData("History", var15, var5, var13, var26, var14, var22);
         DataManager.updateTimeframe("History", var15, var3);
      } finally {
         lock.unlock();
      }

      return var15;
   }

   private double getTickerValue(BigDecimal var1) {
      if (var1 == null) {
         throw new RuntimeException(
            L.t("Symbol is not set properly - some necessary attributes are missing (tick step,tick size, point value, default spread)", new Object[0])
         );
      } else {
         return var1.doubleValue();
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
            throw new Exception("No info found for symbol '" + var1 + "'.");
         }

         while (DataManager.checkDataExists("History", var6)) {
            var6 = DataManager.generateName(var6);
         }

         if (!InstrumentManager.checkInstrumentExists(var7)) {
            if (var4 != null) {
               throw new RuntimeException(L.t("Instrument %s for broker %s doesn't exists. Symbol can't be added.", new Object[]{var7, var5.getName()}));
            }

            InstrumentManager.addInstrument(
               var7,
               "Dukascopy instrument",
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
                  var7 = DataManager.generateName(var7);
               }

               InstrumentManager.addInstrument(
                  var7,
                  var3,
                  "Dukascopy instrument",
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

         DataManager.addData("History", var6, var7, 1, 2, var1, var8.name, -1, var3);
      } finally {
         lock.unlock();
      }

      return var6;
   }

   public void addBatch(List<DataInfo> var1, Map<String, TickerDto> var2, InstrumentValueEvaluator var3, BatchProgressController var4) throws Exception {
      Map var5 = InstrumentManager.list().stream().collect(Collectors.toMap(var0 -> var0.instrument, var0 -> (InstrumentInfo)var0));
      Set var6 = DataManager.list().stream().map(var0 -> var0.symbol).collect(Collectors.toSet());
      LinkedList var7 = new LinkedList();

      for (DataInfo var9 : var1) {
         TickerDto var10 = (TickerDto)var2.get(var9.uSymbol);
         String var11 = var9.symbol;
         Log.debug("Adding symbol: {}", var11);

         while (var6.contains(var11)) {
            var11 = DataManager.generateName(var11);
            Log.debug("Generated new symbol: {}", var11);
         }

         var9.symbol = var11;
         var6.add(var11);
         String var12 = var10.getName() != null ? var10.getName() : var10.getTicker();
         var9.uSymbol = var10.getTicker();
         var9.uSymbolName = var12;
         var9.timeframe = var10.isEod() ? "D1" : "M1";
         byte var13 = var3.getInstrumentType(var10);
         double var14 = var3.getTickSize(var10);
         double var16 = var3.getTickStep(var10);
         double var18 = var3.getPointValue(var10);
         double var20 = var3.getOrderSizeMultiplier(var10);
         double var22 = var3.getOrderSizeStep(var10);
         if (!var5.containsKey(var9.instrument)) {
            InstrumentInfo var30 = new InstrumentInfo();
            var30.description = var3.getDescriptions(var10);
            var30.instrument = var9.instrument;
            var30.tickStep = var16;
            var30.tickSize = var14;
            var30.pointValue = var18;
            var30.orderSizeMultiplier = var20;
            var30.orderSizeStep = var22;
            var30.commissions = "<Method type=\"None\" use=\"true\"><Params/></Method>";
            var30.exchange = var10.getMarketName();
            var30.dataType = var13;
            var7.add(var30);
            var5.put(var30.instrument, var30);
         } else {
            InstrumentInfo var24 = (InstrumentInfo)var5.get(var9.instrument);
            if (!this.matchInstrument(var24, var14, var16, var20, var22, var13)) {
               String var25 = var9.instrument;
               boolean var26 = false;

               while (true) {
                  if (var5.containsKey(var25)) {
                     var25 = DataManager.generateName(var25);
                     InstrumentInfo var27 = (InstrumentInfo)var5.get(var25);
                     if (var27 == null || !this.matchInstrument(var27, var14, var16, var20, var22, var13)) {
                        continue;
                     }

                     var26 = true;
                     var9.instrument = var25;
                  }

                  if (!var26) {
                     InstrumentInfo var31 = new InstrumentInfo();
                     var31.description = var3.getDescriptions(var10);
                     var31.instrument = var25;
                     var31.tickStep = var16;
                     var31.tickSize = var14;
                     var31.pointValue = var18;
                     var31.exchange = var10.getMarketName();
                     var31.commissions = "<Method type=\"None\" use=\"true\"><Params/></Method>";
                     var31.dataType = var13;
                     var31.orderSizeMultiplier = var20;
                     var31.orderSizeStep = var22;
                     var7.add(var31);
                     var5.put(var31.instrument, var31);
                     var9.instrument = var25;
                  }
                  break;
               }
            }
         }
      }

      if (!var7.isEmpty()) {
         InstrumentManager.addInstrumentInBatch(var7);
      }

      for (DataInfo var29 : var1) {
         var29.symbolInfo = InstrumentManager.getInstrumentInfo(var29.instrument);
      }

      DataManager.addDataInBatch("History", var1, var4);
   }

   private boolean matchInstrument(InstrumentInfo var1, double var2, double var4, double var6, double var8, int var10) {
      return var1.tickSize == var2 && var1.tickStep == var4 && var1.dataType == var10 && var1.orderSizeMultiplier == var6 && var1.orderSizeStep == var8;
   }
}
