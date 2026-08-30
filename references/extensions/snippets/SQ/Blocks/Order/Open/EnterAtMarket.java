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
package SQ.Blocks.Order.Open;

import java.util.ArrayList;

import SQ.Internal.XmlStrategy;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.datalib.UpdateEventTypes;
import com.strategyquant.lib.SQTime;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.SettingsMap;
import com.strategyquant.tradinglib.ATM;
import com.strategyquant.tradinglib.ATMExit;
import com.strategyquant.tradinglib.BlockDefinitionException;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.CategoryOrder;
import com.strategyquant.tradinglib.Editor;
import com.strategyquant.tradinglib.Editors;
import com.strategyquant.tradinglib.ExitMethod;
import com.strategyquant.tradinglib.ExitTypes;
import com.strategyquant.tradinglib.Help;
import com.strategyquant.tradinglib.IActionEventListener;
import com.strategyquant.tradinglib.IFormula;
import com.strategyquant.tradinglib.ILiveOrder;
import com.strategyquant.tradinglib.Order;
import com.strategyquant.tradinglib.OrderCloseTypes;
import com.strategyquant.tradinglib.OrderTypes;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.Required;
import com.strategyquant.tradinglib.ReturnTypes;
import com.strategyquant.tradinglib.SortOrder;
import com.strategyquant.tradinglib.StrategyBase;
import com.strategyquant.tradinglib.atm.exits.None;
import com.strategyquant.tradinglib.atm.exits.TrailingStop;
import com.strategyquant.tradinglib.simulator.Engines;
import SQ.ExitMethods.StopLoss;
import SQ.Functions.OrderFunctions;
import SQ.Internal.ActionBlock;
import SQ.Internal.MMFormulaBlock;

@BuildingBlock(name="(MKT) Enter at market", display="EnterAtMarket", returnType=ReturnTypes.Order)
@Help("Opens order at current market price")
@SortOrder(100)
@CategoryOrder(100)
public class EnterAtMarket extends ActionBlock {

	private static final int MaxDistanceFromMarketHash = "MaxDistanceFromMarket.MaxDistanceFromMarket".hashCode();
	private static final int MaxDistanceFromMarketPctHash = "MaxDistanceFromMarket.MaxDistancePct".hashCode();
	
	// Basic parameters
	@Parameter(defaultValue="Any", category="Basic", showIfDefault=false)
	@Editor(type=Editors.SelectionSymbolsWithAny)
	public String Symbol;
	
	@Parameter(defaultValue="1", category="Basic")
	@Editor(type=Editors.Selection, values="Long=1,Short=-1")
	public int Direction;

	@Parameter(category="Basic")
	@Editor(type=Editors.Formula, formulaName="Size")
	@Required
	public IFormula Size;

	// Other parameters
	@Parameter(defaultValue="MagicNumber", category="Order identification", showIfDefault=false)
	@Help("Magic number is used to identify this trade, it should be unique for every trade you open.")
	@Editor(type=Editors.SelectionVariables)
	public int MagicNumber;

	@Parameter(defaultValue="", category="Order identification", showIfDefault=false)
	public String Comment;
	
	@Parameter(defaultValue="false", category="Order identification", showIfDefault=false)
	@Help("If set to true, it will allow to place multiple trades with the same Magic Number")
	public boolean AllowDuplicateTrades;
	
	public ExitMethod[] ExitMethods;

	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	
	@Override
	public void OnAction() throws TradingException {
		if((!AllowDuplicateTrades || !engineSupportsDuplicateTrades()) && checkLiveOrderExists(0, true) != null) {
			// market is not flat and duplicate trades are not allowed
			return;
		}

		if(Strategy != null && Strategy.Trader != null && !Strategy.Trader.IsMarketOpen()) {
			// market is not open (MT5 feature since Build 140)
			return;
		}
/*
		String ss = SQTime.toDateMinuteString(Strategy.Time(0));
		if(ss.contains("2022.04.12 13:00")) {
			int a = 1;
			int b = a;
		}
*/
		// open trade
		byte orderType = (Direction > 0 ? OrderTypes.Buy : OrderTypes.Sell);
		double sl = computeSL(orderType, (Direction > 0 ? Strategy.MarketData.Chart(Symbol).Ask() : Strategy.MarketData.Chart(Symbol).Bid()));
		double size = computeSize(orderType, 0, sl);
		
		if ((Direction > 0 && sl == Strategy.MarketData.Chart(Symbol).Ask()) || (Direction < 0 && sl == Strategy.MarketData.Chart(Symbol).Bid())) {
			return;
		}
		
		ATM atm = Strategy.getATM();
		
		if(atm != null && atm.isApplicable(Strategy, size, sl, orderType)) {
			double pt = computePT(orderType, (Direction > 0 ? Strategy.MarketData.Chart(Symbol).Ask() : Strategy.MarketData.Chart(Symbol).Bid()));

			openATMOrder(atm, -1, size, sl, pt, orderType, 0);
		
		} else {
			openNormalOrder(-1, size, sl, orderType, 0);
		}
	}

