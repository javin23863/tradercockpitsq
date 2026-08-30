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
package SQ.Blocks.Functions;

import SQ.Internal.ValueBlock;

import com.strategyquant.lib.*;
import com.strategyquant.datalib.*;
import com.strategyquant.tradinglib.*;
import java.util.Arrays;

@BuildingBlock(name="(IL) Indicator Lowest Value", display="Indicator Lowest(#NthValue#, #Period#, #Indicator#)", returnType = ReturnTypes.Number)
@Help("Lowest value of indicator in given period")
@SortOrder(1000)
@IgnoreInBuilder
public class IndicatorLowest extends ValueBlock {

	private static final int MAX_VALUES = 1000;

	@Parameter
	public IBlock Indicator;
	
	@Parameter(defaultValue="5", minValue=2, maxValue=MAX_VALUES-1)
	@Help("Period (bars back) in which indicator lowest value is checked")
	public int Period;

	@Parameter(name="Nth lowest value", defaultValue="0", minValue=0)
	@Help("Nth lowest value in given period. ) means lowest value, 1 means second lowest value, etc.")
	public int NthValue;
	
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	
	@Override
	public double OnBlockEvaluate(int relativeShift) throws TradingException {
		if(NthValue < 0 || NthValue >= Period) {
			return(-1);
		}

		double indicatorValues[] = new double[MAX_VALUES];

		for(int i=0; i<MAX_VALUES; i++) {
			if(i<Period) {
				indicatorValues[i] = Indicator.evaluateBlock(i+relativeShift);	
			} else {
				indicatorValues[i] = 2147483647;
			}
		}

		// sort in ascending order
		Arrays.sort(indicatorValues);

		return(indicatorValues[NthValue]);
	}

}
