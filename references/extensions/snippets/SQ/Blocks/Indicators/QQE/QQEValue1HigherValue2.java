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
package SQ.Blocks.Indicators.QQE;

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

@BuildingBlock(name="QQE Value1 line is higher than Value2", display="QQE(@Chart@#RSIPeriod#).Value1[#Shift#] > QQE.Value2", returnType = ReturnTypes.Boolean)
@SortOrder(300)
@OppositeBlock("QQEValue1LowerValue2")
@ParameterSet(set="RSIPeriod=14,sF=5,wF=4.236")
public class QQEValue1HigherValue2 extends ConditionBlock {
	
	@Parameter(defaultChartIndex=0)
	public ChartData Chart;
	
	@Parameter(category="Default", name="RSIPeriod", minValue=2, maxValue=10000, defaultValue="14", step=1, isPeriod=true)
	public int RSIPeriod;
	
	@Parameter(category="Default", name="sF", defaultValue="5", minValue=2, maxValue=650, step=1, builderMinValue=1, builderMaxValue=650, builderStep=1, isPeriod=true)
	public int sF;
	
	@Parameter(category="Default", name="wF", defaultValue="4.236", minValue=0.1, maxValue=100, builderMinValue=0.01, builderMaxValue=100, builderStep=0.025)
	public double wF; 

	@Parameter
	public int Shift;
	
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	
	@Override
	public boolean OnBlockEvaluate() throws TradingException {
		QQE indicator = Strategy.Indicators.QQE(Chart, RSIPeriod, sF, wF);

		double value1 = indicator.Value1.getRounded(Shift);
		double value2 = indicator.Value2.getRounded(Shift);
		
		return value1 > value2;
	}

}
