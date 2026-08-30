package com.strategyquant.gridlib.compute.performer;

import com.strategyquant.gridlib.client.Compute;

public class FinishListener {
   private Compute compute;
   private Object object;
   private String waitGroupId;

   public FinishListener(Compute var1, Object var2, String var3) {
      this.compute = var1;
      this.object = var2;
      this.waitGroupId = var3;
   }

   public String getWaitGroupId() {
      return this.waitGroupId;
   }

   public boolean isFinished() {
      return this.compute.getWaitingJobsCount(this.waitGroupId) + this.compute.getExecutedUndeliveredJobsCount(this.waitGroupId) == 0L;
   }

   public void notifyWaitingObj() {
      try {
         synchronized (this.object) {
            this.object.notify();
         }
      } catch (IllegalMonitorStateException var4) {
         var4.printStackTrace();
      }
   }
}
