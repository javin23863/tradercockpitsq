package SQ.MonteCarlo.Retest;

import com.strategyquant.datalib.ChartDef;
import com.strategyquant.datalib.InstrumentInfo;
import com.strategyquant.datalib.TickEvent;
import com.strategyquant.lib.IRandomGenerator;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.SettingsMap;
import com.strategyquant.tradinglib.ChartSetup;
import com.strategyquant.tradinglib.ClassConfig;
import com.strategyquant.tradinglib.Help;
import com.strategyquant.tradinglib.MonteCarloRetest;
import com.strategyquant.tradinglib.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ClassConfig(
   name = "Randomize history data (by tick, fixed range)",
   display = "Modified randomize history data (by tick), with max change #MaxChange# % of tick price changes"
)
@Help("<b>Note!</b>This option is supposed to work best with Selected Timeframe precision.")
public class RandomizeHistoryDataFixedRange extends MonteCarloRetest {
   public static final Logger Log = LoggerFactory.getLogger(RandomizeHistoryDataFixedRange.class);
   @Parameter(name = "Max change price", defaultValue = "40", minValue = 0.0, maxValue = 1000.0, step = 1.0)
   public int MaxChange;
   private double lastbid = 0.0;
   private double lastorgbid = 0.0;
   private long lastticktime = 0L;
   private int SymDigMod;

   public RandomizeHistoryDataFixedRange() {
      super(2);
   }

   public void modifyData(IRandomGenerator var1, TickEvent var2, double var3) {
      double var5 = var2.getAsk();
      double var7 = var2.getBid();
      double var9 = var5 - var7;
      double var11 = 0.0;
      long var13 = var2.getTime();
      if (this.lastticktime <= var13 && this.lastticktime != 0L) {
         var11 = var7 - this.lastorgbid;
      } else {
         this.lastbid = var7;
         var11 = 0.0;
      }

      this.lastorgbid = var7;
      this.lastticktime = var13;
      if (this.MaxChange <= 0) {
         this.MaxChange = 1;
      }

      int var15 = var1.nextInt(this.MaxChange);
      double var16 = var15 / 100.0;
      double var18 = var11 * var16;
      var7 = var1.nextInt(2) == 0 ? this.lastbid + var11 + var18 : this.lastbid + var11 - var18;
      this.lastbid = var7;
      var7 = SQUtils.round(var7, this.SymDigMod);
      var2.setBid(var7);
      var2.setAsk(var7 + var9);
   }

   public void initSettings(SettingsMap var1) {
      super.initSettings(var1);
      ChartSetup var2 = (ChartSetup)var1.get("BacktestChart");
      ChartDef var3 = var2.getMainChart();
      InstrumentInfo var4 = var3.getSymbolInfo();
      this.SymDigMod = var4.decimals;
   }

   public RandomizeHistoryDataFixedRange getClone() throws Exception {
      RandomizeHistoryDataFixedRange var1 = new RandomizeHistoryDataFixedRange();
      var1.MaxChange = this.MaxChange;
      var1.setParams(this.getParams());
      return var1;
   }
}
