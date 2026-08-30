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
package SQ.Blocks.Indicators.MovingAverage;

import SQ.Internal.ConditionBlock;

import com.strategyquant.datalib.DataSeries;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.Editor;
import com.strategyquant.tradinglib.Editors;
import com.strategyquant.tradinglib.Help;
import com.strategyquant.tradinglib.OppositeBlock;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.ParameterSet;
import com.strategyquant.tradinglib.ReturnTypes;

@BuildingBlock(name="Moving Average is falling", display="#Type# Moving Average(@Chart@#Period#)[#Shift#] is falling", returnType = ReturnTypes.Boolean)
@Help("Is triggered if MA is falling")
@OppositeBlock("MARising")
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
public class MAFalling extends ConditionBlock {
	
	@Parameter
	public DataSeries Input;

	@Parameter(minValue=2, maxValue=10000, defaultValue="14", step=1)
	public int Period;
	
	@Parameter(name="Method", defaultValue="0")
	@Editor(type=Editors.Selection, values="Simple=0,Exponential=1,Smoothed=2,Linear weighted=3")
	public int Type;
	
	@Parameter
	public int Shift;
	
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	
	@Override
	public boolean OnBlockEvaluate() throws TradingException {
		Type = SQUtils.fixAllowedRange(Type, 0, 3, 0);
		
		switch(Type){
		case 0:
			return (Strategy.Indicators.SMA(Input, Period).Value.getRounded(Shift) < Strategy.Indicators.SMA(Input, Period).Value.getRounded(Shift+1));
		case 1:
			return (Strategy.Indicators.EMA(Input, Period).Value.getRounded(Shift) < Strategy.Indicators.EMA(Input, Period).Value.getRounded(Shift+1));
		case 2:
			return (Strategy.Indicators.SMMA(Input, Period).Value.getRounded(Shift) < Strategy.Indicators.SMMA(Input, Period).Value.getRounded(Shift+1));
		case 3:
			return (Strategy.Indicators.LWMA(Input, Period).Value.getRounded(Shift) < Strategy.Indicators.LWMA(Input, Period).Value.getRounded(Shift+1));
		default:
			return (Strategy.Indicators.SMA(Input, Period).Value.getRounded(Shift) < Strategy.Indicators.SMA(Input, Period).Value.getRounded(Shift+1));
		}
	}

}
