/*
 * Copyright (c) 2017-2018, StrategyQuant - All rights reserved.
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
package SQ.Blocks.Indicators.DeMarker;


import com.strategyquant.datalib.DataSeries;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.tradinglib.Buffer;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.ChartData;
import com.strategyquant.tradinglib.Colors;
import com.strategyquant.tradinglib.Help;
import com.strategyquant.tradinglib.Indicator;
import com.strategyquant.tradinglib.Output;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.ParameterSet;
import com.strategyquant.tradinglib.ReturnTypes;

import SQ.Calculators.AverageCalculator;
import SQ.Internal.IndicatorBlock;


@BuildingBlock(name="(DE) DeMarker", display="DeMarker(@Chart@#Period#)[#Shift#]", returnType = ReturnTypes.Number)
@Help("DeMarker")
@Indicator(oscillator=true, middleValue=0.5, min=0, max=1, step=0.01)
@ParameterSet(set="Period=14")
@ParameterSet(set="Period=20")
@ParameterSet(set="Period=30")
@ParameterSet(set="Period=40")
@ParameterSet(set="Period=50")
public class DeMarker extends IndicatorBlock {
	@Parameter
	public ChartData Chart;
	
	@Parameter(defaultValue="14", minValue=2, maxValue=10000, step=1)
	public int Period;

	@Output(name = "DeMarker", color = Colors.Red)
	public DataSeries Value;
	
	@Buffer
	public DataSeries DeMin, DeMax; 

	private AverageCalculator DeMinAverageCalculator;
	private AverageCalculator DeMaxAverageCalculator;
	
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------

	@Override
	protected void OnInit() throws TradingException {
		DeMinAverageCalculator = new AverageCalculator(AverageCalculator.SMA, Period);
		DeMaxAverageCalculator = new AverageCalculator(AverageCalculator.SMA, Period);
	}
	
	//------------------------------------------------------------------------
	
	@Override
	protected void OnBarUpdate() throws TradingException {
		if(getCurrentBar() == 0){
			Value.set(0, 0);
			DeMinAverageCalculator.onBarUpdate(0, getCurrentBar());
			DeMaxAverageCalculator.onBarUpdate(0, getCurrentBar());
			return;
		}
		
		DeMaxAverageCalculator.onBarUpdate(Math.max(Chart.High(0) - Chart.High(1), 0), getCurrentBar());
		DeMinAverageCalculator.onBarUpdate(Math.max(Chart.Low(1) - Chart.Low(0), 0), getCurrentBar());
		
		double avgDeMin = DeMinAverageCalculator.getValue();
		double avgDeMax = DeMaxAverageCalculator.getValue();
		
		double dNum = avgDeMin + avgDeMax;
		
		if(dNum != 0) Value.set(0, avgDeMax / dNum);
	    else Value.set(0, 0);
	}

}
