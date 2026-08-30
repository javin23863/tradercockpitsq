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
package SQ.Blocks.Indicators.MovingAverage;

import SQ.Calculators.AverageCalculator;
import SQ.Internal.IndicatorBlock;

import com.strategyquant.datalib.DataSeries;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.Colors;
import com.strategyquant.tradinglib.Help;
import com.strategyquant.tradinglib.Output;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.ParameterSet;
import com.strategyquant.tradinglib.ReturnTypes;


@BuildingBlock(name="(TEMA) Triple Exponential Moving Average", display="TEMA(@Chart@#Period#)[#Shift#]", returnType = ReturnTypes.Price)
@Help("Triple Exponential Moving Average")
@ParameterSet(set="Period=14")
@ParameterSet(set="Period=20")
@ParameterSet(set="Period=30")
@ParameterSet(set="Period=40")
@ParameterSet(set="Period=14,ComputedFrom=0")
@ParameterSet(set="Period=20,ComputedFrom=0")
@ParameterSet(set="Period=30,ComputedFrom=0")
@ParameterSet(set="Period=40,ComputedFrom=0")
public class TEMA extends IndicatorBlock {

	@Parameter
	public DataSeries Input;

	@Parameter(defaultValue="14")
	public int Period;
	
	@Output(name = "TEMA", color = Colors.Red)
	public DataSeries Value;
	
	private AverageCalculator ema1Calculator;
	private AverageCalculator ema2Calculator;
	private AverageCalculator ema3Calculator;
	
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------

	@Override
	protected void OnInit() throws TradingException {
		ema1Calculator = new AverageCalculator(AverageCalculator.EMA, Period);
		ema2Calculator = new AverageCalculator(AverageCalculator.EMA, Period);
		ema3Calculator = new AverageCalculator(AverageCalculator.EMA, Period);
	}
	
	//------------------------------------------------------------------------
	
	@Override
	protected void OnBarUpdate() throws TradingException {
		ema1Calculator.onBarUpdate(Input.get(0), getCurrentBar());
		ema2Calculator.onBarUpdate(ema1Calculator.getValue(), getCurrentBar());
		ema3Calculator.onBarUpdate(ema2Calculator.getValue(), getCurrentBar());
		
		Value.set(0, 3 * ema1Calculator.getValue() - 3 * ema2Calculator.getValue() + ema3Calculator.getValue());
	}

}
