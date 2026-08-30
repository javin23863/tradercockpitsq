package SQ.Blocks.Indicators.SuperTrend;

import SQ.Internal.ConditionBlock;

import com.strategyquant.lib.*;
import com.strategyquant.datalib.*;
import com.strategyquant.tradinglib.*;

@BuildingBlock(name="(STD) SuperTrend Down Trend", display="SuperTrend(@Chart@#Mode#,#ATRPeriod#,#ATRMult#)[#Shift#] is falling", returnType = ReturnTypes.Boolean)
@Help("Is triggered if SuperTrend is falling")
@OppositeBlock("SuperTrendUPTrend")
@ParameterSet(set="ATRPeriod=12")
@ParameterSet(set="ATRPeriod=24")
@ParameterSet(set="ATRPeriod=48")
@ParameterSet(set="ATRPeriod=120")
@ParameterSet(set="ATRPeriod=480")
@ParameterSet(set="ATRPeriod=10")
@ParameterSet(set="ATRPeriod=20")
@ParameterSet(set="ATRPeriod=40")
@ParameterSet(set="ATRPeriod=100")
@ParameterSet(set="ATRPeriod=500")
public class SuperTrendDownTrend extends ConditionBlock {
	
	@Parameter(defaultChartIndex=0)
	public ChartData Input;
	
	@Parameter(defaultValue="1")
	@Editor(type=Editors.Selection, values="Basic=1")
	public int Mode;

	@Parameter(defaultValue="24", isPeriod=true, minValue=2, maxValue=240, step=1)
	public int ATRPeriod;

	@Parameter(defaultValue="3", isPeriod=true, minValue=0.5, maxValue=10, step=0.1)
	public double ATRMult;

	@Parameter
	public int Shift;
	
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	
	@Override
	public boolean OnBlockEvaluate() throws TradingException {
		
		SuperTrend indicator = Strategy.Indicators.SuperTrend(Input, Mode,ATRPeriod,ATRMult);
		
		double curSTValue = indicator.Value.getRounded(Shift);
		double prevSTValue = indicator.Value.getRounded(Shift+1);
		
		return curSTValue<prevSTValue;
	}

}