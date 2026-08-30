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
package SQ.Blocks.Indicators.BollingerBands;

import SQ.Calculators.AverageCalculator;
import SQ.Internal.IndicatorBlock;

import com.strategyquant.datalib.*;
import com.strategyquant.tradinglib.*;

@BuildingBlock(name="(BBWR) BB Width Ratio", display="BB WR(@Chart@#Period#, #Deviation#)[#Shift#]", returnType=ReturnTypes.PriceRange)
@Indicator(min=0, max=5000, step=0.001)
public class BBWidthRatio extends IndicatorBlock {
	
	@Parameter
	public ChartData Input;

	@Parameter(minValue=2, maxValue=10000, defaultValue="20", step=1)
	public int Period;
	
	@Parameter(defaultValue="2", minValue=0.01, maxValue=10, step=0.01, builderMinValue=0.1, builderMaxValue=7, builderStep=0.1)
	public double Deviation;
	
	@Parameter(defaultValue="0")
	@Editor(type=Editors.Selection, values="Close=0,Open=1,High=2,Low=3,Median=4,Typical=5,Weighted=6")
	public int ComputedFrom;
	
	@Output(name="BBWidthRatio", color=Colors.Green)
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
		averageCalculator.onBarUpdate(Input.getSeries(ComputedFrom).get(0), getCurrentBar());
		
		int from = CurrentBar > Period - 1 ? Period - 1 : CurrentBar;
		double sum = 0;
		
		for(int a=from; a>=0; a--) {
			double newRes = Input.getSeries(ComputedFrom).get(a) - averageCalculator.getValue();
			sum += newRes * newRes;
		}

		double sko = Math.sqrt(sum / Period);
		
		if(averageCalculator.getValue() > 0) {
			Value.set(0, 2 * Deviation * sko / averageCalculator.getValue());
		}
		else {
			Value.set(0, 0);
		}
		
	}


}
