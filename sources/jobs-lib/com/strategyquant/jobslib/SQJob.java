package com.strategyquant.jobslib;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class SQJob {
   public static final int ErrorNone = 0;
   public static final int ErrorException = 1;
   private static final int RunningStatus_BeforeRun = 0;
   private static final int RunningStatus_Running = 1;
   private static final int RunningStatus_Finished = 2;
   public static final int JobStatus_None = 0;
   public static final int JobStatus_Success = 1;
   public static final int JobStatus_Error = 2;
   private AtomicInteger runningStatus = new AtomicInteger(0);
   private AtomicInteger status = new AtomicInteger(0);
   private Object result = null;
   private Exception exception = null;
   private ArrayList<JobCompletionListener> listeners = new ArrayList<>();

   public void addCompletionListener(JobCompletionListener var1) {
      this.listeners.add(var1);
   }

   protected void setResult(Object var1) {
      this.result = var1;
   }

   private void callCompletionListeners(ExecutorService var1) {
      for (JobCompletionListener var3 : this.listeners) {
         var1.submit(new SQJob.Listener(this, var3));
      }
   }

   void runWithCallback(ExecutorService var1) {
      try {
         this.runningStatus.set(1);
         this.run();
         this.status.set(1);
      } catch (Exception var6) {
         this.status.set(2);
         this.exception = var6;
      } finally {
         this.runningStatus.set(2);
         this.callCompletionListeners(var1);
      }
   }

   public int getId() {
      return System.identityHashCode(this);
   }

   public int getStatus() {
      return this.status.get();
   }

   public boolean isFinished() {
      return this.runningStatus.get() == 2;
   }

   public boolean isRunning() {
      return this.runningStatus.get() == 1;
   }

   public Object getResult() {
      return this.result;
   }

   public abstract void run() throws Exception;

   class Listener implements Runnable {
      private SQJob job;
      private JobCompletionListener listener;

      public Listener(SQJob nullx, JobCompletionListener nullxx) {
         this.job = nullx;
         this.listener = nullxx;
      }

      @Override
      public void run() {
         this.listener.onCompleted(this.job);
      }
   }
}
