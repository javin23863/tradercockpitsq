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

import com.strategyquant.datalib.*;
import com.strategyquant.lib.SQTime;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.SettingsMap;
import com.strategyquant.tradinglib.*;
import com.strategyquant.tradinglib.simulator.Engines;

@ClassConfig(name="Profit Target")
@SortOrder(200)
public class ProfitTarget extends ExitMethod {
	
	@Parameter(category="Basic", showIfDefault=false)
	@Editor(type=Editors.Formula, formulaName="SLPT")
	@ExitType(ExitTypes.ProfitTarget)
	public IFormula ProfitTarget;

	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------

	@Override
	public void setForOrder(ILiveOrder order, StrategyBase strategy) throws TradingException {
		if(order.isFilled()) {
			setOrderPTValue(order, strategy);
		}
		else {
			boolean isTSMCEngine = strategy.getEngine() == Engines.Tradestation || strategy.getEngine() == Engines.MultiCharts;

			if(isTSMCEngine && isInitialSLPTApplied(strategy)) {
				if(order.isMarketOrder()) {
					setOrderPTValue(order, strategy);
				}
				else {
					setInitialExit(order, strategy);
				}
			}
		}
	}
	
	//------------------------------------------------------------------------

	private void setOrderPTValue(ILiveOrder order, StrategyBase strategy) throws TradingException {
		int direction = order.isLong() ? 1 : -1;

		double openPrice = order.isNettingMode() ? order.getLastOpenPrice(): order.getOpenPrice();
		double pt = ProfitTarget.evaluateFormula(strategy, order.getSymbol(), openPrice, direction);

		if(pt == openPrice || pt == Order.NOT_DEFINED) {
			return;
		}

		pt = correctSLPT(order, pt, false);
		
		order.setPT(pt).Send();
		
		if(!order.isSuccessful()) {
			// we were not successful in setting Profit Target, close whole order
			order.Close(OrderCloseTypes.Deleted);
		}		
	}

	//------------------------------------------------------------------------

	@Override
	public double computeValue(byte orderType, StrategyBase strategy, String symbol, double price) throws TradingException {
		int direction = OrderTypes.isLongOrder(orderType) ? 1 : -1;

		double pt = ProfitTarget.evaluateFormula(strategy, symbol, price, direction);
		if(pt == Order.NOT_DEFINED) return Order.NOT_DEFINED;
		
		double tickSize = strategy.getInstrumentInfo().tickSize;
		
		return correctSLPT(price, pt, direction, tickSize, false);		//used to calculate SL for money management methods
	}
	
	//------------------------------------------------------------------------

	/** 
	 * used in Tradestation. SL, PT and other exits are not tied 
	 * with the exact orders but they are placed separately.
	 * @throws TradingException 
	 */
	@Override
	public boolean setExit(ILiveOrder order, StrategyBase strategy) throws TradingException {
		int direction = order.isLong() ? 1 : -1;
		
		double pt = ProfitTarget.evaluateFormula(strategy, order.getSymbol(), order.getOpenPrice(), direction);
		if(pt == Order.NOT_DEFINED) {
			return false;
		}
		
		pt = SQUtils.fixPrice(Strategy.getInstrumentInfo().tickStep, pt);
		
		pt = correctSLPT(order, pt, false);
		order.setPT(pt);
		
		byte orderType = (direction < 0 ? OrderTypes.BuyToCoverLimit : OrderTypes.SellToCoverLimit);
		//Log.info(SQTime.toDateMinuteString(Strategy.Time(0))+" - Setting PT to: {}", pt);

		if(!shouldApplySLPTToOrder(order, strategy)) {
			return true;
		}
		
		ILiveOrder ptOrder = Strategy.Trader.Open(orderType, order.getSymbol(), pt)
				.setComment("ExitPT")
				.setMagicNumber(order.getMagicNumber())
				.Send();
		
		return false;
	}

	//------------------------------------------------------------------------

	public boolean setInitialExit(ILiveOrder order, StrategyBase strategy) throws TradingException {
		int direction = order.isLong() ? 1 : -1;

//		if(order.getOpenPrice() == 0) {
//			return false;
//		}

		double pt = ProfitTarget.evaluateFormula(strategy, order.getSymbol(), 0, -1);

		boolean isTSMCEngine = strategy.getEngine() == Engines.Tradestation || strategy.getEngine() == Engines.MultiCharts;
		if(isTSMCEngine) {

			if (pt == Order.NOT_DEFINED) { // || pt <= 0) {
				return false;
			}

			pt = Math.abs(pt);
			pt = SQUtils.fixPrice(Strategy.getInstrumentInfo().tickStep, pt);
			pt = correctSLPT(order, pt, true);
		} else {
			if (pt == Order.NOT_DEFINED || pt <= 0) {
				return false;
			}

			pt = SQUtils.fixPrice(Strategy.getInstrumentInfo().tickStep, pt);
			pt = correctSLPT(order, pt, false);

		}
		order.setPT(pt).Send();

		if(!shouldApplySLPTToOrder(order, strategy)) {
			return true;
		}

		byte orderType = (direction < 0 ? OrderTypes.BuyToCoverLimit : OrderTypes.SellToCoverLimit);

		ILiveOrder ptOrder = Strategy.Trader.Open(orderType, order.getSymbol(), pt)
				.setComment("ExitPT")
				.setMagicNumber(order.getMagicNumber())
				.Send();

		return true;
	}

	//------------------------------------------------------------------------

	@Override
	public IBlock clone(boolean includingParameters, StrategyBase strategy) throws BlockDefinitionException {
		return this; //no need to clone
	}
	

}