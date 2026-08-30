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

@BuildingBlock(display="Last order(#Symbol#, #MagicNumber#, \"#Comment#\") was #Direction#", returnType = ReturnTypes.Boolean)
@Help("Returns true if direction of last order matches. Considers only executed orders, not pending orders that were closed.")
@SortOrder(600)
@IgnoreInBuilder
@ForEngine("*,-MC,-TS,-SP,-SA")
public class LastOrderWas extends ConditionBlock {
	
	@Parameter(defaultValue="Current", category="Order identification", showIfDefault=false, allowAny=true)
	public String Symbol;
	
	@Parameter(defaultValue="MagicNumber", category="Order identification", showIfDefault=false)
	@Help("Magic number that can identify the order.")
	@Editor(type=Editors.SelectionVariablesWithAny)
	public int MagicNumber;

	@Parameter(defaultValue="", category="Order identification", showIfDefault=false)
	@Help("Comment can be also used to identify the order. In case of Comment, order matches if the order comments contains the text specified here.")
	public String Comment;

	@Parameter(defaultValue="1", category="Direction")
	@Editor(type=Editors.Selection, values="Long=1,Short=-1")
	public int Direction;
	
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------

	@Override
	public boolean OnBlockEvaluate() throws TradingException {
	
		for(int i=Strategy.Trader.getHistoryOrdersCount()-1; i>=0; i--) {
			Order order = Strategy.Trader.getHistoryOrder(i);
			
			if(!OrderFunctions.identify(order, Strategy, Symbol, 0, MagicNumber, Comment)) continue;
			
			if(!order.isFilledOrder()) continue; // ignore pending orders

			if((Direction > 0 && order.isLong()) || (Direction < 0 && order.isShort())) {
				return true;
			} 
			else {
				return false;
			}
		}
		
		return false;
	}


}
