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
package SQ.Blocks.Indicators.MACD;

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

@BuildingBlock(name="(MACD) MACD", display="MACD(@Chart@#Fast#, #Slow#, #Smooth#).#Line#[#Shift#]")
@Indicator(oscillator=true, middleValue=0, min=-5, max=5, step=0.001)
@ParameterSet(set="Fast=12,Slow=26,Smooth=9,ComputedFrom=0")
@ParameterSet(set="Fast=24,Slow=52,Smooth=9,ComputedFrom=0")
@ParameterSet(set="Fast=8,Slow=17,Smooth=9,ComputedFrom=0")
@ParameterSet(set="Fast=3,Slow=10,Smooth=16,ComputedFrom=0")
public class MACD extends IndicatorBlock {	
	
	@Parameter
	public DataSeries Input;
	
	@Parameter(defaultValue = "12", isPeriod = true)
	public int Fast;
	
	@Parameter(defaultValue = "26", isPeriod = true)
	public int Slow;

	@Parameter(defaultValue = "9", isPeriod = true)
	public int Smooth;
	
	@Output(name = "Main", color = Colors.Red)
	public DataSeries Main;

	@Output(name = "Signal", color = Colors.Green)
	public DataSeries Signal;

	private AverageCalculator fastEMACalculator;
	private AverageCalculator slowEMACalculator;
	private AverageCalculator signalCalculator;
	
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------

	@Override
	protected void OnInit() throws TradingException {
		fastEMACalculator = new AverageCalculator(AverageCalculator.EMA, Fast);
		slowEMACalculator = new AverageCalculator(AverageCalculator.EMA, Slow);
		signalCalculator = new AverageCalculator(AverageCalculator.SMA, Smooth);
	}
	
	//------------------------------------------------------------------------
	
	@Override
	protected void OnBarUpdate() throws TradingException {
		fastEMACalculator.onBarUpdate(Input.get(0), getCurrentBar());
		slowEMACalculator.onBarUpdate(Input.get(0), getCurrentBar());
		
		double fastEMA = fastEMACalculator.getValue();
		double slowEMA = slowEMACalculator.getValue();
		double main = fastEMA - slowEMA;
		
		signalCalculator.onBarUpdate(main, getCurrentBar());
   
		Main.set(0, main);
		
		double signal = signalCalculator.getValue();
		
		Signal.set(0, signal);
	}
}
