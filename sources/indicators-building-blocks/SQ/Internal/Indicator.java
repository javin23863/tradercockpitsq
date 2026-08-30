package SQ.Internal;

import com.strategyquant.datalib.TradingException;
import com.strategyquant.tradinglib.indicator.IndicatorBase;
import com.strategyquant.tradinglib.indicator.IndicatorsObj;

public abstract class Indicator extends IndicatorBase {
   protected Indicators Indicators;

   public void initialize(IndicatorsObj var1, boolean var2) throws TradingException {
      this.Indicators = (Indicators)var1;
      this.recognizeAndInitializeDataSeries(var2);
      this.callOnInit();
   }
}
