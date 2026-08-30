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
package SQ.Blocks.Indicators.UlcerIndex;

import SQ.Internal.ConditionBlock;


import com.strategyquant.lib.*;
import com.strategyquant.datalib.*;
import com.strategyquant.tradinglib.*;

@BuildingBlock(name="(UIUR) Ulcer Index UP is rising ", display=" Ulcer Index UP (@Chart@#Mode#,#Period#)[#Shift#] is rising", returnType = ReturnTypes.Boolean)
@Help("Is triggered if UlcerIndex UP is rising")
@OppositeBlock("UlcerIndexDownRising")
@ParameterSet(set="Period=12")
@ParameterSet(set="Period=24")
@ParameterSet(set="Period=48")
@ParameterSet(set="Period=96")
@ParameterSet(set="Period=120")
@ParameterSet(set="Period=10")
@ParameterSet(set="Period=20")
@ParameterSet(set="Period=40")
@ParameterSet(set="Period=100")
@ParameterSet(set="Period=200")
public class UlcerIndexUPRising extends ConditionBlock {
	
	@Parameter(defaultChartIndex=0)
	public ChartData Input;

	@Parameter(defaultValue="24", isPeriod=true, minValue=2, maxValue=1000, step=1)
	public int Period;

	@Parameter
	public int Shift;
	
	@Parameter(defaultValue="1", minValue=1, maxValue=1, step=1)
	public int Mode = 1;
	
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	
	@Override
	public boolean OnBlockEvaluate() throws TradingException {
		UlcerIndex indicator = Strategy.Indicators.UlcerIndex(Input,Mode,Period);

		double currValue = indicator.Value.getRounded(Shift);
		double prevValue = indicator.Value.getRounded(Shift+1);

		return(currValue>prevValue);

	}
}