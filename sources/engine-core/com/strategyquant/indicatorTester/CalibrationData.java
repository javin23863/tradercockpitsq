package com.strategyquant.indicatorTester;

public class CalibrationData {
   public boolean disabled = false;
   public String values = null;

   public CalibrationData(boolean var1) {
      this.disabled = var1;
   }

   public CalibrationData(String var1) {
      this.values = var1;
   }
}
