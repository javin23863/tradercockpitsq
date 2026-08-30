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
package SQ.ExitMethods;

import com.strategyquant.lib.*;
import com.strategyquant.datalib.*;
import com.strategyquant.tradinglib.*;

@ClassConfig(name="Exit After Bars")
@SortOrder(500)
public class ExitAfterBars extends ExitMethod {
	
	@Parameter(category="Advanced", defaultValue="0", minValue=0, maxValue=99999, builderMinValue=5, builderMaxValue=500, step=1, showIfDefault=false)
	@Help("Number of bars after which this trade will automatically be closed")
	@ExitType(ExitTypes.ExitAfterBars)
	public int ExitAfterBars;

	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------

	@Override
	public void setForOrder(ILiveOrder order, StrategyBase Strategy) throws TradingException {
		if(ExitAfterBars > 0) {
			order.registerEvent(UpdateEventTypes.BarOpen, new IActionEventListener() {
				@Override
				public void OnActionEvent(StrategyBase strategy) throws TradingException {
					if(order.isMarketOpen()) {
						checkExitAfterBars(order);
					}
				}
			});			
		}		
	}
	
	//------------------------------------------------------------------------

	private void checkExitAfterBars(ILiveOrder order) throws TradingException {
		if(order.isClosedOrder()) return;
		
		if(order.isMarketOrder() && order.getBarsInTrade() >= ExitAfterBars) {
			order.Close(OrderCloseTypes.ExitAfterXBars);
		}
	}

	//------------------------------------------------------------------------

	@Override
	public double computeValue(byte orderType, StrategyBase strategy, String symbol, double price) throws TradingException {
		throw new TradingException("This method shouldn't be called!");
	}

	//------------------------------------------------------------------------

	@Override
	public IBlock clone(boolean includingParameters, StrategyBase strategy) throws BlockDefinitionException {
		return this;	//no need to clone
	}

}