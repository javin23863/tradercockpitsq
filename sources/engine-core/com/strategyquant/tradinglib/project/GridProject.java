package com.strategyquant.tradinglib.project;

import com.strategyquant.gridlib.client.GridClient;
import com.strategyquant.gridlib.client.RunningStatus;
import com.strategyquant.gridlib.client.SQGrid;
import com.strategyquant.lib.L;
import com.strategyquant.lib.SQUtils;
import org.jdom2.Element;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class GridProject implements IProject {
   private static final Logger Log = LoggerFactory.getLogger(GridProject.class);
   protected Databanks databanks;
   private boolean stopped = false;
   private boolean paused = false;
   private boolean feedFinished = false;
   private long lastProcessedIndex;
   private int runningStatus = 0;
   private String projectName = null;
   protected Thread taskGrid = null;
   private long lastSentProgress = 0L;
   protected GridClient gridClient;
   private int progressType = 1;
   private long progressValue = 0L;
   private long progressMax = 0L;

   public GridProject(String var1) throws Exception {
      this.projectName = var1;
      this.databanks = new Databanks(var1);
      this.lastProcessedIndex = 0L;
      this.gridClient = SQGrid.getGridClient();
   }

   @Override
   public Databanks getDatabanks() {
      return this.databanks;
   }

   @Override
   public Element getConfig() throws Exception {
      return ProjectEngine.get(this.projectName).getConfig();
   }

   public abstract void projectStart() throws Exception;

   @Override
   public void start() throws Exception {
      try {
         this.setRunningStatus(1);
         this.initStart();
         this.taskGrid = new Thread() {
            @Override
            public void run() {
               try {
                  GridProject.this.projectStart();
               } catch (Exception var2) {
                  GridProject.Log.error("Exc.", var2);
                  GridProject.this.changeProjectStatus(50, GridProject.this.getExceptionString(var2));
               }
            }
         };
         this.taskGrid.start();
      } catch (Exception var2) {
         Log.error("Exc.", var2);
         this.changeProjectStatus(50, this.getExceptionString(var2));
         throw var2;
      }
   }

   private String getExceptionString(Exception var1) {
      String var2 = SQUtils.stackTraceToString(var1);
      return var2.length() > 500 ? var2.substring(0, 500) : var2;
   }

   private void initStart() throws Exception {
      this.stopped = false;
      this.paused = false;
      this.feedFinished = false;
      if (this.taskGrid != null && this.taskGrid.isAlive()) {
         throw new Exception(L.t("Project '%s' is already running.", new Object[]{this.projectName}));
      }
   }

   public boolean isStopped() {
      return this.stopped;
   }

   public boolean isPaused() {
      return this.paused;
   }

   @Override
   public void stop() {
      this.printLog("Stopping...");
      this.stopped = true;
      this.printLog("Stopped");
      this.lastProcessedIndex = 0L;
      this.progressValue = 0L;
   }

   @Override
   public void pause() {
      this.printLog("Pausing...");
      this.paused = true;
      this.setRunningStatus(2);
   }

   @Override
   public void resume() throws Exception {
      this.start();
   }

   protected long getLastProcessedIndex() {
      return this.lastProcessedIndex;
   }

   protected void setLastProcessedIndex(long var1) {
      this.lastProcessedIndex = var1;
   }

   protected void setProgressType(int var1) {
      this.progressType = var1;
      this.sendProgressInfo();
   }

   protected void setProgressMax(long var1) {
      this.progressMax = var1;
      this.sendProgressInfo();
   }

   protected void increaseProgress() {
      this.progressValue++;
      this.sendProgressInfo();
   }

   private void sendProgressInfo() {
      long var1 = (long)((double)this.progressValue / this.progressMax * 100.0);
      boolean var3 = this.progressType == 0;
      if (var3) {
         var1 = 100L;
      } else if (this.lastSentProgress == var1) {
         return;
      }

      this.lastSentProgress = var1;
   }

   @Override
   public int getRunningStatus() {
      return this.runningStatus;
   }

   @Override
   public void setRunningStatus(int var1) {
      this.changeProjectStatus(var1, null);
   }

   private void changeProjectStatus(int var1, String var2) {
      this.runningStatus = var1;
      JSONObject var3 = new JSONObject();
      var3.put("projectName", this.projectName);
      var3.put("status", var1);
      if (var1 == 50) {
         this.printLog("Running error: " + var2);
         this.stop();
      }

      Log.info("Project status changed to " + RunningStatus.printStatus(var1));
   }

   protected void printLog(String var1) {
      SQLogger.log(this.projectName, var1);
      Log.info(var1);
   }

   @Override
   public String getName() {
      return this.projectName;
   }

   public String getProduct() {
      return "SQUANTTESTAPP";
   }

   public int getPreferredPosition() {
      return 0;
   }

   public void initPlugin() throws Exception {
   }
}
