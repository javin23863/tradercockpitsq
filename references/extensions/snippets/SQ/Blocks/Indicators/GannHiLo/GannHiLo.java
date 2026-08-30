package SQ.Blocks.Indicators.GannHiLo;

import com.strategyquant.datalib.*;
import com.strategyquant.tradinglib.*;

import SQ.Internal.IndicatorBlock;
import SQ.Calculators.AverageCalculator;

@BuildingBlock(name="(GHL) GannHiLo", display="GannHiLo(@Chart@#Period#)[#Shift#]", returnType = ReturnTypes.Price)
@Help("GannHiLo help text")
@ParameterSet(set="Period=12")
@ParameterSet(set="Period=24")
@ParameterSet(set="Period=48")
@ParameterSet(set="Period=120")
@ParameterSet(set="Period=5")
@ParameterSet(set="Period=10")
public class GannHiLo extends IndicatorBlock {

	@Parameter
	public ChartData Chart;

	@Parameter(defaultValue="10", isPeriod=true, minValue=2, maxValue=1000, step=1)
	public int Period;

	@Output
	public DataSeries GHA;

	@Buffer
	public DataSeries GHHigh;
	
	@Buffer
	public DataSeries GHLow;


	private AverageCalculator highAvgCalculator;
	private AverageCalculator lowAvgCalculator;
	private int hld;
	private int hlv;


	@Override
	protected void OnInit() throws TradingException {

		highAvgCalculator = new AverageCalculator(AverageCalculator.SMA, Period);
		lowAvgCalculator = new AverageCalculator(AverageCalculator.SMA, Period);
	}

	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------

	@Override
	protected void OnBarUpdate() throws TradingException {

			if(getCurrentBar() < Period){

				GHA.set(0,0);

			}

			else{

				if(getCurrentBar() > Period){

					
					highAvgCalculator.onBarUpdate(Chart.High.get(0), getCurrentBar());
					lowAvgCalculator.onBarUpdate(Chart.Low.get(0), getCurrentBar());


					GHHigh.set(0,highAvgCalculator.getValue());
					GHLow.set(0,lowAvgCalculator.getValue());

					if(Chart.Close.get(0)>GHHigh.get(1)){
						hld =1; 

					}
					else{
						if(Chart.Close.get(0)<GHLow.get(1)){
							hld =-1; 

						}
						else hld = 0;
					}
					if(hld!=0){
						hlv =  hld;

					}
					if(hlv==-1){
						GHA.set(0,GHHigh.get(0));

					}
					else GHA.set(0,GHLow.get(0));
				}
			}	
	}
}


	

