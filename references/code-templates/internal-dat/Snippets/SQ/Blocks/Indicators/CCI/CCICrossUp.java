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
package SQ.Blocks.Indicators.CCI;

import SQ.Internal.ConditionBlock;

import com.strategyquant.datalib.DataSeries;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.Colors;
import com.strategyquant.tradinglib.Editor;
import com.strategyquant.tradinglib.Editors;
import com.strategyquant.tradinglib.Help;
import com.strategyquant.tradinglib.OppositeBlock;
import com.strategyquant.tradinglib.Output;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.ParameterSet;
import com.strategyquant.tradinglib.ReturnTypes;
import com.strategyquant.tradinglib.SortOrder;

@BuildingBlock(name="CCI crosses above Level", display="CCI(@Chart@#Period#)[#Shift#] crosses above #Level#", returnType = ReturnTypes.Boolean)
@SortOrder(500)
@OppositeBlock(value="CCICrossDown", oscillator=true, middleValue=0, field="Level")
@ParameterSet(set="Period=14")
@ParameterSet(set="Period=20")
@ParameterSet(set="Period=30")
@ParameterSet(set="Period=40")
@ParameterSet(set="Period=50")
@ParameterSet(set="Period=14,ComputedFrom=0")
@ParameterSet(set="Period=20,ComputedFrom=0")
@ParameterSet(set="Period=30,ComputedFrom=0")
@ParameterSet(set="Period=40,ComputedFrom=0")
@ParameterSet(set="Period=50,ComputedFrom=0")
@ParameterSet(weight=3,set="Period=14,ComputedFrom=0,Level=0")
@ParameterSet(weight=3,set="Period=20,ComputedFrom=0,Level=0")
@ParameterSet(weight=3,set="Period=30,ComputedFrom=0,Level=0")
@ParameterSet(weight=3,set="Period=40,ComputedFrom=0,Level=0")
@ParameterSet(weight=3,set="Period=50,ComputedFrom=0,Level=0")
public class CCICrossUp extends ConditionBlock {
	
	@Parameter
	public DataSeries Input;
	
	@Parameter(defaultValue="14", minValue=2, maxValue=10000, step=1)
	public int Period;

	@Parameter(defaultValue="0", minValue=-5000, maxValue=5000, builderMinValue=-300, builderMaxValue=300, step=1)
	public double Level;

	@Parameter
	public int Shift;
	
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	
	@Override
	public boolean OnBlockEvaluate() throws TradingException {
		CCI indicator = Strategy.Indicators.CCI(Input, Period);
		double value1 = indicator.Value.getRounded(Shift + 1, 4);
		double value2 = indicator.Value.getRounded(Shift, 4);
		
		return (value1 < Level) && (value2 > Level);
	}

}
