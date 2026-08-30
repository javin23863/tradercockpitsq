package SQ.Blocks.Indicators.KAMA;

import SQ.Internal.ConditionBlock;

import com.strategyquant.lib.*;
import com.strategyquant.datalib.*;
import com.strategyquant.tradinglib.*;

@BuildingBlock(name="(FASK) Fast KAMA is above Slow KAMA", display="Fast KAMA(@Chart@#FastERPeriod#,#FastShortPeriod#,#FastLongPeriod#)[#Shift#] is above Slow KAMA(@Chart@#SlowERPeriod#,#SlowShortPeriod#,#SlowLongPeriod#)", returnType = ReturnTypes.Boolean)
@Help("Is triggered if Fast KAMA is above Slow KAMA")
@MT5ExtendedTemplate
@OppositeBlock("FastKAMABelowSlowKAMA")
public class FastKAMAAboveSlowKAMA extends ConditionBlock {
	
	@Parameter(defaultChartIndex=0)
	public ChartData Input;

	@Parameter(defaultValue="10", isPeriod=true, minValue=2, maxValue=1000, step=1)
	public int FastERPeriod;

	@Parameter(defaultValue="2", isPeriod=true, minValue=2, maxValue=30, step=1)
	public int FastShortPeriod;

	@Parameter(defaultValue="30", isPeriod=true, minValue=2, maxValue=50, step=1)
	public int FastLongPeriod;


	@Parameter(defaultValue="10", isPeriod=true, minValue=2, maxValue=1000, step=1)
	public int SlowERPeriod;

	@Parameter(defaultValue="5", isPeriod=true, minValue=2, maxValue=30, step=1)
	public int SlowShortPeriod;

	@Parameter(defaultValue="30", isPeriod=true, minValue=2, maxValue=50, step=1)
	public int SlowLongPeriod;

	@Parameter
	public int Shift;

	
	
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	
	@Override
	public boolean OnBlockEvaluate() throws TradingException {
		KAMA fastIndicator = Strategy.Indicators.KAMA(Input, FastERPeriod,FastShortPeriod,FastLongPeriod);
		KAMA slowIndicator = Strategy.Indicators.KAMA(Input, SlowERPeriod,SlowShortPeriod,SlowLongPeriod);
		
		double fastKama = fastIndicator.Value.getRounded(Shift);
		double slowKama = slowIndicator.Value.getRounded(Shift);


		return fastKama>slowKama;
	}

}