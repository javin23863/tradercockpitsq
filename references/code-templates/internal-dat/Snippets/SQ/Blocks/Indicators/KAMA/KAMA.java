package SQ.Blocks.Indicators.KAMA;

import com.strategyquant.lib.*;
import com.strategyquant.datalib.*;
import com.strategyquant.tradinglib.*;
import SQ.Calculators.*;

import SQ.Internal.IndicatorBlock;

/**
 * Indicator name as it will be displayed in UI, and its return type.
 * Possible return types:
 * ReturnTypes.Price - indicator is drawn on the price chart, like SMA, Bollinger Bands etc.
 * ReturnTypes.Price - indicator is drawn on separate chart, like CCI, RSI, MACD
 * ReturnTypes.PriceRange - indicator is price range, like ATR.
 */
@BuildingBlock(name="(KAMA) Kaufman's  Adaptive Moving Average", display="Kaufman's Adaptive Moving Average(@Chart@#ERPeriod#,#ShortPeriod#,#LongPeriod#)[#Shift#]", returnType = ReturnTypes.Price)
@Help("KAMA is another smoothed MA.")
public class KAMA extends IndicatorBlock {

	@Parameter(defaultChartIndex=0)
	public ChartData Chart;

	@Parameter(defaultValue="10", isPeriod=true, minValue=2, maxValue=1000, step=1)
	public int ERPeriod;

	@Parameter(defaultValue="2", isPeriod=true, minValue=2, maxValue=500, step=1)
	public int ShortPeriod;

	@Parameter(defaultValue="30", isPeriod=true, minValue=2, maxValue=500, step=1)
	public int LongPeriod;

	@Output(name = "KAMA", color = Colors.Blue)
	public DataSeries Value;

	private SumCalculator volsumcalculator;

    private AverageCalculator fastestExponentialMovingAverage;
    private AverageCalculator slowestExponentialMovingAverage;

	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------

	@Override
    protected void OnInit() throws TradingException {
		volsumcalculator = new SumCalculator(ERPeriod);
    }

	//------------------------------------------------------------------------

	@Override
	protected void OnBarUpdate() throws TradingException {

		double max = Math.max(Math.max(ShortPeriod,LongPeriod),ERPeriod);

		if(CurrentBar < max){
			Value.set(0, 0);
		} else{

			double fastc = (double)2/(ShortPeriod+1);
			double slowc = (double)2/(LongPeriod+1);
			double er = kaufmanEfficiencyRatio(Chart,ERPeriod,volsumcalculator);
			double sc = Math.pow((er*(fastc-slowc)+slowc),2);

			double kama = (Value.get(1)+ sc*(Chart.Close.get(0)-Value.get(1)));

			Value.set(0, kama);
		}
	}

	//------------------------------------------------------------------------

	private double kaufmanEfficiencyRatio(ChartData Chart,int ERPeriod,SumCalculator volsumcalculator) throws TradingException{

		double change = Math.abs(Chart.Close.get(0)-Chart.Close.get(ERPeriod)); /// Direction = ABS (Close Close[n])	
		volsumcalculator.onBarUpdate(Math.abs(Chart.Close.get(0)-Chart.Close.get(1)), getCurrentBar()); //// Volatility = (ABS(Close  Close[1]))
		double calc = volsumcalculator.getValue(); 
		double ker = SQUtils.safeDivide(change, calc);
				
		return ker;
	}
}


	