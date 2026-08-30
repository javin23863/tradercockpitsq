package com.strategyquant.jobslib;

import com.strategyquant.lib.memory.CpuInfo;
import com.strategyquant.tradinglib.taskImpl.ISQTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JobEngine {
   public static final Logger Log = LoggerFactory.getLogger("JobEngine");
   private static JobEngine instance = null;
   private ExecutorService executor;
   private JobsList jobs = new JobsList();

   public static void init(JobEngine var0) throws Exception {
      if (instance != null) {
         throw new Exception("JobEngine.init() called more than once!");
      }

      instance = var0;
   }

   public JobEngine(int var1) {
      if (var1 <= 0) {
         var1 = CpuInfo.getAvailableProcessors();
      }

      this.executor = Executors.newFixedThreadPool(var1);
   }

   private static JobEngine get() {
      return instance;
   }

   public static void submit(ISQTask var0, SQJob var1) {
      get()._submit(var0, var1);
   }

   public static JobsList jobs() {
      return get()._jobs();
   }

   private JobsList _jobs() {
      return this.jobs;
   }

   private void _submit(ISQTask var1, SQJob var2) {
      this._addJobToArray(var1, var2);
      var2.addCompletionListener(new JobCompletionListener() {
         @Override
         public void onCompleted(SQJob var1) {
            JobEngine.this._removeFromArray(var1);
         }
      });
      this._executeJob(var2);
   }

   private void _executeJob(final SQJob var1) {
      this.executor.submit(new Runnable() {
         @Override
         public void run() {
            var1.runWithCallback(JobEngine.this.executor);
         }
      }, this.executor);
   }

   private void _addJobToArray(ISQTask var1, SQJob var2) {
      this.jobs.add(var2);
   }

   protected void _removeFromArray(SQJob var1) {
      this.jobs.remove(var1);
   }
}
