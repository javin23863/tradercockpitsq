package com.strategyquant.tradinglib.project;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ProjectLog {
   private final Lock lock = new ReentrantLock();
   private String[] buffer = new String[1000];
   private String lastOutput = "";
   private int lastIndex = 0;
   private int totalWritten = 0;
   private boolean changed = true;

   public void add(String var1) {
      try {
         this.lock.lock();
         if (var1 != null) {
            String[] var2 = var1.split("\n");
            int var3 = this.buffer.length - this.lastIndex;
            if (var3 < var2.length) {
               int var4 = var2.length - var3;
               this.shiftBuffer(var4);
               this.lastIndex = this.lastIndex > var4 ? this.lastIndex - var4 : 0;
            }

            for (int var8 = var2.length - 1; var8 >= 0 && this.lastIndex < this.buffer.length; var8--) {
               this.buffer[this.lastIndex++] = var2[var8];
            }

            this.changed = true;
            this.totalWritten += var2.length;
            return;
         }
      } finally {
         this.lock.unlock();
      }
   }

   public String getNext() {
      try {
         this.lock.lock();
         if (this.lastIndex == 0) {
            return null;
         }

         if (this.changed) {
            StringBuilder var1 = new StringBuilder();

            for (int var2 = this.lastIndex - 1; var2 >= 0; var2--) {
               var1.append(this.buffer[var2]);
               var1.append("\n");
            }

            if (this.totalWritten > this.buffer.length) {
               var1.append("\n(log shortened to save memory, see the complete log in the log file)\n...");
            }

            this.lastOutput = var1.toString();
            this.changed = false;
         }

         return this.lastOutput;
      } finally {
         this.lock.unlock();
      }
   }

   public void clear() {
      try {
         this.lock.lock();
         this.lastIndex = 0;
         this.changed = false;
         this.totalWritten = 0;
      } finally {
         this.lock.unlock();
      }
   }

   public void resetLastData() {
      this.changed = true;
   }

   private void shiftBuffer(int var1) {
      for (int var2 = 0; var2 < this.buffer.length && var2 + var1 != this.lastIndex; var2++) {
         this.buffer[var2] = this.buffer[var2 + var1];
      }
   }
}