	//------------------------------------------------------------------------

	protected void openATMOrder(ATM atm, double openPrice, double fullSize, double sl, double pt, byte orderType, int barsValid) throws TradingException {
		ExitMethod slExit = getStopLossExit();
		double sizeSoFar = 0;
		
		if(Strategy.getEngine() == Engines.MetaTrader4 || Strategy.getEngine() == Engines.MetaTrader5Hedged) {
			for(int i=0; i<atm.getExitsCount(); i++) {
				ATMExit atmExit = tryCloneATMExit(atm.getExit(i));
			
				boolean isLastExit = (i == atm.getExitsCount()-1);
				double size = atmExit.computeSize(fullSize, sizeSoFar, isLastExit);
				
				if(size > 0) {
					ILiveOrder order = Strategy.Trader.Open(orderType, Symbol, openPrice)
							.setSize(size)
							.setMagicNumber(MagicNumber)
							.setComment(Comment)
							.Send();
	
					sizeSoFar += order.getSize();
					
					if(order.isSuccessful()) {
						handleBarsValid(order, barsValid);
						
						// add standard SL
						if(slExit != null && !order.isClosedOrder()) {
							slExit.setForOrder(order, Strategy);
						}
						
						// add ATM exit
						atmExit.setForOrder(order, Strategy, sl, pt);
					}
				}
			}
		}
		else {
			ILiveOrder order = Strategy.Trader.Open(orderType, Symbol, openPrice)
					.setSize(fullSize)
					.setMagicNumber(MagicNumber)
					.setComment(Comment)
					.Send();

			if(order.isSuccessful()) {
				handleBarsValid(order, barsValid);
				
				// add standard SL
				if(slExit != null && !order.isClosedOrder()) {
					slExit.setForOrder(order, Strategy);
				}
				
				for(byte i=0; i<atm.getExitsCount(); i++) {
					ATMExit atmExit = tryCloneATMExit(atm.getExit(i));
					boolean isLastExit = (i == atm.getExitsCount() - 1);
					double size = atmExit.computeSize(fullSize, sizeSoFar, isLastExit);
					
					if(size > 0) {
						sizeSoFar += Strategy.getEngine() == Engines.MetaTrader5Netted ? openNewATMNettingOrder(order, atmExit, size, sl, pt, i) : openNewTSATMOrder(order, atmExit, size, sl, pt, i);
					}
				}
			}
		}
	}

	//------------------------------------------------------------------------

