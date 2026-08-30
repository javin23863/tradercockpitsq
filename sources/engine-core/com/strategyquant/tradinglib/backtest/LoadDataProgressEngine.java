package com.strategyquant.tradinglib.backtest;

import com.strategyquant.tradinglib.project.ProgressEngine;
import com.strategyquant.tradinglib.project.StopPauseEngine;

public class LoadDataProgressEngine {
   private ProgressEngine pe;
   private String symbolTF;
   private String testName;

   public LoadDataProgressEngine(ProgressEngine var1) {
      this.pe = var1;
   }

   public StopPauseEngine getProgressEngine() {
      return this.pe;
   }

   public void printToLog(String var1) {
      this.pe.printToLog(var1);
   }

   public void setParams(String var1, String var2, String var3) {
      this.symbolTF = String.format("%s / %s", var2, var3);
      this.testName = var1;
   }

   public String getSymbolTF() {
      return this.symbolTF;
   }

   public String getTestName() {
      return this.testName;
   }

   public void onDataPreparationFinished(double var1, long var3) {
   }
}
