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
package SQ.Blocks.Indicators.OSMA;

import SQ.Calculators.AverageCalculator;
import SQ.Internal.IndicatorBlock;

import com.strategyquant.datalib.DataSeries;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.Colors;
import com.strategyquant.tradinglib.Indicator;
import com.strategyquant.tradinglib.Output;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.ParameterSet;
import com.strategyquant.tradinglib.ReturnTypes;


@BuildingBlock(name="(OSMA) Moving Average Of Oscillator", display="OSMA(@Chart@#FastEMA#, #SlowEMA#, #SignalPeriod#)[#Shift#]", returnType = ReturnTypes.Number)
@Indicator(oscillator=true, middleValue=0, min=-0.3, max=0.3, step=0.001)
@ParameterSet(set="FastEMA=12,SlowEMA=26,SignalPeriod=9,ComputedFrom=0")
@ParameterSet(set="FastEMA=24,SlowEMA=52,SignalPeriod=9,ComputedFrom=0")
@ParameterSet(set="FastEMA=8,SlowEMA=17,SignalPeriod=9,ComputedFrom=0")
@ParameterSet(set="FastEMA=3,SlowEMA=10,SignalPeriod=16,ComputedFrom=0")
public class OSMA extends IndicatorBlock {

	@Parameter
	public DataSeries Input;

	@Parameter(name="Fast EMA", defaultValue="12", isPeriod=true)
	public int FastEMA;
	
	@Parameter(name="Slow EMA", defaultValue="26", isPeriod=true)
	public int SlowEMA;

	@Parameter(defaultValue="9", isPeriod=true)
	public int SignalPeriod;

	@Output(name = "OSMA", color = Colors.Red)
	public DataSeries Value;

	private AverageCalculator fastEMACalculator;
	private AverageCalculator slowEMACalculator;
	private AverageCalculator signalCalculator;
	
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------

	@Override
	protected void OnInit() throws TradingException {
		fastEMACalculator = new AverageCalculator(AverageCalculator.EMA, FastEMA);
		slowEMACalculator = new AverageCalculator(AverageCalculator.EMA, SlowEMA);
		signalCalculator = new AverageCalculator(AverageCalculator.SMA, SignalPeriod);
	}
	
	//------------------------------------------------------------------------
	
	@Override
	protected void OnBarUpdate() throws TradingException {
		fastEMACalculator.onBarUpdate(Input.get(0), getCurrentBar());
		slowEMACalculator.onBarUpdate(Input.get(0), getCurrentBar());
		
		double macd = fastEMACalculator.getValue() - slowEMACalculator.getValue();
		
		signalCalculator.onBarUpdate(macd, getCurrentBar());
		
		Value.set(0, macd - signalCalculator.getValue());
	}
	
}
