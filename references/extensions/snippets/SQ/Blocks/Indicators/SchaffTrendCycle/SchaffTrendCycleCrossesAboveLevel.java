/*
 * Copyright (c) 2017-2021, StrategyQuant & clonex - All rights reserved.
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
package SQ.Blocks.Indicators.SchaffTrendCycle;

import SQ.Internal.ConditionBlock;

import com.strategyquant.lib.*;
import com.strategyquant.datalib.*;
import com.strategyquant.tradinglib.*;

@BuildingBlock(name="(STCCAL) Schaff TrendCycle crosses above Level", display="Schaff Trend Cycle(@Chart@#StochPeriod#,#FastPeriod#,#SlowPeriod#)[#Shift#] crosses above #Level#", returnType = ReturnTypes.Boolean)
@Help("Is triggered if Schaff Trend Cycle crosses above Level")
@OppositeBlock(value="SchaffTrendCycleCrossesBelowLevel",oscillator=true,middleValue=50,field="Level")
@ParameterSet(set="Level=1")
@ParameterSet(set="Level=5")
@ParameterSet(set="Level=10")
@ParameterSet(set="Level=20")
@ParameterSet(set="Level=40")
@ParameterSet(set="Level=60")
@ParameterSet(set="Level=80")
@ParameterSet(set="Level=95")
@ParameterSet(set="Level=99")
public class SchaffTrendCycleCrossesAboveLevel extends ConditionBlock {

	@Parameter
	public ChartData Input;

	@Parameter(defaultValue="10", isPeriod=true, minValue=2, maxValue=480, step=1)
	public int StochPeriod;

	@Parameter(defaultValue="20", isPeriod=true, minValue=2, maxValue=480, step=1)
	public int FastPeriod;

	@Parameter(defaultValue="50", isPeriod=true, minValue=2, maxValue=480, step=1)
	public int SlowPeriod;

	@Parameter(defaultValue="80", isPeriod=false, minValue=0.1, maxValue=99.9, step=0.1)
	public double Level;

	@Parameter
	public int Shift;

	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------

	@Override
	public boolean OnBlockEvaluate() throws TradingException {

		SchaffTrendCycle indicator = Strategy.Indicators.SchaffTrendCycle(Input,StochPeriod, FastPeriod,SlowPeriod);

		double indiValue = indicator.Value.getRounded(Shift);
		double prevIndiValue = indicator.Value.getRounded(Shift+1);


		return (indiValue > Level && prevIndiValue<Level);
	}
}