package SQ.Blocks.Indicators.KAMA;

import SQ.Internal.ConditionBlock;
import com.strategyquant.lib.*;
import com.strategyquant.datalib.*;
import com.strategyquant.tradinglib.*;

@BuildingBlock(name="(CAK) Bar crosses above KAMA", display="Bar crosses above KAMA(@Chart@#ERPeriod#,#ShortPeriod#,#LongPeriod#)[#Shift#]", returnType = ReturnTypes.Boolean)
@Help("Is triggered if Bar closes above KAMA")
@MT5ExtendedTemplate
@OppositeBlock("BarClosesBelowKAMA")
public class BarClosesAboveKAMA extends ConditionBlock {
	
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
		
		double currKama = indicator.Value.getRounded(Shift);
		double prevKama = indicator.Value.getRounded(Shift+1);
		double currClose = Input.Close(Shift);
		double prevClose = Input.Close(Shift+1);

		return currKama<currClose && prevKama>prevClose;
	}

}
