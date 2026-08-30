package com.strategyquant.tradinglib.data;

import com.strategyquant.datalib.DataInfo;
import com.strategyquant.datalib.InstrumentInfo;
import com.strategyquant.datalib.broker.BrokerDto;
import com.strategyquant.datalib.broker.BrokerManager;
import com.strategyquant.datalib.data.DataDb;
import com.strategyquant.datalib.data.DataException;
import com.strategyquant.datalib.data.DataManager;
import com.strategyquant.datalib.data.io.VersatileData;
import com.strategyquant.datalib.data.io.newDataFormat.DataBinReaderNew;
import com.strategyquant.datalib.data.io.newDataFormat.DataBinWriterNew;
import com.strategyquant.datalib.historyData.dto.TickerDto;
import com.strategyquant.datalib.instrument.InstrumentManager;
import com.strategyquant.lib.L;
import com.strategyquant.lib.SQTime;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.app.MainApp;
import com.strategyquant.lib.crypting.DecryptException;
import com.strategyquant.lib.historyData.HistoryDataSubscription;
import com.strategyquant.tradinglib.historyData.HistoryDataManager;
import com.strategyquant.tradinglib.project.websocket.DataManagerAddProgressSender;
import com.strategyquant.tradinglib.project.websocket.DataManagerDataProgress;
import com.strategyquant.tradinglib.project.websocket.DataManagerProgressListener;
import com.strategyquant.tradinglib.project.websocket.SQWebSocketManager;
import com.strategyquant.tradinglib.project.websocket.WSDataObjects;
import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.http.client.ClientProtocolException;
import org.jdom2.JDOMException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AppDataImporter {
   public static final Logger Log = LoggerFactory.getLogger(AppDataImporter.class);
   private String path;
   private String license;
   private DataDb oldDatabase;
   private List<DataInfo> tickers;
   private List<InstrumentInfo> instruments;
   private String dataRoot;
   private Map<String, InstrumentInfo> instrumentMap;
   private Map<String, InstrumentInfo> newInstrumentMap = new HashMap<>();
   private boolean publicData;
   private boolean freeSymbol;
   private int total;
   private JSONObject progress;
   private volatile boolean cancel;
   private long dateTo;
   private long dateFrom;
   private int records;
   private List<BrokerDto> brokers;
   private Map<Integer, BrokerDto> brokerMap = new HashMap<>();
   private Map<Integer, BrokerDto> newBrokerMap = new HashMap<>();

   public AppDataImporter(String var1, String var2) {
      this.path = var1;
      this.license = var2;
      this.dataRoot = var1 + File.separator + "user" + File.separator + "data";
   }

   public void start() throws Exception {
      this.progress = new JSONObject();

      try {
         this.openDatabase();
         this.updateMessage("Reading list of tickers");
         this.tickers = this.readTickers();
         this.updateMessage("Reading list of instruments");
         this.instruments = this.readInstruments();
         this.instrumentMap = this.instruments.stream().collect(Collectors.toMap(var0 -> var0.instrument, var0 -> (InstrumentInfo)var0));
         this.brokers = this.readBrokers();
         this.brokerMap = this.brokers.stream().collect(Collectors.toMap(BrokerDto::getId, var0 -> (BrokerDto)var0));
         this.handle();
         this.newInstrumentMap.clear();
      } catch (Exception var5) {
         Log.error("Error while importing data", var5);
         this.progress.put("error", "Error while importing data: " + var5.getMessage());
         DataManagerAddProgressSender.getInstance().sendData(this.progress);
      } finally {
         this.progress.put("percent", 100);
         DataManagerAddProgressSender.getInstance().sendData(this.progress);
      }
   }

   private void handle() throws Exception {
      this.total = this.tickers.size();
      int var1 = 0;
      this.updateMessage("Importing data...");

      for (DataInfo var3 : this.tickers) {
         boolean var4 = false;

         try {
            if (this.cancel) {
               break;
            }

            this.publicData = var3.source != 5 && var3.source != 6;
            if (this.publicData || !MainApp.checkProduct("QDM")) {
               if (this.publicData || this.license != null && !this.license.isBlank()) {
                  if (!this.checkData(var3)) {
                     this.writeToLog(var3, "skipped - not valid subscriptions");
                  } else {
                     DataBinReaderNew var5 = this.getDataReader(var3, false);
                     String var6 = var3.symbol;

                     while (DataManager.checkDataExists("History", var6)) {
                        var6 = DataManager.generateName(var6);
                     }

                     try {
                        if (var5.isCrypted() && this.publicData) {
                           this.writeToLog(var3, "skipped - invalid symbol's type");
                           continue;
                        }

                        this.performCopy(var6, var3, var5, var1, false);
                        this.saveToDatabase(var6, var3);
                     } finally {
                        var5.closeFile();
                        Object var31 = null;
                     }

                     this.writeToLog(var3, "imported");
                     if (var3.source == 5 && "M1".equals(var3.timeframe)) {
                        var5 = this.getDataReader(var3, true);

                        try {
                           this.performCopy(var6, var3, var5, var1, true);
                        } finally {
                           var5.closeFile();
                           Object var33 = null;
                        }
                     }

                     var4 = true;
                  }
               } else {
                  this.writeToLog(var3, "skipped - no license was inserted");
               }
            }
         } catch (DecryptException var27) {
            Log.error("", var27);
            this.writeToLog(var3, "failed - inserted license is wrong");
         } catch (Exception var28) {
            Log.error("", var28);
            this.writeToLog(var3, "failed - " + var28.getMessage());
         } finally {
            this.updateProgress(var3.symbol, ++var1, var4);
         }
      }
   }

   private void performCopy(String var1, DataInfo var2, DataBinReaderNew var3, int var4, boolean var5) throws Exception {
      String var6 = var5
         ? L.t("Importing unadjusted data (%d/%d) for symbol %s", new Object[]{var4 + 1, this.total, var2.symbol})
         : L.t("Importing data (%d/%d) for symbol %s", new Object[]{var4 + 1, this.total, var2.symbol});
      this.updateMessage(var6);
      DataBinWriterNew var7 = this.getDataWriter(var1, var2, var5);
      File var8 = new File(var7.getFileName());
      if (var8.exists()) {
         var8.delete();
      }

      try {
         this.copy(var2, var3, var7, var6);
      } finally {
         var7.close();
         String var12 = var7.getFileName().substring(0, var7.getFileName().length() - 4);
         File var13 = new File(var12);
         if (var13.exists()) {
            var13.delete();
         }

         var8.renameTo(var13);
         Object var16 = null;
      }
   }

   private void writeToLog(DataInfo var1, String var2) {
      DataManagerProgressListener var3 = (DataManagerProgressListener)DataManagerDataProgress.get().createListener(var1.symbol, null);
      var3.setMessage(var2);
      DataManagerDataProgress.get().removeListener(var1.symbol);
   }

   private void updateProgress(String var1, int var2, boolean var3) {
      Integer var4 = SQUtils.round(100.0 * var2 / this.total);
      this.progress.put("percent", var4);
      if (var3) {
         this.progress.put("info", L.t("Importing symbol '%s' finished", new Object[]{var1}));
      } else {
         this.progress.put("info", L.t("Importing symbol '%s' failed", new Object[]{var1}));
      }

      DataManagerAddProgressSender.getInstance().sendData(this.progress);
   }

   private void updateMessage(String var1) {
      this.progress.put("info", var1);
      DataManagerAddProgressSender.getInstance().sendData(this.progress);
   }

   private boolean checkData(DataInfo var1) throws ClientProtocolException, IllegalStateException, NoSuchAlgorithmException, SQLException, IOException, JDOMException {
      if (this.publicData) {
         return true;
      } else {
         boolean var2 = var1.timeframe.equals("D1");
         boolean var3 = var1.source == 5;
         if (var3) {
            this.freeSymbol = HistoryDataSubscription.getInstance().isFreeSymbol(var1.uSymbol, false, var2);
            return HistoryDataSubscription.getInstance().isAllowedEquity(var1.uSymbol, var2);
         } else {
            this.freeSymbol = HistoryDataSubscription.getInstance().isFreeSymbol(var1.uSymbol, true, var2);
            TickerDto var4 = HistoryDataManager.get().getFutureTicker(var1.uSymbol);
            String var5 = var4 == null ? null : var4.getMarketName();
            return HistoryDataSubscription.getInstance().isAllowedFuture(var1.uSymbol, var5 == null ? "X" : var5, var2);
         }
      }
   }

   private void saveToDatabase(String var1, DataInfo var2) throws Exception {
      DataInfo var3 = new DataInfo();
      var3.barTimeType = var2.barTimeType;
      var3.timeframe = var2.timeframe;
      var3.timezone = var2.timezone;
      var3.connection = "History";
      var3.symbol = var1;
      var3.source = var2.source;
      var3.sourceDataId = var2.sourceDataId;
      var3.uSymbol = var2.uSymbol;
      var3.uSymbolName = var2.uSymbolName;
      InstrumentInfo var4 = this.getOrCreateInstrument(var2.instrument);
      var3.instrument = var4.instrument;
      var3.brokerId = this.getOrCreateBroker(var2.brokerId).getId();
      DataManager.addData("History", var3.symbol, var4.instrument, var3.barTimeType, var3.source, var3.uSymbol, var3.uSymbolName, -1, var3.brokerId);
      int var5 = SQTime.getDaysBetween(this.dateFrom, this.dateTo) + 1;
      long var6 = var5 * 86400;
      DataManager.updateData("History", var3.symbol, this.dateFrom, this.dateTo, this.records, var6, var3.barTimeType, var3.timeframe, "Etc/UCT");

      try {
         SQWebSocketManager.addToDataQueue(WSDataObjects.getData(var1, "download"), "SQUANT", "QDM", "AlgoWizard");
      } catch (Exception var9) {
         Log.error("Cannot send data Websocket message. ", var9);
      }
   }

   private DataBinWriterNew getDataWriter(String var1, DataInfo var2, boolean var3) throws Exception {
      DataBinWriterNew var4;
      if (this.publicData) {
         int var5 = var2.timeframe.equals("TICK") ? 2 : 1;
         var4 = DataBinWriterNew.getInstance(var5, MainApp.getDataPath(), null);
      } else {
         boolean var8 = HistoryDataSubscription.getInstance().isFreeSymbol(var2.uSymbol, var2.sourceDataId == 6, var2.timeframe.equals("D1"));
         var4 = var8
            ? DataBinWriterNew.getInstance(1, MainApp.getDataPath(), var2.symbolInfo)
            : DataBinWriterNew.getCryptedInstance(1, MainApp.getDataPath(), var2.symbolInfo);
      }

      String var9 = DataManager.getSymbolFolderName(DataManager.get()._getDirectory(), "History", var1, var2);
      String var6 = var3 ? "unadjusted" : "";
      File var7 = new File(var9, DataManager.fixFilename(var1 + var6 + "_" + var2.timeframe + ".dat.tmp"));
      if (var7.exists()) {
         var7.delete();
      }

      var4.setFileName(var7.getAbsolutePath());
      var4.open();
      return var4;
   }

   private InstrumentInfo getOrCreateInstrument(String var1) throws Exception {
      InstrumentInfo var2 = this.newInstrumentMap.get(var1);
      if (var2 != null) {
         return var2;
      }

      InstrumentInfo var3 = this.instrumentMap.get(var1);
      int var4 = var3.broker;
      BrokerDto var5 = this.getOrCreateBroker(var4);
      var3.broker = var5.getId();
      InstrumentInfo var6 = this.findSuitableInstrument(var3);
      if (var6 == null) {
         var6 = this.createInstrument(var3);
      }

      this.newInstrumentMap.put(var1, var6);
      return var6;
   }

   private BrokerDto getOrCreateBroker(int var1) throws Exception {
      if (var1 == -1) {
         return BrokerManager.getInstance().getDefaultBroker();
      }

      BrokerDto var2 = this.newBrokerMap.get(var1);
      if (var2 != null) {
         return var2;
      }

      BrokerDto var3 = this.brokerMap.get(var1);
      BrokerDto var4 = this.findSuitableBroker(var3);
      if (var4 == null) {
         var4 = this.createBroker(var3);
      }

      this.newBrokerMap.put(var1, var4);
      return var4;
   }

   private InstrumentInfo findSuitableInstrument(InstrumentInfo var1) throws DataException {
      String var2 = var1.instrument;
      String[] var3 = var2.split("\\(");

      for (String var5 = var3[0]; InstrumentManager.checkInstrumentExists(var5); var5 = DataManager.generateName(var5)) {
         InstrumentInfo var4 = InstrumentManager.getInstrumentInfo(var5);
         if (this.fit(var4, var1)) {
            return var4;
         }
      }

      return null;
   }

   private BrokerDto findSuitableBroker(BrokerDto var1) throws DataException {
      String var2 = var1.getName();
      String[] var3 = var2.split("\\(");

      for (String var5 = var3[0]; BrokerManager.getInstance().checkBrokerExists(var5); var5 = DataManager.generateName(var5)) {
         BrokerDto var4 = BrokerManager.getInstance().getBroker(var5);
         if (this.fit(var4, var1)) {
            return var4;
         }
      }

      return null;
   }

   private boolean equalValue(Object var1, Object var2) {
      if (var1 == null) {
         return var2 == null;
      } else {
         return var2 == null ? false : var1.equals(var2);
      }
   }

   private boolean fit(BrokerDto var1, BrokerDto var2) {
      return var1.isMtUse() == var2.isMtUse()
         && var1.isStockPickerUse() == var2.isStockPickerUse()
         && this.equalValue(var1.getMtTimezone(), var2.getMtTimezone())
         && this.equalValue(var1.getPostfix(), var2.getPostfix());
   }

   private boolean fit(InstrumentInfo var1, InstrumentInfo var2) {
      return var1.tickSize == var2.tickSize
         && var1.tickStep == var2.tickStep
         && var1.dataType == var2.dataType
         && var1.orderSizeMultiplier == var2.orderSizeMultiplier
         && var1.orderSizeStep == var2.orderSizeStep
         && var1.broker == var2.broker;
   }

   private InstrumentInfo createInstrument(InstrumentInfo var1) throws Exception, DataException {
      String var2 = var1.instrument;

      while (InstrumentManager.checkInstrumentExists(var2)) {
         var2 = DataManager.generateName(var2);
      }

      InstrumentManager.addInstrument(
         var2,
         var1.broker,
         var1.description,
         var1.pointValue,
         var1.tickSize,
         var1.tickStep,
         var1.defaultSpread,
         var1.defaultSlippage,
         0.0,
         var1.commissions,
         (byte)var1.dataType,
         var1.exchange,
         var1.country,
         var1.sector,
         var1.swap,
         var1.orderSizeMultiplier,
         var1.orderSizeStep
      );
      InstrumentInfo var3 = InstrumentManager.getInstrumentInfo(var2);

      try {
         SQWebSocketManager.addToDataQueue(WSDataObjects.getInstruments(), "SQUANT", "QDM", "AlgoWizard");
      } catch (Exception var5) {
         Log.error("Cannot send data Websocket message. ", var5);
      }

      return var3;
   }

   private BrokerDto createBroker(BrokerDto var1) throws Exception, DataException {
      String var2 = var1.getName();

      while (BrokerManager.getInstance().checkBrokerExists(var2)) {
         var2 = DataManager.generateName(var2);
      }

      BrokerDto var3 = new BrokerDto();
      var3.setName(var2);
      var3.setDesc(var1.getName());
      var3.setMtTimezone(var1.getMtTimezone());
      var3.setMtUse(var1.isMtUse());
      var3.setPostfix(var1.getPostfix());
      var3.setStockPickerUse(var1.isStockPickerUse());
      BrokerManager.getInstance().saveBroker(var3);

      try {
         SQWebSocketManager.addToDataQueue(WSDataObjects.getBrokers(), "SQUANT", "QDM", "AlgoWizard");
      } catch (Exception var5) {
         Log.error("Cannot send data Websocket message. ", var5);
      }

      return var3;
   }

   private void copy(DataInfo var1, DataBinReaderNew var2, DataBinWriterNew var3, String var4) throws Exception {
      this.dateFrom = -1L;
      this.records = 0;
      int var5 = 1000;
      if (var1.rows != 0) {
         var5 = var1.rows / 10;
      }

      while (var2.readData()) {
         VersatileData var6 = var2.tickData;
         var3.writeData(var6);
         if (this.dateFrom == -1L) {
            this.dateFrom = var6.time;
         }

         this.dateTo = var6.time;
         this.records++;
         if (this.cancel) {
            break;
         }

         if (this.records % var5 == 0) {
            this.updateMessage(var4 + " " + SQTime.toDateString(var6.time));
         }
      }
   }

   private DataBinReaderNew getDataReader(DataInfo var1, boolean var2) throws Exception {
      boolean var3 = var1.timeframe.equals("TICK");
      DataBinReaderNew var4 = null;
      if (var3) {
         var4 = DataBinReaderNew.getInstance(2, var1.symbolInfo);
      } else {
         var4 = DataBinReaderNew.getInstance(1, var1.symbolInfo);
      }

      String var5 = var2 ? "unadjusted" : "";
      String var6 = DataManager.getSymbolFolderName(this.dataRoot, "History", var1.symbol, var1);
      File var7 = new File(var6, DataManager.fixFilename(var1.symbol + var5 + "_" + var1.timeframe + ".dat"));
      if (!this.publicData && !this.freeSymbol) {
         var4.setPassword(this.license);
      }

      var4.setFileName(var7.getAbsolutePath());
      var4.openFile();
      return var4;
   }

   private List<InstrumentInfo> readInstruments() throws Exception {
      LinkedList var1 = new LinkedList();
      Connection var2 = this.oldDatabase.getConnection();

      try {
         Statement var3 = var2.createStatement();

         try {
            ResultSet var4 = var3.executeQuery("SELECT * FROM INSTRUMENTS");

            try {
               while (var4.next()) {
                  InstrumentInfo var5 = new InstrumentInfo();
                  var5.instrument = var4.getString("INSTRUMENT").intern();
                  var5.description = var4.getString("DESCRIPTION").intern();
                  var5.pointValue = var4.getDouble("POINTVALUE");
                  var5.tickSize = var4.getDouble("TICKSIZE");
                  var5.tickStep = var4.getDouble("TICKSTEP");
                  var5.defaultSpread = var4.getDouble("DEFAULTSPREAD");
                  var5.defaultSlippage = var4.getDouble("DEFAULTSLIPPAGE");
                  var5.commissions = var4.getString("COMMISSIONS");
                  var5.dataType = var4.getInt("DATATYPE");
                  var5.exchange = var4.getString("EXCHANGE");
                  var5.country = var4.getString("COUNTRY");
                  var5.sector = var4.getString("SECTOR");
                  var5.decimals = SQUtils.getDecimalPlaces(var5.tickStep);
                  var5.swap = var4.getString("SWAP");
                  var5.broker = var4.getInt("BROKER_ID");

                  try {
                     var5.orderSizeMultiplier = var4.getDouble("ORDERSIZEMULTIPLIER");
                  } catch (Exception var11) {
                     var5.orderSizeMultiplier = 1.0;
                  }

                  try {
                     var5.orderSizeStep = var4.getDouble("ORDERSIZESTEP");
                  } catch (Exception var10) {
                     var5.orderSizeStep = 1.0;
                  }

                  var1.add(var5);
               }
            } catch (Throwable var12) {
               if (var4 != null) {
                  try {
                     var4.close();
                  } catch (Throwable var9) {
                     var12.addSuppressed(var9);
                  }
               }

               throw var12;
            }

            if (var4 != null) {
               var4.close();
            }
         } catch (Throwable var13) {
            if (var3 != null) {
               try {
                  var3.close();
               } catch (Throwable var8) {
                  var13.addSuppressed(var8);
               }
            }

            throw var13;
         }

         if (var3 != null) {
            var3.close();
         }
      } catch (Throwable var14) {
         if (var2 != null) {
            try {
               var2.close();
            } catch (Throwable var7) {
               var14.addSuppressed(var7);
            }
         }

         throw var14;
      }

      if (var2 != null) {
         var2.close();
      }

      return var1;
   }

   private List<BrokerDto> readBrokers() throws Exception {
      LinkedList var1 = new LinkedList();
      Connection var2 = this.oldDatabase.getConnection();

      try {
         Statement var3 = var2.createStatement();

         try {
            ResultSet var4 = var3.executeQuery("SELECT * FROM BROKER");

            try {
               while (var4.next()) {
                  BrokerDto var5 = new BrokerDto();
                  var5.setId(var4.getInt("ID"));
                  var5.setName(var4.getString("NAME"));
                  var5.setDesc(var4.getString("DESC"));
                  var5.setSystem(var4.getBoolean("SYSTEM"));
                  var5.setMtTimezone(var4.getString("MT_TIMEZONE"));
                  var5.setMtUse(var4.getInt("MT_USE") == 1);
                  var5.setStockPickerUse(var4.getInt("STOCKPICKER_USE") == 1);
                  var5.setPostfix(var4.getString("POSTFIX"));
                  var1.add(var5);
               }
            } catch (Throwable var10) {
               if (var4 != null) {
                  try {
                     var4.close();
                  } catch (Throwable var9) {
                     var10.addSuppressed(var9);
                  }
               }

               throw var10;
            }

            if (var4 != null) {
               var4.close();
            }
         } catch (Throwable var11) {
            if (var3 != null) {
               try {
                  var3.close();
               } catch (Throwable var8) {
                  var11.addSuppressed(var8);
               }
            }

            throw var11;
         }

         if (var3 != null) {
            var3.close();
         }
      } catch (Throwable var12) {
         if (var2 != null) {
            try {
               var2.close();
            } catch (Throwable var7) {
               var12.addSuppressed(var7);
            }
         }

         throw var12;
      }

      if (var2 != null) {
         var2.close();
      }

      return var1;
   }

   private List<DataInfo> readTickers() throws Exception {
      LinkedList var1 = new LinkedList();
      Connection var2 = this.oldDatabase.getConnection();

      try {
         Statement var3 = var2.createStatement();

         try {
            ResultSet var4 = var3.executeQuery("SELECT * FROM DATA");

            try {
               while (var4.next()) {
                  DataInfo var5 = new DataInfo();
                  var5.id = var4.getInt("ID");
                  var5.connection = var4.getString("CONNECTION");
                  var5.symbol = var4.getString("SYMBOL");
                  var5.instrument = var4.getString("INSTRUMENT");
                  var5.timeframe = var4.getString("TIMEFRAME");
                  var5.timezone = var4.getString("TIMEZONE");
                  var5.filename = var4.getString("FILENAME");
                  var5.dateFrom = var4.getLong("DATEFROM");
                  var5.dateTo = var4.getLong("DATETO");
                  var5.rows = var4.getInt("ROWS");
                  var5.secondsRecords = var4.getLong("SECONDS_RECORDS");
                  var5.barTimeType = var4.getInt("DATATYPE");
                  var5.decimals = var4.getInt("DECIMALS");
                  var5.source = var4.getInt("SOURCE");
                  var5.sourceDataId = var4.getInt("SOURCEDATA_ID");
                  var5.uSymbol = var4.getString("USYMBOL");
                  var5.uSymbolName = var4.getString("USYMBOLNAME");
                  var5.show = var4.getBoolean("SHOW");
                  var5.basketId = var4.getInt("BASKET_ID");
                  var5.brokerId = var4.getInt("BROKER_ID");
                  var5.timeframe = var5.timeframe != null && !var5.timeframe.equals("null") ? var5.timeframe : null;
                  if (!var5.symbol.startsWith("[") && !var5.symbol.contains("_limited") && !var5.symbol.contains("SPY_benchmark")) {
                     var1.add(var5);
                  }
               }
            } catch (Throwable var10) {
               if (var4 != null) {
                  try {
                     var4.close();
                  } catch (Throwable var9) {
                     var10.addSuppressed(var9);
                  }
               }

               throw var10;
            }

            if (var4 != null) {
               var4.close();
            }
         } catch (Throwable var11) {
            if (var3 != null) {
               try {
                  var3.close();
               } catch (Throwable var8) {
                  var11.addSuppressed(var8);
               }
            }

            throw var11;
         }

         if (var3 != null) {
            var3.close();
         }
      } catch (Throwable var12) {
         if (var2 != null) {
            try {
               var2.close();
            } catch (Throwable var7) {
               var12.addSuppressed(var7);
            }
         }

         throw var12;
      }

      if (var2 != null) {
         var2.close();
      }

      return var1;
   }

   private void openDatabase() {
      String var1 = this.path + File.separator + "user" + File.separator + "data";
      this.oldDatabase = new DataDb(var1) {
         public void initDatabase() {
         }
      };
   }

   public String validate() {
      return !new File(this.dataRoot).exists() ? L.t("Not found path on your disk: %s", new Object[]{this.dataRoot}) : null;
   }

   public void cancel() {
      this.cancel = true;
   }
}