	private double openNewTSATMOrder(ILiveOrder mainOrder, ATMExit atmExit, double size, double sl, double pt, byte exitIndex) throws TradingException {
		mainOrder.registerEvent(UpdateEventTypes.BarOpen, new IActionEventListener() {
			private ILiveOrder exitOrder = null;
			private double actualSL = -1, actualPT = -1;
			private double lastTS = -1;
			private boolean filled = false;
			
			@Override
			public void OnActionEvent(StrategyBase strategy) throws TradingException {
				if(mainOrder.isClosedOrder()) {
					if(exitOrder != null) {
						exitOrder.Close(OrderCloseTypes.Deleted);
						exitOrder = null;
					}
					return;
				}
				
				if(!mainOrder.isMarketOrder()) return;
				
				//SL and PT must be calculated when order is filled. Otherwise it may use old indicator values (TradeStation fills on next bar)
				
				actualSL = computeSL(mainOrder.getOrderType(), mainOrder.getOpenPrice());
				actualPT = computePT(mainOrder.getOrderType(), mainOrder.getOpenPrice());
				
				if(atmExit.exitLevel instanceof None) {
					None noneExit = (None) atmExit.exitLevel;
					
					if(noneExit.checkExitAfterBars(mainOrder)) {
						noneExit.deactivate();
						
						Strategy.Trader.Open(mainOrder.isLong() ? OrderTypes.Sell : OrderTypes.Buy, mainOrder.getSymbol(), 0)
								.setSize(size)
								.setMagicNumber(MagicNumber)
								.setComment(Comment)
								.setExitIndex(exitIndex)
								.Send();
					}
				}
				else if(atmExit.exitLevel instanceof TrailingStop) {		
					if(filled) return;
					
					double newTrailingPrice = SQUtils.fixPrice(Strategy.getInstrumentInfo().tickStep, atmExit.exitLevel.getNettingPrice(Strategy, mainOrder, actualSL, actualPT));
					
					if(mainOrder.isLong()) {
						if(newTrailingPrice > mainOrder.getOpenPrice() && newTrailingPrice > actualSL && newTrailingPrice > lastTS) {
							//move trailing exit closer to current price
							lastTS = newTrailingPrice;
						}
					}
					else {
						if(newTrailingPrice < mainOrder.getOpenPrice() && newTrailingPrice < actualSL && (newTrailingPrice < lastTS || lastTS < 0)) {
							//move trailing exit closer to current price
							lastTS = newTrailingPrice;
						}
					}

					if(exitOrder != null) {
						if(exitWasFilled(exitOrder)) {	//Trailing stop has been hit
							filled = true;
							return;		
						}
						else {
							exitOrder.Close(OrderCloseTypes.Replaced);
						}
					}
					
					if(lastTS > 0) {
						exitOrder = Strategy.Trader.Open(Direction > 0 ? OrderTypes.SellStop : OrderTypes.BuyStop, Symbol, lastTS)
							.setSize(size)
							.setMagicNumber(MagicNumber)
							.setComment(Comment)
							.setExitIndex(exitIndex)
							.Send();

						exitOrder.registerEvent(UpdateEventTypes.OrderFilled, new IActionEventListener() {
							@Override
							public void OnActionEvent(StrategyBase Strategy) throws TradingException {
								if(exitWasFilled(exitOrder)) {
									filled = true;
								}
							}
						});
					}
				}
				else {
					if(filled) return;
					
					double exitOpenPrice = SQUtils.fixPrice(Strategy.getInstrumentInfo().tickStep, atmExit.exitLevel.getNettingPrice(Strategy, mainOrder, actualSL, actualPT));

					if(exitOpenPrice <= 0) {
						return;
					}
					
					if(exitOrder != null) {
						if(exitWasFilled(exitOrder)) return;
						else {
							exitOrder.Close(OrderCloseTypes.Deleted);
							exitOrder = null;
						}
					}
					
					exitOrder = Strategy.Trader.Open(Direction > 0 ? OrderTypes.SellLimit : OrderTypes.BuyLimit, Symbol, exitOpenPrice)
							.setSize(size)
							.setMagicNumber(MagicNumber)
							.setComment(Comment)
							.setExitIndex(exitIndex)
							.Send();
					
					if(exitOrder.isSuccessful()) {
						exitOrder.registerEvent(UpdateEventTypes.BarOpen, new IActionEventListener() {
							@Override
							public void OnActionEvent(StrategyBase strategy) throws TradingException {
								boolean mainOrderExists = false;
								
								for(int i=Strategy.Trader.getOpenOrdersCount(false) - 1; i >= 0; i--) {
									ILiveOrder order = Strategy.Trader.getOpenOrder(i, false);
									
									if(order.getOrderId() == mainOrder.getOrderId()) {
										mainOrderExists = true;
									}
								}
								
								if(!mainOrderExists) {
									exitOrder.Close(OrderCloseTypes.Deleted);
								}
							}
						});	
						exitOrder.registerEvent(UpdateEventTypes.OrderFilled, new IActionEventListener() {
							@Override
							public void OnActionEvent(StrategyBase Strategy) throws TradingException {
								if(exitWasFilled(exitOrder)) {
									filled = true;
								}
							}
						});
					}
				}
			}
		});	
		
		return size;
	}
	
	//------------------------------------------------------------------------

