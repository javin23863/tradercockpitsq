package com.strategyquant.tradinglib.data.download;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.Map.Entry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Queue {
   private static final Logger Log = LoggerFactory.getLogger(Queue.class);
   private int queueSize = 3;
   private Set<String> executedJobs = new HashSet<>();
   private LinkedHashMap<String, DownloadDispatcher.Details> downloadQueue = new LinkedHashMap<>();
   private IDownloadDispatcher dispatcher;

   public Queue(IDownloadDispatcher var1) {
      this.dispatcher = var1;
   }

   public synchronized void register(String var1, DownloadDispatcher.Details var2) {
      this.downloadQueue.put(var1, var2);
   }

   public synchronized DownloadDispatcher.Details removeFromDownloadQueue(String var1) {
      return this.downloadQueue.remove(var1);
   }

   public synchronized DownloadDispatcher.Details getFromDownloadQueue(String var1) {
      return this.downloadQueue.get(var1);
   }

   public boolean setQueueSize(int var1) {
      boolean var2 = var1 > this.queueSize;
      this.queueSize = var1;
      return var2;
   }

   public synchronized int getJobsCount() {
      return this.executedJobs.size() + this.downloadQueue.size();
   }

   public synchronized int getRunningJobsCount() {
      return this.executedJobs.size();
   }

   public synchronized int getWaitingJobsCount() {
      return this.downloadQueue.size();
   }

   public boolean exists(String var1) {
      return this.downloadQueue.containsKey(var1) || this.executedJobs.contains(var1);
   }

   public synchronized void tryToRunDownload() {
      Log.debug("Trying to execute next download. Current downloading size: " + this.executedJobs.size());
      if (this.executedJobs.size() < this.queueSize) {
         Iterator var1 = this.downloadQueue.entrySet().iterator();

         while (var1.hasNext()) {
            Entry var2 = (Entry)var1.next();
            if (!((DownloadDispatcher.Details)var2.getValue()).paused) {
               DownloadDispatcher.Details var3 = (DownloadDispatcher.Details)var2.getValue();
               if (var3.dataInfo.source == 7 && this.dispatcher.isCryptoExchangeDownload(var3.dataInfo.uSymbolName)) {
                  Log.debug("Skipping crypto symbol, because the same exchange is already downloading. " + var3.symbol);
               } else if (var3.dataInfo.source == 8 && this.dispatcher.isMt5DownloadExecuted()) {
                  Log.debug("Skipping mt5api symbol, because another symbol is already downloading. " + var3.symbol);
               } else {
                  var1.remove();

                  try {
                     this.dispatcher.extecuteDownloadJob(var3);
                     this.executedJobs.add(var3.symbol);
                  } catch (Exception var5) {
                     var3.listener.onError("Error while executing download job: " + var5.getMessage());
                     Log.error("", var5);
                  }

                  Log.debug("Executed symbol: " + var3.symbol + ", executed: " + this.executedJobs.size());
                  if (this.executedJobs.size() >= this.queueSize) {
                     return;
                  }
               }
            }
         }
      }
   }

   public int getQueueSize() {
      return this.queueSize;
   }

   public synchronized void stopAll() {
      for (DownloadDispatcher.Details var2 : this.downloadQueue.values()) {
         this.dispatcher.performStop(var2.symbol, var2);
      }

      this.downloadQueue.clear();

      for (String var4 : this.executedJobs) {
         this.dispatcher.performStop(var4, null);
      }

      this.executedJobs.clear();
   }

   public synchronized void pauseAll() {
      for (DownloadDispatcher.Details var2 : this.downloadQueue.values()) {
         this.dispatcher.performPause(var2.symbol, var2);
      }

      Log.info("Paused waiting jobs");

      for (String var4 : this.executedJobs) {
         this.dispatcher.performPause(var4, null);
      }

      Log.info("Paused running jobs");
   }

   public synchronized void resumeAll() {
      for (DownloadDispatcher.Details var2 : this.downloadQueue.values()) {
         this.dispatcher.performResume(var2.symbol, var2);
      }

      Log.info("Resumed waiting jobs");

      for (String var4 : this.executedJobs) {
         this.dispatcher.performResume(var4, null);
      }

      Log.info("Resumed executed jobs");
   }

   public synchronized boolean downloadJobFinished(String var1) {
      return this.executedJobs.remove(var1);
   }

   public synchronized boolean isBusy() {
      return !this.executedJobs.isEmpty() || !this.downloadQueue.isEmpty();
   }
}
