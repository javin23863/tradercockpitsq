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
package SQ.Blocks.Indicators.CCI;


import com.strategyquant.datalib.DataSeries;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.Colors;
import com.strategyquant.tradinglib.Help;
import com.strategyquant.tradinglib.Indicator;
import com.strategyquant.tradinglib.Output;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.ParameterSet;
import com.strategyquant.tradinglib.ReturnTypes;

import SQ.Calculators.AverageCalculator;
import SQ.Internal.IndicatorBlock;

@BuildingBlock(name="(CCI) Commodity Channel Index", display="CCI(@Chart@#Period#)[#Shift#]", returnType = ReturnTypes.Number)
@Help("Commodity channel index")
@Indicator(oscillator=true, middleValue=0, min=-500, max=500, step=5)
@ParameterSet(set="Period=14")
@ParameterSet(set="Period=20")
@ParameterSet(set="Period=30")
@ParameterSet(set="Period=40")
@ParameterSet(set="Period=50")
@ParameterSet(set="Period=14,ComputedFrom=0")
@ParameterSet(set="Period=20,ComputedFrom=0")
@ParameterSet(set="Period=30,ComputedFrom=0")
@ParameterSet(set="Period=40,ComputedFrom=0")
@ParameterSet(set="Period=50,ComputedFrom=0")
public class CCI extends IndicatorBlock {
	@Parameter
	public DataSeries Input;
	
	@Parameter(defaultValue="14", minValue=2, maxValue=10000, step=1)
	public int Period;

	@Output(name = "CCI", color = Colors.Red)
	public DataSeries Value;
	
	private AverageCalculator averageCalculator;
	
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------

	@Override
	protected void OnInit() throws TradingException {
		averageCalculator = new AverageCalculator(AverageCalculator.SMA, Period);
	}
	
	//------------------------------------------------------------------------
	
	@Override
	protected void OnBarUpdate() throws TradingException {
		averageCalculator.onBarUpdate(Input.get(0), getCurrentBar());
		
		if (getCurrentBar() == 0) {
			Value.set(0, 0);
		} 
		else {
			double mean = 0;
			double sma = averageCalculator.getValue();
			
			for (int idx = Math.min(CurrentBar, Period - 1); idx >= 0; idx--) {
				mean += Math.abs(Input.get(idx) - sma);
			}
			
			if(mean < 0.0000000001) {
				Value.set(0, 0);
			} else {
				double cci = (Input.get(0) - sma) / (mean == 0 ? 1 : (0.015 * (mean / Math.min(Period, CurrentBar + 1))));
				Value.set(0, cci);
			}
		}
	}
	
}
