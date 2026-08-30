package com.strategyquant.datalib.customData;

import com.strategyquant.lib.SQTime;
import java.util.Arrays;

public class CustomData {
   public long time = -1L;
   public double[] values = null;

   public CustomData(int var1) {
      this.values = new double[var1];
   }

   public void reset() {
      this.time = -1L;

      for (int var1 = 0; var1 < this.values.length; var1++) {
         this.values[var1] = 0.0;
      }
   }

   @Override
   public String toString() {
      return this.time != -1L && this.values != null ? SQTime.toDateMinuteString(this.time) + " - " + Arrays.toString(this.values) : null;
   }
}
