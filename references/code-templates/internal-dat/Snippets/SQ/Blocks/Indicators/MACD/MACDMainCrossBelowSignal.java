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
package SQ.Blocks.Indicators.MACD;

import SQ.Internal.ConditionBlock;

import com.strategyquant.datalib.DataSeries;
import com.strategyquant.datalib.TradingException;
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

@BuildingBlock(name="MACD Main line crosses below Signal", display="MACD(@Chart@#Fast#, #Slow#, #Smooth#).Main[#Shift#] crosses below MACD.Signal", returnType = ReturnTypes.Boolean)
@OppositeBlock("MACDMainCrossAboveSignal")
@SortOrder(600)
@ParameterSet(set="Fast=12,Slow=26,Smooth=9,ComputedFrom=0")
@ParameterSet(set="Fast=24,Slow=52,Smooth=9,ComputedFrom=0")
@ParameterSet(set="Fast=8,Slow=17,Smooth=9,ComputedFrom=0")
@ParameterSet(set="Fast=3,Slow=10,Smooth=16,ComputedFrom=0")
public class MACDMainCrossBelowSignal extends ConditionBlock {
	
	@Parameter
	public DataSeries Input;
	
	@Parameter(defaultValue = "12", isPeriod=true)
	public int Fast;
	
	@Parameter(defaultValue = "26", isPeriod=true)
	public int Slow;

	@Parameter(defaultValue = "9", isPeriod=true)
	public int Smooth;

	@Parameter
	public int Shift;
	
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	
	@Override
	public boolean OnBlockEvaluate() throws TradingException {
		MACD indicator = Strategy.Indicators.MACD(Input, Fast, Slow, Smooth);

		double main1 = indicator.Main.getRounded(Shift + 1);
		double main2 = indicator.Main.getRounded(Shift);

		double signal1 = indicator.Signal.getRounded(Shift + 1);
		double signal2 = indicator.Signal.getRounded(Shift);

		return (main1 > signal1) && (main2 < signal2);
	}

}
