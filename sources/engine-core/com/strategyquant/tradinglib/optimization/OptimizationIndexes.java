package com.strategyquant.tradinglib.optimization;

import com.strategyquant.gridlib.client.JobDetails;
import com.strategyquant.tradinglib.ResultsGroup;
import com.strategyquant.tradinglib.gp.GPIDs;
import com.strategyquant.tradinglib.gp.IGPNode;
import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OptimizationIndexes implements IGPNode {
   public static final Logger Log2 = LoggerFactory.getLogger("OptimizationIndexes");
   private short[] indexes;
   private double fitness = -1.0;
   private GPIDs gpIDs = new GPIDs();
   private JobDetails jobDetails;
   private int dismissalReason;
   private ResultsGroup result = null;
   private String errorMsg;
   private Exception exception;

   public OptimizationIndexes(short[] var1) {
      this.indexes = var1;
   }

   @Override
   public void setFitness(double var1, byte var3) {
      this.fitness = var1;
   }

   @Override
   public double getFitness(byte var1) {
      return this.fitness;
   }

   @Override
   public void setGPIDs(GPIDs var1) {
      this.gpIDs.set(var1);
   }

   @Override
   public GPIDs getGPIDs() {
      return this.gpIDs;
   }

   @Override
   public void destroy() {
      if (this.result != null) {
         this.result.clear();
         this.result = null;
      }
   }

   public OptimizationIndexes clone() {
      OptimizationIndexes var1 = new OptimizationIndexes(this.cloneArray(this.indexes));
      var1.fitness = this.fitness;
      var1.gpIDs.set(this.gpIDs);
      if (this.result != null) {
         var1.result = this.result.clone();
      }

      return var1;
   }

   @Override
   public IGPNode clonePassed() {
      return this.clone();
   }

   @Override
   public IGPNode cloneForMigration() {
      return this.clone();
   }

   @Override
   public IGPNode cloneDismissed(int var1) {
      OptimizationIndexes var2 = new OptimizationIndexes(this.cloneArray(this.indexes));
      var2.fitness = this.fitness;
      var2.gpIDs.set(this.gpIDs);
      var2.dismissalReason = this.dismissalReason;
      var2.result = null;
      return var2;
   }

   private short[] cloneArray(short[] var1) {
      if (var1 == null) {
         return null;
      }

      short[] var2 = new short[var1.length];

      for (int var3 = 0; var3 < var1.length; var3++) {
         var2[var3] = var1[var3];
      }

      return var2;
   }

   @Override
   public int getDismissalReason() {
      return this.dismissalReason;
   }

   @Override
   public boolean passedEvaluation() {
      return this.errorMsg == null;
   }

   public void setError(int var1, String var2, Exception var3) {
      this.fitness = 0.0;
      this.dismissalReason = var1;
      this.errorMsg = var2;
      this.exception = var3;
   }

   public static String convertToString(short[] var0) {
      if (var0 == null) {
         return "[null]";
      }

      StringBuilder var1 = new StringBuilder("[");

      for (int var2 = 0; var2 < var0.length; var2++) {
         if (var2 > 0) {
            var1.append(",");
         }

         var1.append(String.format("%d", var0[var2]));
      }

      var1.append("]");
      return var1.toString();
   }

   @Override
   public String getErrorMsg() {
      return this.errorMsg;
   }

   @Override
   public void setJobDetails(JobDetails var1) {
      this.jobDetails = var1;
   }

   @Override
   public JobDetails getJobDetails() {
      return this.jobDetails;
   }

   public short[] getIndexes() {
      return this.indexes;
   }

   public void setIndexes(short[] var1) {
      this.indexes = var1;
   }

   public ResultsGroup getResult() {
      return this.result;
   }

   public void setResult(ResultsGroup var1) {
      this.result = var1;
   }

   public int getLength() {
      return this.indexes.length;
   }

   public boolean isSame(short[] var1) {
      if (var1 != null && var1.length == this.indexes.length) {
         for (int var2 = 0; var2 < this.indexes.length; var2++) {
            if (this.indexes[var2] != var1[var2]) {
               return false;
            }
         }

         return true;
      } else {
         return false;
      }
   }

   @Override
   public int getHash() {
      return Arrays.hashCode(this.indexes);
   }

   @Override
   public String getAsString() {
      StringBuilder var1 = new StringBuilder(this.gpIDs.toString());
      var1.append("[");

      for (int var2 = 0; var2 < this.indexes.length; var2++) {
         if (var2 > 0) {
            var1.append(",");
         }

         var1.append(this.indexes[var2]);
      }

      var1.append("]");
      return var1.toString();
   }

   @Override
   public IGPNode cloneFull() {
      return null;
   }

   @Override
   public void setFitness(IGPNode var1) {
      OptimizationIndexes var2 = (OptimizationIndexes)var1;
      this.fitness = var2.getFitness((byte)0);
      this.result = var2.getResult();
   }

   @Override
   public int getFingerprint() {
      return 0;
   }

   @Override
   public Exception getException() {
      return this.exception;
   }
}
