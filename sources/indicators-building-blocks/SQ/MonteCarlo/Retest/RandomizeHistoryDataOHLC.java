package SQ.MonteCarlo.Retest;

import com.strategyquant.datalib.data.io.VersatileData;
import com.strategyquant.lib.IRandomGenerator;
import com.strategyquant.lib.SettingsMap;
import com.strategyquant.tradinglib.ClassConfig;
import com.strategyquant.tradinglib.Help;
import com.strategyquant.tradinglib.MonteCarloRetest;
import com.strategyquant.tradinglib.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ClassConfig(
   name = "Randomize OHLC history data",
   display = "Randomize OHLC history data, max price change #MaxChange# % of ATR(#ATRPeriod#) and probabilities (O,H,L,C): #ProbabilityOpen#, #ProbabilityHigh#, #ProbabilityLow#, #ProbabilityClose#"
)
@Help("<b>Note!</b>This option is supposed to work best with Selected Timeframe precision.")
public class RandomizeHistoryDataOHLC extends MonteCarloRetest {
   public static final Logger Log = LoggerFactory.getLogger(RandomizeHistoryDataOHLC.class);
   @Parameter(name = "% Probability, Open", defaultValue = "100", minValue = 0.0, maxValue = 100.0, step = 1.0)
   @Help("Probability of changing the Open price")
   public int ProbabilityOpen;
   @Parameter(name = "% Probability, High", defaultValue = "100", minValue = 0.0, maxValue = 100.0, step = 1.0)
   @Help("Probability of changing the High price")
   public int ProbabilityHigh;
   @Parameter(name = "% Probability, Low", defaultValue = "100", minValue = 0.0, maxValue = 100.0, step = 1.0)
   @Help("Probability of changing the Low price")
   public int ProbabilityLow;
   @Parameter(name = "% Probability, Close", defaultValue = "100", minValue = 0.0, maxValue = 100.0, step = 1.0)
   @Help("Probability of changing the Close price")
   public int ProbabilityClose;
   @Parameter(name = "Max Change in % of ATR", defaultValue = "10", minValue = 1.0, maxValue = 100.0, step = 1.0)
   @Help("Maximum up change in % of ATR")
   public int MaxChange;
   @Parameter(name = "ATR Period", defaultValue = "14", minValue = 3.0, maxValue = 200.0, step = 1.0)
   @Help("Maximum up change in % of ATR")
   public int ATRPeriod;
   @Parameter(name = "Keep connected", defaultValue = "true")
   @Help("Preserve original gaps between previous Close and new Open bars")
   public boolean KeepConnected;
   @Parameter(name = "% Probability, Close", defaultValue = "100", minValue = 0.0, maxValue = 100.0, step = 1.0)
   @Help("Probability of changing the gap - used only if KeepConnected=true")
   public int ProbabilityGapChange;
   @Parameter(name = "Max Change of gap in %", defaultValue = "10", minValue = 1.0, maxValue = 100.0, step = 1.0)
   @Help("Maximum change of gap in %. Used only if KeepConnected=true")
   public int MaxChangeOfGap;
   private double lastOriginalOpen = 0.0;
   private double lastOriginalClose = 0.0;
   private double lastClose = 0.0;
   private double lastGap = 0.0;
   private int CurrentBar = 0;
   private double atrValue = 0.0;

   public RandomizeHistoryDataOHLC() {
      super(2);
   }

   public void modifyOHLCData(IRandomGenerator var1, VersatileData var2, double var3) {
      if (var2.type != 1) {
         double var5 = this.computeATR(var2);
         double var7 = var5 * var1.nextInt(this.MaxChange) / 100.0;
         double var9 = 0.0;
         double var11 = 0.0;
         this.lastOriginalOpen = var2.open;
         if (this.lastOriginalClose != 0.0) {
            this.lastGap = this.lastOriginalOpen - this.lastOriginalClose;
         }

         this.lastOriginalClose = var2.close;
         if (this.shouldChange(var1, this.ProbabilityOpen)) {
            if (!this.KeepConnected || this.lastClose == 0.0) {
               var9 = var1.nextInt(1) == 0 ? 1.0 : -1.0;
               var11 = var9 * var7;
               var2.open += var11;
            } else if (this.shouldChange(var1, this.ProbabilityGapChange)) {
               var9 = var1.nextInt(1) == 0 ? 1.0 : -1.0;
               var11 = var9 * (this.lastGap * var1.nextInt(this.MaxChangeOfGap) / 100.0);
               var2.open = this.lastClose + this.lastGap + var11;
            } else {
               var2.open = this.lastClose + this.lastGap;
            }
         }

         if (this.shouldChange(var1, this.ProbabilityHigh)) {
            var9 = var1.nextInt(1) == 0 ? 1.0 : -1.0;
            var2.high = var2.high + var11 + var9 * var7;
         }

         if (this.shouldChange(var1, this.ProbabilityLow)) {
            var9 = var1.nextInt(1) == 0 ? 1.0 : -1.0;
            var2.low = var2.low + var11 + var9 * var7;
         }

         if (this.shouldChange(var1, this.ProbabilityClose)) {
            var9 = var1.nextInt(1) == 0 ? 1.0 : -1.0;
            var2.close = var2.close + var11 + var9 * var7;
         }

         if (var2.high < var2.open) {
            var2.high = var2.open;
         }

         if (var2.high < var2.close) {
            var2.high = var2.close;
         }

         if (var2.low > var2.open) {
            var2.low = var2.open;
         }

         if (var2.low > var2.close) {
            var2.low = var2.close;
         }

         this.lastClose = var2.close;
      }
   }

   private boolean shouldChange(IRandomGenerator var1, int var2) {
      if (var2 < 1) {
         return false;
      }

      double var3 = var2 / 100.0;
      return var1.probability(var3);
   }

   private double computeATR(VersatileData var1) {
      double var2 = var1.high;
      double var4 = var1.low;
      double var6 = var2 - var4;
      if (this.CurrentBar == 0) {
         this.atrValue = var6;
      } else {
         double var8 = this.lastClose;
         var6 = Math.max(Math.abs(var4 - var8), Math.max(var6, Math.abs(var2 - var8)));
         this.atrValue = ((Math.min(this.CurrentBar + 1, this.ATRPeriod) - 1) * this.atrValue + var6) / Math.min(this.CurrentBar + 1, this.ATRPeriod);
      }

      this.CurrentBar++;
      return this.atrValue;
   }

   public void initSettings(SettingsMap var1) {
      super.initSettings(var1);
   }

   public RandomizeHistoryDataOHLC getClone() throws Exception {
      RandomizeHistoryDataOHLC var1 = new RandomizeHistoryDataOHLC();
      var1.ProbabilityOpen = this.ProbabilityOpen;
      var1.ProbabilityHigh = this.ProbabilityHigh;
      var1.ProbabilityLow = this.ProbabilityLow;
      var1.ProbabilityClose = this.ProbabilityClose;
      var1.MaxChange = this.MaxChange;
      var1.ATRPeriod = this.ATRPeriod;
      var1.KeepConnected = this.KeepConnected;
      var1.ProbabilityGapChange = this.ProbabilityGapChange;
      var1.MaxChangeOfGap = this.MaxChangeOfGap;
      var1.setParams(this.getParams());
      return var1;
   }
}
