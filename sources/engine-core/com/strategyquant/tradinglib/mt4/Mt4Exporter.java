package com.strategyquant.tradinglib.mt4;

import com.strategyquant.datalib.DataInfo;
import com.strategyquant.datalib.broker.BrokerDto;
import com.strategyquant.datalib.broker.BrokerManager;
import com.strategyquant.datalib.data.DataManager;
import com.strategyquant.datalib.data.DateShifter;
import com.strategyquant.datalib.data.io.VersatileData;
import com.strategyquant.datalib.data.io.newDataFormat.DataBinReaderNew;
import com.strategyquant.datalib.metatrader4.Mt4SymbolProperties;
import com.strategyquant.lib.L;
import com.strategyquant.lib.SQTime;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.utils.IProgressListener;
import com.strategyquant.tradinglib.project.websocket.WebSocketNotification;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Mt4Exporter {
   private static final Logger LOG = LoggerFactory.getLogger(Mt4Exporter.class);
   private static final String COPYRIGHT = "(C)opyright 2003, MetaQuotes Software Corp.";
   private static final String[] TIMEFRAMES = new String[]{"M1", "M5", "M15", "M30", "H1", "H4", "D1"};
   private static final int[] TIMEFRAMES_INT = new int[]{1, 5, 15, 30, 60, 240, 1440};
   private static final int REFRESH_RATE = 10;
   private DataOutputStream[] outputsFxt = new DataOutputStream[TIMEFRAMES.length];
   private DataOutputStream[] outputsHst = new DataOutputStream[TIMEFRAMES.length];
   private long fromDate;
   private long toDate;
   private String sqSymbol;
   private String mt4Symbol;
   private int digits;
   private String dataFolder;
   private String serverName;
   private Mt4SymbolProperties properties;
   private volatile boolean canceled = false;
   private IProgressListener listener;
   private Mt4Exporter.HstExporter hstExporter;
   private Mt4Exporter.FxtExporter fxtExporter;
   private int[] extPeriodSeconds = new int[TIMEFRAMES.length];
   private int[] extBars = new int[TIMEFRAMES.length];
   private long[] extLastBarTime = new long[TIMEFRAMES.length];
   private double[] extLastOpen = new double[TIMEFRAMES.length];
   private double[] extLastLow = new double[TIMEFRAMES.length];
   private double[] extLastHigh = new double[TIMEFRAMES.length];
   private double[] extLastClose = new double[TIMEFRAMES.length];
   private int[] extLastVolume = new int[TIMEFRAMES.length];
   private long extLastTime;
   private long extStartTick;
   private long extEndTick;
   private long totalRecords;
   private volatile boolean paused;
   private DataBinReaderNew reader;
   private double tickSize;
   private String timeframe;
   private String baseCurrency;
   private String encoding;
   private boolean exportHst;
   private boolean exportFxt;
   private DateShifter dateShifter;
   private String targetTimezone;

   public Mt4Exporter(
      String var1,
      String var2,
      String var3,
      long var4,
      long var6,
      String var8,
      String var9,
      Mt4SymbolProperties var10,
      String var11,
      String var12,
      String var13,
      IProgressListener var14
   ) {
      this.encoding = var3;
      this.sqSymbol = var1;
      this.mt4Symbol = var2;
      this.fromDate = var4;
      this.toDate = SQTime.addDays(var6, 1);
      this.properties = var10;
      this.dataFolder = var8;
      this.serverName = var9;
      this.timeframe = var11;
      this.targetTimezone = var13;
      this.listener = var14;
      this.exportHst = true;
      this.exportFxt = true;
      switch (var12) {
         case "hst":
            this.exportFxt = false;
            break;
         case "fxt":
            this.exportHst = false;
      }

      this.hstExporter = new Mt4Exporter.HstExporter(this.exportHst);
      this.fxtExporter = new Mt4Exporter.FxtExporter(this.exportFxt);
   }

   private void evalTimeZones(DataInfo var1, String var2) {
      String var3 = "UTC";
      if (var1.brokerId != -1) {
         BrokerDto var4 = BrokerManager.getInstance().getBroker(var1.brokerId);
         var3 = var4.getMtTimezone();
      }

      this.dateShifter = new DateShifter(var3, var2);
   }

   public void export() throws IOException {
      try {
         LOG.info("Exporting MT4 symbol: " + this.mt4Symbol);
         this.canceled = false;

         for (int var1 = 0; var1 < TIMEFRAMES.length; var1++) {
            this.extPeriodSeconds[var1] = TIMEFRAMES_INT[var1] * 60 * 1000;
            this.extBars[var1] = 0;
            this.extLastBarTime[var1] = 0L;
         }

         this.extStartTick = 0L;
         this.extEndTick = 0L;
         DataInfo var3 = DataManager.getDataInfo("History", this.sqSymbol);
         this.evalTimeZones(var3, this.targetTimezone);
         this.prepareReader();
         this.prepareWriters();
         this.performExport();
         this.closeWriters();
      } catch (Exception var2) {
         LOG.error("Error while exporting to MT4. Exc.", var2);
         this.listener.onError(var2.getMessage());
      }
   }

   private void prepareReader() throws Exception {
      DataInfo var1 = DataManager.getDataInfo("History", this.sqSymbol);
      this.reader = DataBinReaderNew.getInstance(2, var1.symbolInfo);
      String var2 = DataManager.getDataFileName("History", this.sqSymbol, "TICK", "No Session");
      File var3 = new File(var2);
      this.reader.setFileName(var3.getAbsolutePath());
      this.reader.openFile();
      DataManager.checkDataExport(this.reader, var1.source);
      String var4 = var1.uSymbol;
      if (var4 == null) {
         var4 = var1.instrument;
      }

      if (this.properties.exists("currency")) {
         this.baseCurrency = this.properties.getStrValue("currency");
      } else {
         this.baseCurrency = this.getBaseCurrency(var4);
      }

      this.totalRecords = var1.rows;
      if (this.properties.exists("mode_digits")) {
         this.digits = this.properties.getIntValue("mode_digits");
      } else {
         this.digits = var1.decimals;
      }

      this.tickSize = var1.symbolInfo.tickSize;
   }

   private String getBaseCurrency(String var1) {
      return var1.length() >= 6 ? var1.substring(var1.length() - 3, var1.length()) : var1.substring(var1.length() - 3);
   }

   private void performExport() throws Exception, IOException {
      long var1 = 0L;
      int var3 = 0;
      new VersatileData();

      while (this.reader.dataRemaining()) {
         this.reader.readData();
         VersatileData var4 = this.reader.tickData;
         var4.ask = SQUtils.round(var4.ask, this.digits);
         var4.bid = SQUtils.round(var4.bid, this.digits);
         this.dateShifter.transformToTimeZone(var4);
         var1 = var4.time;
         if (this.canceled) {
            break;
         }

         this.checkPaused();
         var3++;
         if (this.toDate > 0L && var1 >= this.toDate) {
            break;
         }

         boolean var5 = this.fromDate > 0L && var1 < this.fromDate;
         if (var3 % 10 != 0) {
            if (var5) {
               this.listener.setMessage(L.t("Export to MT4 - skipped %s", new Object[]{SQTime.toDateMinuteString(var1)}));
            } else {
               this.listener.setMessage(L.t("Exported to MT4 %s", new Object[]{SQTime.toDateMinuteString(var1)}));
            }
         }

         this.showProgress(var3, false);
         if (!var5) {
            this.extLastTime = var4.time;

            for (int var6 = 0; var6 < TIMEFRAMES.length; var6++) {
               long var7 = var1 / this.extPeriodSeconds[var6];
               var7 *= this.extPeriodSeconds[var6];
               boolean var9 = false;
               if (var6 < 7) {
                  if (this.extLastBarTime[var6] != var7) {
                     var9 = true;
                  }
               } else if (var6 == 7) {
                  if (var1 - this.extLastBarTime[var6] >= this.extPeriodSeconds[var6]) {
                     var9 = true;
                  }

                  if (var9) {
                     var7 = var1;
                     var7 -= var7 % 86400L;

                     while (SQTime.getDayOfWeekOriginal(var7) != 0) {
                        var7 -= 86400L;
                     }
                  }
               } else if (var6 == 8) {
                  if (this.extLastBarTime[var6] == 0L) {
                     var9 = true;
                  }

                  if (SQTime.getDay(var1) < 5 && var1 - this.extLastBarTime[var6] > 864000L) {
                     var9 = true;
                  }

                  if (var9) {
                     var7 = var1;
                     var7 -= var7 % 86400L;

                     while (SQTime.getDay(var7) != 1) {
                        var7 -= 86400L;
                     }
                  }
               }

               if (!var9) {
                  if (this.extLastLow[var6] > var4.bid) {
                     this.extLastLow[var6] = var4.bid;
                  }

                  if (this.extLastHigh[var6] < var4.bid) {
                     this.extLastHigh[var6] = var4.bid;
                  }

                  this.extLastClose[var6] = var4.bid;
                  this.extLastVolume[var6] = this.extLastVolume[var6] + (int)var4.volume;
               } else {
                  if (this.extBars[var6] > 0) {
                     this.hstExporter.writeBar(var6);
                     if (var6 == 0) {
                        long var10 = (var7 - this.extLastBarTime[var6]) / this.extPeriodSeconds[var6];
                        if (var10 > 1L && var10 < 5L) {
                           long var12 = this.extLastBarTime[var6];
                           this.extLastLow[var6] = this.extLastClose[var6];
                           this.extLastHigh[var6] = this.extLastClose[var6];
                           this.extLastOpen[var6] = this.extLastClose[var6];
                           this.extLastVolume[var6] = 0;

                           for (int var14 = 1; var14 < var10; var14++) {
                              this.extLastBarTime[var6] = var12 + var14 * this.extPeriodSeconds[var6];
                              this.hstExporter.writeBar(var6);
                              this.extBars[var6]++;
                           }
                        }
                     }
                  }

                  this.extLastBarTime[var6] = var7;
                  this.extLastOpen[var6] = var4.bid;
                  this.extLastLow[var6] = var4.bid;
                  this.extLastHigh[var6] = var4.bid;
                  this.extLastClose[var6] = var4.bid;
                  if (var4.volume > 0.0) {
                     this.extLastVolume[var6] = (int)var4.volume;
                  } else {
                     this.extLastVolume[var6] = 1;
                  }

                  this.extBars[var6]++;
               }
            }

            if (this.extStartTick == 0L) {
               this.extStartTick = var1;
            }

            this.extEndTick = var1;
            int var16 = this.computeSpread(var4.ask, var4.bid);
            this.fxtExporter.writeTick(var16);
            if (this.isFxtTooBig(this.fxtExporter.saved)) {
               String var20 = String.format(
                  "Export to FXT finished, 4 GB file size limit reached. Data exported: %s - %s",
                  SQTime.formatDate(this.extStartTick),
                  SQTime.formatDate(this.extEndTick)
               );
               WebSocketNotification.send("Metatrader 4 exporter", var20, "SQMANAGER");
               break;
            }
         }
      }
   }

   private int computeSpread(double var1, double var3) {
      double var5 = Math.abs(var3 - var1);
      var5 /= this.tickSize;
      return (int)(var5 * 10.0);
   }

   private boolean isFxtTooBig(long var1) {
      return false;
   }

   private void checkPaused() throws InterruptedException {
      if (this.paused) {
         this.listener.onPause();

         while (this.paused && !this.canceled) {
            Thread.sleep(100L);
         }

         this.listener.onContinue();
      }
   }

   private void closeWriters() throws IOException {
      for (int var1 = 0; var1 < TIMEFRAMES.length; var1++) {
         if (!this.skipTimeframe(var1)) {
            this.hstExporter.writeBar(var1);
            if (this.exportFxt) {
               this.outputsFxt[var1].close();
            }

            if (this.exportHst) {
               this.outputsHst[var1].close();
            }

            File var2 = this.getTargetFile(this.fxtExporter, var1);
            this.fxtExporter.writeSummary(var2, this.extBars[var1]);
            this.fxtExporter.setReadOnly(var2);
         }
      }
   }

   private File getTargetFile(Mt4Exporter.Exporter var1, int var2) {
      File var3 = new File(var1.getPath());
      var3.mkdirs();
      String var4 = var1.getFileName(var2);
      return new File(var3, var4);
   }

   private DataOutputStream getDataOutputStream(Mt4Exporter.Exporter var1, int var2) throws FileNotFoundException {
      File var3 = this.getTargetFile(var1, var2);
      if (var3.exists()) {
         var3.setWritable(true);
      }

      return new DataOutputStream(new BufferedOutputStream(new FileOutputStream(var3)));
   }

   private boolean skipTimeframe(int var1) {
      if (this.timeframe != null && !this.timeframe.equalsIgnoreCase("All")) {
         String var2 = TIMEFRAMES[var1];
         boolean var3 = var2.equalsIgnoreCase(this.timeframe);
         return !var3;
      } else {
         return false;
      }
   }

   private void prepareWriters() throws IOException {
      for (int var1 = 0; var1 < TIMEFRAMES.length; var1++) {
         if (!this.skipTimeframe(var1)) {
            this.outputsFxt[var1] = this.exportFxt ? this.getDataOutputStream(this.fxtExporter, var1) : null;
            this.fxtExporter.writeHeader(this.outputsFxt[var1], var1);
            this.outputsHst[var1] = this.exportHst ? this.getDataOutputStream(this.hstExporter, var1) : null;
            this.hstExporter.writeHeader(this.outputsHst[var1], var1);
         }
      }
   }

   public boolean isCanceled() {
      return this.canceled;
   }

   public void showProgress(double var1, boolean var3) {
      double var4 = SQUtils.round(var1 * 100.0 / this.totalRecords, 1);
      if (var4 < 1.0) {
         var4 = 1.0;
      }

      if (var4 < 100.0 || var3) {
         this.listener.onProgress(var4);
      }
   }

   public void cancel() {
      this.canceled = true;
   }

   private byte[] getBytes(String var1, int var2, String var3) {
      byte[] var4 = new byte[var2];
      if (var1.length() > var2) {
         var1 = var1.substring(0, var2 - 1);
      }

      byte[] var5;
      if (var3 != null && !var3.isBlank()) {
         Charset var6 = null;

         try {
            var6 = Charset.forName(var3);
         } catch (Exception var8) {
            LOG.error("", var8);
            throw new RuntimeException("Invalid encoding: " + var3);
         }

         var5 = var1.getBytes(var6);
      } else {
         var5 = var1.getBytes();
      }

      for (int var10 = 0; var10 < var5.length; var10++) {
         var4[var10] = var5[var10];
      }

      return var4;
   }

   private void writeString(DataOutputStream var1, String var2, int var3) throws IOException {
      var1.write(this.getBytes(var2, var3, null));
   }

   private void writeString(DataOutputStream var1, String var2, int var3, String var4) throws IOException {
      var1.write(this.getBytes(var2, var3, var4));
   }

   private void writeLong(DataOutputStream var1, long var2) throws IOException {
      ByteBuffer var4 = ByteBuffer.allocate(8);
      var4.order(ByteOrder.LITTLE_ENDIAN);
      var4.putLong(var2);
      var1.write(var4.array());
   }

   private void writeInt(DataOutputStream var1, int var2) throws IOException {
      var1.write(this.getIntBytes(var2));
   }

   private void writeIntArray(DataOutputStream var1, int[] var2) throws IOException {
      for (int var6 : var2) {
         this.writeInt(var1, var6);
      }
   }

   private void writeTimestamp(DataOutputStream var1, long var2) throws IOException {
      var1.write(this.getIntBytesFromTimestamp(var2));
   }

   private byte[] getIntBytesFromTimestamp(long var1) {
      ByteBuffer var3 = ByteBuffer.allocate(4);
      var3.order(ByteOrder.LITTLE_ENDIAN);
      var1 /= 1000L;
      int var4 = (int)var1;
      var3.putInt(var4);
      return var3.array();
   }

   private byte[] getIntBytes(int var1) {
      ByteBuffer var2 = ByteBuffer.allocate(4);
      var2.order(ByteOrder.LITTLE_ENDIAN);
      var2.putInt(var1);
      return var2.array();
   }

   private void writeDouble(DataOutputStream var1, double var2) throws IOException {
      ByteBuffer var4 = ByteBuffer.allocate(8);
      var4.order(ByteOrder.LITTLE_ENDIAN);
      var4.putDouble(var2);
      var1.write(var4.array());
   }

   public void pause() {
      this.paused = true;
   }

   public void restart() {
      this.paused = false;
   }

   private interface Exporter {
      String getFileName(int var1);

      void writeHeader(DataOutputStream var1, int var2) throws IOException;

      String getPath();
   }

   private class FxtExporter implements Mt4Exporter.Exporter {
      private static final int FXT_HEADER_SIZE = 728;
      private static final int FXT_RECORD_SIZE = 56;
      private static final String FXT_COPYRIGHT = "(C)opyright 2005-2007, MetaQuotes Software Corp.";
      private static final int FXT_LEVERAGE = 100;
      private static final double FXT_COMM_BASE = 0.0;
      private static final int SWAP_ROLLOVER3DAYS = 3;
      private static final int FXT_SWAP_ENABLE = 1;
      private static final int FXT_GTC_PENDINGS = 0;
      private static final double FXT_MODEL_QUALITY = 68.0;
      private static final int FXT_BARS = 0;
      private static final int FXT_MODEL = 0;
      private static final int FXT_VERSION = 405;
      private static final int COMM_TYPE_PIPS = 1;
      private static final int COMMISSION_PER_LOT = 0;
      private static final int FXT_FROM = 0;
      private static final int FXT_TO = 0;
      private static final int MARGIN_CALC_FOREX = 0;
      private static final int MARGIN_CALC_CFD = 1;
      private static final int MARGIN_CALC_FUTURES = 2;
      private static final int MARGIN_CALC_CFDINDEX = 3;
      private static final int MARGIN_CALC_CFDLEVERAGE = 4;
      private long saved = 0L;
      private boolean export;

      public FxtExporter(boolean nullx) {
         this.export = nullx;
      }

      public void writeTick(int var1) throws IOException {
         if (this.export) {
            for (int var2 = 0; var2 < Mt4Exporter.TIMEFRAMES.length; var2++) {
               if (!Mt4Exporter.this.skipTimeframe(var2)) {
                  Mt4Exporter.this.writeLong(Mt4Exporter.this.outputsFxt[var2], Mt4Exporter.this.extLastBarTime[var2] / 1000L);
                  Mt4Exporter.this.writeDouble(Mt4Exporter.this.outputsFxt[var2], Mt4Exporter.this.extLastOpen[var2]);
                  Mt4Exporter.this.writeDouble(Mt4Exporter.this.outputsFxt[var2], Mt4Exporter.this.extLastHigh[var2]);
                  Mt4Exporter.this.writeDouble(Mt4Exporter.this.outputsFxt[var2], Mt4Exporter.this.extLastLow[var2]);
                  Mt4Exporter.this.writeDouble(Mt4Exporter.this.outputsFxt[var2], Mt4Exporter.this.extLastClose[var2]);
                  Mt4Exporter.this.writeInt(Mt4Exporter.this.outputsFxt[var2], Math.max(1, Mt4Exporter.this.extLastVolume[var2]));
                  Mt4Exporter.this.writeInt(Mt4Exporter.this.outputsFxt[var2], var1);
                  Mt4Exporter.this.writeTimestamp(Mt4Exporter.this.outputsFxt[var2], Mt4Exporter.this.extLastTime);
                  Mt4Exporter.this.writeInt(Mt4Exporter.this.outputsFxt[var2], 4);
               }
            }

            this.saved += 56L;
         }
      }

      @Override
      public String getFileName(int var1) {
         return Mt4Exporter.this.mt4Symbol + Mt4Exporter.TIMEFRAMES_INT[var1] + "_0.fxt";
      }

      @Override
      public void writeHeader(DataOutputStream var1, int var2) throws IOException {
         if (this.export) {
            Mt4Exporter.this.writeInt(var1, 405);
            Mt4Exporter.this.writeString(var1, "(C)opyright 2005-2007, MetaQuotes Software Corp.", 64);
            Mt4Exporter.this.writeString(var1, Mt4Exporter.this.serverName, 128);
            Mt4Exporter.this.writeString(var1, Mt4Exporter.this.mt4Symbol, 12, Mt4Exporter.this.encoding);
            Mt4Exporter.this.writeInt(var1, Mt4Exporter.TIMEFRAMES_INT[var2]);
            Mt4Exporter.this.writeInt(var1, 0);
            Mt4Exporter.this.writeInt(var1, 0);
            Mt4Exporter.this.writeTimestamp(var1, Mt4Exporter.this.fromDate);
            Mt4Exporter.this.writeTimestamp(var1, Mt4Exporter.this.toDate);
            Mt4Exporter.this.writeInt(var1, 0);
            Mt4Exporter.this.writeDouble(var1, 68.0);
            Mt4Exporter.this.writeString(var1, Mt4Exporter.this.baseCurrency, 12);
            int var3 = Mt4Exporter.this.properties.getIntValue("spread");
            Mt4Exporter.this.writeInt(var1, var3);
            Mt4Exporter.this.writeInt(var1, Mt4Exporter.this.digits);
            Mt4Exporter.this.writeInt(var1, 0);
            double var4 = Mt4Exporter.this.properties.getDoubleValue("mode_point");
            if (var4 == 0.0) {
               var4 = 1.0E-5;
            }

            Mt4Exporter.this.writeDouble(var1, var4);
            double var6 = Mt4Exporter.this.properties.getDoubleValue("lot_min");
            Mt4Exporter.this.writeInt(var1, (int)(var6 * 100.0));
            double var8 = Mt4Exporter.this.properties.getDoubleValue("lot_max");
            Mt4Exporter.this.writeInt(var1, (int)(var8 * 100.0));
            double var10 = Mt4Exporter.this.properties.getDoubleValue("lot_step");
            Mt4Exporter.this.writeInt(var1, (int)(var10 * 100.0));
            int var12 = Mt4Exporter.this.properties.getIntValue("stops_level");
            Mt4Exporter.this.writeInt(var1, var12);
            Mt4Exporter.this.writeInt(var1, 0);
            Mt4Exporter.this.writeInt(var1, 0);
            double var13 = Mt4Exporter.this.properties.getDoubleValue("contract_size");
            Mt4Exporter.this.writeDouble(var1, var13);
            double var15 = Mt4Exporter.this.properties.getDoubleValue("tick_value");
            Mt4Exporter.this.writeDouble(var1, var15);
            double var17 = Mt4Exporter.this.properties.getDoubleValue("tick_size");
            Mt4Exporter.this.writeDouble(var1, var17);
            int var19 = Mt4Exporter.this.properties.getIntValue("profit_mode");
            Mt4Exporter.this.writeInt(var1, var19);
            Mt4Exporter.this.writeInt(var1, 1);
            Mt4Exporter.this.writeInt(var1, Mt4Exporter.this.properties.getIntValue("swap_type"));
            Mt4Exporter.this.writeInt(var1, 0);
            Mt4Exporter.this.writeDouble(var1, Mt4Exporter.this.properties.getDoubleValue("swap_long"));
            Mt4Exporter.this.writeDouble(var1, Mt4Exporter.this.properties.getDoubleValue("swap_short"));
            Mt4Exporter.this.writeInt(var1, 3);
            Mt4Exporter.this.writeInt(var1, 100);
            Mt4Exporter.this.writeInt(var1, Mt4Exporter.this.properties.getIntValue("free_margin_mode"));
            Mt4Exporter.this.writeInt(var1, Mt4Exporter.this.properties.getIntValue("margin_mode"));
            Mt4Exporter.this.writeInt(var1, Mt4Exporter.this.properties.getIntValue("margin_stopout"));
            Mt4Exporter.this.writeInt(var1, Mt4Exporter.this.properties.getIntValue("margin_stopout_mode"));
            Mt4Exporter.this.writeDouble(var1, Mt4Exporter.this.properties.getDoubleValue("margin_initial"));
            Mt4Exporter.this.writeDouble(var1, Mt4Exporter.this.properties.getDoubleValue("margin_maintenance"));
            Mt4Exporter.this.writeDouble(var1, Mt4Exporter.this.properties.getDoubleValue("margin_hedged"));
            int var20 = Mt4Exporter.this.properties.getIntValue("mode_margincalcmode");
            double var21 = 0.0;
            String var23 = "";
            double var24 = 1.0;
            switch (var20) {
               case 0:
                  var24 = 1.0;
                  var23 = Mt4Exporter.this.baseCurrency;
                  break;
               case 1:
               case 2:
               case 3:
               case 4:
                  var23 = "USD";
                  var21 = Mt4Exporter.this.properties.getDoubleValue("mode_bid") * var13 / Mt4Exporter.this.properties.getDoubleValue("mode_marginrequired");
                  if (var21 < 0.97 || var21 > 1.03) {
                     var24 = Math.floor(var21);
                  }
            }

            Mt4Exporter.this.writeDouble(var1, var24);
            Mt4Exporter.this.writeString(var1, var23, 12);
            Mt4Exporter.this.writeInt(var1, 0);
            Mt4Exporter.this.writeDouble(var1, 0.0);
            Mt4Exporter.this.writeInt(var1, 1);
            Mt4Exporter.this.writeInt(var1, 0);
            Mt4Exporter.this.writeInt(var1, 0);
            Mt4Exporter.this.writeInt(var1, 0);
            int[] var26 = new int[6];
            var26[0] = 1;
            Mt4Exporter.this.writeIntArray(var1, var26);
            Mt4Exporter.this.writeInt(var1, 0);
            Mt4Exporter.this.writeInt(var1, 0);
            Mt4Exporter.this.writeInt(var1, Mt4Exporter.this.properties.getIntValue("freeze_level"));
            var1.write(new byte[244]);
            this.saved = 728L;
         }
      }

      @Override
      public String getPath() {
         return Mt4Exporter.this.dataFolder + "//tester//history";
      }

      public void setReadOnly(File var1) {
         if (this.export) {
            var1.setReadOnly();
         }
      }

      public void writeSummary(File var1, int var2) throws IOException {
         if (this.export) {
            RandomAccessFile var3 = new RandomAccessFile(var1, "rw");
            var3.seek(216L);
            var3.write(Mt4Exporter.this.getIntBytes(var2));
            var3.write(Mt4Exporter.this.getIntBytesFromTimestamp(Mt4Exporter.this.extStartTick));
            var3.write(Mt4Exporter.this.getIntBytesFromTimestamp(Mt4Exporter.this.extEndTick));
            var3.close();
         }
      }
   }

   private class HstExporter implements Mt4Exporter.Exporter {
      private static final int HST_VERSION = 401;
      private boolean export;

      public HstExporter(boolean nullx) {
         this.export = nullx;
      }

      public void writeBar(int var1) throws IOException {
         if (this.export) {
            if (!Mt4Exporter.this.skipTimeframe(var1)) {
               DataOutputStream var2 = Mt4Exporter.this.outputsHst[var1];
               Mt4Exporter.this.writeTimestamp(var2, Mt4Exporter.this.extLastBarTime[var1]);
               Mt4Exporter.this.writeInt(var2, 0);
               Mt4Exporter.this.writeDouble(var2, Mt4Exporter.this.extLastOpen[var1]);
               Mt4Exporter.this.writeDouble(var2, Mt4Exporter.this.extLastHigh[var1]);
               Mt4Exporter.this.writeDouble(var2, Mt4Exporter.this.extLastLow[var1]);
               Mt4Exporter.this.writeDouble(var2, Mt4Exporter.this.extLastClose[var1]);
               Mt4Exporter.this.writeLong(var2, Math.max(1L, Mt4Exporter.this.extLastVolume[var1]));
               Mt4Exporter.this.writeInt(var2, 0);
               Mt4Exporter.this.writeLong(var2, Mt4Exporter.this.extLastVolume[var1]);
            }
         }
      }

      @Override
      public String getFileName(int var1) {
         return Mt4Exporter.this.mt4Symbol + Mt4Exporter.TIMEFRAMES_INT[var1] + ".hst";
      }

      @Override
      public void writeHeader(DataOutputStream var1, int var2) throws IOException {
         if (this.export) {
            Mt4Exporter.this.writeInt(var1, 401);
            Mt4Exporter.this.writeString(var1, "(C)opyright 2003, MetaQuotes Software Corp.", 64);
            Mt4Exporter.this.writeString(var1, Mt4Exporter.this.mt4Symbol, 12, Mt4Exporter.this.encoding);
            Mt4Exporter.this.writeInt(var1, Mt4Exporter.TIMEFRAMES_INT[var2]);
            Mt4Exporter.this.writeInt(var1, Mt4Exporter.this.digits);
            Mt4Exporter.this.writeInt(var1, 0);
            Mt4Exporter.this.writeInt(var1, 0);
            var1.write(new byte[52]);
         }
      }

      @Override
      public String getPath() {
         return Mt4Exporter.this.dataFolder + "//history//" + Mt4Exporter.this.serverName;
      }
   }
}
