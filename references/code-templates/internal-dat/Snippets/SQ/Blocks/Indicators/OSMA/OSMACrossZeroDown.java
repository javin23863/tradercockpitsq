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
package SQ.Blocks.Indicators.OSMA;

import SQ.Internal.ConditionBlock;

import com.strategyquant.datalib.DataSeries;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.OppositeBlock;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.ParameterSet;
import com.strategyquant.tradinglib.ReturnTypes;
import com.strategyquant.tradinglib.SortOrder;

@BuildingBlock(name="OSMA crosses below 0", display="OSMA(@Chart@#FastEMA#, #SlowEMA#, #SignalPeriod#)[#Shift#] crosses below 0", returnType = ReturnTypes.Boolean)
@SortOrder(600)
@OppositeBlock("OSMACrossZeroUp")
@ParameterSet(set="FastEMA=12,SlowEMA=26,SignalPeriod=9,ComputedFrom=0")
@ParameterSet(set="FastEMA=24,SlowEMA=52,SignalPeriod=9,ComputedFrom=0")
@ParameterSet(set="FastEMA=8,SlowEMA=17,SignalPeriod=9,ComputedFrom=0")
@ParameterSet(set="FastEMA=3,SlowEMA=10,SignalPeriod=16,ComputedFrom=0")
public class OSMACrossZeroDown extends ConditionBlock {
	
	@Parameter
	public DataSeries Input;
	
	@Parameter(name="Fast EMA", defaultValue="12", isPeriod=true)
	public int FastEMA;
	
	@Parameter(name="Slow EMA", defaultValue="26", isPeriod=true)
	public int SlowEMA;

	@Parameter(defaultValue="9", isPeriod=true)
	public int SignalPeriod;

	@Parameter
	public int Shift;
	
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	
	@Override
	public boolean OnBlockEvaluate() throws TradingException {
		OSMA indicator = Strategy.Indicators.OSMA(Input, FastEMA, SlowEMA, SignalPeriod);
		double value1 = indicator.Value.getRounded(Shift + 1);
		double value2 = indicator.Value.getRounded(Shift);
		
		return (value1 > 0) && (value2 < 0);
	}

}
