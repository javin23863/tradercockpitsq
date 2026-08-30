package SQ.Calculators;

import com.strategyquant.datalib.TradingException;
import com.strategyquant.tradinglib.debug.Debugger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractCalculator extends Debugger {
   public static final Logger Log = LoggerFactory.getLogger("AbstractCalculator");
   protected int Period;
   private double[] buffer;
   private int bufferIndexOffset = 0;
   protected double oldBufferValue = 0.0;
   protected int CurrentBar = 0;
   private int bufferLastBar = 0;

   public AbstractCalculator(int var1) throws TradingException {
      if (var1 <= 0) {
         Log.debug("Period is 0, used value 2");
         var1 = 2;
      }

      this.Period = var1;
   }

   public void onBarUpdate(double var1, int var3) throws TradingException {
      if (this.buffer == null) {
         this.buffer = new double[this.Period];
         this.bufferIndexOffset = 0;
         this.bufferLastBar = var3;
      }

      this.CurrentBar = var3;
      boolean var4 = this.bufferLastBar == var3;
      if (var4) {
         this.bufferSet(0, var1);
      } else {
         this.shiftBuffer(var1);
         this.bufferLastBar = var3;
      }

      this.calculate(var1, var4);
   }

   protected abstract void calculate(double var1, boolean var3) throws TradingException;

   protected void shiftBuffer(double var1) {
      this.oldBufferValue = this.bufferGet(this.bufferLength() - 1);
      this.increaseBufferIndexOffset();
      this.bufferSet(0, var1);
   }

   protected void increaseBufferIndexOffset() {
      this.bufferIndexOffset++;
      if (this.bufferIndexOffset > this.buffer.length - 1) {
         this.bufferIndexOffset = 0;
      }
   }

   public double bufferGet(int var1) {
      int var2 = this.computeRealIndex(var1);
      return this.buffer[var2];
   }

   private void bufferSet(int var1, double var2) {
      int var4 = this.computeRealIndex(var1);
      this.buffer[var4] = var2;
   }

   public double AveragebufferGet() {
      Double var1 = 0.0;

      for (int var2 = 0; var2 <= Math.min(this.CurrentBar, this.Period - 1); var2++) {
         var1 = var1 + this.buffer[var2];
      }

      return var1 / Math.min(this.CurrentBar + 1, this.Period);
   }

   protected int computeRealIndex(int var1) {
      int var2 = this.bufferIndexOffset - var1;
      if (var2 < 0) {
         var2 += this.buffer.length;
      }

      return var2;
   }

   protected int bufferLength() {
      return this.buffer.length;
   }
}
