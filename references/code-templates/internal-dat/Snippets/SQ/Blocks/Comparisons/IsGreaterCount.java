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
package SQ.Blocks.Comparisons;

import SQ.Internal.ComparisonBlock;
import com.strategyquant.lib.*;
import com.strategyquant.datalib.*;
import com.strategyquant.tradinglib.*;

@BuildingBlock(name="(> X) Is greater for X bars", display="#IndicatorLeft# > #IndicatorRight# is true #Bars# bars" , returnType = ReturnTypes.Boolean)
@OppositeBlock("IsLowerCount")
@SortOrder(100)
public class IsGreaterCount extends CountComparisonBlockAbstract {

	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	
	@Override
	public boolean OnEvaluateComparison() throws TradingException {
		boolean atLeastOnce = false;
		
		for(int i=0; i<Bars; i++) {
			int index = Shift+i;
			
			double leftIndi = SQUtils.round(IndicatorLeft.evaluateBlock(index), 5); /// precision = 5. I returns more acccurate backtest synchronisation
			double rightIndi = SQUtils.round(IndicatorRight.evaluateBlock(index), 5);

			if(leftIndi<rightIndi){
				
				return (false);
			}
			if(leftIndi == rightIndi && NotStrict == false) {

				return(false);
			}
			if(leftIndi>rightIndi) {
			
				atLeastOnce = true;
			}
		}

		return(atLeastOnce);
	}	
}
