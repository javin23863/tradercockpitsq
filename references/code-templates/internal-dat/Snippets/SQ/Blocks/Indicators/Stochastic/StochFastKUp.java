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
package SQ.Blocks.Indicators.Stochastic;

import SQ.Internal.ConditionBlock;

import com.strategyquant.datalib.DataSeries;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.ChartData;
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

@BuildingBlock(name="Stochastic.Fast %K is higher than Slow %D", display="Stoch(@Chart@#KPeriod#, #DPeriod#, #Slowing#).Fast %K[#Shift#] is higher than Stoch(#KPeriod#, #DPeriod#, #Slowing#).Slow %D", returnType = ReturnTypes.Boolean)
@SortOrder(100)
@OppositeBlock("StochFastKDown")
@ParameterSet(set="KPeriod=5,DPeriod=3,Slowing=3,MAMethod=0,PriceField=0")
@ParameterSet(set="KPeriod=14,DPeriod=3,Slowing=3,MAMethod=0,PriceField=0")
@ParameterSet(set="KPeriod=21,DPeriod=7,Slowing=7,MAMethod=0,PriceField=0")
public class StochFastKUp extends ConditionBlock {
	
	@Parameter(defaultChartIndex=0)
	public ChartData Input;
	
	@Parameter(name="%K Period", defaultValue="9", minValue=2, maxValue=10000, step=1)
	public int KPeriod;

	@Parameter(name="%D Period", defaultValue="3", minValue=2, maxValue=10000, step=1)
	public int DPeriod;

	@Parameter(defaultValue="3", minValue=2, isPeriod=true, maxValue=10000, step=1)
	public int Slowing;

	@Parameter(name="MA Method", defaultValue="0")
	@Editor(type=Editors.Selection, values="Simple=0,Exponential=1,Smoothed=2,Linear weighted=3")
	public int MAMethod;
    
	@Parameter(defaultValue="0")
	@Editor(type=Editors.Selection, values="Low/High=0,Close/Close=1")
	public int PriceField;

	@Parameter
	public int Shift;
	
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	
	@Override
	public boolean OnBlockEvaluate() throws TradingException {
		Stochastic indicator = Strategy.Indicators.Stochastic(Input, KPeriod, DPeriod, Slowing, MAMethod, PriceField);

		double value1 = indicator.FastK.getRounded(Shift);
		double value2 = indicator.SlowD.getRounded(Shift);
		
		return (value1 > value2);
	}

}
