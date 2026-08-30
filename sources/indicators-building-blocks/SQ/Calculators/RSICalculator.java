package SQ.Calculators;

import com.strategyquant.datalib.TradingException;

public class RSICalculator extends AbstractCalculator {
   private double currentValue;
   private double curAvgUp;
   private double lastAvgUp;
   private double curAvgDown;
   private double lastAvgDown;
   private AverageCalculator downAverageCalculator;
   private AverageCalculator upAverageCalculator;

   public RSICalculator(int var1) throws TradingException {
      super(var1);
      this.downAverageCalculator = new AverageCalculator(0, var1);
      this.upAverageCalculator = new AverageCalculator(0, var1);
   }

   @Override
   protected void calculate(double var1, boolean var3) throws TradingException {
      if (this.CurrentBar == 0) {
         this.downAverageCalculator.onBarUpdate(0.0, this.CurrentBar);
         this.upAverageCalculator.onBarUpdate(0.0, this.CurrentBar);
      } else {
         double var4 = this.bufferGet(1);
         double var6 = Math.max(var4 - var1, 0.0);
         double var8 = Math.max(var1 - var4, 0.0);
         if (!var3) {
            this.lastAvgDown = this.curAvgDown;
            this.lastAvgUp = this.curAvgUp;
         }

         if (this.CurrentBar < this.Period) {
            this.downAverageCalculator.onBarUpdate(var6, this.CurrentBar);
            this.upAverageCalculator.onBarUpdate(var8, this.CurrentBar);
         } else {
            if (this.CurrentBar == this.Period) {
               this.downAverageCalculator.onBarUpdate(Math.max(var4 - var1, 0.0), this.CurrentBar);
               this.upAverageCalculator.onBarUpdate(Math.max(var1 - var4, 0.0), this.CurrentBar);
               this.curAvgDown = this.downAverageCalculator.getValue();
               this.curAvgUp = this.upAverageCalculator.getValue();
            } else {
               this.curAvgDown = (this.lastAvgDown * (this.Period - 1) + var6) / this.Period;
               this.curAvgUp = (this.lastAvgUp * (this.Period - 1) + var8) / this.Period;
            }

            this.currentValue = 0.0;
            if (this.curAvgDown != 0.0) {
               this.currentValue = 100.0 - 100.0 / (1.0 + this.curAvgUp / this.curAvgDown);
            } else if (this.curAvgUp != 0.0) {
               this.currentValue = 100.0;
            } else {
               this.currentValue = 50.0;
            }
         }
      }
   }

   public double getValue() {
      return this.currentValue;
   }
}