	private double openNewATMNettingOrder(ILiveOrder mainOrder, ATMExit atmExit, double size, double sl, double pt, byte exitIndex) throws TradingException {
		// for None attach a BarOpen event listener and close after number of bars set
		
		if(atmExit.exitLevel instanceof None) {
			mainOrder.registerEvent(UpdateEventTypes.BarOpen, new IActionEventListener() {
				@Override
				public void OnActionEvent(StrategyBase strategy) throws TradingException {
					if(mainOrder.isClosedOrder() || !mainOrder.isMarketOrder()) return;
					
					None noneExit = (None) atmExit.exitLevel;
					
					if(noneExit.checkExitAfterBars(mainOrder)) {
						noneExit.deactivate();
						
						ILiveOrder exitOrder = Strategy.Trader.Open(mainOrder.isLong() ? OrderTypes.Sell : OrderTypes.Buy, mainOrder.getSymbol(), 0)
								.setSize(size)
								.setMagicNumber(MagicNumber)
								.setComment(Comment)
								.setExitIndex(exitIndex)
								.Send();
					}
				}
			});	
			
			return size;
		}
		else if(atmExit.exitLevel instanceof TrailingStop) {
			mainOrder.registerEvent(UpdateEventTypes.BarOpen, new IActionEventListener() {
				private ILiveOrder exitOrder = null;
				private double lastTS = -1;
				private boolean filled = false;
				
				@Override
				public void OnActionEvent(StrategyBase strategy) throws TradingException {
					if(filled) return;
					
					if(mainOrder.isClosedOrder()) {
						if(exitOrder != null) {
							exitOrder.Close(OrderCloseTypes.Deleted);
							exitOrder = null;
						}
						return;
					}
					
					if(mainOrder.isMarketOrder()) {
						double newTrailingPrice = SQUtils.fixPrice(Strategy.getInstrumentInfo().tickStep, atmExit.exitLevel.getNettingPrice(Strategy, mainOrder, mainOrder.getSL(), mainOrder.getPT()));
						double prevTS = lastTS;
						
						if(mainOrder.isLong()) {
							if(newTrailingPrice > mainOrder.getOpenPrice() && newTrailingPrice > mainOrder.getSL() && newTrailingPrice > lastTS) {
								//move trailing exit closer to current price
								lastTS = newTrailingPrice;
							}
						}
						else {
							if(newTrailingPrice < mainOrder.getOpenPrice() && newTrailingPrice < mainOrder.getSL() && (newTrailingPrice < lastTS || lastTS < 0)) {
								//move trailing exit closer to current price
								lastTS = newTrailingPrice;
							}
						}
						
						if(prevTS != lastTS && lastTS > 0) {
							if(exitOrder != null) {
								exitOrder.Close(OrderCloseTypes.Replaced);
							}
							
							exitOrder = Strategy.Trader.Open(Direction > 0 ? OrderTypes.SellStop : OrderTypes.BuyStop, Symbol, lastTS)
								.setSize(size)
								.setMagicNumber(MagicNumber)
								.setComment(Comment)
								.setExitIndex(exitIndex)
								.Send();
							
							exitOrder.registerEvent(UpdateEventTypes.OrderFilled, new IActionEventListener() {
								@Override
								public void OnActionEvent(StrategyBase Strategy) throws TradingException {
									if(exitWasFilled(exitOrder)) {
										filled = true;
									}
								}
							});
						}
					}
				}
			});	

			return size;
		}
		
		// for other types create limit orders
		
		double exitOpenPrice = SQUtils.fixPrice(Strategy.getInstrumentInfo().tickStep, atmExit.exitLevel.getNettingPrice(Strategy, mainOrder, sl, pt));

		if(exitOpenPrice <= 0) {
			return 0;
		}
		
		ILiveOrder exitOrder = Strategy.Trader.Open(Direction > 0 ? OrderTypes.SellLimit : OrderTypes.BuyLimit, Symbol, exitOpenPrice)
				.setSize(size)
				.setMagicNumber(MagicNumber)
				.setComment(Comment)
				.setExitIndex(exitIndex)
				.Send();
		
		if(exitOrder.isSuccessful()) {
			exitOrder.registerEvent(UpdateEventTypes.BarOpen, new IActionEventListener() {
				@Override
				public void OnActionEvent(StrategyBase strategy) throws TradingException {
					boolean mainOrderExists = false;
					
					for(int i=Strategy.Trader.getOpenOrdersCount(false) - 1; i >= 0; i--) {
						ILiveOrder order = Strategy.Trader.getOpenOrder(i, false);
						
						if(order.getOrderId() == mainOrder.getOrderId()) {
							mainOrderExists = true;
						}
					}
					
					if(!mainOrderExists) {
						exitOrder.Close(OrderCloseTypes.Deleted);
					}
				}
			});	
			
			return size;
		}
		else return 0;
	}

	//------------------------------------------------------------------------

