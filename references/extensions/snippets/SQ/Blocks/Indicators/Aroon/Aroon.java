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
package SQ.Blocks.Indicators.Aroon;

import SQ.Calculators.HighestCalculator;
import SQ.Calculators.LowestCalculator;
import SQ.Internal.IndicatorBlock;

import com.strategyquant.datalib.*;
import com.strategyquant.tradinglib.*;

@BuildingBlock(name="(ARO) Aroon", display="Aroon(@Chart@#Period#).#Line#[#Shift#]")
@Indicator(oscillator=true, middleValue=50, min=0, max=100, step=5)
@ParameterSet(set="Period=14")
@ParameterSet(set="Period=20")
@ParameterSet(set="Period=30")
@ParameterSet(set="Period=40")
@ParameterSet(set="Period=50")
public class Aroon extends IndicatorBlock {
	
	@Parameter(category="Default", name="Input", defaultValue="0")
	public ChartData Chart;
	
	@Parameter(category="Default", name="Period", minValue=0, maxValue=1000, defaultValue="14", step=1)
	public int Period;
	
	@Output(name="Up", color=Colors.Green)
	public DataSeries Up;
	
	@Output(name="Down", color=Colors.Red)
	public DataSeries Down;

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
		
		int HH = highestCalculator.getHighestIndex(); 	   
  	   	int LL = lowestCalculator.getLowestIndex();
      
  	    Up.set(0, 100-(100.0/Period)*(HH));            	//Adjusted Aroon Up
  	    Down.set(0, 100-(100.0/Period)*(LL));           //Adjusted Aroon Down
	}
	
}
