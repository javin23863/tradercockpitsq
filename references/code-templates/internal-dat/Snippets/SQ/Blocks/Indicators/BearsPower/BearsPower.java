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
package SQ.Blocks.Indicators.BearsPower;

import SQ.Calculators.AverageCalculator;
import SQ.Internal.IndicatorBlock;

import com.strategyquant.datalib.*;
import com.strategyquant.tradinglib.*;

@BuildingBlock(name="(BP) Bears Power", display="BearsPower(@Chart@#Period#, #ComputedFrom#)[#Shift#]", returnType=ReturnTypes.Number)
@Indicator(oscillator=true, middleValue=0, min=-0.5, max=0.5, step=0.01)
@OppositeBlock("BullsPower")
@ParameterSet(set="Period=13,ComputedFrom=0")
@ParameterSet(set="Period=14,ComputedFrom=0")
@ParameterSet(set="Period=15,ComputedFrom=0")
@ParameterSet(set="Period=20,ComputedFrom=0")
public class BearsPower extends IndicatorBlock {
	
	@Parameter
	public ChartData Input;
	
	@Parameter(minValue=2, maxValue=10000, defaultValue="14", step=1)
	public int Period;
	
	@Parameter(defaultValue="0")
	@Editor(type=Editors.Selection, values="Close=0,Open=1,High=2,Low=3,Median=4,Typical=5,Weighted=6")
	public int ComputedFrom;
	
	@Output(name="Value", color=Colors.Green)
	public DataSeries Value;

	private AverageCalculator averageCalculator;
	
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------

	@Override
	protected void OnInit() throws TradingException {
		averageCalculator = new AverageCalculator(AverageCalculator.EMA, Period);
	}
	
	//------------------------------------------------------------------------
	
	@Override
	protected void OnBarUpdate() throws TradingException {
		averageCalculator.onBarUpdate(Input.getSeries(ComputedFrom).get(0), getCurrentBar());
		
		Value.set(0, Input.Low.get(0) - averageCalculator.getValue());
	}
	
}