	private boolean exitWasFilled(ILiveOrder exitOrder) {
		return exitOrder.isMarketOrder() && exitOrder.getCloseTime() > 0;
	}
	
	//------------------------------------------------------------------------

	protected void openNormalOrder(double openPrice, double size, double sl, byte orderType, int barsValid) throws TradingException {
		ILiveOrder order = Strategy.Trader.Open(orderType, Symbol, openPrice)
				.setSize(size)
				.setMagicNumber(MagicNumber)
				.setComment(Comment)
				.Send();

		if(order.isSuccessful()) {
			handleBarsValid(order, barsValid);
			
			for(ExitMethod exitMethod : ExitMethods) {
				if(!order.isClosedOrder()) {
					if(AllowDuplicateTrades) {
						//we have to clone the exit method, otherwise it makes problems when strategy has duplicate trades enabled (problem example: Trailing Stop is set only for the first order)
						try {
							ExitMethod exitMethodCloned = (ExitMethod) exitMethod.clone(true, Strategy);
							exitMethodCloned.setForOrder(order, Strategy);
						} 
						catch (BlockDefinitionException e) {
							Log.error("Cannot clone exit method '" + exitMethod.getClass().getName() + "' for order #" + order.getOrderId(), e);
						}
					}
					else {
						exitMethod.setForOrder(order, Strategy);
					}
				}
			}
		}
	}

	//------------------------------------------------------------------------

	protected void handleBarsValid(ILiveOrder order, int barsValid) throws TradingException {
		if(order.isClosedOrder() || order.isMarketOrder()) return;
		
		// order is placed, now handle order validity
		if(barsValid != 0) {
			order.registerEvent(UpdateEventTypes.BarOpen, new IActionEventListener() {
				@Override
				public void OnActionEvent(StrategyBase strategy) throws TradingException {
					checkBarsValid(order, barsValid);
				}
			});
		}
	}
	

	//------------------------------------------------------------------------

	protected void checkBarsValid(ILiveOrder order, int barsValid) throws TradingException {
		if(order.isClosedOrder()) return;
		
		if(order.isPendingOrder() && order.getBarsInTrade() >= barsValid) {
			order.Close(OrderCloseTypes.Expired);
		}
	}
	
	//------------------------------------------------------------------------

	protected double computeSL(byte orderType, double orderPrice) throws TradingException {
		ExitMethod slExit = getStopLossExit();
		
		if(slExit == null || orderPrice == 0) {
			// no SL
			return Order.NOT_DEFINED;
		}
		
		return SQUtils.fixPrice(Strategy.getInstrumentInfo().tickStep, slExit.computeValue(orderType, Strategy, Symbol, orderPrice));
	}
	

	//------------------------------------------------------------------------

	protected double computePT(byte orderType, double orderPrice) throws TradingException {
		ExitMethod ptExit = getProfitTargetExit();
		
		if(ptExit == null) {
			// no PT
			return Order.NOT_DEFINED;
		}

		double tickStep = Strategy.getInstrumentInfo().tickStep;
		return SQUtils.fixPrice(tickStep, ptExit.computeValue(orderType, Strategy, Symbol, orderPrice));
	}
	
	//------------------------------------------------------------------------

	public ExitMethod getStopLossExit() {
		ExitMethod slExit = null;
		
		for(ExitMethod exitMethod : ExitMethods) {
			if(exitMethod.getExitType() == ExitTypes.StopLoss) {
				slExit = exitMethod;
				break;
			}
		}
		
		return slExit;
	}
	
	//------------------------------------------------------------------------

	public ExitMethod getProfitTargetExit() {
		ExitMethod ptExit = null;
		
		for(ExitMethod exitMethod : ExitMethods) {
			if(exitMethod.getExitType() == ExitTypes.ProfitTarget) {
				ptExit = exitMethod;
				break;
			}
		}
		
		return ptExit;
	}
	
	//------------------------------------------------------------------------

	public ExitMethod getExitAfterBarsExit() {
		ExitMethod exit = null;
		
		for(ExitMethod exitMethod : ExitMethods) {
			if(exitMethod.getExitType() == ExitTypes.ExitAfterBars) {
				exit = exitMethod;
				break;
			}
		}
		
		return exit;
	}
	
	//------------------------------------------------------------------------

	public double computeSize(byte orderType, double price, double sl) throws TradingException {
		MMFormulaBlock sizeFormula = (MMFormulaBlock) Size;
		
		return sizeFormula.computeSize(Strategy, Symbol, orderType, price, sl);
	}

