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
package SQ.Blocks.Indicators.KeltnerChannel;

import SQ.Calculators.AverageCalculator;
import SQ.Internal.IndicatorBlock;

import com.strategyquant.datalib.DataSeries;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.tradinglib.Buffer;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.ChartData;
import com.strategyquant.tradinglib.Output;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.ParameterSet;
import com.strategyquant.tradinglib.ReturnTypes;

@BuildingBlock(name="(KC) Keltner Channel", display="Keltner Channel(@Chart@#Period#, #Deviation#).#Line#[#Shift#]", returnType = ReturnTypes.Price)
@ParameterSet(set="Period=20,Deviation=1.5,ComputedFrom=0")
@ParameterSet(set="Period=20,Deviation=2,ComputedFrom=0")
@ParameterSet(set="Period=20,Deviation=2.25,ComputedFrom=0")
@ParameterSet(set="Period=20,Deviation=2.5,ComputedFrom=0")
public class KeltnerChannel extends IndicatorBlock {

	@Parameter
	public ChartData Input;

	@Parameter(name="Period", defaultValue="20", minValue=2, maxValue=10000, step=1)
	public int Period;

	@Parameter(defaultValue="1.5", minValue=0.01, maxValue=10, step=0.01, builderMinValue=0.1, builderMaxValue=7, builderStep=0.1)
	public double Deviation;
	
	@Output
	public DataSeries Upper;
	
	@Output
	public DataSeries Lower;
	
	@Buffer
	public DataSeries diff, typical;
	
	private AverageCalculator middleAverageCalculator;
	private AverageCalculator offsetAverageCalculator;

	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------

	@Override
	protected void OnInit() throws TradingException {
		middleAverageCalculator = new AverageCalculator(AverageCalculator.SMA, Period);
		offsetAverageCalculator = new AverageCalculator(AverageCalculator.SMA, Period);
	}
	
	//------------------------------------------------------------------------
	
	@Override
	protected void OnBarUpdate() throws TradingException {
		middleAverageCalculator.onBarUpdate(Input.Typical.get(0), getCurrentBar());
		offsetAverageCalculator.onBarUpdate(Input.High.get(0) - Input.Low.get(0), getCurrentBar());

		double middle = middleAverageCalculator.getValue();
		double offset = offsetAverageCalculator.getValue() * Deviation;

		Upper.set(0, middle + offset);
		Lower.set(0, middle - offset);
	}

}
