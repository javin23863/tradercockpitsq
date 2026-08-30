package com.strategyquant.gridlib.client;

import com.strategyquant.gridlib.compute.JobResult;
import com.strategyquant.gridlib.compute.common.ExecuteOptions;
import com.strategyquant.gridlib.config.Config;
import com.strategyquant.gridlib.message.JmsConnectionInfo;
import com.strategyquant.gridlib.sync.guard.FolderGuards;
import com.strategyquant.gridlib.topology.GridTopology;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import javax.jms.JMSException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GridClient implements AutoCloseable {
   private static final Logger LOGGER = LoggerFactory.getLogger(GridClient.class);
   private FolderGuards guards;
   private Compute compute;
   private Config config;

   protected GridClient(Config var1, boolean var2) throws JMSException, IOException {
      this.config = var1;
      JmsConnectionInfo var3 = new JmsConnectionInfo(var1.getServerLocation());
      LOGGER.debug("Jms ident:" + var3.getIdent());
      this.compute = new Compute(var3, var1, var2);
      if (!this.compute.isLocalMode()) {
         this.guards = new FolderGuards(var1.getFolderConfigs().getDataFolderRoot(), var3);
         this.prepareGuards();
      }
   }

   public GridClient(Config var1) throws JMSException, IOException {
      this(var1, false);
   }

   public Config getConfig() {
      return this.config;
   }

   protected void jobFinished(String var1, String var2, JobResult<?> var3) {
      this.compute.jobFinished(var1, var2, var3);
   }

   public GridTopology getGridTopology() {
      return this.compute.getGridTopology();
   }

   public String getGridDescriptions() {
      return this.compute.getGridDescriptions();
   }

   private void prepareGuards() throws IOException, JMSException {
      LOGGER.debug("Preparing guards");
      this.guards.create("libs", false);
      this.guards.create("data", false);
      this.guards.create("extends", true);
   }

   public void dataChanged() throws JMSException, IOException {
      LOGGER.debug("Current data version changed");
      if (this.guards != null) {
         this.guards.updateHash();
      }
   }

   @Override
   public void close() throws Exception {
      LOGGER.debug("Disposing gridClient.");
      this.compute.close();
      if (this.guards != null) {
         this.guards.close();
      }
   }

   public void executeOnGrid(String var1, List<? extends GridJob<?>> var2) throws Exception {
      this.executeOnGrid(var1, var2, null);
   }

   public void executeOnGrid(String var1, GridJob<?> var2) throws Exception {
      LinkedList var3 = new LinkedList();
      var3.add(var2);
      this.executeOnGrid(var1, var3, null);
   }

   public void executeOnGrid(String var1, List<? extends GridJob<?>> var2, ExecuteOptions var3) throws Exception {
      this.compute.execute(var1, var2, var3);
   }

   public long countRunningJobs(String var1, boolean var2) {
      return var2 ? this.compute.getExecutedUndeliveredJobsCount(var1) : this.compute.getRunningJobsCount(var1);
   }

   public long countRunningJobs(String var1) {
      return this.countRunningJobs(var1, true);
   }

   public long countWaitingJobs(String var1) {
      return this.compute.getWaitingJobsCount(var1);
   }

   public long countAllJobs(String var1, boolean var2) {
      long var3 = var2 ? this.compute.getExecutedUndeliveredJobsCount(var1) : this.compute.getRunningJobsCount(var1);
      long var5 = this.compute.getWaitingJobsCount(var1);
      return var3 + var5;
   }

   public long countAllJobs(String var1) {
      return this.countAllJobs(var1, true);
   }

   public int getRunningThreads() {
      return this.getGridTopology().getRunningThreads();
   }

   public int getComputedThreads() {
      return this.compute.getComputedThread();
   }

   public int getUsedComputedThreads() {
      return this.compute.getUsedComputedThreads();
   }

   public void setUsedComputedThreads(int var1) {
      this.compute.setUsedComputedThreads(var1);
   }

   public void pause(String var1) {
      this.compute.pause(var1);
   }

   public void restart(String var1) {
      this.compute.restart(var1);
   }

   public void stop(String var1) {
      this.compute.stop(var1);
   }

   public void stop(String var1, String var2) {
      this.compute.stop(var1, var2);
   }

   public void pause(String var1, String var2) {
      this.compute.pause(var1, var2);
   }

   public void restart(String var1, String var2) {
      this.compute.restart(var1, var2);
   }

   public void registerMessageListener(String var1, IGridMessageListener var2) {
      this.compute.registerMessageListener(var1, var2);
   }

   public boolean isRegisteredMessageListener(String var1) {
      return this.compute.isRegisteredMessageListener(var1);
   }

   public void removeMessageListener(String var1) {
      this.compute.removeMessageListener(var1);
   }

   public boolean sendMessage(String var1, String var2, GridMessage var3) throws Exception {
      return var2 == null ? this.compute.sendMessageToListener(var1, var3) : this.compute.sendMessageToJob(var2, var1, var3);
   }

   protected Compute getCompute() {
      return this.compute;
   }

   public void sendProgress(String var1, String var2, int var3) {
      this.compute.setProgress(var1, var2, var3);
   }

   public void waitForFinish(Object var1, String var2, String var3, String var4) throws InterruptedException {
      this.compute.waitForFinish(var1, var2, var3, var4);
   }

   public void setGroupRestrictions(String var1, int var2) {
      this.compute.setGroupRestrictions(var1, var2);
   }

   public int getRecommendedBatchSize() {
      int var1 = 2 * this.getGridTopology().getTotalCores();
      if (var1 < 5) {
         var1 = 5;
      }

      return var1;
   }

   public void check() {
      this.compute.check();
   }

   public void waitForAllTaskFinished() throws InterruptedException {
      this.compute.waitForAllTaskFinished();
   }
}
