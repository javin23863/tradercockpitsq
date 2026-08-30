package SQ.Blocks.Indicators.AnchoredVWAP;

import SQ.Internal.ConditionBlock;

import com.strategyquant.lib.*;
import com.strategyquant.datalib.*;
import com.strategyquant.tradinglib.*;

@BuildingBlock(name="(CBAVWAP) Close is below Anchored VWAP ", display="Close is below AnchoredVWAP(@Chart@#SessionType#,#StdDevMult#)[#Shift#]", returnType = ReturnTypes.Boolean)
@Help("Close is below VWAP")
@ForEngine("MT4,MT5,TS,MC")
@OppositeBlock("CloseAboveAnchoredVWAP")
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
public class CloseBelowAnchoredVWAP extends ConditionBlock {
	
	@Parameter
	public ChartData Chart;
	
    @Parameter(defaultValue="1", name="SessionType")
    @Help("Anchor point for VWAP calculation")
    @Editor(type=Editors.Selection, values="Daily=1,Weekly=2,Monthly=3")
    public int SessionType;
    
    @Parameter(defaultValue="1.0", name="StdDevMult", minValue=0.5, maxValue=4.0, step=0.1)
    @Help("Standard deviation multiplier for bands")
    public double StdDevMult;

	@Parameter
	public int Shift;
	
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	
	@Override
	public boolean OnBlockEvaluate() throws TradingException {
		AnchoredVWAP indicator = Strategy.Indicators.AnchoredVWAP(Chart, SessionType, StdDevMult);

		double curVal = indicator.VWAP.getRounded(Shift);
		double close = Chart.Close.get(Shift);
		
		return (close < curVal);
	}

}