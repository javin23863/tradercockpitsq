package SQ.Blocks.Indicators.KAMA;

import SQ.Internal.ConditionBlock;

import com.strategyquant.lib.*;
import com.strategyquant.datalib.*;
import com.strategyquant.tradinglib.*;

@BuildingBlock(name="(KAR) KAMA is rising", display="KAMA(@Chart@#ERPeriod#,#ShortPeriod#,#LongPeriod#)[#Shift#] is rising", returnType = ReturnTypes.Boolean)
@Help("Is triggered if KAMA is rising 2 bars")
@OppositeBlock("KAMAFalling")
public class KAMARising extends ConditionBlock {
	
	@Parameter(defaultChartIndex=0)
	public ChartData Input;

	@Parameter(defaultValue="10", isPeriod=true, minValue=2, maxValue=1000, step=1)
	public int ERPeriod;

	@Parameter(defaultValue="2", isPeriod=true, minValue=2, maxValue=30, step=1)
	public int ShortPeriod;

	@Parameter(defaultValue="30", isPeriod=true, minValue=2, maxValue=50, step=1)
	public int LongPeriod;

	@Parameter
	public int Shift;
	
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	
	@Override
	public boolean OnBlockEvaluate() throws TradingException {
		KAMA indicator = Strategy.Indicators.KAMA(Input, ERPeriod,ShortPeriod,LongPeriod);
		double value1 = indicator.Value.getRounded(Shift + 1);
		double value2 = indicator.Value.getRounded(Shift);
		
		return value1 < value2;
	}

}