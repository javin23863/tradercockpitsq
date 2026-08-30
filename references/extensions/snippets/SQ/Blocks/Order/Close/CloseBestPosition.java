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
package SQ.Blocks.Order.Close;

import SQ.Functions.OrderFunctions;
import SQ.Internal.ActionBlock;

import com.strategyquant.lib.*;
import com.strategyquant.datalib.*;
import com.strategyquant.tradinglib.*;

@BuildingBlock(name="Close most profitable position", returnType = ReturnTypes.Action)
@Help("Close most profitable position that fits the criteria")
@SortOrder(300)
@IgnoreInBuilder
@ForEngine("*,-MC,-TS,-SP,-SA")
public class CloseBestPosition extends ActionBlock {
	
	@Parameter(defaultValue="Current", category="Order identification", showIfDefault=false, allowAny=true)
	public String Symbol;
	
	@Parameter(defaultValue="0", category="Order identification")
	@Editor(type=Editors.Selection, values="Long=1,Short=-1,Any=0")
	public int Direction;

	@Parameter(defaultValue="MagicNumber", category="Order identification", showIfDefault=false)
	@Help("Magic number that can identify the order.")
	@Editor(type=Editors.SelectionVariablesWithAny)
	public int MagicNumber;

	@Parameter(defaultValue="", category="Order identification", showIfDefault=false)
	@Help("Comment can be also used to identify the order. In case of Comment, order matches if the order comments contains the text specified here.")
	public String Comment;
	
	
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------

	@Override
	public void OnAction() throws TradingException {
		double maxProfit = -Double.MAX_VALUE;
		ILiveOrder orderToClose = null;
		
		for(int i=Strategy.Trader.getOpenOrdersCount(false)-1; i>=0; i--) {
			ILiveOrder order = Strategy.Trader.getOpenOrder(i, false);
			
			if(OrderFunctions.identify(order, Strategy, Symbol, Direction, MagicNumber, Comment)) {
				double orderPL = order.getPL();
				
				if(orderToClose == null || orderPL > maxProfit) {
					orderToClose = order;
					maxProfit = orderPL;
				}
			}
		}
		
		if(orderToClose != null) {
			orderToClose.setExitIndex((byte) -1);
			orderToClose.Close(OrderCloseTypes.Manual);
		}
	}
}
