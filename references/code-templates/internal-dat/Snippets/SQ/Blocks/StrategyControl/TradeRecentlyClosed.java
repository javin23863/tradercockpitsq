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
package SQ.Blocks.StrategyControl;

import SQ.Functions.OrderFunctions;
import SQ.Internal.ConditionBlock;

import com.strategyquant.lib.*;
import com.strategyquant.datalib.*;
import com.strategyquant.tradinglib.*;

@BuildingBlock(display="TradeRecentlyClosed(#Symbol#, #MagicNumber#)", returnType = ReturnTypes.Boolean)
@Help("Returns true if there was active trade that was closed recently - which means at the same bar or even the same minute.")
@SortOrder(400)
@IgnoreInBuilder
@ForEngine("*,-MC,-TS,-SP,-SA")
public class TradeRecentlyClosed extends ConditionBlock {
	
	@Parameter(defaultValue="Current", category="Order identification", showIfDefault=false, allowAny=true)
	public String Symbol;
	
	@Parameter(defaultValue="MagicNumber", category="Order identification", showIfDefault=false)
	@Help("Magic number that can identify the order.")
	@Editor(type=Editors.SelectionVariablesWithAny)
	public int MagicNumber;
	
	@Parameter(defaultValue="true", category="Close Condition")
	@Help("Check if trade closed this bar.")
	public boolean CheckThisBar;
	
	@Parameter(defaultValue="true", category="Close Condition")
	@Help("Check if trade closed in the last minute.")
	public boolean CheckThisMinute;
	
	private int lastIndexChecked = 0;
	
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------

	@Override
	public boolean OnBlockEvaluate() throws TradingException {
		return checkTradeClosedRecently();
	}

	//------------------------------------------------------------------------

	protected boolean checkTradeClosedRecently() throws TradingException {
		int historyOrdersCount = Strategy.Trader.getHistoryOrdersCount();
		if(historyOrdersCount == lastIndexChecked) {
			// no new orders since last check
			return false;
		}
		
		long currentBarTime = Strategy.Time(0);
		
		long beginningMinuteTime = 0;
		if(CheckThisMinute) {
			beginningMinuteTime = SQTime.setSecond(currentBarTime, 0);
			beginningMinuteTime = SQTime.setMiliSeconds(beginningMinuteTime, 0);
		}
		
		int ordersChecked = 0;
		
		if(lastIndexChecked < 0) lastIndexChecked = 0;
		
		for(int i=historyOrdersCount-1; i>=lastIndexChecked; i--) {
			Order order = Strategy.Trader.getHistoryOrder(i);
			
			if(!OrderFunctions.identify(order, Strategy, Symbol, 0, MagicNumber, null)) continue;
			
			if(!order.isFilledOrder()) continue; // ignore pending orders

			ordersChecked++;
			if(ordersChecked > 10) {
				// check only the very last 10 orders
				break;
			}
			
			if(CheckThisBar) {
				if(order.CloseTime >= currentBarTime) {
					// order finished this bar
					return true;
				}
			}
			
			if(CheckThisMinute) {
				if(order.CloseTime >= beginningMinuteTime) {
					// order finished this minute
					return true;
				}
			}
		}
		
		lastIndexChecked = historyOrdersCount;
		return false;
	}
}
