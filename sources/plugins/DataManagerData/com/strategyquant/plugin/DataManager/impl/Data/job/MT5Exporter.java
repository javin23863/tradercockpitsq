package com.strategyquant.plugin.DataManager.impl.Data.job;

import com.strategyquant.datalib.ChartDef;
import com.strategyquant.datalib.DataInfo;
import com.strategyquant.datalib.TickEvent;
import com.strategyquant.datalib.bartype.BarType;
import com.strategyquant.datalib.bartype.BarTypeFactory;
import com.strategyquant.datalib.bartype.BarTypeStatus;
import com.strategyquant.datalib.broker.BrokerDto;
import com.strategyquant.datalib.broker.BrokerManager;
import com.strategyquant.datalib.data.DataManager;
import com.strategyquant.datalib.data.DateShifter;
import com.strategyquant.datalib.data.io.IDataLoader;
import com.strategyquant.datalib.data.io.VersatileData;
import com.strategyquant.datalib.ticksimulator.DefaultTickSimulator;
import com.strategyquant.lib.L;
import com.strategyquant.lib.SQTime;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.utils.IProgressListener;
import java.io.File;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MT5Exporter {
   private static final int REFRESH_RATE = 100;
   public static final Logger Log = LoggerFactory.getLogger(MT5Exporter.class);
   private volatile boolean canceled;
   private boolean paused;
   private IProgressListener listener;
   private DataInfo dataInfo;
   private int decimals = 6;
   private DateShifter dateShifter;

   public void exportData(
      IProgressListener var1, String var2, String var3, String var4, String var5, double var6, double var8, String var10, long var11, long var13, String var15
   ) {
      this.canceled = false;
      this.listener = var1;

      try {
         if (!var4.equals("TICK") && !var4.equals("M1")) {
            throw new Exception(L.t("Export to MT5 works only with TICK and M1 data.", new Object[0]));
         }

         this.dataInfo = DataManager.getDataInfo(var2, var3);
         this.decimals = this.dataInfo.symbolInfo.decimals;
         this.evalTimeZones(this.dataInfo, var15);
         var1.onStart();
         File var16 = new File(var10);
         if (!var16.exists()) {
            var16.getParentFile().mkdirs();
         }

         if (!var4.equals("TICK") && !var5.equals("pips") && !var5.equals("points")) {
            this.exportM1WithRealSpread(var1, var2, var3, this.dataInfo, var10, var11, var13);
         } else {
            this.exportTickOrM1WithFixedSpread(var1, var2, var3, this.dataInfo, var4, var5, var6, var8, var10, var11, var13);
         }

         var1.onProgress(100.0);
         var1.setMessage(L.t("Completed", new Object[0]));
         var1.onFinish();
      } catch (Exception var17) {
         Log.error("Error while exporting data to MT5. Exc.", var17);
         var1.onError(var17.getMessage());
      }
   }

   private void evalTimeZones(DataInfo var1, String var2) {
      String var3 = "UTC";
      if (var1.brokerId != -1) {
         BrokerDto var4 = BrokerManager.getInstance().getBroker(var1.brokerId);
         var3 = var4.getMtTimezone();
      }

      this.dateShifter = new DateShifter(var3, var2);
   }

   private void exportTickOrM1WithFixedSpread(
      IProgressListener var1, String var2, String var3, DataInfo var4, String var5, String var6, double var7, double var9, String var11, long var12, long var14
   ) throws Exception {
      ChartDef var16 = new ChartDef(var2, var3, var5, var12, var14, 2.5, "No Session");
      IDataLoader var17 = DataManager.getDataLoader(var16, 1);
      var17.open();
      DataManager.checkDataExport(var17, this.dataInfo.source);
      int var18 = var4.rows;
      RandomAccessFile var19 = new RandomAccessFile(var11, "rw");
      var19.setLength(0L);
      ByteBuffer var20 = ByteBuffer.allocate(65536);
      VersatileData var21 = new VersatileData();
      int var22 = 0;

      while (var17.hasNextTick() && !this.canceled) {
         this.checkPaused();
         var21.reset();
         var17.getNextTick(var21);
         this.dateShifter.transformToTimeZone(var21);
         String var26 = "";
         int var25 = var21.volume > 1.0 ? SQUtils.round(var21.volume) : 1;
         if (var21.type == 2) {
            String var27 = SQTime.formatDate(var21.time);
            String var28 = SQTime.formatTime(var21.time);
            double var23;
            if (var6.equals("pips")) {
               var23 = var9 * 10.0;
            } else {
               var23 = var7;
            }

            var26 = var27
               + ","
               + var28
               + ","
               + this.d(var21.open)
               + ","
               + this.d(var21.high)
               + ","
               + this.d(var21.low)
               + ","
               + this.d(var21.close)
               + ","
               + var25
               + ","
               + var25
               + ","
               + SQUtils.round(var23)
               + "\n";
         } else {
            String var40 = SQTime.toFullDateTimeString(var21.time);
            Double var41 = null;
            if (var6.equals("pips")) {
               var41 = var9 * 10.0;
            } else if (var6.equals("points")) {
               var41 = var7;
            }

            if (var41 != null) {
               double var29 = var41 * var4.symbolInfo.tickStep;
               double var31 = (var21.bid + var21.ask) / 2.0;
               double var33 = var29 / 2.0;
               double var35 = var31 - var33;
               double var37 = var31 + var33;
               var26 = var40 + "," + this.d(var35) + "," + this.d(var37) + "\n";
            } else {
               var26 = var40 + "," + this.d(var21.bid) + "," + this.d(var21.ask) + "\n";
            }
         }

         var20.put(var26.getBytes());
         var22++;
         if (var20.remaining() < 100) {
            var20.flip();
            var19.getChannel().write(var20);
            var20.clear();
         }

         if (var22 % 100 == 0) {
            var1.setMessage(L.t("Exported to MT5 %s", new Object[]{SQTime.toDateMinuteString(var21.time)}));
            var1.onProgress(SQUtils.round((double)var22 / var18 * 100.0, 1));
         }
      }

      if (var20.position() > 0) {
         var20.flip();
         var19.getChannel().write(var20);
         var20.clear();
      }

      var17.close();
      var19.close();
   }

   private String d(double var1) {
      return SQUtils.d2String(var1, this.decimals);
   }

   private void exportM1WithRealSpread(IProgressListener var1, String var2, String var3, DataInfo var4, String var5, long var6, long var8) throws Exception {
      ChartDef var10 = new ChartDef(var2, var3, "TICK", var6, var8, 2.5, "No Session");
      IDataLoader var11 = DataManager.getDataLoader(var10, 1);
      var11.open();
      DataManager.checkDataExport(var11, var4.source);
      int var12 = var4.rows;
      RandomAccessFile var13 = new RandomAccessFile(var5, "rw");
      ByteBuffer var14 = ByteBuffer.allocate(65536);
      VersatileData var15 = new VersatileData();
      BarTypeStatus var16 = new BarTypeStatus();
      DefaultTickSimulator var17 = new DefaultTickSimulator();
      TickEvent var18 = new TickEvent();
      VersatileData var19 = new VersatileData();
      BarType var20 = BarTypeFactory.getBarType("M1", var4.barTimeType);
      var20 = var20.clone(var20.getTimeframe());
      int var21 = 0;
      boolean var22 = false;

      while (var11.hasNextTick() && !this.canceled) {
         this.checkPaused();
         var11.getNextTick(var15);
         this.dateShifter.transformToTimeZone(var15);
         var15.spread = -1.0;
         var17.init(var15);

         while (var17.getNextTick(var18)) {
            var20.processTick(var18, var16, 1);
            if (var16.status == 0) {
               this.writeData(var13, var14, var19);
               var22 = false;
            } else if (var16.status == 1) {
               this.writeData(var13, var14, var19);
               this.initBarData(var19, var16.barTime, var18.getAsk(), var18.getBid(), var18.getVolume());
               var22 = true;
            } else {
               this.updateBarData(var19, var18.getAsk(), var18.getBid(), var18.getVolume());
            }
         }

         if (++var21 % 100 == 0) {
            var1.setMessage(L.t("Exported to MT5 %s", new Object[]{SQTime.toDateMinuteString(var15.time)}));
            var1.onProgress(SQUtils.round((double)var21 / var12 * 100.0, 1));
         }
      }

      if (var22) {
         this.writeData(var13, var14, var19);
      }

      if (var14.position() > 0) {
         var14.flip();
         var13.getChannel().write(var14);
         var14.clear();
      }

      var11.close();
      var13.close();
   }

   private void writeData(RandomAccessFile var1, ByteBuffer var2, VersatileData var3) throws Exception {
      if (var3.time != -1L && var3.time != Long.MIN_VALUE) {
         String var4 = "";
         String var5 = SQTime.formatDate(var3.time);
         String var6 = SQTime.formatTime(var3.time);
         int var7 = new Double(var3.spread / this.dataInfo.symbolInfo.tickStep).intValue();
         var4 = var5
            + ","
            + var6
            + ","
            + this.d(var3.open)
            + ","
            + this.d(var3.high)
            + ","
            + this.d(var3.low)
            + ","
            + this.d(var3.close)
            + ","
            + SQUtils.round2(var3.volume)
            + ","
            + SQUtils.round2(var3.volume)
            + ","
            + var7
            + "\n";
         var2.put(var4.getBytes());
         if (var2.remaining() < 100) {
            var2.flip();
            var1.getChannel().write(var2);
            var2.clear();
         }
      }
   }

   private void updateBarData(VersatileData var1, double var2, double var4, double var6) {
      if (var4 > var1.high) {
         var1.high = var4;
      }

      if (var4 < var1.low) {
         var1.low = var4;
      }

      var1.close = var4;
      var1.volume += var6;
      double var8 = var2 - var4;
      if (var8 > var1.spread) {
         var1.spread = var8;
      }
   }

   private void initBarData(VersatileData var1, long var2, double var4, double var6, double var8) {
      var1.time = var2;
      var1.open = var6;
      var1.high = var6;
      var1.low = var6;
      var1.close = var6;
      var1.volume = var8;
      var1.spread = var4 - var6;
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

   public void pause() {
      this.paused = true;
   }

   public void restart() {
      this.paused = false;
   }

   public void cancel() {
      this.canceled = true;
   }
}
