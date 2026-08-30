package com.strategyquant.tradinglib.simplegrid;

import com.strategyquant.datalib.TradingException;
import com.strategyquant.gridlib.client.GridClient;
import com.strategyquant.gridlib.client.GridJob;
import com.strategyquant.gridlib.client.GridMessage;
import com.strategyquant.gridlib.client.IGridMessageListener;
import com.strategyquant.gridlib.client.JobDetails;
import com.strategyquant.gridlib.client.SQGrid;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.app.MainApp;
import com.strategyquant.lib.utils.SQUUID;
import com.strategyquant.tradinglib.project.IProgressStatusListener;
import com.strategyquant.tradinglib.project.StopPauseEngine;
import java.io.Serializable;
import java.util.ArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class SimpleGridEngine<JobClass extends GridJob<ResultClass>, ResultClass extends Serializable> implements IProgressStatusListener {
   public static Logger Log = LoggerFactory.getLogger("SimpleGridEngine");
   private long start;
   protected int batchSize = 0;
   private boolean allDone = false;
   private GridClient gridClient;
   private StopPauseEngine progressEngine;
   private String jobGroupID;
   private int jobsCountThreshold;
   private GridJob parentGridJob;
   private boolean singleThreaded = false;
   ArrayList<JobClass> firstJobsGroup = null;

   public SimpleGridEngine(StopPauseEngine var1, GridJob var2) throws Exception {
      this(var1, var2, false);
   }

   public SimpleGridEngine(StopPauseEngine var1, GridJob var2, boolean var3) throws Exception {
      this.gridClient = SQGrid.getGridClient();
      this.progressEngine = var1;
      this.jobGroupID = SQUUID.randomUUID();
      this.parentGridJob = var2;
      this.singleThreaded = var3;
      this.batchSize = 5;
      this.jobsCountThreshold = SQGrid.getGridClient().getRecommendedBatchSize();
      this.gridClient.registerMessageListener(this.jobGroupID, new IGridMessageListener() {
         public void messageReceived(GridMessage var1) {
            SimpleGridEngine.this.processMessage(var1);
         }
      });
      var1.registerStatusListener(this);
   }

   public void unregisterListeners() {
      if (this.progressEngine != null) {
         this.progressEngine.unregisterStatusListener(this);
      }

      if (this.gridClient != null) {
         this.gridClient.removeMessageListener(this.jobGroupID);
      }
   }

   @Override
   public void onStatusChanged(int var1) {
      if (var1 == 4 || var1 == 3 || var1 == 50) {
         this.gridClient.stop(this.jobGroupID);
         this.progressEngine.unregisterStatusListener(this);
      }
   }

   public void start() throws Exception {
      if (this.singleThreaded) {
         this.runSingleThreaded();
      } else {
         this.runOnGrid();
      }
   }

   private void runSingleThreaded() throws Exception {
      Object var1 = null;

      while (true) {
         ArrayList var2;
         if (this.firstJobsGroup != null) {
            var2 = this.firstJobsGroup;
         } else {
            var2 = this.createJobsBatch(this.batchSize, this.gridClient);
         }

         if (var2 == null) {
            return;
         }

         for (int var3 = 0; var3 < var2.size(); var3++) {
            GridJob var4 = (GridJob)var2.get(var3);
            this.progressEngine.checkPaused();
            if (this.progressEngine.isStopped()) {
               return;
            }

            JobDetails var5 = new JobDetails(var4.getJobId());
            Serializable var6 = null;

            try {
               long var7 = System.currentTimeMillis();
               var6 = (Serializable)var4.call();
               long var9 = System.currentTimeMillis() - var7;
               var5.setDuration(var9);
               this.processResult((ResultClass)var6, var5);
            } catch (TradingException var15) {
               Log.error("Trading exception during optimization", var15);
            } catch (Throwable var16) {
               if ("BACKTESTNODE".equals(MainApp.getProduct())) {
                  var5.setException(var16.getMessage());
               } else {
                  var5.setException(SQUtils.getStackTrace(var16));
               }

               this.processResult((ResultClass)var6, var5);
               Log.error("Error while running task #{}", var3, var16);
            } finally {
               if (var4 != null) {
                  var4.destroy();
               }
            }
         }
      }
   }

   private void runOnGrid() throws Exception {
      ArrayList var1;
      if (this.firstJobsGroup != null) {
         var1 = this.firstJobsGroup;
      } else {
         var1 = this.createJobsBatch(this.batchSize, this.gridClient);
      }

      if (var1 != null) {
         if (this.parentGridJob != null) {
            for (GridJob var3 : var1) {
               var3.setPriority(this.parentGridJob.getPriority() + 1);
            }
         }

         this.gridClient.executeOnGrid(this.jobGroupID, var1);
         if (this.parentGridJob != null) {
            this.gridClient.waitForFinish(this, this.parentGridJob.getJobId(), this.parentGridJob.getJobGroupId(), this.jobGroupID);
         } else {
            this.gridClient.waitForFinish(this, null, null, this.jobGroupID);
         }

         this.gridClient.removeMessageListener(this.jobGroupID);
      }
   }

   protected void processMessage(GridMessage var1) {
      try {
         if (var1.getMessageID() == 1) {
            JobDetails var2 = var1.getJobDetails();
            Serializable var3 = null;
            if (var1.getData() != null) {
               var3 = var1.getData();
            }

            this.processResult((ResultClass)var3, var2);
            this.progressEngine.checkPaused();
            if (this.progressEngine.isStopped()) {
               this.gridClient.stop(this.jobGroupID);
               return;
            }
         }
      } catch (Exception var7) {
         Log.error("Cannot process finished job", var7);
         this.onError("Cannot process finished job", var7);
      }

      try {
         if (var1.getMessageID() == 1) {
            synchronized (this.gridClient) {
               if (this.gridClient.countAllJobs(this.jobGroupID) < this.jobsCountThreshold && !this.progressEngine.isStopped()) {
                  ArrayList var9 = this.createJobsBatch(this.batchSize, this.gridClient);
                  if (var9 != null) {
                     this.gridClient.executeOnGrid(this.jobGroupID, var9);
                  } else if (this.gridClient.countAllJobs(this.jobGroupID) == 0L && !this.progressEngine.isPaused()) {
                     this.gridClient.stop(this.jobGroupID);
                  }
               }
            }
         }
      } catch (Exception var6) {
         Log.error("Cannot create new batch of jobs", var6);
         this.onError("Cannot create new batch of jobs", var6);
      }
   }

   public boolean isStopped() {
      return false;
   }

   public boolean shouldRestart() {
      return this.gridClient.countAllJobs(this.jobGroupID) == 0L;
   }

   public void stopped() {
      this.gridClient.stop(this.jobGroupID);
   }

   public void addFirstJobsBatch(ArrayList<JobClass> var1) {
      this.firstJobsGroup = var1;
   }

   protected abstract ArrayList<JobClass> createJobsBatch(int var1, GridClient var2) throws Exception;

   protected abstract void processResult(ResultClass var1, JobDetails var2) throws Exception;

   protected abstract void onError(String var1, Exception var2);
}
