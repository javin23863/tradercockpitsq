package SQ.Calculators;

import com.strategyquant.datalib.TradingException;

public class AverageCalculator extends AbstractCalculator {
   public static final int SMA = 0;
   public static final int EMA = 1;
   public static final int SMMA = 2;
   public static final int LWMA = 3;
   private int Type = 0;
   private double lastValue;
   private double currentValue;
   private int NbBarLastReset = 0;
   private int AveragePrecision;

   public AverageCalculator(int var1, int var2) throws TradingException {
      super(var2);
      this.Type = var1;
      if (var1 >= 0 && var1 <= 3) {
         this.AveragePrecision = 0;
      } else {
         throw new TradingException("Invalid MA type used. Must be in range 0-3");
      }
   }

   public AverageCalculator(int var1, int var2, int var3) throws TradingException {
      super(var2);
      this.Type = var1;
      this.AveragePrecision = var3;
      if (this.AveragePrecision < 0) {
         this.AveragePrecision = 0;
      }

      if (var1 < 0 || var1 > 3) {
         throw new TradingException("Invalid MA type used. Must be in range 0-3");
      }
   }

   @Override
   protected void calculate(double var1, boolean var3) throws TradingException {
      if (!var3) {
         this.lastValue = this.currentValue;
      }

      switch (this.Type) {
         case 0:
            this.calculateSMA(this.CurrentBar, var1);
            break;
         case 1:
            this.calculateEMA(this.CurrentBar, var1);
            break;
         case 2:
            this.calculateSMMA(this.CurrentBar, var1);
            break;
         case 3:
            this.calculateLWMA(this.CurrentBar, var1);
      }
   }

   private void calculateSMA(int var1, double var2) {
      if (var1 == 0) {
         this.currentValue = var2;
      } else if (this.AveragePrecision == 0 || this.AveragePrecision > 0 && this.NbBarLastReset < this.Period * this.AveragePrecision / 100) {
         double var4 = this.lastValue * Math.min(var1, this.Period);
         if (var1 >= this.Period) {
            this.currentValue = (var4 + var2 - this.oldBufferValue) / Math.min(var1, this.Period);
         } else {
            this.currentValue = (var4 + var2) / (Math.min(var1, this.Period) + 1);
         }

         if (this.AveragePrecision > 0) {
            this.NbBarLastReset++;
         }
      } else {
         this.currentValue = this.AveragebufferGet();
         this.NbBarLastReset = 0;
      }
   }

   private void calculateEMA(int var1, double var2) {
      if (var1 == 0) {
         this.currentValue = var2;
      } else {
         this.currentValue = var2 * (2.0 / (1 + this.Period)) + (1.0 - 2.0 / (1 + this.Period)) * this.lastValue;
      }
   }

   private void calculateSMMA(int var1, double var2) {
      if (var1 == 1) {
         this.currentValue = var2;
      } else {
         this.currentValue = (this.lastValue * (this.Period - 1) + var2) / this.Period;
      }
   }

   private void calculateLWMA(int var1, double var2) {
      int var4 = Math.min(this.Period, var1);
      int var5 = this.Period;
      int var6 = 0;
      double var7 = 0.0;

      for (int var9 = 0; var9 < var4; var9++) {
         var6 += var5;
         var7 += this.bufferGet(var9) * var5--;
      }

      this.currentValue = var7 / var6;
   }

   public double getValue() {
      return this.currentValue;
   }
}
