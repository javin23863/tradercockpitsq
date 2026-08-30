package SQ.Blocks.Indicators.GannHiLo;

import SQ.Internal.ConditionBlock;

import com.strategyquant.lib.*;
import com.strategyquant.datalib.*;
import com.strategyquant.tradinglib.*;


@BuildingBlock(name="(GHD) GannHiLo is in Down Trend", display="GannHiLo(@Chart@#Period#)[#Shift#] is in Down Trend", returnType = ReturnTypes.Boolean)
@Help("Is triggered if Close < GannHiLo")
@OppositeBlock("GannHiLoUPTrend")
@ParameterSet(set="Period=12")
@ParameterSet(set="Period=24")
@ParameterSet(set="Period=48")
@ParameterSet(set="Period=120")
@ParameterSet(set="Period=5")
@ParameterSet(set="Period=10")
public class GannHiLoDownTrend extends ConditionBlock {
	
	@Parameter
	public ChartData Chart;
	
	@Parameter(defaultValue="10", minValue=2, maxValue=10000, step=1)
	public int Period;

	@Parameter
	public int Shift;
	
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	
	@Override
	public boolean OnBlockEvaluate() throws TradingException {
		GannHiLo indicator = Strategy.Indicators.GannHiLo(Chart, Period);
		double currentGH = indicator.GHA.getRounded(Shift);
		double currentClose = Chart.Close(Shift);

		return (currentClose < currentGH);
	}

}