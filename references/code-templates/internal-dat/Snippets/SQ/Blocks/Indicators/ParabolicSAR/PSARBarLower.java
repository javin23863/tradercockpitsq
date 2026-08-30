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
package SQ.Blocks.Indicators.ParabolicSAR;

import com.strategyquant.datalib.TradingException;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.ChartData;
import com.strategyquant.tradinglib.OppositeBlock;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.ParameterSet;
import com.strategyquant.tradinglib.ReturnTypes;
import com.strategyquant.tradinglib.SortOrder;

import SQ.Internal.ConditionBlock;

@BuildingBlock(name="Price is below ParabolicSAR(@Chart@#Step#, #Maximum#)[#Shift#]", returnType = ReturnTypes.Boolean)
@SortOrder(400)
@OppositeBlock("PSARBarHigher")
@ParameterSet(set="Step=0.02,Maximum=0.2")
@ParameterSet(set="Step=0.02,Maximum=0.1")
@ParameterSet(set="Step=0.01,Maximum=0.2")
public class PSARBarLower extends ConditionBlock {
	
	@Parameter
	public ChartData Input;
	
	@Parameter(defaultValue="0.02", minValue=0.01, maxValue=0.6, step=0.01, builderMinValue=0.01, builderMaxValue=0.4, builderStep=0.001)
	public double Step;
	
	@Parameter(defaultValue="0.2", minValue=0.01, maxValue=1, step=0.1, builderMinValue=0.01, builderMaxValue=1, builderStep=0.01)
	public double Maximum;
 
	@Parameter
	public int Shift;
	
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	
	@Override
	public boolean OnBlockEvaluate() throws TradingException {
		return (Input.Bid()) < Strategy.Indicators.ParabolicSAR(Input, Step, Maximum).Value.getRounded(Shift);
	}

}
