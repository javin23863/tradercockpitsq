/*
 * Copyright (c) 2021, StrategyQuant & clonex - All rights reserved.
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
package SQ.Blocks.Indicators.SRPercentRank;

import SQ.Internal.ConditionBlock;

import com.strategyquant.lib.*;
import com.strategyquant.datalib.*;
import com.strategyquant.tradinglib.*;

@BuildingBlock(name="(PRBL) SR Percent Rank is below Level", display=" SR Percent Rank(@Chart@#Mode#,#Length#,#ATRPeriod#)[#Shift#] is below #Level#", returnType = ReturnTypes.Boolean)
@Help("Is triggered if SRPercentRank is below Level")
@OppositeBlock("SRPercRankBelowLevel") /// this is correct 
@ParameterSet(set="Mode=1,Length=12,ATRPeriod=12,Level = 50")
@ParameterSet(set="Mode=1,Length=24,ATRPeriod=12,Level = 40")
@ParameterSet(set="Mode=1,Length=48,ATRPeriod=12,Level = 30")
@ParameterSet(set="Mode=1,Length=120,ATRPeriod=12,Level = 20")
@ParameterSet(set="Mode=1,Length=240,ATRPeriod=12,Level = 10")
@ParameterSet(set="Mode=1,Length=480,ATRPeriod=12")
@ParameterSet(set="Mode=2,Length=12,ATRPeriod=12,Level = 50")
@ParameterSet(set="Mode=2,Length=24,ATRPeriod=12,Level = 40")
@ParameterSet(set="Mode=2,Length=48,ATRPeriod=12,Level = 30")
@ParameterSet(set="Mode=2,Length=120,ATRPeriod=12,Level = 20")
@ParameterSet(set="Mode=2,Length=240,ATRPeriod=12,Level = 10")
@ParameterSet(set="Mode=2,Length=480,ATRPeriod=12")

public class SRPercRankBelowLevel extends ConditionBlock {
	
	@Parameter(defaultChartIndex=0)
	public ChartData Chart;
	
	@Parameter(defaultValue="2")
	@Editor(type=Editors.Selection, values="Basic Mode=1,ATR Mode=2")
	public int Mode;

	@Parameter(defaultValue="120", isPeriod=false, minValue=4, maxValue=480, step=4)
	public int Length;

	@Parameter(defaultValue="12", isPeriod=true, minValue=2, maxValue=240, step=2)
	public int ATRPeriod;

	@Parameter(defaultValue="20", isPeriod=true, minValue=0.5, maxValue=100, step=0.5)
	public double Level;

	@Parameter
	public int Shift;
	
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	
	@Override
	public boolean OnBlockEvaluate() throws TradingException {
		SRPercentRank indicator = Strategy.Indicators.SRPercentRank(Chart,Mode,Length,ATRPeriod);
		double currValue = indicator.Value.getRounded(Shift);
	
		return (currValue< Level);

	}
}