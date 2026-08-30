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
package SQ.Blocks.Indicators.LinReg;

import SQ.Calculators.AverageCalculator;
import SQ.Calculators.SumCalculator;
import SQ.Internal.IndicatorBlock;

import com.strategyquant.datalib.DataSeries;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.Colors;
import com.strategyquant.tradinglib.Output;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.ParameterSet;
import com.strategyquant.tradinglib.ReturnTypes;

@BuildingBlock(name="(LinReg) Linear Regression", display="LinReg(@Chart@#Period#)[#Shift#]", returnType = ReturnTypes.Price)
@ParameterSet(set="Period=13,ComputedFrom=0")
@ParameterSet(set="Period=14,ComputedFrom=0")
@ParameterSet(set="Period=20,ComputedFrom=0")
@ParameterSet(set="Period=30,ComputedFrom=0")
@ParameterSet(set="Period=40,ComputedFrom=0")
@ParameterSet(set="Period=50,ComputedFrom=0")
public class LinearRegression extends IndicatorBlock {

	@Parameter
	public DataSeries Input;

	@Parameter(defaultValue="14")
	public int Period;
	
	@Output(name = "LinReg", color = Colors.Red)
	public DataSeries Value;
	
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	
	@Override
	protected void OnBarUpdate() throws TradingException {
		double sumX	= (double) Period * (Period - 1) * 0.5;
		double divisor = sumX * sumX - (double) Period * Period * (Period - 1) * (2 * Period - 1) / 6;
		double sumXY = 0;
		double sum = 0;

		for(int count = 0; count < Period && CurrentBar - count >= 0; count++){
			double inputValue = Input.get(count);
			sumXY += count * inputValue;
			sum += inputValue;
		}
		
        double slope = ((double)Period * sumXY - sumX * sum) / divisor;
        double intercept = (sum - slope * sumX) / Period;
		
		Value.set(0, intercept + slope * (Period - 1));
	}
	
}
