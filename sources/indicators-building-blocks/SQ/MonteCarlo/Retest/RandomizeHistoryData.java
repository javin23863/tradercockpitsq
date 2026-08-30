package SQ.MonteCarlo.Retest;

import com.strategyquant.datalib.TickEvent;
import com.strategyquant.lib.IRandomGenerator;
import com.strategyquant.lib.SettingsMap;
import com.strategyquant.tradinglib.ChartSetup;
import com.strategyquant.tradinglib.ClassConfig;
import com.strategyquant.tradinglib.Help;
import com.strategyquant.tradinglib.MonteCarloRetest;
import com.strategyquant.tradinglib.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ClassConfig(
   name = "Randomize history data (by tick)",
   display = "Randomize history data (by tick), with probability #ProbabilityUp# % up / #ProbabilityDown# % down and max price change of ATR #MaxChangeUp# % up / #MaxChangeDown# % down"
)
@Help("<b>Note!</b>This option is supposed to work best with Selected Timeframe precision.")
public class RandomizeHistoryData extends MonteCarloRetest {
   public static final Logger Log = LoggerFactory.getLogger(RandomizeHistoryData.class);
   @Parameter(name = "Probability up", defaultValue = "20", minValue = 1.0, maxValue = 100.0, step = 1.0)
   @Help("Probability of changing the price up")
   public int ProbabilityUp;
   @Parameter(name = "Max up change", defaultValue = "10", minValue = 1.0, maxValue = 100.0, step = 1.0)
   @Help("Maximum up change in % of ATR(14)")
   public int MaxChangeUp;
   @Parameter(name = "Probability down", defaultValue = "20", minValue = 1.0, maxValue = 100.0, step = 1.0)
   @Help("Probability of changing the price down")
   public int ProbabilityDown;
   @Parameter(name = "Max change down", defaultValue = "10", minValue = 1.0, maxValue = 100.0, step = 1.0)
   @Help("Maximum down change in % of ATR(14)")
   public int MaxChangeDown;
   @Parameter(name = "Keep connected", defaultValue = "true")
   @Help("Preserve original gaps between previous Close and new Open bars")
   public boolean KeepConnected;
   private int relativeMaxChangeUp = -1;
   private int relativeMaxChangeDown = -1;
   private TickEvent lastTick = new TickEvent();
   private double lastOrigAsk;
   private double lastOrigBid;

   public RandomizeHistoryData() {
      super(2);
   }

   public void modifyData(IRandomGenerator var1, TickEvent var2, double var3) {
      boolean var9 = var1.nextInt(2) == 0;
      double var10 = var2.getAsk();
      double var12 = var2.getBid();
      double var5;
      int var7;
      int var8;
      if (var9) {
         var5 = this.ProbabilityUp / 100.0;
         var7 = this.relativeMaxChangeUp;
         var8 = this.MaxChangeUp;
      } else {
         var5 = this.ProbabilityDown / 100.0;
         var7 = this.relativeMaxChangeDown;
         var8 = this.MaxChangeDown;
      }

      if (var1.probability(var5)) {
         double var14 = var2.getAsk();
         double var16 = var2.getBid();
         double var18 = var14 - var16;
         if (this.KeepConnected && this.lastTick.getTime() > 0L && this.lastTick.getTime() != var2.getTime()) {
            var2.setAsk(this.lastTick.getAsk() + (var14 - this.lastOrigAsk));
            var2.setBid(this.lastTick.getBid() + (var16 - this.lastOrigBid));
         } else {
            int var20;
            if (var7 <= 0) {
               var20 = var1.nextInt(var8);
            } else {
               var20 = var1.nextInt(var7);
            }

            double var21 = var20 / 100.0;
            double var23 = 2.0 * var3 * var21;
            var16 = var9 ? var16 + var23 : var16 - var23;
            var14 = var16 + var18;
            var2.setAsk(var14);
            var2.setBid(var16);
         }
      }

      this.lastTick.set(var2.getTime(), 0, 0, var2.getBid(), var2.getAsk(), 0.0, 0L, true, true);
      this.lastOrigAsk = var10;
      this.lastOrigBid = var12;
   }

   public void initSettings(SettingsMap var1) {
      super.initSettings(var1);
      ChartSetup var2 = (ChartSetup)var1.get("BacktestChart");
      if (var2 != null) {
         int var3 = var2.getTestPrecision();
         if (var3 == 1) {
            this.relativeMaxChangeUp = this.MaxChangeUp;
            this.relativeMaxChangeDown = this.MaxChangeDown;
            return;
         }

         this.relativeMaxChangeUp = this.MaxChangeUp / 3;
         if (this.relativeMaxChangeUp <= 0) {
            this.relativeMaxChangeUp = 1;
         }

         this.relativeMaxChangeDown = this.MaxChangeDown / 3;
         if (this.relativeMaxChangeDown <= 0) {
            this.relativeMaxChangeDown = 1;
         }
      }
   }

   public RandomizeHistoryData getClone() throws Exception {
      RandomizeHistoryData var1 = new RandomizeHistoryData();
      var1.ProbabilityUp = this.ProbabilityUp;
      var1.ProbabilityDown = this.ProbabilityDown;
      var1.MaxChangeUp = this.MaxChangeUp;
      var1.MaxChangeDown = this.MaxChangeDown;
      var1.KeepConnected = this.KeepConnected;
      var1.setParams(this.getParams());
      return var1;
   }
}
