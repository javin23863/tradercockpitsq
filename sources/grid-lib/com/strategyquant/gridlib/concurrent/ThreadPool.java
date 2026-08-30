package com.strategyquant.gridlib.concurrent;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.logging.Level;
import net.openhft.affinity.AffinityLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ThreadPool implements AutoCloseable {
   private static final Logger LOGGER = LoggerFactory.getLogger(ThreadPool.class);
   private ThreadPool.BlockingQueue<Runnable> queue;
   private List<Thread> threads = new LinkedList<>();
   private boolean useAffinity;
   private volatile boolean shutdown;
   private int additonalCount = 0;

   public ThreadPool(int var1, boolean var2) {
      this.queue = new ThreadPool.BlockingQueue<>(var1, false);
      this.useAffinity = var2;
      this.initializePool(var1);
      if (var2) {
         this.setAffinityLibraryLoging();
      }
   }

   private void setAffinityLibraryLoging() {
      java.util.logging.Logger var1 = java.util.logging.Logger.getLogger(AffinityLock.class.getName());
      var1.setLevel(Level.WARNING);
   }

   private void initializePool(int var1) {
      LOGGER.info("Preparing thread executors: {}", var1);

      for (int var2 = 0; var2 < var1; var2++) {
         this.prepareThreadExecutor(var2);
      }
   }

   private void prepareThreadExecutor(int var1) {
      ThreadPool.TaskExecutor var2 = new ThreadPool.TaskExecutor(this.queue);
      String var3 = "Blocking computeThread common ";
      var3 = var3 + "#" + var1;
      Thread var4 = new Thread(var2, var3);
      this.threads.add(var4);
      var4.start();
      LOGGER.debug("Preparing common thread executor: #{}", var1);
   }

   private void prepareThreadExecutor(int var1, Runnable var2) {
      ThreadPool.TaskExecutor var3 = new ThreadPool.TaskExecutor(var2);
      String var4 = "Blocking computeThread additional ";
      var4 = var4 + "#" + var1;
      Thread var5 = new Thread(var3, var4);
      this.threads.add(var5);
      var5.start();
      LOGGER.debug("Preparing one time additional thread executor: #{}", var1);
   }

   @Override
   public void close() {
      LOGGER.debug("Cleaning the pool");
      this.shutdown = true;
      this.queue.clear();

      for (Thread var2 : this.threads) {
         var2.interrupt();
      }
   }

   public void waitForTermination(long var1) {
      for (Thread var4 : this.threads) {
         try {
            var4.join(var1);
            LOGGER.debug("Thread: {} finished.", var4.getName());
         } catch (InterruptedException var6) {
            var6.printStackTrace();
         }
      }

      this.threads.clear();
   }

   public void execute(Runnable var1, boolean var2) {
      if (!this.shutdown) {
         try {
            synchronized (this.queue) {
               if (var2) {
                  this.prepareThreadExecutor(this.additonalCount++, var1);
               } else {
                  this.queue.enqueue(var1);
               }
            }
         } catch (InterruptedException var6) {
            LOGGER.error("", var6);
         }
      }
   }

   private class BlockingQueue<T> {
      private Queue<T> queue = new LinkedList<>();
      private int maxTasksInPool;
      private boolean limitMax;

      public BlockingQueue(int nullx, boolean nullxx) {
         this.maxTasksInPool = nullx;
         this.limitMax = nullxx;
      }

      public synchronized void remove(T var1) {
         this.queue.remove(var1);
      }

      public synchronized void clear() {
         this.queue.clear();
      }

      public synchronized boolean isEmpty() {
         return this.queue.isEmpty();
      }

      public synchronized void enqueue(T var1) throws InterruptedException {
         while (this.limitMax && this.queue.size() == this.maxTasksInPool) {
            this.wait();
         }

         if (this.queue.isEmpty()) {
            this.notifyAll();
         }

         this.queue.offer((T)var1);
      }

      public synchronized T dequeue() throws InterruptedException {
         while (this.queue.isEmpty()) {
            this.wait();
         }

         if (this.limitMax && this.queue.size() == this.maxTasksInPool) {
            this.notifyAll();
         }

         return this.queue.poll();
      }
   }

   private class TaskExecutor implements Runnable {
      private ThreadPool.BlockingQueue<Runnable> queue;
      private boolean additionalExecutor;
      private Runnable task;

      public TaskExecutor(ThreadPool.BlockingQueue<Runnable> nullx) {
         this.queue = nullx;
         this.additionalExecutor = false;
      }

      public TaskExecutor(Runnable nullx) {
         this.task = nullx;
         this.additionalExecutor = true;
      }

      @Override
      public void run() {
         if (this.additionalExecutor) {
            this.runAdditionalExecutor();
         } else {
            this.runCommonExecutor();
         }
      }

      private void runAdditionalExecutor() {
         if (!ThreadPool.this.shutdown) {
            try {
               this.task.run();
            } catch (Throwable var2) {
               ThreadPool.LOGGER.error("Error while performing task!", var2);
            }

            ThreadPool.LOGGER.debug("Removing executor from pool.");
            if (!ThreadPool.this.shutdown) {
               ThreadPool.this.threads.remove(Thread.currentThread());
            }
         }

         ThreadPool.LOGGER.debug("Shutting thread in pool");
      }

      private void runCommonExecutor() {
         AffinityLock var1 = null;
         if (ThreadPool.this.useAffinity) {
            ThreadPool.LOGGER.debug("Binding cpu's core");
            var1 = this.acquireLock();
         }

         try {
            while (!ThreadPool.this.shutdown) {
               try {
                  Runnable var2 = this.queue.dequeue();
                  var2.run();
               } catch (InterruptedException var7) {
               } catch (Throwable var8) {
                  ThreadPool.LOGGER.error("Error while performing task!", var8);
               }
            }

            ThreadPool.LOGGER.debug("Shutting thread in pool");
         } finally {
            if (var1 != null) {
               ThreadPool.LOGGER.debug("Releasing lock");
               var1.release();
            }
         }
      }

      private AffinityLock acquireLock() {
         try {
            return AffinityLock.acquireLock();
         } catch (Throwable var2) {
            ThreadPool.LOGGER.error("Error while acquiring affinity lock", var2);
            return null;
         }
      }
   }
}
