/**
 * 
 */
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
package SQ.Blocks.Indicators.ATR;

import SQ.Internal.IndicatorBlock;

import com.strategyquant.lib.*;
import com.strategyquant.datalib.*;
import com.strategyquant.tradinglib.*;

@BuildingBlock(name="(ATR) Average True Range", display="ATR(@Chart@#Period#)[#Shift#]", returnType=ReturnTypes.PriceRange)
@Indicator(min=0, max=5000, step=0.001)
@ParameterSet(set="Period=14")
@ParameterSet(set="Period=20")
@ParameterSet(set="Period=30")
@ParameterSet(set="Period=40")
@ParameterSet(set="Period=50")
public class ATR extends IndicatorBlock {
	
	@Parameter(defaultChartIndex=0)
	public ChartData Chart;
	
	@Parameter(category="Default", name="Period", minValue=2, maxValue=10000, defaultValue="14", step=1)
	public int Period;
	
	@Output(name="ATR", color=Colors.Green)
	public DataSeries Value;

	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------

	@Override
	protected void OnBarUpdate() throws TradingException {
		double curHigh = Chart.High.get(0);
		double curLow = Chart.Low.get(0);
		double trueRange = curHigh - curLow;
		
		if (getCurrentBar() == 0){
			Value.set(0, trueRange);
		}
		else {
			double prevClose = Chart.Close.get(1);
			trueRange = Math.max(Math.abs(curLow - prevClose), Math.max(trueRange, Math.abs(curHigh - prevClose)));
			
			Value.set(0, ((Math.min(CurrentBar + 1, Period) - 1 ) * Value.get(1) + trueRange) / Math.min(CurrentBar + 1, Period));
		}
	}
}
