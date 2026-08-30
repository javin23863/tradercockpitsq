package SQ.Blocks.Indicators.Reflex;

import SQ.Internal.ConditionBlock;

import com.strategyquant.lib.*;
import com.strategyquant.datalib.*;
import com.strategyquant.tradinglib.*;

@BuildingBlock(name="(RFXD) Reflex Changes Direction Down", display="Reflex(@Chart@#Period#)[#Shift#] changes direction downwards", returnType = ReturnTypes.Boolean)
@Help("Is triggered if Reflex changes direction downwards")
@OppositeBlock("ReflexChangesDirectionUP")
@ParameterSet(set="Period=6")
@ParameterSet(set="Period=12")
@ParameterSet(set="Period=24")
@ParameterSet(set="Period=36")
@ParameterSet(set="Period=48")
@ParameterSet(set="Period=120")
@ParameterSet(set="Period=240")
@ParameterSet(set="Period=10")
@ParameterSet(set="Period=20")
@ParameterSet(set="Period=30")
@ParameterSet(set="Period=40")
@ParameterSet(set="Period=100")
@ParameterSet(set="Period=200")
public class ReflexChangesDirectionDown extends ConditionBlock {
	
	@Parameter
	public ChartData Chart;
	
	@Parameter(defaultValue="24", minValue=2, maxValue=10000, step=1)
	public int Period;

	@Parameter
	public int Shift;
	
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	
	@Override
	public boolean OnBlockEvaluate() throws TradingException {
		Reflex indicator = Strategy.Indicators.Reflex(Chart, Period);
		
		double v1 = indicator.Value.getRounded(Shift);
		double v2 = indicator.Value.getRounded(Shift + 1);
		double v3 = indicator.Value.getRounded(Shift + 2);
		double v4 = indicator.Value.getRounded(Shift + 3);

		return (v1 < v2 && v3<v2) || (v1 < v2 && v4<v3);
	}

}