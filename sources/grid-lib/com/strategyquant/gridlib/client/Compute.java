package com.strategyquant.gridlib.client;

import com.strategyquant.gridlib.compute.JobResult;
import com.strategyquant.gridlib.compute.common.ExecuteOptions;
import com.strategyquant.gridlib.compute.performer.FinishListener;
import com.strategyquant.gridlib.compute.performer.IComputePerformer;
import com.strategyquant.gridlib.compute.performer.IJobStatusChangedHandler;
import com.strategyquant.gridlib.compute.performer.JmsComputePerformer;
import com.strategyquant.gridlib.compute.performer.MultithreadComputePerformer;
import com.strategyquant.gridlib.config.Config;
import com.strategyquant.gridlib.message.JmsConnectionInfo;
import com.strategyquant.gridlib.topology.GridTopology;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicLong;
import javax.jms.JMSException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Compute implements AutoCloseable {
   private static final Logger LOGGER = LoggerFactory.getLogger(Compute.class);
   private Map<Integer, AtomicLong> runningCounts = new Int2ObjectOpenHashMap();
   private Map<Integer, AtomicLong> waitingCounts = new Int2ObjectOpenHashMap();
   private Map<Integer, AtomicLong> runningUndeliveredCounts = new Int2ObjectOpenHashMap();
   private JmsComputePerformer jmsPerformer;
   private MultithreadComputePerformer localPerformer;
   private final Config config;
   private boolean runOnNode;
   private ListenerManager listenerManager;
   private List<FinishListener> finishListeners = new ArrayList<>();
   private volatile String justStoppingJobGroupID;

   public Compute(JmsConnectionInfo var1, Config var2, boolean var3) throws JMSException {
      this.config = var2;
      this.runOnNode = var3;
      this.createPerformers(var1);
      boolean var4 = this.isLocalMode() && !var3;
      this.listenerManager = new ListenerManager(var1, var4);
      TimerTask var5 = new TimerTask() {
         @Override
         public void run() {
            try {
               Compute.this.checkFinishListeners();
            } catch (Exception var2x) {
               Compute.LOGGER.error("Error while handling finish listeners", var2x);
            }
         }
      };
      Timer var6 = new Timer("Timer");
      long var7 = 500L;
      long var9 = 200L;
      var6.scheduleAtFixedRate(var5, var7, var9);
   }

   protected void checkFinishListeners() {
      synchronized (this.finishListeners) {
         Iterator var2 = this.finishListeners.iterator();

         while (var2.hasNext()) {
            FinishListener var3 = (FinishListener)var2.next();
            if (var3.isFinished()) {
               LOGGER.debug("Group " + var3.getWaitGroupId() + " is finished - notify");
               var3.notifyWaitingObj();
               var2.remove();
            }
         }
      }
   }

   private void wakeupAllWaiting() {
      synchronized (this.finishListeners) {
         for (FinishListener var3 : this.finishListeners) {
            LOGGER.debug("Group " + var3.getWaitGroupId() + " is finished - notify");
            var3.notifyWaitingObj();
         }
      }
   }

   public void registerMessageListener(String var1, IGridMessageListener var2) {
      this.listenerManager.registerMessageListener(var1, var2);
   }

   public boolean isRegisteredMessageListener(String var1) {
      return this.listenerManager.isRegisteredMessageListener(var1);
   }

   public void removeMessageListener(String var1) {
      this.listenerManager.removeMessageListener(var1);
   }

   public boolean sendMessageToJob(String var1, String var2, GridMessage var3) {
      try {
         return this.getPerformer().sendMessageToJob(var1, var2, var3);
      } catch (Exception var5) {
         LOGGER.error("", var5);
         return false;
      }
   }

   public boolean sendMessageToListener(String var1, GridMessage var2) {
      return this.listenerManager.sendToListener(var1, var2);
   }

   private void prepareComputePerformer(IComputePerformer var1) {
      var1.addJobFinishedHandler(new IJobStatusChangedHandler() {
         @Override
         public void onJobFinished(String var1, String var2, JobResult<Serializable> var3) {
            Compute.this.jobFinished(var1, var2, var3);
         }

         @Override
         public void onJobExecuted(String var1, String var2) {
            Compute.this.jobExecuted(var1, var2);
         }
      });
   }

   private void jobExecuted(String var1, String var2) {
      this.decrementJobCounter(var2, this.waitingCounts);
      this.incrementJobsCounter(var2, this.runningCounts);
      this.incrementJobsCounter(var2, this.runningUndeliveredCounts);
   }

   protected void jobFinished(String var1, String var2, JobResult<?> var3) {
      GridMessage var4 = new GridMessage(1, null, var3.getData());
      JobDetails var5 = new JobDetails(var1);
      var5.setDuration(var3.getDuration());
      var5.setException(var3.getErrorMessage());
      var4.setJobDetails(var5);
      this.decrementJobCounter(var2, this.runningCounts);

      try {
         this.sendMessageToListener(var2, var4);
      } finally {
         this.decrementJobCounter(var2, this.runningUndeliveredCounts);
      }
   }

   private void createPerformers(JmsConnectionInfo var1) {
      try {
         if (var1.getTarget() != null && !this.runOnNode) {
            this.jmsPerformer = new JmsComputePerformer(var1, this.config);
            this.prepareComputePerformer(this.jmsPerformer);
            return;
         }
      } catch (JMSException var3) {
         LOGGER.error("Error while creating JMS compute performer", var3);
      }

      this.localPerformer = new MultithreadComputePerformer(this.config);
      this.prepareComputePerformer(this.localPerformer);
   }

   public boolean isLocalMode() {
      return this.jmsPerformer == null;
   }

   public IComputePerformer getPerformer() {
      return this.jmsPerformer != null && this.jmsPerformer.getGridTopology().getTotalCores() != 0 ? this.jmsPerformer : this.localPerformer;
   }

   public void execute(String var1, List<? extends GridJob<?>> var2, ExecuteOptions var3) throws Exception {
      for (GridJob var5 : var2) {
         if (var1.equals(this.justStoppingJobGroupID)) {
            LOGGER.debug("Skipping execution loop due stopping the group:" + var1);
            break;
         }

         this.incrementJobsCounter(var1, this.waitingCounts);
         var5.setJobGroupId(var1);

         try {
            this.getPerformer().execute(var5, var1, var3);
         } catch (Exception var7) {
            LOGGER.error("Error while registering job. Removing it from wating counts.", var7);
            this.decrementJobCounter(var1, this.waitingCounts);
         }
      }

      this.getPerformer().commit();
   }

   private void incrementJobsCounter(String var1, Map<Integer, AtomicLong> var2) {
      synchronized (var2) {
         int var4 = var1.hashCode();
         AtomicLong var5 = (AtomicLong)var2.get(var4);
         if (var5 == null) {
            var5 = new AtomicLong(0L);
         }

         var5.incrementAndGet();
         var2.put(var4, var5);
      }
   }

   private void decrementJobCounter(String var1, Map<Integer, AtomicLong> var2) {
      synchronized (var2) {
         int var4 = var1.hashCode();
         AtomicLong var5 = (AtomicLong)var2.get(var4);
         if (var5 != null) {
            long var6 = var5.decrementAndGet();
            if (var6 == 0L) {
               var2.remove(var4);
            }
         }
      }
   }

   @Override
   public void close() throws Exception {
      if (this.localPerformer != null) {
         this.localPerformer.close();
      }

      if (this.jmsPerformer != null) {
         this.jmsPerformer.close();
      }

      this.listenerManager.close();
      synchronized (this.finishListeners) {
         this.finishListeners.clear();
      }
   }

   public void pause(String var1) {
      try {
         this.getPerformer().pause(var1);
         this.listenerManager.sendToListener(var1, new GridMessage(2));
      } catch (Exception var3) {
         LOGGER.error("", var3);
      }
   }

   public void stop(String var1) {
      this.justStoppingJobGroupID = var1;

      try {
         this.getPerformer().stop(var1);
         synchronized (this.waitingCounts) {
            this.waitingCounts.remove(var1.hashCode());
         }

         this.listenerManager.sendToListener(var1, new GridMessage(4));
      } catch (Exception var5) {
         LOGGER.error("", var5);
      }

      this.justStoppingJobGroupID = null;
   }

   public void restart(String var1) {
      try {
         this.getPerformer().restart(var1);
         this.listenerManager.sendToListener(var1, new GridMessage(3));
      } catch (Exception var3) {
         LOGGER.error("", var3);
      }
   }

   public long getRunningJobsCount(String var1) {
      return this.getCount(var1, this.runningCounts);
   }

   public long getExecutedUndeliveredJobsCount(String var1) {
      return this.getCount(var1, this.runningUndeliveredCounts);
   }

   public long getWaitingJobsCount(String var1) {
      return this.getCount(var1, this.waitingCounts);
   }

   public long getCount(String var1, Map<Integer, AtomicLong> var2) {
      synchronized (var2) {
         if (var1 != null) {
            int var10 = var1.hashCode();
            AtomicLong var5 = (AtomicLong)var2.get(var10);
            return var5 == null ? 0L : var5.get();
         }

         long var4 = 0L;

         for (AtomicLong var7 : var2.values()) {
            var4 += var7.get();
         }

         return var4;
      }
   }

   public GridTopology getGridTopology() {
      return this.getPerformer().getGridTopology();
   }

   public String getGridDescriptions() {
      return this.getPerformer().getGridDescriptions();
   }

   public void setProgress(String var1, String var2, int var3) {
      this.getPerformer().setProgress(var1, var2, var3);
      GridMessage var4 = new GridMessage(6, var1, var3);
      this.sendMessageToListener(var2, var4);
   }

   public void setJobWaiting(String var1, String var2, boolean var3) {
      this.getPerformer().setJobWaiting(var1, var2, var3);
   }

   public void waitForAllTaskFinished() throws InterruptedException {
      LOGGER.debug("Waiting for all task completed");

      do {
         Thread.sleep(2000L);
      } while (!this.runningCounts.isEmpty());

      LOGGER.info("All tasks completed");
   }

   public void waitForFinish(Object var1, String var2, String var3, String var4) throws InterruptedException {
      this.getPerformer().setJobWaiting(var2, var3, true);
      LOGGER.debug("Job: {}({}) is going to sleep", var2, var3);
      FinishListener var5 = new FinishListener(this, var1, var4);
      synchronized (this.finishListeners) {
         this.finishListeners.add(var5);
      }

      try {
         synchronized (var1) {
            var1.wait();
         }
      } finally {
         this.setJobWaiting(var2, var3, false);
      }

      LOGGER.debug("Job: {}({}) woken up", var2, var3);
   }

   public void setGroupRestrictions(String var1, int var2) {
      this.getPerformer().setGroupRestrictions(var1, var2);
   }

   public void check() {
      this.getPerformer().check();
   }

   public int getComputedThread() {
      return this.getPerformer().getComputedThread();
   }

   public int getUsedComputedThreads() {
      return this.getPerformer().getUsedComputedThreads();
   }

   public void setUsedComputedThreads(int var1) {
      this.getPerformer().setUsedComputedThreads(var1);
   }

   public void stop(String var1, String var2) {
      this.justStoppingJobGroupID = var1;

      try {
         this.getPerformer().stop(var1, var2);
         this.listenerManager.sendToListener(var1, new GridMessage(4, null, var2));
      } catch (Exception var4) {
         LOGGER.error("", var4);
      }

      this.justStoppingJobGroupID = null;
   }

   public void pause(String var1, String var2) {
      try {
         this.getPerformer().pause(var1, var2);
         this.listenerManager.sendToListener(var1, new GridMessage(2, null, var2));
      } catch (Exception var4) {
         LOGGER.error("", var4);
      }
   }

   public void restart(String var1, String var2) {
      try {
         this.getPerformer().restart(var1, var2);
         this.listenerManager.sendToListener(var1, new GridMessage(3, null, var2));
      } catch (Exception var4) {
         LOGGER.error("", var4);
      }
   }
}
