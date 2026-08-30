package com.strategyquant.gridlib.compute.common;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JobQueue<T extends Serializable> implements AutoCloseable {
   private static final Logger LOGGER = LoggerFactory.getLogger(JobQueue.class);
   private static final int DEFAULT_MAX_LAST_JOB_KEPT = 150;
   private Map<Integer, JobQueue.JobInfo<T>> dataMap = new Int2ObjectOpenHashMap();
   private RingBuffer<JobQueue.JobInfo<T>> finishedDataMap;
   private Map<Integer, LinkedHashSet<Integer>> queueOfTaskForExecute = new HashMap<>();
   private TaskGroupMap groupMap = new TaskGroupMap();
   private Set<Integer> pausedTasks = new HashSet<>();
   private LinkedHashSet<Integer> notConfirmedRunningTasks = new LinkedHashSet<>();
   private Object lock = new Object();
   private DescriptionsSerializer serializer = new DescriptionsSerializer();

   private int getJobIdentHash(String var1, String var2) {
      String var3 = var1 + "___" + var2;
      return var3.hashCode();
   }

   public void setFinishedJobsCount(int var1) {
      this.finishedDataMap = new RingBuffer<>(Math.min(var1, 150));
   }

   public PausedTasks<T> pause(String var1) {
      synchronized (this.lock) {
         LinkedList var3 = new LinkedList();
         LinkedList var4 = new LinkedList();
         Set var5 = this.groupMap.getJobsForGroup(var1);
         if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Pausing (waiting) group:{}. Jobs for pause:{}", var1, var5.size());
         }

         for (String var7 : var5) {
            int var8 = this.getJobIdentHash(var7, var1);
            if (!this.notConfirmedRunningTasks.contains(var8)) {
               if (LOGGER.isDebugEnabled()) {
                  LOGGER.debug("Pausing task: #{}({}).", var7, var1);
               }

               this.pausedTasks.add(var8);
               JobQueue.JobInfo var9 = this.dataMap.get(var8);
               if (var9 != null) {
                  this.removeFromQueue(var8, var9.priority);
                  var3.add(this.dataMap.get(var8));
               }
            } else {
               var4.add(this.dataMap.get(var8));
            }
         }

         return new PausedTasks<>(var3, var4);
      }
   }

   public JobQueue.JobInfo<T> pause(String var1, String var2) {
      synchronized (this.lock) {
         int var4 = this.getJobIdentHash(var2, var1);
         JobQueue.JobInfo var5 = this.dataMap.get(var4);
         if (!this.notConfirmedRunningTasks.contains(var4)) {
            if (LOGGER.isDebugEnabled()) {
               LOGGER.debug("Pausing task: #{}({}).", var2, var1);
            }

            this.pausedTasks.add(var4);
            if (var5 != null) {
               this.removeFromQueue(var4, var5.priority);
            }

            return null;
         } else {
            return var5;
         }
      }
   }

   public List<JobQueue.JobInfo<T>> restore(String var1) {
      synchronized (this.lock) {
         LinkedList var3 = new LinkedList();
         Set var4 = this.groupMap.getJobsForGroup(var1);
         if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Restoring group:{}. Jobs for restore:{}", var1, var4.size());
         }

         for (String var6 : var4) {
            int var7 = this.getJobIdentHash(var6, var1);
            if (this.pausedTasks.remove(var7)) {
               if (LOGGER.isDebugEnabled()) {
                  LOGGER.debug("Restoring task: #{}({}).", var6, var1);
               }

               JobQueue.JobInfo var8 = this.dataMap.get(var7);
               if (var8 != null) {
                  this.addToQueue(var7, var8.priority, false);
                  var3.add(this.dataMap.get(var7));
               }
            } else if (this.notConfirmedRunningTasks.contains(var7)) {
               var3.add(this.dataMap.get(var7));
            }
         }

         return var3;
      }
   }

   public JobQueue.JobInfo<T> restore(String var1, String var2) {
      synchronized (this.lock) {
         int var4 = this.getJobIdentHash(var2, var1);
         JobQueue.JobInfo var5 = this.dataMap.get(var4);
         if (this.pausedTasks.remove(var4)) {
            if (LOGGER.isDebugEnabled()) {
               LOGGER.debug("Restoring task: #{}({}).", var2, var1);
            }

            if (var5 != null) {
               this.addToQueue(var4, var5.priority, false);
               return this.dataMap.get(var4);
            }
         }

         return var5;
      }
   }

   public Set<JobQueue.JobInfo<T>> stop(String var1) {
      HashSet var2 = new HashSet();
      synchronized (this.lock) {
         HashSet var4 = new HashSet();
         var4.addAll(this.groupMap.getJobsForGroup(var1));

         for (String var6 : var4) {
            int var7 = this.getJobIdentHash(var6, var1);
            JobQueue.JobInfo var8 = this.dataMap.get(var7);
            if (var8 != null) {
               var2.add(var8);
               if (!var8.executed) {
                  this.dataMap.remove(var7);
                  this.removeFromQueue(var7, var8.priority);
                  this.groupMap.remove(var6, var1);
                  var8.success = true;
                  var8.message = "stopped";
               } else {
                  var8.message = "stopping";
               }
            }
         }

         return var2;
      }
   }

   public JobQueue.JobInfo<T> stop(String var1, String var2) {
      synchronized (this.lock) {
         int var4 = this.getJobIdentHash(var2, var1);
         JobQueue.JobInfo var5 = this.dataMap.get(var4);
         if (var5 != null) {
            if (!var5.executed) {
               this.dataMap.remove(var4);
               this.removeFromQueue(var4, var5.priority);
               var5.success = true;
               var5.message = "stopped";
            } else {
               var5.message = "stopping";
            }

            return var5;
         } else {
            return null;
         }
      }
   }

   public boolean isEmpty() {
      synchronized (this.lock) {
         return this.queueOfTaskForExecute.isEmpty();
      }
   }

   public JobQueue.JobInfo<T> tryToTakeNotFinishedTask(JobQueue.TimeEvaluator var1) {
      synchronized (this.lock) {
         if (this.notConfirmedRunningTasks.isEmpty()) {
            return null;
         }

         Integer var3 = this.notConfirmedRunningTasks.iterator().next();
         JobQueue.JobInfo var4 = this.dataMap.get(var3);
         if (var4 == null) {
            this.notConfirmedRunningTasks.remove(var3);
            return null;
         }

         long var5 = System.currentTimeMillis() - var4.executeTime;
         long var7 = var1.getTimeForWaitForTask(var4.jobGroupId);
         if (var5 < var7) {
            return null;
         }

         if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Resending task #{}.", var3);
         }

         this.notConfirmedRunningTasks.remove(var3);
         return var4;
      }
   }

   public void add(String var1, String var2, int var3, boolean var4, T var5, ExecuteOptions var6) {
      int var7 = this.getJobIdentHash(var1, var2);
      synchronized (this.lock) {
         if (this.dataMap.containsKey(var7)) {
            throw new IllegalArgumentException(String.format("Job: %s(%s) already in queue.", var1, var2));
         }

         JobQueue.JobInfo var9 = new JobQueue.JobInfo();
         var9.data = (T)var5;
         var9.id = var1;
         var9.jobGroupId = var2;
         var9.blocking = var4;
         var9.priority = this.normalizePriority(var3);
         var9.setCreationTime(System.currentTimeMillis());
         if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Adding task to JobQueue: {}({})", var1, var2);
         }

         this.dataMap.put(var7, var9);
         this.groupMap.add(var1, var2, var6);
         this.addToQueue(var7, var3, false);
      }
   }

   public void updateProgress(String var1, String var2, int var3) {
      synchronized (this.lock) {
         int var5 = this.getJobIdentHash(var1, var2);
         JobQueue.JobInfo var6 = this.dataMap.get(var5);
         if (var6 != null) {
            var6.progress = var3;
         }
      }
   }

   public void computing(String var1, String var2) {
      int var3 = this.getJobIdentHash(var1, var2);
      synchronized (this.lock) {
         JobQueue.JobInfo var5 = this.dataMap.get(var3);
         var5.executed = true;
         var5.executeTime = System.currentTimeMillis();
         this.notConfirmedRunningTasks.add(var3);
         this.removeFromQueue(var3, var5.priority);
         this.groupMap.executed(var2);
      }
   }

   public long computed(String var1, String var2, boolean var3, String var4) {
      synchronized (this.lock) {
         int var6 = this.getJobIdentHash(var1, var2);
         this.notConfirmedRunningTasks.remove(var6);
         this.groupMap.remove(var1, var2);
         JobQueue.JobInfo var7 = this.dataMap.remove(var6);
         if (var7 == null) {
            return 0L;
         }

         var7.data = null;
         var7.success = var3;
         var7.message = var4;
         if (this.finishedDataMap != null) {
            this.finishedDataMap.add(var7);
         }

         var7.finishTime = System.currentTimeMillis();
         long var8 = var7.finishTime - var7.executeTime;
         var7.duration = var8;
         var7.durationWithWait = var7.finishTime - var7.creationTime;
         return var8;
      }
   }

   public JobQueue.JobInfo<T> getNext(boolean var1, boolean var2) {
      synchronized (this.lock) {
         JobQueue.JobInfo var4 = null;
         if (var1) {
            var4 = this.getSuitableJob(false);
         }

         if (var4 == null) {
            Boolean var5 = null;
            if (!var2) {
               var5 = false;
            }

            var4 = this.getSuitableJob(var5);
         }

         return var4;
      }
   }

   private boolean canExecuteTaskDueGroupLimitation(JobQueue.JobInfo<T> var1) {
      ExecuteOptions var2 = this.groupMap.getOptions(var1.getJobGroupId());
      if (var2 != null && var2.getMaxRunningCount() >= 1) {
         int var3 = this.groupMap.getRunningCount(var1.getJobGroupId());
         if (var3 < var2.getMaxRunningCount()) {
            return true;
         }

         if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(
               "Can't run the job "
                  + var1.getId()
                  + " - number of jobs in the the group "
                  + var1.getJobGroupId()
                  + " is limited to:"
                  + var2.getMaxRunningCount()
            );
         }

         return false;
      } else {
         return true;
      }
   }

   private int normalizePriority(int var1) {
      if (var1 < 0) {
         return 0;
      } else {
         return var1 > 9 ? 9 : var1;
      }
   }

   private JobQueue.JobInfo<T> getSuitableJob(Boolean var1) {
      JobQueue.JobInfo var2 = null;

      for (int var3 = 9; var3 >= 0; var3--) {
         LinkedHashSet var4 = this.queueOfTaskForExecute.get(var3);
         if (var4 != null) {
            var2 = this.searchForTask(var4);
            if (var2 != null) {
               return var2;
            }
         }
      }

      return null;
   }

   private void removeFromQueue(int var1, int var2) {
      this.queueOfTaskForExecute.get(var2).remove(var1);
   }

   private JobQueue.JobInfo<T> searchForTask(Collection<Integer> var1) {
      for (Integer var3 : var1) {
         JobQueue.JobInfo var4 = this.dataMap.get(var3);
         if (this.canExecuteTaskDueGroupLimitation(var4)) {
            return var4;
         }
      }

      return null;
   }

   private void addToQueue(int var1, int var2, boolean var3) {
      LinkedHashSet var4 = this.queueOfTaskForExecute.get(var2);
      if (var4 == null) {
         var4 = new LinkedHashSet();
         this.queueOfTaskForExecute.put(var2, var4);
      }

      var4.add(var1);
   }

   public JobQueue.JobInfo<T> getJobInfo(String var1, String var2) {
      synchronized (this.lock) {
         int var4 = this.getJobIdentHash(var1, var2);
         return this.dataMap.get(var4);
      }
   }

   public Set<JobQueue.JobInfo<T>> getRunningTasks() {
      HashSet var1 = new HashSet();
      synchronized (this.lock) {
         for (Integer var4 : this.notConfirmedRunningTasks) {
            var1.add(this.dataMap.get(var4));
         }

         return var1;
      }
   }

   @Override
   public void close() {
      synchronized (this.lock) {
         this.dataMap.clear();
         this.finishedDataMap.clear();
      }
   }

   public String getJsonDescriptions() {
      synchronized (this.lock) {
         return this.serializer.getJson(this.queueOfTaskForExecute, this.notConfirmedRunningTasks, this.dataMap, this.finishedDataMap);
      }
   }

   public void setGroupRestrictions(String var1, int var2) {
   }

   public void check() {
      synchronized (this.lock) {
         for (int var2 = 9; var2 >= 0; var2--) {
            LinkedHashSet var3 = this.queueOfTaskForExecute.get(var2);
            if (var3 != null) {
               for (int var5 : var3) {
                  if (this.dataMap.get(var5) == null) {
                     throw new IllegalStateException("Job in queue lost its data!");
                  }
               }
            }
         }

         for (int var9 : this.notConfirmedRunningTasks) {
            if (this.dataMap.get(var9) == null) {
               throw new IllegalStateException("Not configrmed job in queue lost its data!");
            }
         }
      }
   }

   public JobQueue.JobInfo<?> setJobWaiting(String var1, String var2, boolean var3) {
      int var4 = this.getJobIdentHash(var1, var2);
      synchronized (this.lock) {
         JobQueue.JobInfo var6 = this.dataMap.get(var4);
         if (var6 != null) {
            var6.waiting = var3;
         }

         return var6;
      }
   }

   public static class JobInfo<T extends Serializable> implements Serializable {
      private static final long serialVersionUID = 1L;
      private boolean executed;
      private String id;
      private String jobGroupId;
      private long creationTime;
      private long executeTime;
      private long finishTime;
      private long duration;
      private long durationWithWait;
      private boolean blocking;
      private T data;
      private int progress = -1;
      private boolean success;
      private String message;
      private int priority;
      public volatile boolean waiting = false;

      public boolean isWaiting() {
         return this.waiting;
      }

      public long getFinishTime() {
         return this.finishTime;
      }

      public long getDurationWithWait() {
         return this.durationWithWait;
      }

      public boolean isSuccess() {
         return this.success;
      }

      public String getMessage() {
         return this.message;
      }

      public boolean isBlocking() {
         return this.blocking;
      }

      public String getJobGroupId() {
         return this.jobGroupId;
      }

      public boolean isExecuted() {
         return this.executed;
      }

      public String getId() {
         return this.id;
      }

      public T getData() {
         return this.data;
      }

      public void setData(T var1) {
         this.data = (T)var1;
      }

      public int getProgress() {
         return this.progress;
      }

      public void setProgress(int var1) {
         this.progress = var1;
      }

      public long getCreationTime() {
         return this.creationTime;
      }

      public void setCreationTime(long var1) {
         this.creationTime = var1;
      }

      public long getDuration() {
         return this.duration;
      }

      public void setDuration(long var1) {
         this.duration = var1;
      }

      public long getExecuteTime() {
         return this.executeTime;
      }

      @Override
      public String toString() {
         return this.id + " (" + this.jobGroupId + ")";
      }
   }

   public interface TimeEvaluator {
      long getTimeForWaitForTask(String var1);
   }
}
