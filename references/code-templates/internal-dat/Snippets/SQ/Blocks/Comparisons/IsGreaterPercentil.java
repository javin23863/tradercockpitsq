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

@BuildingBlock(name="(> % Rank) Is Greater or Equal Percent Rank", display="#Indicator# is greater or equal than #Percentile# % of the values over #Bars# bars in the past" , returnType = ReturnTypes.Boolean)
@OppositeBlock("IsLowerPercentil")
@SortOrder(900)
@ForEngine("*,-SP,-SA")
public class IsGreaterPercentil extends IsOneComparisonBlockAbstractPercentil {
	
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	
	@Override
	public boolean OnEvaluateComparison() throws TradingException {
		int count = 1;
		double percrank = 0;
		for(int i=0; i<Bars; i++) {
			//int index = Shift+i;

			double currVal = SQUtils.round(Indicator.evaluateBlock(Shift), 5);
			double prevVal = SQUtils.round(Indicator.evaluateBlock(Shift+i), 5); 
			
			if (currVal > prevVal){
				
				count++;
				}
			}
			percrank = (double)count/Bars*100;

		double RB = SQUtils.round(Percentile, 5);
		
		return percrank > RB;
	}

}