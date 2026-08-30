package SQ.Blocks.Indicators.AnchoredVWAP;

import SQ.Internal.ConditionBlock;

import com.strategyquant.lib.*;
import com.strategyquant.datalib.*;
import com.strategyquant.tradinglib.*;

@BuildingBlock(name="(DVWAPU) Faster Anchored VWAP is above Slower Anchored VWAP", display="Faster AnchoredVWAP(@Chart@#FastSessionType#,#FastStdDevMult#)[#Shift#] is above Slower AnchoredVWAP(@Chart@#SlowSessionType#,#SlowStdDevMult#)[#Shift#]", returnType = ReturnTypes.Boolean)
@Help("Is triggered if Faster Anchored VWAP is above Slower Anchored VWAP")
@ForEngine("MT4,MT5,TS,MC")
@OppositeBlock("FastAnchoredVWAPBelowSlowAnchoredVWAP")
@ParameterSet(set="FastPeriod=10,SlowPeriod=20")
@ParameterSet(set="FastPeriod=20,SlowPeriod=40")
@ParameterSet(set="FastPeriod=40,SlowPeriod=80")
@ParameterSet(set="FastPeriod=100,SlowPeriod=200")
@ParameterSet(set="FastPeriod=12,SlowPeriod=24")
@ParameterSet(set="FastPeriod=24,SlowPeriod=48")
@ParameterSet(set="FastPeriod=48,SlowPeriod=96")
@ParameterSet(set="FastPeriod=120,SlowPeriod=240")
public class FastAnchoredVWAPAboveSlowAnchoredVWAP extends ConditionBlock {
	
	@Parameter
	public ChartData Chart;
	
    @Parameter(defaultValue="1", name="SessionType")
    @Help("Anchor point for VWAP calculation")
    @Editor(type=Editors.Selection, values="Daily=1,Weekly=2,Monthly=3")
    public int FastSessionType;
    
    @Parameter(defaultValue="1.0", name="StdDevMult", minValue=0.5, maxValue=4.0, step=0.1)
    @Help("Standard deviation multiplier for bands")
    public double FastStdDevMult;
    
    @Parameter(defaultValue="1", name="SessionType")
    @Help("Anchor point for VWAP calculation")
    @Editor(type=Editors.Selection, values="Daily=1,Weekly=2,Monthly=3")
    public int SlowSessionType;
    
    @Parameter(defaultValue="1.0", name="StdDevMult", minValue=0.5, maxValue=4.0, step=0.1)
    @Help("Standard deviation multiplier for bands")
    public double SlowStdDevMult;
	

	@Parameter
	public int Shift;
	
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	
	@Override
	public boolean OnBlockEvaluate() throws TradingException {
		AnchoredVWAP fastIndicator = Strategy.Indicators.AnchoredVWAP(Chart, FastSessionType, FastStdDevMult);
		AnchoredVWAP slowIndicator = Strategy.Indicators.AnchoredVWAP(Chart, SlowSessionType, SlowStdDevMult);

		double fastVal = fastIndicator.VWAP.getRounded(Shift);
		double slowVal = slowIndicator.VWAP.getRounded(Shift);
		
		return (fastVal > slowVal);
	}

}