package SQ.Calculators;

import com.strategyquant.datalib.TradingException;

public class HighestCalculator extends AbstractCalculator {
   private int highestIndex;
   private double highestValue = -Double.MAX_VALUE;

   public HighestCalculator(int var1) throws TradingException {
      super(var1);
   }

   @Override
   protected void calculate(double var1, boolean var3) throws TradingException {
      int var4 = this.CurrentBar < this.Period ? this.CurrentBar : this.bufferLength() - 1;
      if (var1 > this.highestValue) {
         this.highestIndex = 0;
         this.highestValue = var1;
      } else if (this.highestIndex < var4) {
         if (!var3) {
            this.highestIndex++;
         } else if (this.highestIndex == 0) {
            this.findHighestInBuffer(var4);
         }
      } else {
         this.findHighestInBuffer(var4);
      }
   }

   private void findHighestInBuffer(int var1) {
      this.highestIndex = 0;
      this.highestValue = -Double.MAX_VALUE;

      for (int var2 = var1; var2 >= 0; var2--) {
         double var3 = this.bufferGet(var2);
         if (this.highestValue < var3) {
            this.highestValue = var3;
            this.highestIndex = var2;
         }
      }
   }

   public double getHighestValue() {
      return this.highestValue;
   }

   public int getHighestIndex() {
      return this.highestIndex;
   }
}
