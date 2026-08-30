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
package SQ.Blocks.Price;

import SQ.Internal.ValueBlock;

import com.strategyquant.datalib.TradingException;
import com.strategyquant.tradinglib.BlockDefinitionException;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.ChartData;
import com.strategyquant.tradinglib.ForEngine;
import com.strategyquant.tradinglib.OppositeBlock;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.ReturnTypes;
import com.strategyquant.tradinglib.SortOrder;

@BuildingBlock(name="(SH) Session High", returnType=ReturnTypes.Price, display="SessionHigh(@Chart@#StartHours#:#StartMinutes#-#EndHours#:#EndMinutes#)[#Shift#]")
@OppositeBlock("SessionLow")
@SortOrder(800)
@ForEngine("*,-SP,-SA")
public class SessionHigh extends ValueBlock {

	@Parameter
	public ChartData Chart;
	
	@Parameter(defaultValue="8", minValue=0, maxValue=23, step=1)
	public int StartHours;

	@Parameter(defaultValue="30", minValue=0, maxValue=59, step=1)
	public int StartMinutes;
	
	@Parameter(defaultValue="15", minValue=0, maxValue=23, step=1)
	public int EndHours;

	@Parameter(defaultValue="15", minValue=0, maxValue=59, step=1)
	public int EndMinutes;
	
	@Parameter(defaultValue="0", minValue=0, step=1)
	public int Shift;
	
	private SessionOHLCCalculator sessionCalculator = null;
	
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	@Override
	protected void OnInit() throws BlockDefinitionException {
		sessionCalculator = new SessionOHLCCalculator(SessionOHLCCalculator.HIGH, StartHours, StartMinutes, EndHours, EndMinutes, Shift, Chart);
	}

	//------------------------------------------------------------------------
	
	
	@Override
	public double OnBlockEvaluate(int relativeShift) throws TradingException {
		return sessionCalculator.get();
	}

}