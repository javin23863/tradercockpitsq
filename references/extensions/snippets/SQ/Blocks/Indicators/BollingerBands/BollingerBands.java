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
import SQ.Calculators.StdDevCalculator;
import SQ.Internal.IndicatorBlock;

import com.strategyquant.datalib.*;
import com.strategyquant.tradinglib.*;

@BuildingBlock(name="(BB) Bollinger Bands", display="BollingerBands(@Chart@#Period#, #Deviation#).#Line#[#Shift#]", returnType = ReturnTypes.Price)
@ParameterSet(set="Period=20,Deviation=2,ComputedFrom=0")
@ParameterSet(set="Period=10,Deviation=1.9,ComputedFrom=0")
@ParameterSet(set="Period=50,Deviation=2.1,ComputedFrom=0")
public class BollingerBands extends IndicatorBlock {
	
	@Parameter
	public DataSeries Input;

	@Parameter(minValue=2, maxValue=10000, defaultValue="20", step=1)
	public int Period;
	
	@Parameter(defaultValue="2", minValue=0.01, maxValue=10, step=0.01, builderMinValue=0.1, builderMaxValue=7, builderStep=0.1)
	public double Deviation;
	
	@Output(name="Upper", color=Colors.Green)
	public DataSeries Upper;

	@Output(name="Lower", color=Colors.Red)
	public DataSeries Lower;
       
	private AverageCalculator averageCalculator;
	private StdDevCalculator stdDevCalculator;
	
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------

	@Override
	protected void OnInit() throws TradingException {
		averageCalculator = new AverageCalculator(AverageCalculator.SMA, Period);
		stdDevCalculator = new StdDevCalculator(Period);
	}
	
	//------------------------------------------------------------------------
	
	@Override
	protected void OnBarUpdate() throws TradingException {
		averageCalculator.onBarUpdate(Input.get(0), getCurrentBar());
		stdDevCalculator.onBarUpdate(Input.get(0), getCurrentBar());
		
		double smaValue = averageCalculator.getValue();
	    double stdDevValue = stdDevCalculator.getValue();
	    
        Upper.set(0, smaValue + Deviation * stdDevValue);
        Lower.set(0, smaValue - Deviation * stdDevValue);
	}

}
