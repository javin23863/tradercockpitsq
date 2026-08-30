package com.strategyquant.tradinglib.project;

import java.util.concurrent.locks.StampedLock;
import org.json.JSONObject;

public class ProjectProgress {
   private ProjectProgressInfo info = new ProjectProgressInfo();
   private StampedLock lock = new StampedLock();

   public void update(String var1, double var2, String var4) {
      long var5 = this.lock.writeLock();

      try {
         this.info.action = var1;
         this.info.percent = (int)var2;
         this.info.error = var4;
         this.info.hasExtra = false;
      } finally {
         this.lock.unlock(var5);
      }
   }

   public void update(String var1, double var2, String var4, String var5) {
      long var6 = this.lock.writeLock();

      try {
         this.info.action = var1;
         this.info.percent = (int)var2;
         this.info.error = var4;
         this.info.info = var5;
         this.info.hasExtra = false;
      } finally {
         this.lock.unlock(var6);
      }
   }

   public void update(String var1, double var2, String var4, String var5, JSONObject var6) {
      long var7 = this.lock.writeLock();

      try {
         this.info.action = var1;
         this.info.percent = (int)var2;
         this.info.error = var4;
         this.info.extraName = var5;
         this.info.extraValue = var6;
         this.info.hasExtra = true;
      } finally {
         this.lock.unlock(var7);
      }
   }

   public ProjectProgressInfo get() {
      long var1 = this.lock.readLock();

      try {
         return this.info;
      } finally {
         this.lock.unlock(var1);
      }
   }
}
