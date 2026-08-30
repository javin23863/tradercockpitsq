package com.strategyquant.tradinglib.databank;

import com.strategyquant.lib.L;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.tradinglib.Databank;
import com.strategyquant.tradinglib.ResultsGroup;
import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;
import java.util.ArrayList;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractDatabankFilter implements IDatabankFilter {
   public static final Logger Log = LoggerFactory.getLogger(DefaultDatabankFilter.class);
   protected Databank databank;
   protected int maxRecords = 1000;
   private Int2DoubleOpenHashMap rankValues = new Int2DoubleOpenHashMap();
   private ArrayList<String> strategiesSorted = new ArrayList<>();
   private Thread maxRecordsThread;
   private ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

   public AbstractDatabankFilter(int var1) {
      this.maxRecords = var1;
   }

   @Override
   public void init(Databank var1) throws Exception {
      this.init(var1, null);
   }

   @Override
   public void init(Databank var1, IProgressListener var2) throws Exception {
      this.databank = var1;
      this.calculateRankValues();
      this.removeOverabundantResults(var2);
   }

   protected void calculateRankValues() {
      this.lock.writeLock().lock();

      try {
         this.rankValues.clear();
         this.strategiesSorted.clear();
         ArrayList var1 = this.databank.getRecords();

         for (int var2 = 0; var2 < var1.size(); var2++) {
            String var3 = ((ResultsGroup)var1.get(var2)).getName();
            int var4 = SQUtils.betterHashCode(var3);
            double var5 = this.getRankValue((ResultsGroup)var1.get(var2));
            this.insert(null, var4, var3, var5);
         }
      } catch (Exception var10) {
         Log.error("Error while calculating rank values", var10);
      } finally {
         this.lock.writeLock().unlock();
      }
   }

   protected void removeWorseStrategy(String var1) {
      this.lock.writeLock().lock();

      try {
         int var2 = SQUtils.betterHashCode(var1);
         this.rankValues.remove(var2);
         this.strategiesSorted.remove(var1);
      } finally {
         this.lock.writeLock().unlock();
      }
   }

   public double getRankValue(ResultsGroup var1) {
      try {
         return var1.getFitness((byte)10);
      } catch (Exception var3) {
         Log.error("Cannot calculate rank value of strategy '" + var1.getName() + "'", var3);
         return 0.0;
      }
   }

   @Override
   public int getMaxStrategies() {
      return this.maxRecords;
   }

   @Override
   public void destroy() {
      this.lock.writeLock().lock();

      try {
         this.strategiesSorted.clear();
         this.rankValues.clear();
      } finally {
         this.lock.writeLock().unlock();
      }
   }

   @Override
   public boolean changeMaxRecords(int var1, IProgressListener var2) throws Exception {
      if (this.databank.noActionRunning() && (this.maxRecordsThread == null || !this.maxRecordsThread.isAlive())) {
         this.maxRecords = var1;
         if (this.databank.getRecords().size() > var1) {
            this.removeOverabundantResults(var2);
            return true;
         } else {
            return false;
         }
      } else {
         throw new Exception(L.t("Another databank action is already in progress.", new Object[0]));
      }
   }

   private void removeOverabundantResults(final IProgressListener var1) {
      this.maxRecordsThread = new Thread() {
         @Override
         public void run() {
            AbstractDatabankFilter.this.lock.writeLock().lock();
            double var1x = 0.0;

            try {
               int var3 = AbstractDatabankFilter.this.strategiesSorted.size() - AbstractDatabankFilter.this.maxRecords;
               if (var3 > 0) {
                  for (int var4 = 0; var4 < var3; var4++) {
                     String var5 = AbstractDatabankFilter.this.strategiesSorted.remove(AbstractDatabankFilter.this.strategiesSorted.size() - 1);
                     AbstractDatabankFilter.this.rankValues.remove(SQUtils.betterHashCode(var5));
                     AbstractDatabankFilter.this.databank.removeNoLock(var5, true, false, true, "removeOverabundantResults");
                     var1x = (var4 + 1.0) / var3 * 100.0;
                     if (var1 != null) {
                        var1.onProgress(var1x);
                     }
                  }

                  AbstractDatabankFilter.this.databank.refreshGrid();
                  if (var1 != null) {
                     var1.onDone();
                  }

                  return;
               }
            } catch (Exception var9) {
               AbstractDatabankFilter.Log.error("Error while removing overabundant results", var9);
               if (var1 != null) {
                  var1.onError(var1x, "Removing result failed. " + var9.getMessage());
               }

               return;
            } finally {
               AbstractDatabankFilter.this.lock.writeLock().unlock();
            }
         }
      };
      this.maxRecordsThread.start();
   }

   @Override
   public double getWorstFitness() {
      this.lock.readLock().lock();

      try {
         return this.getWorstRankValue();
      } finally {
         this.lock.readLock().unlock();
      }
   }

   public double getWorstRankValue() {
      this.lock.readLock().lock();
      String var1 = null;

      try {
         if (this.strategiesSorted == null) {
            Log.error("Strategies sorted is null");
            return 0.0;
         }

         if (this.strategiesSorted.size() == 0) {
            return 0.0;
         }

         int var2 = this.strategiesSorted.size() - 1;
         var1 = this.strategiesSorted.get(var2);
         return this.rankValues.get(SQUtils.betterHashCode(var1));
      } finally {
         this.lock.readLock().unlock();
      }
   }

   public String getWorstStrategy() {
      this.lock.readLock().lock();

      try {
         return this.strategiesSorted.size() > 0 ? this.strategiesSorted.get(this.strategiesSorted.size() - 1) : null;
      } finally {
         this.lock.readLock().unlock();
      }
   }

   protected double getWorstRankValue(String var1) {
      this.lock.readLock().lock();

      try {
         if (this.rankValues == null) {
            Log.error("rankValues is null");
            return 0.0;
         } else {
            return this.rankValues.get(SQUtils.betterHashCode(var1));
         }
      } finally {
         this.lock.readLock().unlock();
      }
   }

   protected boolean hasWorseRankValue(ArrayList<String> var1, double var2) {
      this.lock.readLock().lock();

      try {
         if (this.rankValues == null) {
            Log.error("rankValues is null");
            return true;
         }

         for (int var4 = 0; var4 < var1.size(); var4++) {
            String var5 = (String)var1.get(var4);
            int var6 = SQUtils.betterHashCode(var5);
            if (this.rankValues.containsKey(var6)) {
               double var7 = this.rankValues.get(var6);
               if (var7 < var2) {
                  return true;
               }
            }
         }

         return false;
      } finally {
         this.lock.readLock().unlock();
      }
   }

   protected String getStrategyWithWorseRank(ArrayList<String> var1, double var2) {
      this.lock.readLock().lock();

      try {
         if (this.rankValues == null) {
            Log.error("rankValues is null");
            return null;
         }

         for (int var4 = 0; var4 < var1.size(); var4++) {
            String var5 = (String)var1.get(var4);
            int var6 = SQUtils.betterHashCode(var5);
            if (this.rankValues.containsKey(var6)) {
               double var7 = this.rankValues.get(var6);
               if (var7 < var2) {
                  return var5;
               }
            }
         }

         return null;
      } finally {
         this.lock.readLock().unlock();
      }
   }

   protected String getWorstStrategy(ArrayList<String> var1) {
      this.lock.readLock().lock();

      try {
         for (int var2 = this.strategiesSorted.size() - 1; var2 >= 0; var2--) {
            String var3 = this.strategiesSorted.get(var2);
            if (var1.contains(var3)) {
               return var3;
            }
         }

         return null;
      } finally {
         this.lock.readLock().unlock();
      }
   }

   @Override
   public void onRemove(String var1) {
      this.lock.writeLock().lock();

      try {
         this.rankValues.remove(SQUtils.betterHashCode(var1));
         this.strategiesSorted.remove(var1);
      } finally {
         this.lock.writeLock().unlock();
      }
   }

   @Override
   public boolean onAdd(ResultsGroup var1) {
      double var2 = this.getRankValue(var1);
      String var4 = var1.getName();
      this.lock.writeLock().lock();

      try {
         if (this.addStrategy(var1, var2)) {
            int var5 = SQUtils.betterHashCode(var4);
            this.insert(var1, var5, var4, var2);
            return true;
         } else {
            return false;
         }
      } catch (Exception var10) {
         Log.error("Error while adding strategy", var10);
         return false;
      } finally {
         this.lock.writeLock().unlock();
      }
   }

   @Override
   public boolean checkShouldAdd(ResultsGroup var1) {
      double var2 = this.getRankValue(var1);
      return this.shouldAddStrategy(var1, var2);
   }

   @Override
   public void onUpdate(ResultsGroup var1) {
      this.lock.writeLock().lock();

      try {
         String var2 = var1.getName();
         double var3 = this.getRankValue(var1);
         int var5 = SQUtils.betterHashCode(var2);
         if (this.strategiesSorted.size() > 1) {
            this.strategiesSorted.remove(var2);
            this.insert(var1, var5, var2, var3);
         } else {
            this.rankValues.put(var5, var3);
         }
      } catch (Exception var9) {
         Log.error("Error while updating strategy", var9);
      } finally {
         this.lock.writeLock().unlock();
      }
   }

   protected void insert(ResultsGroup var1, int var2, String var3, double var4) {
      boolean var6 = false;

      for (int var7 = 0; var7 < this.strategiesSorted.size(); var7++) {
         String var8 = this.strategiesSorted.get(var7);
         double var9 = this.rankValues.get(SQUtils.betterHashCode(var8));
         if (var9 < var4) {
            this.strategiesSorted.add(var7, var3);
            var6 = true;
            break;
         }
      }

      if (!var6) {
         this.strategiesSorted.add(var3);
      }

      this.rankValues.put(var2, var4);
      this.actionOnInsert(var1);
   }

   protected void actionOnInsert(ResultsGroup var1) {
   }

   protected abstract boolean shouldAddStrategy(ResultsGroup var1, double var2);

   protected abstract boolean addStrategy(ResultsGroup var1, double var2) throws Exception;
}
