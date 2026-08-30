/*
 * Copyright (c) 2017-2021, StrategyQuant & clonex - All rights reserved.
 *
 * Code in this file was made in a good faith that it is correct and does what it should.
 * If you found a bug in this code OR you have an improvement suggestion OR you want to include
 * your own code snippet into our standard library please contact us at:
 * https://roadmap.strategyquant.com
 *
 * This code can be used only within StrategyQuant products.
 * Every owner of valid (free, trial or commercial) license of any StrategyQuant product
 * is allowed to freely use, copy, modify or make derivative work of this code without limitations,
 * to be used in all StrategyQuant products and share his/her modifications or derivative work
 * with the StrategyQuant community.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS BE LIABLE FOR ANY CLAIM, DAMAGES
 * OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */
package SQ.Blocks.Indicators.SchaffTrendCycle;

import com.strategyquant.datalib.*;
import com.strategyquant.tradinglib.*;
import SQ.Calculators.*;

import SQ.Internal.IndicatorBlock;

//------------------------------------------------------------------------
//------------------------------------------------------------------------
//------------------------------------------------------------------------

@BuildingBlock(name="(SCHTC) Schaff Trend Cycle", display="Schaff Trend Cycle(@Chart@#StochPeriod#,#FastPeriod#,#SlowPeriod#)[#Shift#]", returnType = ReturnTypes.Number)
@Indicator(oscillator=true, middleValue=50, min=0, max=100, step=0.01)
@Help("Schaff TrendCycle help text")
@ParameterSet(set="StochPeriod=12")
@ParameterSet(set="StochPeriod=24")
@ParameterSet(set="StochPeriod=48")
@ParameterSet(set="StochPeriod=96")
@ParameterSet(set="StochPeriod=120")
@ParameterSet(set="StochPeriod=10")
@ParameterSet(set="StochPeriod=20")
@ParameterSet(set="StochPeriod=40")
@ParameterSet(set="StochPeriod=60")
@ParameterSet(set="StochPeriod=100")
@ParameterSet(set="StochPeriod=200")
public class SchaffTrendCycle extends IndicatorBlock {

	@Parameter
	public ChartData Input;

	@Parameter(defaultValue="10", isPeriod=true, minValue=2, maxValue=480, step=1)
	public int StochPeriod;

	@Parameter(defaultValue="20", isPeriod=true, minValue=2, maxValue=480, step=1)
	public int FastPeriod;

	@Parameter(defaultValue="50", isPeriod=true, minValue=2, maxValue=480, step=1)
	public int SlowPeriod;

	@Output
	public DataSeries Value;

	private AverageCalculator fastEmaAvgCalculator;
	private AverageCalculator slowEmaAvgCalculator;

	private HighestCalculator highestCalculator;
	private LowestCalculator lowestCalculator;

	private HighestCalculator highestCalculator2;
	private LowestCalculator lowestCalculator2;

	@Buffer
	public DataSeries Frac1;
	
	@Buffer
	public DataSeries Frac2;

	@Buffer
	public DataSeries PF;

	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------

	@Override
	protected void OnInit() throws TradingException {

		fastEmaAvgCalculator = new AverageCalculator(AverageCalculator.EMA, FastPeriod);
		slowEmaAvgCalculator = new AverageCalculator(AverageCalculator.EMA, SlowPeriod);

		highestCalculator = new HighestCalculator(StochPeriod);
		lowestCalculator = new LowestCalculator(StochPeriod);

		highestCalculator2 = new HighestCalculator(StochPeriod);
		lowestCalculator2 = new LowestCalculator(StochPeriod);
	}

	//------------------------------------------------------------------------

	@Override
	protected void OnBarUpdate() throws TradingException {

		double factor = 0.5;
      
		if (CurrentBar < SlowPeriod) {

			Value.set(0, 0);
			Frac1.set(0, 0);
			Frac2.set(0, 0);

		} else {
			
			fastEmaAvgCalculator.onBarUpdate(Input.Close.get(0), getCurrentBar());
			slowEmaAvgCalculator.onBarUpdate(Input.Close.get(0), getCurrentBar());
			
			double macd = fastEmaAvgCalculator.getValue()-slowEmaAvgCalculator.getValue();
			
			highestCalculator.onBarUpdate(macd, getCurrentBar());
			lowestCalculator.onBarUpdate(macd, getCurrentBar());
			
				
			double value1 =  lowestCalculator.getLowestValue();
			double value2 =  highestCalculator.getHighestValue()-value1;
			
			if(value2>0){
				Frac1.set(0,((macd-value1)/value2)*100);
			} 
			else Frac1.set(0,Frac1.get(1));

			PF.set(0,(PF.get(1)+factor*(Frac1.get(0)-PF.get(1))));
			
			lowestCalculator2.onBarUpdate(PF.get(0), getCurrentBar());
			highestCalculator2.onBarUpdate(PF.get(0), getCurrentBar());
			
			double value3 =  lowestCalculator2.getLowestValue();
			double value4 =  highestCalculator2.getHighestValue()-value3;

			if (value4>0){
				Frac2.set(0,(PF.get(0)-value3)/value4*100);

			}
			else Frac2.set(0,Frac2.get(1)); 
			
			Value.set(0,Value.get(1)+(factor*(Frac2.get(0)-Value.get(1))));
		}
	}
}