package SQ.Internal;

import com.strategyquant.datalib.TradingException;
import com.strategyquant.tradinglib.ChartData;
import com.strategyquant.tradinglib.StrategyBase;
import com.strategyquant.tradinglib.engine.TradingSetup;
import com.strategyquant.tradinglib.indicator.IndicatorsCache;

public abstract class Strategy extends StrategyBase {
   public Indicators Indicators;

   public void callOnInit(TradingSetup var1) throws Exception {
      this.tradingSetup = var1;
      this.Indicators = new Indicators(this);
      this.Indicators.Engine = var1.getEngineId();
      this.setEngine(var1.getEngineId());
      this.initializeFromMarketData(var1.getMarketData());
      this.dismissBadStrategies = var1.getDismissBadStrategies();
      this.warningsBadStrategies = var1.getWarningsBadStrategies();
      this.Initialize();
   }

   public void destroy() {
      if (this.Indicators != null) {
         this.Indicators.destroy();
      }
   }

   public IndicatorsCache getIndicatorsCache() {
      return this.Indicators.getIndicatorsCache();
   }

   public double getATRValue(ChartData var1, int var2, int var3) throws TradingException {
      return this.Indicators.ATR(var1, var2).Value.get(var3);
   }
}
