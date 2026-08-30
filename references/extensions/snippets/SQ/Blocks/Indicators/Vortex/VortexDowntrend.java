package SQ.Blocks.Indicators.Vortex;

import SQ.Internal.ConditionBlock;

import com.strategyquant.lib.*;
import com.strategyquant.datalib.*;
import com.strategyquant.tradinglib.*;

@BuildingBlock(name="(VXD) Vortex is In Down Trend", display="Vortex(@Chart@#Period#)[#Shift#] is In Down Trend", returnType = ReturnTypes.Boolean)
@Help("Is triggered when Vortex is in downtrend")
@OppositeBlock("VortexUptrend")
@ParameterSet(set="Period=10")
@ParameterSet(set="Period=20")
@ParameterSet(set="Period=30")
@ParameterSet(set="Period=40")
@ParameterSet(set="Period=12")
@ParameterSet(set="Period=24")
@ParameterSet(set="Period=120")
public class VortexDowntrend extends ConditionBlock {
	
	@Parameter
	public ChartData Chart;
	
	@Parameter(defaultValue="14", minValue=2, maxValue=10000, step=1)
	public int Period;

	@Parameter
	public int Shift;
	
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	
	@Override
	public boolean OnBlockEvaluate() throws TradingException {
		Vortex indicator = Strategy.Indicators.Vortex(Chart, Period);
		double valuePlus = indicator.VIPlusSumRge.getRounded(Shift);
		double valueMinus = indicator.VIMinusSumRge.getRounded(Shift);
	
		return (valuePlus<valueMinus);
	}

}