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
package SQ.Blocks.Indicators.WilliamsPR;

import SQ.Calculators.HighestCalculator;
import SQ.Calculators.LowestCalculator;
import SQ.Internal.IndicatorBlock;

import com.strategyquant.datalib.DataSeries;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.ChartData;
import com.strategyquant.tradinglib.Colors;
import com.strategyquant.tradinglib.Help;
import com.strategyquant.tradinglib.Indicator;
import com.strategyquant.tradinglib.Output;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.ParameterSet;
import com.strategyquant.tradinglib.ReturnTypes;


@BuildingBlock(name="(WILL%R) Williams Percent Range", display="Williams %R(@Chart@#Period#)[#Shift#]", returnType = ReturnTypes.Number)
@Help("The Williams Percent Range is a momentum indicator that is designed to identify overbought and oversold areas in a nontrending market.")
@Indicator(oscillator=true, middleValue=-50, min=-100, max=0, step=1)
@ParameterSet(set="Period=14")
@ParameterSet(set="Period=20")
@ParameterSet(set="Period=30")
public class WilliamsPR extends IndicatorBlock {

	@Parameter
	public ChartData Chart;

	@Parameter(defaultValue="14")
	public int Period;
	
	@Output(name = "Williams %R", color = Colors.Red)
	public DataSeries Value;
	
	private HighestCalculator highestCalculator;
	private LowestCalculator lowestCalculator;
	
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------

	@Override
	protected void OnInit() throws TradingException {
		highestCalculator = new HighestCalculator(Period);
		lowestCalculator = new LowestCalculator(Period);
	}
	
	//------------------------------------------------------------------------
	
	@Override
	protected void OnBarUpdate() throws TradingException {
		highestCalculator.onBarUpdate(Chart.High.get(0), getCurrentBar());
		lowestCalculator.onBarUpdate(Chart.Low.get(0), getCurrentBar());
		
		double highestHigh = highestCalculator.getHighestValue();
		double lowestLow = lowestCalculator.getLowestValue();
		
		double willR = -100 * (highestHigh - Chart.Close.get(0)) / ((highestHigh - lowestLow) == 0 ? 1 : (highestHigh - lowestLow));
		
		Value.set(0, willR);
	}

}
