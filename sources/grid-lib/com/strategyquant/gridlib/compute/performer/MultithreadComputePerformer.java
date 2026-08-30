package com.strategyquant.gridlib.compute.performer;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.strategyquant.gridlib.client.GridJob;
import com.strategyquant.gridlib.client.GridMessage;
import com.strategyquant.gridlib.compute.JobResult;
import com.strategyquant.gridlib.compute.common.ExecuteOptions;
import com.strategyquant.gridlib.compute.common.JobQueue;
import com.strategyquant.gridlib.compute.common.PausedTasks;
import com.strategyquant.gridlib.concurrent.ThreadPool;
import com.strategyquant.gridlib.config.Config;
import com.strategyquant.gridlib.topology.GridTopology;
import com.strategyquant.gridlib.topology.NodeInfo;
import com.strategyquant.gridlib.utils.CoreUsagesEvaluator;
import com.strategyquant.lib.memory.CpuInfo;
import com.sun.management.OperatingSystemMXBean;
import java.io.Serializable;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import net.openhft.affinity.AffinityLock;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MultithreadComputePerformer implements IComputePerformer {
   private static final int MINIMAL_CORES_FOR_SQ = 3;
   private static final int MINIMAL_RESERVED_CORES_FOR_OS = 4;
   private static final int TERMINATION_WAIT = 5000;
   private static final Logger LOGGER = LoggerFactory.getLogger(MultithreadComputePerformer.class);
   private JobQueue<GridJob<?>> taskQueue = new JobQueue<>();
   private List<IJobStatusChangedHandler> handlers = new LinkedList<>();
   private ThreadPool threadPool;
   private ExecutorService nonBlockingPool;
   private AtomicInteger runningBlockingJobs = new AtomicInteger(0);
   private AtomicInteger runningNonBlockingJobs = new AtomicInteger(0);
   private AtomicInteger waitingBlockingJobs = new AtomicInteger(0);
   private AtomicInteger coreCount = new AtomicInteger();
   private int maximumCoreCount;
   private GridTopology gridTopology;
   private Config config;
   private volatile boolean shuttingDown;

   public MultithreadComputePerformer(Config var1) {
      this.config = var1;
      this.taskQueue.setFinishedJobsCount(var1.getFinishedJobsInDesc());
      this.gridTopology = new GridTopology();
      this.initExecutors();
   }

   private void fillGridTopology() {
      synchronized (this.gridTopology) {
         OperatingSystemMXBean var2 = (OperatingSystemMXBean)ManagementFactory.getOperatingSystemMXBean();
         NodeInfo var3 = null;
         if (this.gridTopology.getNodes().isEmpty()) {
            var3 = new NodeInfo();
            var3.setIp(this.evalIp());
            var3.setOsVersion(var2.getVersion());
            this.gridTopology.getNodes().add(var3);
         } else {
            var3 = this.gridTopology.getNodes().get(0);
         }

         double var4 = 1.0737418E9F;
         double var6 = var2.getTotalPhysicalMemorySize() / var4;
         double var8 = var6 - var2.getFreePhysicalMemorySize() / var4;
         int var10 = (int)(var2.getSystemCpuLoad() * 100.0);
         var3.setTotalMemory(Math.round(var6 * 10.0) / 10.0);
         var3.setUsedMemory(Math.round(var8 * 10.0) / 10.0);
         var3.setRunningThreads(this.runningBlockingJobs.get());
         var3.setCpuUsage(var10);
         var3.setUsedCores(this.coreCount.get());
         this.gridTopology.setTotalCores(var3.getUsedCores());
      }
   }

   private String evalIp() {
      String var1 = null;
      if (this.config.getNodeId() != null && !this.config.getNodeId().trim().isEmpty()) {
         var1 = this.config.getNodeId();
      } else {
         try {
            var1 = InetAddress.getLocalHost().getHostAddress();
         } catch (Exception var3) {
            var1 = "Can't be determined";
         }
      }

      return var1;
   }

   private int adjustCoresForUsage(int var1, boolean var2) {
      int var3 = CpuInfo.getAvailableProcessors();
      int var4 = var1;
      if (var2) {
         int var5 = var3 - 4;
         int var6 = (int)(var3 * 0.8F);
         int var7 = Math.max(var6, var5);
         if (var1 > var7) {
            var4 = var7;
         }
      }

      if (var4 < 3) {
         var4 = 3;
      }

      if (var4 != var1) {
         LOGGER.info("Adjusting used cores to: {} from requested: {}", var4, var1);
      }

      return var4;
   }

   private void initExecutors() {
      this.maximumCoreCount = CoreUsagesEvaluator.getCores(this.config.getCoreUsage());
      int var1 = this.adjustCoresForUsage(this.maximumCoreCount, this.config.isAdjustTopSizeOfCores());
      this.coreCount.set(var1);
      LOGGER.info("Using: {} cores, core usage configuration: '{}'", this.coreCount, this.config.getCoreUsage());
      this.threadPool = new ThreadPool(var1, this.config.isUseAffinity());
      java.util.logging.Logger var2 = java.util.logging.Logger.getLogger(AffinityLock.class.getName());
      var2.setUseParentHandlers(false);
      ThreadFactory var3 = new ThreadFactoryBuilder().setNameFormat("Nonblocking computeThread - %d").build();
      this.nonBlockingPool = Executors.newCachedThreadPool(var3);
   }

   @Override
   public void close() throws Exception {
      this.shuttingDown = true;
      this.threadPool.close();
      this.nonBlockingPool.shutdownNow();
      this.stopAll();
      this.taskQueue.close();
      this.threadPool.waitForTermination(5000L);
      this.nonBlockingPool.awaitTermination(5000L, TimeUnit.MILLISECONDS);
      this.handlers.clear();
   }

   private void stopAll() {
      Set var1 = this.taskQueue.getRunningTasks();
      LOGGER.debug("Stopping all running tasks:" + var1.size());
      GridMessage var2 = new GridMessage(4);

      for (JobQueue.JobInfo var4 : var1) {
         this.sendMessageToJob((GridJob<?>)var4.getData(), var2);
      }
   }

   @Override
   public void addJobFinishedHandler(IJobStatusChangedHandler var1) {
      this.handlers.add(var1);
   }

   @Override
   public void execute(GridJob<?> var1, String var2, ExecuteOptions var3) throws Exception {
      if (!this.shuttingDown) {
         this.taskQueue.add(var1.getJobId(), var2, var1.getPriority(), var1.isBlocking(), var1, var3);
         this.tryToExecute();
      }
   }

   private void tryToExecute() {
      if (!this.shuttingDown) {
         if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(
               "Trying to execute job. Running blocking jobs: {}, non-blocking tasks: {}. Available cores:{}, Waiting tasks:{}",
               new Object[]{this.runningBlockingJobs, this.runningNonBlockingJobs, this.coreCount, this.waitingBlockingJobs}
            );
         }

         JobQueue.JobInfo var1 = null;
         synchronized (this.taskQueue) {
            boolean var3 = this.runningBlockingJobs.get() < this.coreCount.get() + this.waitingBlockingJobs.get();
            var1 = this.taskQueue.getNext(true, var3);
            if (var1 == null) {
               if (LOGGER.isDebugEnabled()) {
                  LOGGER.debug("Nothing to compute");
               }

               return;
            }

            if (var1.isBlocking() && this.runningBlockingJobs.get() >= this.coreCount.get() + this.waitingBlockingJobs.get() && this.coreCount.get() > 1) {
               if (LOGGER.isDebugEnabled()) {
                  LOGGER.debug("Job can't be executed - no free thread.");
               }

               return;
            }

            this.taskQueue.computing(var1.getId(), var1.getJobGroupId());
            this.incrementRunningTasksCount(var1);
         }

         this.runJob(var1);
      }
   }

   private void incrementRunningTasksCount(JobQueue.JobInfo<?> var1) {
      if (var1.isBlocking()) {
         this.runningBlockingJobs.incrementAndGet();
      } else {
         this.runningNonBlockingJobs.incrementAndGet();
      }
   }

   private void decrementRunningTasksCount(JobQueue.JobInfo<?> var1) {
      if (var1.isBlocking()) {
         this.runningBlockingJobs.decrementAndGet();
      } else {
         this.runningNonBlockingJobs.decrementAndGet();
      }
   }

   private void runJob(final JobQueue.JobInfo<GridJob<?>> var1) {
      if (!this.shuttingDown) {
         if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Running job: {}({}). Blocking: {}", new Object[]{var1.getId(), var1.getJobGroupId(), var1.isBlocking()});
         }

         Runnable var2 = new Runnable() {
            @Override
            public void run() {
               MultithreadComputePerformer.this.callJobStartedHandlers(var1);
               String var1x = Thread.currentThread().getName();
               Thread.currentThread().setName(var1x + " - " + var1.getId());
               JobResult var2x = new JobResult();

               try {
                  if (var1.getData() != null) {
                     Serializable var3 = (Serializable)((GridJob)var1.getData()).call();
                     var2x.setSuccess(true);
                     var2x.setData(var3);
                  } else {
                     var2x.setSuccess(true);
                  }
               } catch (Throwable var10) {
                  var2x.setSuccess(false);
                  var2x.setErrorMessage(ExceptionUtils.getStackTrace(var10));
                  if (var10.getClass().getSimpleName().contains("TaskStoppedException")) {
                     MultithreadComputePerformer.LOGGER.info("Task stopped: {}({})", var1.getId(), var1.getJobGroupId() + ")");
                  } else {
                     MultithreadComputePerformer.LOGGER
                        .error("Error while running task: {}({})", new Object[]{var1.getId(), var1.getJobGroupId() + ")", var10});
                  }
               } finally {
                  Thread.currentThread().setName(var1x);
                  long var6 = MultithreadComputePerformer.this.taskQueue
                     .computed(var1.getId(), var1.getJobGroupId(), var2x.isSuccess(), var2x.getErrorMessage());
                  var2x.setDuration(var6);
                  MultithreadComputePerformer.this.callJobFinishedHandlers(var1, var2x);
                  MultithreadComputePerformer.this.decrementRunningTasksCount(var1);
                  MultithreadComputePerformer.LOGGER
                     .debug(
                        "Job {}({}) finished. Running blocking jobs: {}, non-blocking tasks: {}. Available cores:{}, Waiting tasks:{}",
                        new Object[]{
                           var1.getId(),
                           var1.getJobGroupId(),
                           MultithreadComputePerformer.this.runningBlockingJobs,
                           MultithreadComputePerformer.this.runningNonBlockingJobs,
                           MultithreadComputePerformer.this.coreCount,
                           MultithreadComputePerformer.this.waitingBlockingJobs
                        }
                     );
                  MultithreadComputePerformer.this.tryToExecute();
               }
            }
         };
         if (var1.isBlocking()) {
            this.threadPool.execute(var2, this.waitingBlockingJobs.get() > 0);
         } else {
            this.nonBlockingPool.execute(var2);
            this.tryToExecute();
         }
      }
   }

   protected void callJobFinishedHandlers(JobQueue.JobInfo<GridJob<?>> var1, JobResult<Serializable> var2) {
      for (IJobStatusChangedHandler var4 : this.handlers) {
         try {
            var4.onJobFinished(var1.getId(), var1.getJobGroupId(), var2);
         } catch (Throwable var6) {
            LOGGER.error("Error while running handler", var6);
         }
      }
   }

   protected void callJobStartedHandlers(JobQueue.JobInfo<GridJob<?>> var1) {
      for (IJobStatusChangedHandler var3 : this.handlers) {
         try {
            var3.onJobExecuted(var1.getId(), var1.getJobGroupId());
         } catch (Throwable var5) {
            LOGGER.error("Error while running handler", var5);
         }
      }
   }

   @Override
   public void commit() throws Exception {
   }

   @Override
   public void pause(String var1) {
      PausedTasks var2 = this.taskQueue.pause(var1);
      GridMessage var3 = new GridMessage(2);

      for (JobQueue.JobInfo var6 : var2.getRunningTasks()) {
         this.sendMessageToJob((GridJob<?>)var6.getData(), var3);
      }
   }

   @Override
   public void pause(String var1, String var2) throws Exception {
      JobQueue.JobInfo var3 = this.taskQueue.pause(var1, var2);
      if (var3 != null) {
         this.sendMessageToJob((GridJob<?>)var3.getData(), new GridMessage(2));
      }
   }

   private void performStop(JobQueue.JobInfo<GridJob<?>> var1, GridMessage var2) {
      if (LOGGER.isDebugEnabled()) {
         LOGGER.debug("Stopping job: {}", var1.getId());
      }

      if (var1.isExecuted()) {
         if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Sending stop message to job: {}", var1.getId());
         }

         this.sendMessageToJob((GridJob<?>)var1.getData(), var2);
      } else {
         if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Destroying job: {}", var1.getId());
         }

         try {
            ((GridJob)var1.getData()).destroy();
         } catch (Throwable var4) {
            LOGGER.error("Error while destroying job", var4);
         }

         var1.setData(null);
      }
   }

   @Override
   public void stop(String var1) {
      Set var2 = this.taskQueue.stop(var1);
      GridMessage var3 = new GridMessage(4);

      for (JobQueue.JobInfo var5 : var2) {
         this.performStop(var5, var3);
      }

      this.tryToExecute();
   }

   @Override
   public void stop(String var1, String var2) throws Exception {
      JobQueue.JobInfo var3 = this.taskQueue.stop(var1, var2);
      if (var3 != null) {
         this.performStop(var3, new GridMessage(4));
         this.tryToExecute();
      }
   }

   @Override
   public void restart(String var1) {
      List var2 = this.taskQueue.restore(var1);
      GridMessage var3 = new GridMessage(3);

      for (JobQueue.JobInfo var5 : var2) {
         this.sendMessageToJob((GridJob<?>)var5.getData(), var3);
      }

      this.tryToExecute();
   }

   @Override
   public void restart(String var1, String var2) throws Exception {
      JobQueue.JobInfo var3 = this.taskQueue.restore(var1, var2);
      if (var3 != null) {
         this.sendMessageToJob((GridJob<?>)var3.getData(), new GridMessage(3));
         this.tryToExecute();
      }
   }

   @Override
   public boolean sendMessageToJob(String var1, String var2, GridMessage var3) {
      JobQueue.JobInfo var4 = this.taskQueue.getJobInfo(var1, var2);
      if (var4 != null) {
         this.sendMessageToJob((GridJob<?>)var4.getData(), var3);
      }

      return var4 != null;
   }

   private void sendMessageToJob(GridJob<?> var1, GridMessage var2) {
      try {
         if (var1 != null) {
            var1.messageReceived(var2);
         }
      } catch (Throwable var4) {
         LOGGER.error("Error while handling message in job", var4);
      }
   }

   @Override
   public GridTopology getGridTopology() {
      this.fillGridTopology();
      return this.gridTopology;
   }

   @Override
   public String getGridDescriptions() {
      return this.taskQueue.getJsonDescriptions();
   }

   @Override
   public void setProgress(String var1, String var2, int var3) {
      this.taskQueue.updateProgress(var1, var2, var3);
   }

   @Override
   public void setGroupRestrictions(String var1, int var2) {
      this.taskQueue.setGroupRestrictions(var1, var2);
   }

   @Override
   public void check() {
      synchronized (this.taskQueue) {
         this.taskQueue.check();
      }
   }

   @Override
   public void setJobWaiting(String var1, String var2, boolean var3) {
      LOGGER.debug("Setting job:{}({}) as waiting:{}", new Object[]{var1, var2, var3});
      JobQueue.JobInfo var4 = null;
      if (var1 != null && var2 != null) {
         var4 = this.taskQueue.setJobWaiting(var1, var2, var3);
      }

      if (var4 != null && var4.isBlocking()) {
         synchronized (this.taskQueue) {
            if (var3) {
               this.waitingBlockingJobs.incrementAndGet();
            } else {
               int var6 = this.waitingBlockingJobs.decrementAndGet();
               if (var6 < 0) {
                  this.waitingBlockingJobs.set(0);
               }
            }
         }
      }

      this.tryToExecute();
   }

   @Override
   public int getComputedThread() {
      return this.maximumCoreCount;
   }

   @Override
   public int getUsedComputedThreads() {
      return this.coreCount.get();
   }

   @Override
   public void setUsedComputedThreads(int var1) {
      if (var1 >= 0 && var1 <= this.maximumCoreCount) {
         this.coreCount.set(var1);
      } else {
         throw new IllegalArgumentException("Invalid count: " + var1);
      }
   }
}
