package SQ.Blocks.Indicators.VWAP;

import SQ.Internal.ConditionBlock;

import com.strategyquant.lib.*;
import com.strategyquant.datalib.*;
import com.strategyquant.tradinglib.*;

@BuildingBlock(name="(VWAPF) VWAP is falling", display="VWAP(@Chart@#VWAPPeriod#)[#Shift#] is falling", returnType = ReturnTypes.Boolean)
@Help("Is triggered if VWAP is falling")
@OppositeBlock("VWAPRising")
@ParameterSet(set="Period=10")
@ParameterSet(set="Period=20")
@ParameterSet(set="Period=40")
@ParameterSet(set="Period=80")
@ParameterSet(set="Period=100")
@ParameterSet(set="Period=12")
@ParameterSet(set="Period=24")
@ParameterSet(set="Period=48")
@ParameterSet(set="Period=96")
@ParameterSet(set="Period=120")
public class VWAPFalling extends ConditionBlock {
	
	@Parameter
	public ChartData Chart;
	
	@Parameter(defaultValue="10", minValue=2, maxValue=480, step=1)
	public int VWAPPeriod;

	@Parameter
	public int Shift;
	
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	
	@Override
	public boolean OnBlockEvaluate() throws TradingException {
		VWAP indicator = Strategy.Indicators.VWAP(Chart, VWAPPeriod);

		double curVal = indicator.Value.getRounded(Shift);
		double prevVal = indicator.Value.getRounded(Shift+1);
		
		return (curVal < prevVal);
	}

}