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
package SQ.Blocks.Indicators.AwesomeOscillator;

import SQ.Calculators.AverageCalculator;
import SQ.Internal.IndicatorBlock;

import com.strategyquant.datalib.*;
import com.strategyquant.tradinglib.*;

@BuildingBlock(name="(AWO) Awesome Oscillator", display="AwesomeOscillator(@Chart@)[#Shift#]", returnType = ReturnTypes.Number)
@Indicator(oscillator=true, middleValue=0, min=-5, max=5, step=0.01)
public class AwesomeOscillator extends IndicatorBlock {

	@Parameter
	public ChartData Input;
	
	@Output(name = "AWO", color = Colors.Red)
	public DataSeries Value;
	
	private static final int PERIOD_FAST = 5;
	private static final int PERIOD_SLOW = 34;

	private AverageCalculator fastAverageCalculator;
	private AverageCalculator slowAverageCalculator;
	
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------

	@Override
	protected void OnInit() throws TradingException {
		fastAverageCalculator = new AverageCalculator(AverageCalculator.SMA, PERIOD_FAST);
		slowAverageCalculator = new AverageCalculator(AverageCalculator.SMA, PERIOD_SLOW);
	}
	
	//------------------------------------------------------------------------
	
	@Override
	protected void OnBarUpdate() throws TradingException {
		if(getCurrentBar() == 0){
			return;
		}
		
		fastAverageCalculator.onBarUpdate(Input.Median.get(0), getCurrentBar());
		slowAverageCalculator.onBarUpdate(Input.Median.get(0), getCurrentBar());
		
		double fastMA = fastAverageCalculator.getValue();
		double slowMA = slowAverageCalculator.getValue();
		
		Value.set(0, fastMA - slowMA);
		/*
		if(Value.get(0) > Value.get(1)){
			Up.set(0, Value.get(0));
			Down.set(0, 0);
		}
		else {
			Up.set(0, 0);
			Down.set(0, Value.get(0));
		}
		*/
	}
}
