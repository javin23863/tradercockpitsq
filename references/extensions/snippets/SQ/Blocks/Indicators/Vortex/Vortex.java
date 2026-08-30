package SQ.Blocks.Indicators.Vortex;

import com.strategyquant.datalib.*;
import com.strategyquant.tradinglib.*;
import SQ.Blocks.Indicators.MTATR.MTATR;
import SQ.Calculators.SumCalculator;

import SQ.Internal.IndicatorBlock;

/**
 * Indicator name as it will be displayed in UI, and its return type.
 * Possible return types:
 * ReturnTypes.Price - indicator is drawn on the price chart, like SMA, Bollinger Bands etc.
 * ReturnTypes.Price - indicator is drawn on separate chart, like CCI, RSI, MACD
 * ReturnTypes.PriceRange - indicator is price range, like ATR.
 */
@BuildingBlock(name="(VRX ) Vortex", display="Vortex(@Chart@#Period#)[#Shift#]", returnType = ReturnTypes.Number)
@ParameterSet(set="Period=10")
@ParameterSet(set="Period=20")
@ParameterSet(set="Period=30")
@ParameterSet(set="Period=40")
@ParameterSet(set="Period=12")
@ParameterSet(set="Period=24")
@ParameterSet(set="Period=120")
public class Vortex extends IndicatorBlock {

	@Parameter
	public ChartData Chart;

	@Parameter(defaultValue="12", isPeriod=true, minValue=2, maxValue=1000, step=1)
	public int Period;

	@Output(name = "VIPlusSumRge", color = Colors.Blue)
	public DataSeries VIPlusSumRge;

	@Output(name = "VIMinusSumRge", color = Colors.Red)
	public DataSeries VIMinusSumRge;

	private SumCalculator sumVplusCalculator ;
	private SumCalculator sumVminusCalculator ;
	private SumCalculator sumTrCalculator  ;


	@Override
	protected void OnInit() throws TradingException {
	sumVplusCalculator = new SumCalculator(Period);
	sumVminusCalculator = new SumCalculator(Period);
	sumTrCalculator = new SumCalculator(Period);
	}

	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------

	@Override
	protected void OnBarUpdate() throws TradingException {

		MTATR indicator = Indicators.MTATR(Chart, 1);
    	double atrValue = indicator.Value.getRounded(Shift);

		if (CurrentBar < Period){

			VIPlusSumRge.set(0,0);
			VIMinusSumRge.set(0,0);

		}
		else{

			double VMPlus = Math.abs(Chart.High(0)-Chart.Low(1));
			double VMMinus = Math.abs(Chart.Low(0)-Chart.High(1));
			double Tr = atrValue;

			sumVplusCalculator.onBarUpdate(VMPlus, getCurrentBar());
			sumVminusCalculator.onBarUpdate(VMMinus, getCurrentBar());
			sumTrCalculator.onBarUpdate(Tr, getCurrentBar());
			
			double sumVMPlus = sumVplusCalculator.getValue();
			double sumVMMinus = sumVminusCalculator.getValue();
			double sumTr = sumTrCalculator.getValue();

			VIPlusSumRge.set(0,sumVMPlus/sumTr);
			VIMinusSumRge.set(0,sumVMMinus/sumTr);
	
		}
 
	}	
}