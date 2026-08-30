package SQ.Blocks.Indicators.Reflex;

import SQ.Internal.ConditionBlock;

import com.strategyquant.lib.*;
import com.strategyquant.datalib.*;
import com.strategyquant.tradinglib.*;

@BuildingBlock(name="(RCU) Fast Reflex Crosses UP Slow Reflex", display="Fast Reflex(@Chart@#FastPeriod#)[#Shift#] Crosses UP Slow Reflex(#SlowPeriod#)", returnType = ReturnTypes.Boolean)
@Help("Is triggered if fast Reflex crosses Slow reflex UP")
@MT5ExtendedTemplate
@OppositeBlock("FastReflexCrossDownSlowReflex")
@ParameterSet(set="FastPeriod=6,FastPeriod=12")
@ParameterSet(set="FastPeriod=12,FastPeriod=24")
@ParameterSet(set="FastPeriod=12,FastPeriod=36")
@ParameterSet(set="FastPeriod=24,FastPeriod=48")
@ParameterSet(set="FastPeriod=12,FastPeriod=120")
@ParameterSet(set="FastPeriod=10,FastPeriod=20")
@ParameterSet(set="FastPeriod=20,FastPeriod=40")
@ParameterSet(set="FastPeriod=50,FastPeriod=100")
public class FastReflexCrossUPSlowReflex extends ConditionBlock {
	
	@Parameter
	public ChartData Chart;
	
	@Parameter(defaultValue="24", minValue=2, maxValue=1000, step=1)
	public int FastPeriod;

	@Parameter(defaultValue="36", minValue=2, maxValue=1000, step=1)
	public int SlowPeriod;

	@Parameter
	public int Shift;
	
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	
	@Override
	public boolean OnBlockEvaluate() throws TradingException {
		
		Reflex fastIndicator = Strategy.Indicators.Reflex(Chart, FastPeriod);
		Reflex slowIndicator = Strategy.Indicators.Reflex(Chart, SlowPeriod);

		double currFastReflex = fastIndicator.Value.getRounded(Shift);
		double currSlowReflex = slowIndicator.Value.getRounded(Shift);

		double prevFastReflex = fastIndicator.Value.getRounded(Shift+1);
		double prevSlowReflex = slowIndicator.Value.getRounded(Shift+1);

		return (currFastReflex > currSlowReflex) & (prevFastReflex < prevSlowReflex);
	}

}