	//------------------------------------------------------------------------

	protected ILiveOrder checkLiveOrderExists(int direction, boolean includeClosingOrders) {
		int count = Strategy.Trader.getOpenOrdersCount(includeClosingOrders) - 1;
		
		for(int i=count; i >= 0; i--) {
			ILiveOrder order = Strategy.Trader.getOpenOrder(i, includeClosingOrders);
			
			if(OrderFunctions.identify(order, Strategy, Symbol, direction, MagicNumber, Comment) && order.isMarketOrder()) {
				return order;
			}
		}
		
		return null;
	}
	
	//------------------------------------------------------------------------

	/** 
	 * this method is called only in Tradestation engine, to handle exits (SL, PT, etc.) as same as they are handled in TS.
	 * @throws TradingException 
	 */
	public void OnApplyExits() throws TradingException {
		ArrayList<ILiveOrder> orders = getOpenOrders(Direction);
		ATM atm = Strategy.getATM();
		
		boolean slPlaced = false;
		if(orders != null) {
			for(int i=0; i<orders.size(); i++) {
				ILiveOrder order = orders.get(i);
				boolean atmUsed = atm != null && atm.isApplicable(Strategy, order.getSize(), order.getSL(), order.getOrderType());
				
				for(ExitMethod exitMethod : ExitMethods) {
					if(!atmUsed || exitMethod instanceof StopLoss) {
						if(exitMethod.setExit(order, Strategy)) {
							slPlaced = true;
						}
					}
				}
				
				if(!slPlaced && order.getSL() != Order.NOT_DEFINED) {
					// we have to set SL from order - handling if there is no SL, only for Trailing stop or Move2BE
					int direction = order.isLong() ? -1 : 1;

					byte orderType = (direction > 0 ? OrderTypes.BuyToCoverStop : OrderTypes.SellToCoverStop);

					ILiveOrder slOrder = Strategy.Trader.Open(orderType, order.getSymbol(), order.getSL())
							.setComment("SL")
							.setMagicNumber(order.getMagicNumber())
							.Send();
				}
			}
		}
	}

	//------------------------------------------------------------------------

	private ArrayList<ILiveOrder> getOpenOrders(int direction) {
		ArrayList<ILiveOrder> orders = null;
		
		for(int i=0; i<Strategy.Trader.getOpenOrdersCount(false); i++) {
			ILiveOrder order = Strategy.Trader.getOpenOrder(i, false);
			
			if(order.isPendingOrder()) {
				continue;
			}
			
			if(OrderFunctions.identify(order, Strategy, Symbol, direction, MagicNumber, Comment) && order.isMarketOrder()) {
				if(orders == null) {
					orders = new ArrayList<ILiveOrder>();
				}
				
				orders.add(order);
			}
		}
		
		return orders;
	}

	//------------------------------------------------------------------------

	protected boolean engineSupportsDuplicateTrades() {
		return Strategy.Trader.supportsDuplicateTrades();
	}

	//------------------------------------------------------------------------

	private ATMExit tryCloneATMExit(ATMExit exit) throws TradingException {
		ATMExit clone = exit.clone();
		if(clone == null) {
			throw new TradingException("Unable to create ATMExit object");
		}
		
		return clone;
	}

	//------------------------------------------------------------------------

	protected boolean checkOpenPriceWithinRange(double openPrice) {
		try {
			SettingsMap settings = Strategy.getSettings();
			if(settings.containsKey(MaxDistanceFromMarketHash) && settings.containsKey(MaxDistanceFromMarketPctHash) && ((boolean) settings.get(MaxDistanceFromMarketHash))) {
				double maxPctDistance = SQUtils.round2((double) settings.get(MaxDistanceFromMarketPctHash));
				double currentPrice = Direction > 0 ? Strategy.MarketData.Chart(Symbol).Ask() : Strategy.MarketData.Chart(Symbol).Bid();
				
				double distancePct = SQUtils.round2(Math.abs(currentPrice - openPrice) / currentPrice * 100);
				if(distancePct > maxPctDistance) {
					Log.debug("Order skipped - too far from market. Open price: {}, Market price: {}, Max distance: {}%", openPrice, currentPrice, maxPctDistance);
					return false;
				}
			}
		}
		catch(Throwable t) {
			Log.error("Error while checking open price max distance", t);
		}
		
		return true;
	}
}
