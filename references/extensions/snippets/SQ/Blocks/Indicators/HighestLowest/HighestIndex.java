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
package SQ.Blocks.Indicators.HighestLowest;

import SQ.Calculators.HighestCalculator;
import SQ.Internal.IndicatorBlock;

import com.strategyquant.datalib.DataSeries;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.Help;
import com.strategyquant.tradinglib.IgnoreInBuilder;
import com.strategyquant.tradinglib.OppositeBlock;
import com.strategyquant.tradinglib.Output;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.ReturnTypes;

/**
 * @author Tomas Brynda
 *
 *	This snippet provides largest value index in selected period.
 *
 *	Note: The index numbers differ in MetaTrader and Java - Java indexes are smaller (MetatraderIndex - 1). 
 *	It is caused by different value shift logic in both platforms. However the statements:
 *	
 *	High.get((int) Indicators.HighestIndex(High, Period).Value.get(0));		-Java
 *	and
 * 	High[iHighest(NULL, 0, MODE_LOW, 14, 1)]								-MetaTrader
 * 
 * 	will output the exact same values.
 *
 */
@BuildingBlock(name="(HI) Highest index", display="HighestIndex(@Chart@#Period#)[#Shift#]", returnType = ReturnTypes.Number, mainIndicator="HighestIndex")
@Help("returns shift of the bar with highest value from the bars of given type")
@OppositeBlock("HighestIndex")
@IgnoreInBuilder
public class HighestIndex extends IndicatorBlock {

	@Parameter
	public DataSeries Input;
	
	@Parameter(minValue=0, maxValue=10000, defaultValue="14", step=1)
	public int Period;
	
	@Output
	public DataSeries Value;
	
	private HighestCalculator highestCalculator;
	
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------

	@Override
	protected void OnInit() throws TradingException {
		highestCalculator = new HighestCalculator(Period);
	}
	
	//------------------------------------------------------------------------
	
	@Override
	protected void OnBarUpdate() throws TradingException {
		highestCalculator.onBarUpdate(Input.get(0), getCurrentBar());
		
		Value.set(0, highestCalculator.getHighestIndex());
	}
	
}
