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
package SQ.Blocks.Comparisons;

import com.strategyquant.lib.*;
import com.strategyquant.datalib.*;
import com.strategyquant.tradinglib.*;

@BuildingBlock(name="Is rising", display="#Indicator# is rising", returnType = ReturnTypes.Boolean)
@OppositeBlock("IsFalling")
@SortOrder(900)
public class IsRising extends IsOneComparisonBlockAbstract {

	@Parameter(name="Bars rising", defaultValue="2", minValue=2, builderMaxValue=50, maxValue=100, category="Properties")
	@Help("Number of bars the value has to be rising")
	public int Bars;

	@Parameter(name="Allow same values", defaultValue="false", category="Properties")
	@Help("If set to true, then indicator doesn't have to be rising all the time, it can have some values that are equal (but it cannot be falling)")
	public boolean NotStrict;
	
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	
	@Override
	public boolean OnEvaluateComparison() throws TradingException {
		boolean atLeastOnce = false;

		double previousValue = SQUtils.round(Indicator.evaluateBlock(Bars+Shift-1), 6);
			
		for(int i=1; i<Bars; i++) {
			double currentValue = SQUtils.round(Indicator.evaluateBlock(Bars+Shift-1-i), 6);
			
			if(currentValue < previousValue) {
				// indicator was falling
				return(false);
			}
			if(currentValue == previousValue && NotStrict == false) {
				// indicator was the same, not allowed
				return(false);
			}
			if(currentValue > previousValue) {
				atLeastOnce = true;
			}

			previousValue = currentValue;
		}

		return(atLeastOnce); 
	}

}
