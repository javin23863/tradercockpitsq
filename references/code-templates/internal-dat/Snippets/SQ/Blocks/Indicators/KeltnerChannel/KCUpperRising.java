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

import SQ.Internal.ConditionBlock;

import com.strategyquant.datalib.TradingException;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.CategoryOrder;
import com.strategyquant.tradinglib.ChartData;
import com.strategyquant.tradinglib.Help;
import com.strategyquant.tradinglib.OppositeBlock;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.ParameterSet;
import com.strategyquant.tradinglib.ReturnTypes;
import com.strategyquant.tradinglib.SortOrder;

@BuildingBlock(name="(KC) Upper band is rising", display="Keltner Channel(@Chart@#Period#, #Deviation#).Upper[#Shift#] is rising", returnType = ReturnTypes.Boolean)
@Help("Is triggered if KC Upper band is rising")
@CategoryOrder(1100)
@OppositeBlock("KCLowerFalling")
@ParameterSet(set="Period=20,Deviation=1.5,ComputedFrom=0")
@ParameterSet(set="Period=20,Deviation=2,ComputedFrom=0")
@ParameterSet(set="Period=20,Deviation=2.25,ComputedFrom=0")
@ParameterSet(set="Period=20,Deviation=2.5,ComputedFrom=0")
public class KCUpperRising extends ConditionBlock {
	
	@Parameter
	public ChartData Input;
	
	@Parameter(name="Period", defaultValue="20", minValue=2, maxValue=10000, step=1)
	public int Period;

	@Parameter(defaultValue="1.5", minValue=0.01, maxValue=10, step=0.01, builderMinValue=0.1, builderMaxValue=7, builderStep=0.1)
	public double Deviation;

	@Parameter
	public int Shift;
	
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	
	@Override
	public boolean OnBlockEvaluate() throws TradingException {
		double value1 = Strategy.Indicators.KeltnerChannel(Input, Period, Deviation).Upper.getRounded(Shift + 1);
		double value2 = Strategy.Indicators.KeltnerChannel(Input, Period, Deviation).Upper.getRounded(Shift);
		return (value1 < value2);
	}

}
