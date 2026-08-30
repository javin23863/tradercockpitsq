package SQ.Calculators;

import com.strategyquant.datalib.TradingException;

public class SumCalculator extends AbstractCalculator {
   private double lastValue;
   private double currentValue;

   public SumCalculator(int var1) throws TradingException {
      super(var1);
   }

   @Override
   protected void calculate(double var1, boolean var3) throws TradingException {
      if (!var3) {
         this.lastValue = this.currentValue;
      }

      this.currentValue = this.lastValue - this.oldBufferValue + var1;
   }

   public double getValue() {
      return this.currentValue;
   }
}
