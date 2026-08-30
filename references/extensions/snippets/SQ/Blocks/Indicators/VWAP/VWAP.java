package SQ.Blocks.Indicators.VWAP;

import com.strategyquant.lib.*;
import com.strategyquant.datalib.*;
import com.strategyquant.tradinglib.*;

import SQ.Internal.IndicatorBlock;

/**
 * Indicator name as it will be displayed in UI, and its return type.
 * Possible return types:
 * ReturnTypes.Price - indicator is drawn on the price chart, like SMA, Bollinger Bands etc.
 * ReturnTypes.Number - indicator is drawn on separate chart, like CCI, RSI, MACD
 * ReturnTypes.PriceRange - indicator is price range, like ATR.
 */
@BuildingBlock(name="(VWAP) Volume Weighted Average Price - VWAP", display="Volume Weighted Average Price - VWAP(@Chart@#VWAPPeriod#)[#Shift#]", returnType = ReturnTypes.Price)
@Help("VWAP help text")
public class VWAP extends IndicatorBlock {

	@Parameter(defaultChartIndex=0)
	public ChartData Input;

	@Output
	public DataSeries Value;

	@Parameter(defaultValue="10", minValue=2, maxValue=480, step=1)
	public int VWAPPeriod;

	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------


	@Override
	protected void OnBarUpdate() throws TradingException {
		double  __ohlcvTotal = 0;
		double __volumeTotal = 0;
	  
		if (CurrentBar < VWAPPeriod) {

			Value.set(0,0);

		} else {

			for(int i = 0;i<VWAPPeriod;i++){
				double ohlcAvg = (Input.Open.get(i)+Input.High.get(i)+Input.Low.get(i)+Input.Close.get(i))/4;
				double vol = Input.Volume.get(i);

				__ohlcvTotal+= ohlcAvg * vol;
				__volumeTotal += vol;

			}

			double vwap = 0;
			if (__volumeTotal != 0){
				vwap = __ohlcvTotal/__volumeTotal;
			}
			
			Value.set(0, vwap);
			
		}   
	}
}