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
package SQ.Blocks.Indicators.WaveTrend;

import SQ.Internal.ConditionBlock;

import com.strategyquant.datalib.DataSeries;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.ChartData;
import com.strategyquant.tradinglib.Colors;
import com.strategyquant.tradinglib.Editor;
import com.strategyquant.tradinglib.Editors;
import com.strategyquant.tradinglib.ForEngine;
import com.strategyquant.tradinglib.Help;
import com.strategyquant.tradinglib.OppositeBlock;
import com.strategyquant.tradinglib.Output;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.ParameterSet;
import com.strategyquant.tradinglib.ReturnTypes;
import com.strategyquant.tradinglib.SortOrder;

@BuildingBlock(name="WaveTrend Main line crosses above 0", display="WaveTrend(@Chart@#ChannelLength#,#AverageLength#).Main[#Shift#] crosses above 0", returnType = ReturnTypes.Boolean)
@SortOrder(700)
@ForEngine("MT4,MT5,TS,MC")
@OppositeBlock("WaveTrendMainCrossBelowZero")
@ParameterSet(set="ChannelLength=10,AverageLength=21")
@ParameterSet(set="ChannelLength=9,AverageLength=12")
@ParameterSet(set="ChannelLength=14,AverageLength=21")
@ParameterSet(set="ChannelLength=10,AverageLength=21,ComputedFrom=0")
@ParameterSet(set="ChannelLength=9,AverageLength=12,ComputedFrom=0")
@ParameterSet(set="ChannelLength=14,AverageLength=21,ComputedFrom=0")
public class WaveTrendMainCrossAboveZero extends ConditionBlock {
	
	@Parameter(defaultChartIndex=0)
	public ChartData Chart;
	
	@Parameter(category="Default", name="ChannelLength", minValue=2, maxValue=200, defaultValue="10", step=1, isPeriod=true)
	public int ChannelLength;
	
	@Parameter(category="Default", name="AverageLength", minValue=2, maxValue=200, defaultValue="21", step=1, isPeriod=true)
	public int AverageLength;

	@Parameter
	public int Shift;
	
	
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	
	@Override
	public boolean OnBlockEvaluate() throws TradingException {
		WaveTrend indicator = Strategy.Indicators.WaveTrend(Chart, ChannelLength, AverageLength);
		double value1 = indicator.WT1.getRounded(Shift + 1);
		double value2 = indicator.WT1.getRounded(Shift);

		return (value1 < 0) && (value2 > 0);
	}

}
