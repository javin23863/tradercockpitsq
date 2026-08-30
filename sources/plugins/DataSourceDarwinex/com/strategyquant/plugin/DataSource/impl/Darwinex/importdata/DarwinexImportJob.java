package com.strategyquant.plugin.DataSource.impl.Darwinex.importdata;

import com.strategyquant.datalib.basket.BasketOfStocksManager;
import com.strategyquant.datalib.darwinex.DarwinexUtils;
import com.strategyquant.datalib.data.DataManager;
import com.strategyquant.datalib.data.io.VersatileData;
import com.strategyquant.datalib.data.io.newDataFormat.DataBinWriterNew;
import com.strategyquant.gridlib.client.GridJob;
import com.strategyquant.gridlib.client.GridMessage;
import com.strategyquant.lib.L;
import com.strategyquant.lib.SQTime;
import com.strategyquant.lib.app.MainApp;
import com.strategyquant.tradinglib.dukascopy.ImportInfo;
import com.strategyquant.tradinglib.project.websocket.DataManagerDataProgress;
import com.strategyquant.tradinglib.project.websocket.DataManagerProgressListener;
import com.strategyquant.tradinglib.project.websocket.SQWebSocketManager;
import com.strategyquant.tradinglib.project.websocket.WSDataObjects;
import it.unimi.dsi.fastutil.longs.Long2ObjectAVLTreeMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap.Entry;
import it.unimi.dsi.fastutil.objects.ObjectBidirectionalIterator;
import java.io.File;
import java.text.DecimalFormat;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DarwinexImportJob extends GridJob<Void> {
   private static final long serialVersionUID = 1L;
   public static final Logger Log = LoggerFactory.getLogger(DarwinexImportJob.class);
   DataBinWriterNew writer = null;
   String fileName;
   String tempFileName;
   private ImportInfo importInfo;
   private String path;
   private volatile boolean stopped;
   private volatile boolean paused;
   private DataManagerProgressListener listener;
   private DateTimeFormatter dateFormatter = DateTimeFormat.forPattern("yyyy-MM-dd");
   private static DecimalFormat int2Form = new DecimalFormat("00");
   double lastAsk = 0.0;
   double lastBid = 0.0;
   double lastVolume = 1.0;

   public DarwinexImportJob(ImportInfo var1, String var2, String var3) throws Exception {
      super(var3, 0, null);
      this.importInfo = var1;
      this.path = var2;
   }

   private void evalFromTo(File var1) {
      Long var2 = this.getMinMaxDate(var1.getAbsolutePath(), true);
      if (var2 != null) {
         this.importInfo.dateFrom = var2;
      }

      Long var3 = this.getMinMaxDate(var1.getAbsolutePath(), false);
      if (var3 != null) {
         this.importInfo.dateTo = var3;
      }
   }

   private Long getMinMaxDate(String var1, boolean var2) {
      File var3 = new File(var1);
      if (!var3.exists()) {
         return null;
      }

      long var4 = 0L;
      long var6 = 0L;
      File[] var8 = var3.listFiles();

      for (int var9 = 0; var9 < var8.length; var9++) {
         try {
            var6 = this.parseTime(var8[var9]);
         } catch (Exception var11) {
            Log.error("Cannot parse file.", var11);
         }

         if (var4 == 0L) {
            var4 = var6;
         } else if (var2) {
            if (var4 > var6) {
               var4 = var6;
            }
         } else if (var4 < var6) {
            var4 = var6;
         }
      }

      return var4;
   }

   private long parseTime(File var1) {
      String var2 = var1.getName();
      int var3 = var2.indexOf(46);
      if (var3 >= 0) {
         var2 = var2.substring(0, var3);
      }

      String[] var4 = var2.split("_");
      return this.dateFormatter.parseMillis(var4[2]);
   }

   public void messageReceived(GridMessage var1) {
      super.messageReceived(var1);
      switch (var1.getMessageID()) {
         case 2:
            this.paused = true;
            break;
         case 3:
            this.paused = false;
            break;
         case 4:
            this.stopped = true;
      }
   }

   private void finish() {
      try {
         int var1 = SQTime.getDaysBetween(this.importInfo.fromTime, this.importInfo.toTime) + 1;
         long var2 = var1 * 86400;
         DataManager.updateData(
            "History", this.importInfo.symbol, this.importInfo.fromTime, this.importInfo.toTime, this.importInfo.rows, var2, 1, "TICK", "Etc/UCT"
         );
         if (BasketOfStocksManager.getInstance() != null) {
            BasketOfStocksManager.getInstance().updateGroupsOfSymbols();
            SQWebSocketManager.addToDataQueue(WSDataObjects.getBaskets(), new String[]{"SQUANT", "QDM", "AlgoWizard"});
         }

         try {
            this.writer.close();
         } catch (Exception var5) {
         }

         this.listener.setMessage(this.stopped ? L.t("Stopped", new Object[0]) : L.t("Completed", new Object[0]));
         new File(this.tempFileName).renameTo(new File(this.fileName));
         this.listener.onProgress(100.0);
         this.listener.onFinish();
      } catch (Exception var6) {
         Log.error("Error while saving results", var6);
         throw new RuntimeException(L.t("Error while saving results", new Object[0]));
      }
   }

   private void initWriter() throws Exception {
      this.listener.setMessage(L.t("Preparing data files", new Object[]{true}));
      this.writer = DataBinWriterNew.getInstance(2, MainApp.getDataPath(), this.importInfo.instrumentInfo);
      this.fileName = DataManager.getDataFileName("History", this.importInfo.symbol, "TICK", "No Session");
      this.tempFileName = DataManager.getTempFileName(this.fileName);
      this.writer.setFileName(this.tempFileName);
      this.writer.open();
   }

   public void setProgressListener(DataManagerProgressListener var1) {
      this.listener = var1;
   }

   public Void call() throws Exception {
      try {
         this.initWriter();
         File var1 = new File(new File(this.path), this.importInfo.origSymbol);
         if (!var1.exists()) {
            this.listener.setMessage(L.t("Completed - no data found", new Object[]{true}));
            return null;
         }

         this.evalFromTo(var1);
         this.listener.setMessage(L.t("Importing Darwinex data ...", new Object[]{true}));
         this.listener.onProgress(1.0);
         double var5 = SQTime.getDaysBetween(this.importInfo.dateFrom, this.importInfo.dateTo) + 1;
         double var7 = 0.0;
         Long2ObjectAVLTreeMap var9 = new Long2ObjectAVLTreeMap();
         long var10 = this.importInfo.dateFrom;

         while (var10 <= this.importInfo.dateTo) {
            String var4 = this.dateFormatter.print(var10);

            for (int var12 = 0; var12 < 24; var12++) {
               File var2 = new File(var1, this.importInfo.origSymbol + "_ASK_" + var4 + "_" + int2Form.format(var12) + ".log.gz");
               File var3 = new File(var1, this.importInfo.origSymbol + "_BID_" + var4 + "_" + int2Form.format(var12) + ".log.gz");
               this.writeData(var2, var3, var9);
            }

            var10 += 86400000L;
            var7++;
            this.listener.onProgress(var7 / var5 * 100.0);
            if (this.stopped) {
               break;
            }

            this.checkPaused();
         }

         this.finish();
         this.sendRefreshMessage();
      } catch (Exception var16) {
         Log.error("Error while importing Darwinex data", var16);
         this.listener.onError(L.t("Error while importing Darwinex data", new Object[]{true}));
      } finally {
         DataManagerDataProgress.get().removeListener(this.importInfo.symbol);
      }

      return null;
   }

   private void writeData(File var1, File var2, Long2ObjectAVLTreeMap<VersatileData> var3) throws Exception {
      if (var1.exists() || var2.exists()) {
         var3.clear();
         DarwinexUtils.getTickData(var1, var3, true);
         DarwinexUtils.getTickData(var2, var3, false);
         ObjectBidirectionalIterator var7 = var3.long2ObjectEntrySet().iterator();

         while (var7.hasNext()) {
            Entry var8 = (Entry)var7.next();
            long var5 = var8.getLongKey();
            VersatileData var4 = (VersatileData)var8.getValue();
            if (var4.ask == 0.0) {
               var4.ask = this.lastAsk;
            }

            if (var4.bid == 0.0) {
               var4.bid = this.lastBid;
            }

            if (var4.volume == 0.0) {
               var4.volume = this.lastVolume;
            }

            this.lastAsk = var4.ask;
            this.lastBid = var4.bid;
            this.lastVolume = var4.volume;
            this.writer.writeData(var4);
            this.importInfo.rows++;
            if (this.importInfo.fromTime == 0L) {
               this.importInfo.fromTime = var5;
            }

            this.importInfo.toTime = var5;
         }
      }
   }

   private void checkPaused() throws InterruptedException {
      if (this.paused) {
         this.listener.onPause();

         while (this.paused && !this.stopped) {
            Thread.sleep(100L);
         }

         this.listener.onContinue();
      }
   }

   private void sendRefreshMessage() {
      try {
         SQWebSocketManager.addToDataQueue(WSDataObjects.getData(this.importInfo.symbol, "import"), new String[]{"SQUANT", "QDM", "AlgoWizard"});
      } catch (Exception var2) {
         Log.error("Cannot send data Websocket message. ", var2);
      }
   }
}
