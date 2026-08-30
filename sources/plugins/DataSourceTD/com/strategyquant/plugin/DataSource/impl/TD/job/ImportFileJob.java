package com.strategyquant.plugin.DataSource.impl.TD.job;

import com.strategyquant.datalib.basket.BasketOfStocksManager;
import com.strategyquant.datalib.data.DataManager;
import com.strategyquant.datalib.data.io.newDataFormat.DataBinReaderNew;
import com.strategyquant.datalib.data.io.newDataFormat.DataBinWriterNew;
import com.strategyquant.gridlib.client.GridJob;
import com.strategyquant.gridlib.client.GridMessage;
import com.strategyquant.lib.L;
import com.strategyquant.lib.SQTime;
import com.strategyquant.lib.utils.Pair;
import com.strategyquant.tradinglib.dukascopy.DownloadResultDto;
import com.strategyquant.tradinglib.dukascopy.DukascopyBinaryMerger;
import com.strategyquant.tradinglib.dukascopy.DukascopyUtils;
import com.strategyquant.tradinglib.dukascopy.ImportInfo;
import com.strategyquant.tradinglib.project.websocket.DataManagerDataProgress;
import com.strategyquant.tradinglib.project.websocket.DataManagerProgressListener;
import com.strategyquant.tradinglib.project.websocket.SQWebSocketManager;
import com.strategyquant.tradinglib.project.websocket.WSDataObjects;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ImportFileJob extends GridJob<Void> {
   private static final long serialVersionUID = 1L;
   private static final int HOURS = 24;
   public static final Logger Log = LoggerFactory.getLogger(ImportFileJob.class);
   private DataBinReaderNew reader;
   private DataBinWriterNew writer;
   private DukascopyBinaryMerger merger;
   private ImportInfo importInfo;
   private String path;
   private volatile boolean stopped;
   private volatile boolean paused;
   private DataManagerProgressListener listener;

   public ImportFileJob(ImportInfo var1, String var2, String var3) throws Exception {
      super(var3, 0, null);
      this.importInfo = var1;
      this.path = var2;
   }

   private void evalFromTo(File var1) {
      Long var2 = this.getFolderDate(var1.getAbsolutePath(), true);
      if (var2 != null) {
         this.importInfo.dateFrom = var2;
      }

      Long var3 = this.getFolderDate(var1.getAbsolutePath(), false);
      if (var3 != null) {
         this.importInfo.dateTo = var3;
      }
   }

   private Long getFolderDate(String var1, boolean var2) {
      File var3 = new File(var1);
      if (!var3.exists()) {
         return null;
      }

      Pair var4 = this.getMinMaxFromFolder(var3.getAbsolutePath());
      if (var4 == null) {
         return null;
      }

      int var5 = var2 ? (Integer)var4.getA() : (Integer)var4.getB();
      Pair var6 = this.getMinMaxFromFolder(var3.getAbsolutePath() + "\\" + String.format("%02d", var5));
      if (var6 == null) {
         return var2 ? SQTime.toLong(var5, 1, 1) : SQTime.toLong(var5, 12, 31);
      }

      int var7 = var2 ? (Integer)var6.getA() : (Integer)var6.getB();
      Pair var8 = this.getMinMaxFromFolder(var3.getAbsolutePath() + "\\" + String.format("%02d", var5) + "\\" + String.format("%02d", var7));
      if (var8 == null) {
         return var2 ? SQTime.toLong(var5, var7, 1) : SQTime.addMonths(SQTime.toLong(var5, var7, 1), 1);
      }

      int var9 = var2 ? (Integer)var8.getA() : (Integer)var8.getB();
      return SQTime.toLong(var5, var7 + 1, var9);
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
         if (this.stopped) {
            this.merger.setStopped(this.stopped);
         }

         this.merger.finish();
         long var1 = this.merger.getFromDate();
         long var3 = this.merger.getToDate();
         DataManager.updateData("History", this.importInfo.symbol, var1, var3, this.merger.getSavedRows(), this.merger.getSavedSecs(), 1, "TICK", "Etc/UCT");
         if (BasketOfStocksManager.getInstance() != null) {
            BasketOfStocksManager.getInstance().updateGroupsOfSymbols();
            SQWebSocketManager.addToDataQueue(WSDataObjects.getBaskets(), new String[]{"SQUANT", "QDM", "AlgoWizard"});
         }

         try {
            this.writer.close();
         } catch (Exception var7) {
         }

         try {
            if (this.reader != null) {
               this.reader.closeFile();
            }
         } catch (Exception var6) {
         }

         DukascopyUtils.deleteCopyFile(this.importInfo.symbol, "TICK");
      } catch (Exception var8) {
         Log.error("Error while saving results", var8);
         throw new RuntimeException(L.t("Error while saving results", new Object[0]));
      }
   }

   private String[] getSortedFolders(String var1) {
      String[] var2 = new File(var1).list(new FilenameFilter() {
         @Override
         public boolean accept(File var1, String var2x) {
            try {
               Integer.parseInt(var2x);
            } catch (Exception var4) {
               return false;
            }

            return new File(var1, var2x).isDirectory();
         }
      });
      if (var2 != null && var2.length != 0) {
         Arrays.sort(var2);
         return var2;
      } else {
         return null;
      }
   }

   private Pair<Integer, Integer> getMinMaxFromFolder(String var1) {
      String[] var2 = this.getSortedFolders(var1);
      String var3 = var2[0];
      String var4 = var2[var2.length - 1];
      return new Pair(Integer.parseInt(var3), Integer.parseInt(var4));
   }

   private void prepareFiles() throws Exception {
      String var1 = "TICK";
      this.listener.setMessage(L.t("Preparing data files", new Object[0]));
      Pair var2 = DukascopyUtils.prepareFiles(this.importInfo, var1);
      this.reader = (DataBinReaderNew)var2.getA();
      this.writer = (DataBinWriterNew)var2.getB();
   }

   public void setProgressListener(DataManagerProgressListener var1) {
      this.listener = var1;
   }

   public Void call() throws Exception {
      try {
         this.prepareFiles();
         File var1 = new File(new File(this.path), this.importInfo.origSymbol);
         this.evalFromTo(var1);
         this.merger = new DukascopyBinaryMerger("Imported", this.getJobId(), 24, this.importInfo);
         this.merger.setWriter(this.writer);
         this.merger.setReader(this.reader);
         this.merger.setProgressListener(this.listener);
         if (this.listener != null) {
            this.listener.setMessage(L.t("Starting import from file...", new Object[0]));
            this.listener.onProgress(1.0);
         }

         if (!var1.exists()) {
            this.merger.setFinishProgress(L.t("Completed - no data found", new Object[0]));
            return null;
         }

         String[] var2 = this.getSortedFolders(var1.getAbsolutePath());
         if (var2 != null) {
            for (String var6 : var2) {
               if (this.stopped) {
                  break;
               }

               String[] var7 = this.getSortedFolders(var1.getAbsolutePath() + "/" + var6);
               if (var7 != null) {
                  for (String var11 : var7) {
                     if (this.stopped) {
                        break;
                     }

                     String[] var12 = this.getSortedFolders(var1.getAbsolutePath() + "/" + var6 + "/" + var11);

                     for (String var16 : var12) {
                        long var17 = SQTime.toLong(Integer.parseInt(var6), Integer.parseInt(var11) + 1, Integer.parseInt(var16));
                        this.merger.checkDate(var17);
                        this.merger.reset(24);

                        for (int var19 = 0; var19 < 24; var19++) {
                           String var20 = var1.getAbsolutePath() + "/" + var6 + "/" + var11 + "/" + var16 + "/" + String.format("%02d", var19) + "h_ticks.bi5";
                           String var21 = this.getJobId() + "_" + var19;
                           this.merger.register(var19, var17);
                           this.handleBinaryFile(var21, var20, var17);
                           var17 = SQTime.addHours(var17, 1);
                        }

                        if (this.stopped) {
                           break;
                        }

                        this.checkPaused();
                     }
                  }
               }
            }
         }

         this.finish();
         this.merger.setFinishProgress(this.stopped ? L.t("stopped", new Object[0]) : L.t("completed", new Object[0]));
         this.sendRefreshMessage();
         return null;
      } finally {
         DataManagerDataProgress.get().removeListener(this.importInfo.symbol);
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

   private void handleBinaryFile(String var1, String var2, long var3) {
      byte[] var5 = null;
      boolean var6 = true;
      File var7 = new File(var2);

      try {
         var5 = Files.readAllBytes(var7.toPath());
      } catch (IOException var10) {
         var6 = false;
         this.merger.showError(var3, L.t("File %s is missing", new Object[]{var7.getAbsolutePath()}));
         Log.error("Error while reading file", var10);
         return;
      }

      try {
         DownloadResultDto var8 = DownloadResultDto.success(var5, 0L);
         this.merger.handleBi5Data(var8, var1, true, var6);
      } catch (Exception var9) {
         Log.error("Error while handling data", var9);
      }
   }
}
