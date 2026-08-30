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
import SQ.Internal.ValueBlock;

import com.strategyquant.lib.*;
import com.strategyquant.datalib.*;
import com.strategyquant.tradinglib.*;

@BuildingBlock(name="Closed P/L (in money)", display="Closed P/L in money(#Symbol#, #MagicNumber#, #Direction#, \"#Comment#\")", returnType = ReturnTypes.Number)
@Help("Returns last closed P/L in money for order with given Magic Number. It will return 0 if the order hasn't closed yet. If Magic Number is 0, it will return closed P/L of last order.")
@SortOrder(900)
@IgnoreInBuilder
public class ClosedPLInMoney extends ValueBlock {
	
	@Parameter(defaultValue="Current", category="Order identification", showIfDefault=false, allowAny=true)
	public String Symbol;
	
	@Parameter(defaultValue="0", category="Order identification", showIfDefault=false)
	@Editor(type=Editors.Selection, values="Long=1,Short=-1,Any=0")
	public int Direction;
	
	@Parameter(defaultValue="MagicNumber", category="Order identification", showIfDefault=false)
	@Help("Magic number that can identify the order.")
	@Editor(type=Editors.SelectionVariablesWithAny)
	public int MagicNumber;

	@Parameter(defaultValue="", category="Order identification", showIfDefault=false)
	@Help("Comment can be also used to identify the order. In case of Comment, order matches if the order comments contains the text specified here.")
	public String Comment;
	
	@Parameter(defaultValue="0", minValue=0, maxValue=10000, step=1, category="Other")
	@Help("Leave at 0 for very last trade. 1 means trade befor ethe last one, 2 means trade before that, etc.")
	public int TradesAgo;
	
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------

	@Override
	public double OnBlockEvaluate(int relativeShift) throws TradingException {
		int index = 0;
		
		for(int i=Strategy.Trader.getHistoryOrdersCount() - 1; i>=0; i--) {
			Order order = Strategy.Trader.getHistoryOrder(i);
			
			if(!order.isRealOrder()) continue;
			
			if(OrderFunctions.identify(order, Strategy, Symbol, Direction, MagicNumber, Comment)) {
				if(index == TradesAgo) {
					return order.isFilledOrder() ? getPL(order) : 0;
				}
				
				index++;
			}
		}
		
		return 0;
	}
	
	//------------------------------------------------------------------------

	private double getPL(Order o) {
		double priceDelta = 0;
		
		if(o.isLong()) {
			priceDelta = o.ClosePrice - o.OpenPrice;
		} 
		else {
			priceDelta = o.OpenPrice - o.ClosePrice;
		}

		return SQUtils.round(priceDelta * o.Size * Strategy.getInstrumentInfo().pointValue, 2) + o.CommSwap;
	}

}
