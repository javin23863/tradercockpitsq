package com.strategyquant.plugin.DataManager.impl.Data.csvexport;

import com.strategyquant.datalib.ChartDef;
import com.strategyquant.datalib.DataInfo;
import com.strategyquant.datalib.broker.BrokerDto;
import com.strategyquant.datalib.broker.BrokerManager;
import com.strategyquant.datalib.data.DataManager;
import com.strategyquant.datalib.data.DateShifter;
import com.strategyquant.datalib.data.io.IDataLoader;
import com.strategyquant.datalib.data.io.VersatileData;
import com.strategyquant.lib.L;
import com.strategyquant.lib.SQTime;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.utils.IProgressListener;
import com.strategyquant.plugin.DataManager.impl.Data.csvexport.format.Format;
import java.io.File;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CsvExporter {
   private static final int REFRESH_RATE = 100;
   public static final Logger Log = LoggerFactory.getLogger(CsvExporter.class);
   private volatile boolean canceled;
   private boolean paused;
   private IProgressListener listener;
   private DateShifter dateShifter;

   public void exportData(
      IProgressListener var1, String var2, String var3, String var4, String var5, String var6, long var7, long var9, Format var11, String var12
   ) {
      int var14 = 0;
      this.canceled = false;
      this.listener = var1;

      try {
         var1.onStart();
         ChartDef var15 = new ChartDef(var2, var3, var4, var7, var9, 2.5, var5);
         IDataLoader var16 = DataManager.getDataLoader(var15, 1);
         var16.open();
         DataInfo var17 = DataManager.getDataInfo(var2, var3);
         this.evalTimeZones(var17, var12);
         DataManager.checkDataExport(var16, var17.source);
         int var18 = var17.rows;
         File var19 = new File(var6);
         if (!var19.exists()) {
            var19.getParentFile().mkdirs();
         }

         RandomAccessFile var20 = new RandomAccessFile(var6, "rw");
         ByteBuffer var21 = ByteBuffer.allocate(65536);
         VersatileData var22 = new VersatileData();
         if (var11.includeHeader) {
            String var13 = var11.printHeader() + "\n";
            var21.put(var13.getBytes());
            var21.flip();
            var20.getChannel().write(var21);
            var21.clear();
         }

         while (var16.hasNextTick() && !this.canceled) {
            this.checkPaused();
            var22.reset();
            var16.getNextTick(var22);
            this.dateShifter.transformToTimeZone(var22);
            String var24 = var11.printData(var3, var22, var17.symbolInfo.decimals) + "\n";
            var21.put(var24.getBytes());
            var14++;
            if (var21.remaining() < 200) {
               var21.flip();
               var20.getChannel().write(var21);
               var21.clear();
            }

            if (var14 % 100 == 0) {
               var1.setMessage(L.t("Exported to CSV %s", new Object[]{SQTime.toDateMinuteString(var22.time)}));
               var1.onProgress(SQUtils.round((double)var14 / var18 * 100.0, 1));
            }
         }

         if (var21.position() > 0) {
            var21.flip();
            var20.getChannel().write(var21);
            var21.clear();
         }

         var1.onProgress(100.0);
         var1.setMessage(L.t("Completed", new Object[0]));
         var1.onFinish();
         var16.close();
         var20.close();
      } catch (Exception var23) {
         Log.error("Error while exporting data", var23);
         var1.onError(var23.getMessage());
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
