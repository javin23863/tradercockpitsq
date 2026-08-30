package SQ.Internal.RulesImpl;

import java.util.ArrayList;
import java.util.List;

import org.jdom2.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.strategyquant.datalib.TradingException;
import com.strategyquant.lib.SQTime;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.XMLUtil;
import com.strategyquant.tradinglib.BlockDefinitionException;
import com.strategyquant.tradinglib.Directions;
import com.strategyquant.tradinglib.ExitMethod;
import com.strategyquant.tradinglib.IBlock;
import com.strategyquant.tradinglib.IFormula;
import com.strategyquant.tradinglib.Order;
import com.strategyquant.tradinglib.OrderCloseTypes;
import com.strategyquant.tradinglib.OrderTypes;
import com.strategyquant.tradinglib.engine.stockpicker.backtester.BacktestData;
import com.strategyquant.tradinglib.engine.stockpicker.constants.PickerEodTypes;
import com.strategyquant.tradinglib.engine.stockpicker.constants.PickerTriggerTypes;
import com.strategyquant.tradinglib.engine.stockpicker.signals.exit.PickerExitSignal;
import com.strategyquant.tradinglib.options.parameters.StockpickerOptions;

import SQ.Blocks.Order.Open.EnterAtLimit;
import SQ.Blocks.Order.Open.EnterAtMarket;
import SQ.Blocks.Order.Open.EnterAtStop;
import SQ.Internal.ActionBlock;
import SQ.Internal.ITradingOptionsEvaluator;
import SQ.Internal.Rule;
import SQ.ExitMethods.ExitAfterBars;

public class StockpickerEntryExit extends Rule {	
	public static final Logger Log = LoggerFactory.getLogger("StockpickerEntryExit");
	
	private IBlock entry = null;
	private ActionBlock order = null;
	
	private IBlock exitConditional = null;
	
	private byte direction = -1;

	private int shift;
	private BacktestData data;

	private byte eod = PickerEodTypes.NotDefined;
		
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------

	@Override
	public void evaluateRule(int updateEventType, ITradingOptionsEvaluator evaluator, String event) throws Exception {
		double ifResult = 0;
		
		data = Strategy.Stockpicker.data;
		
		String symbol = Strategy.getSymbol();
		int symbolHash = Strategy.getSymbolHash();
		
		boolean setTradeDelays;
		
		if(direction == Directions.Long && this.Strategy.Stockpicker.MaxOpenPositionsLong < 1) {
			return;
		}
		
		if(direction == Directions.Short && this.Strategy.Stockpicker.MaxOpenPositionsShort < 1) {
			return;
		}		
		
		//entry ------------------------------------------------------------------------------------------
		if(Strategy.Stockpicker.entryType() == Strategy.Stockpicker.strategyTriggeredAt()) {

			if(entry != null) {
				ifResult = entry.evaluateBlock();
			}
			
			if(ifResult > 0) {
				if(order != null) {
					setTradeDelays = Strategy.Stockpicker.entryType() == PickerTriggerTypes.BeforeBarOpen;	
					if(setTradeDelays) { //delayed signal
						shift = -1;
						
						if(!Strategy.Stockpicker.data.exists(shift)) {
							return;
						}
					} else {
						shift = 0;
					}
					
					byte orderType = -1;
					double sl = 0;
					double pt = 0;
										
					double price = 0;
					int barsValid = 0;
					
					int exitAfterBars = 0;
					
					if(order instanceof EnterAtLimit) { //EnterAtLimit
						EnterAtLimit enterAtLimit = (EnterAtLimit) order;
						
						orderType = (enterAtLimit.Direction > 0 ? OrderTypes.BuyLimit : OrderTypes.SellLimit);
					} 
					
					if(order instanceof EnterAtStop) { //EnterAtStop
						EnterAtStop enterAtStop = (EnterAtStop) order;
						
						if(orderType==-1) orderType = (enterAtStop.Direction > 0 ? OrderTypes.BuyStop : OrderTypes.SellStop);
						
						price = evaluateOpenPrice(enterAtStop.Price, symbol, enterAtStop.Direction);
						barsValid = enterAtStop.BarsValid;
						
						if(barsValid > 0 && (Strategy.Stockpicker.entryType() == PickerTriggerTypes.OnBarClose || Strategy.Stockpicker.entryType() == Strategy.Stockpicker.exitType())) {
							barsValid+=1;	
						}
					}
										
					if(order instanceof EnterAtMarket) {
						EnterAtMarket enterAtMarket = (EnterAtMarket) order;
						
						if(orderType==-1) orderType = (enterAtMarket.Direction > 0 ? OrderTypes.Buy : OrderTypes.Sell);
						
						if(price==0) {
							if(Strategy.Stockpicker.entryType() == PickerTriggerTypes.BeforeBarOpen || Strategy.Stockpicker.entryType() == PickerTriggerTypes.OnBarOpen) {
								price =  data.OpenD(0, shift);
								
							} else {
								price =  data.CloseD(0, shift);
							}
						}
									
						sl = computeSL((EnterAtMarket)order, orderType, price, symbol);
						pt = computePT((EnterAtMarket)order, orderType, price, symbol);		
																		
						ExitMethod exit = enterAtMarket.getExitAfterBarsExit();
						if(exit!=null) {
							exitAfterBars = ((ExitAfterBars) exit).ExitAfterBars;
						}
					}
					
					//System.out.println("Entry signal " + symbol + ", "+SQTime.toDateString(data.getCurrentTime())+ ", "+data.OpenD(0, 1)+ ", "+data.CloseD(0, 1)+", Price: "+price);
					
					if(price <= 0) {
						Log.debug(String.format("SKIPPED Entry signal for %s, %s - Entry price must be greater than 0, got %s", symbol, SQTime.toDateString(data.getCurrentTime()), SQUtils.d2(price)));
						return;
					}
					
					if(sl <= 0 && sl != Order.NOT_DEFINED) {
						Log.debug(String.format("SKIPPED Entry signal for %s, %s - SL price must be greater than 0, got %s", symbol, SQTime.toDateString(data.getCurrentTime()), SQUtils.d2(sl)));
						return;
					}
					
					if(pt <= 0 && pt != Order.NOT_DEFINED) {
						Log.debug(String.format("SKIPPED Entry signal for %s, %s - PT price must be greater than 0, got %s", symbol, SQTime.toDateString(data.getCurrentTime()), SQUtils.d2(pt)));
						return;
					}
					
										
					//add Entry signal
					data.Signals(data.getTime(shift), true).addEntrySignal(symbolHash, symbol,
							orderType, 
							price, 
							barsValid, 
							sl, 
							pt, 
							slptValidFrom(sl, pt, orderType),
							exitAfterBars, 
							eod);
				}
			}
		}
		
		//exit ------------------------------------------------------------------------------------------
		if(Strategy.Stockpicker.exitType() == Strategy.Stockpicker.strategyTriggeredAt()) {			
			if(exitConditional != null) {
				ifResult = exitConditional.evaluateBlock();
				if(ifResult > 0) {
					setTradeDelays = Strategy.Stockpicker.exitType() == PickerTriggerTypes.BeforeBarOpen;
					if(setTradeDelays) { //delayed signal
						shift = -1;
						
						if(!Strategy.Stockpicker.data.exists(shift)) {
							return;
						}
					} else {
						shift = 0;
					}
					
					//add Exit signal
					data.Signals(data.getTime(shift), true).addExitSignal(new PickerExitSignal(
							symbol,
							direction,
							OrderCloseTypes.ExitSignal));
				}
			}	
		}
	}
	
	//------------------------------------------------------------------------

	/*
	 * Handling Stop Loss and Profit Targets in backtesting
	 * https://strategyquant.com/doc/strategyquant/sp-backtest-limitations/#SLPT
	 */
	private int slptValidFrom(double sl, double pt, byte orderType) {
		if(OrderTypes.isMarketOrder(orderType) && ((sl > 0 && pt <= 0) || (sl <= 0 && pt > 0))) {
			//When Entry is Market, only SL or only PT is set – in this case we can evaluate SL or PT immediately, at the same bar
			return 0;
		}
		
		if(OrderTypes.isLimitOrder(orderType) && (sl > 0 && pt <= 0)) {
			//When Entry is Limit, only SL is set – in this case we can evaluate SL, at the same bar
			return 0;
		}
		
		//in other case, SL & PT are applied only next day
		return 1;
	}
	
	//------------------------------------------------------------------------

	private double evaluateOpenPrice(IFormula Price, String Symbol, int Direction) throws TradingException {
		double openPrice = Price.evaluateFormula(Strategy, Symbol, 0, Direction);
		if(openPrice == Order.NOT_DEFINED) {
			throw new TradingException("Open price not defined");
		}
		
		return openPrice;
	}
	
	//------------------------------------------------------------------------

	protected double computeSL(EnterAtMarket action, byte orderType, double orderPrice, String symbol) throws TradingException {
		ExitMethod slExit = action.getStopLossExit();
		
		if(slExit == null) {
			// no SL
			return Order.NOT_DEFINED;
		}
		
		return SQUtils.fixPrice(0.01, slExit.computeValue(orderType, Strategy, symbol, orderPrice));
	}
	
	//------------------------------------------------------------------------

	protected double computePT(EnterAtMarket action, byte orderType, double orderPrice, String symbol) throws TradingException {
		ExitMethod ptExit = action.getProfitTargetExit();
		
		if(ptExit == null) {
			// no PT
			return Order.NOT_DEFINED;
		}

		return SQUtils.fixPrice(0.01, ptExit.computeValue(orderType, Strategy, symbol, orderPrice));
	}
	
	//------------------------------------------------------------------------

	@Override
	protected void parseXml(Element elRule) throws BlockDefinitionException {
		super.parseXml(elRule);

		String name = elRule.getAttributeValue("name");
		if(name.equalsIgnoreCase("Long")) {
			direction = Directions.Long;
		} else {
			direction = Directions.Short;
		}
		
		// get parsed Entry part
		ArrayList<IBlock> blocks = getBlocks("Entry");
		
		if(blocks.size() > 1) {
			throw new BlockDefinitionException("Entry part cannot have more than one sub-block!");
		}
		
		if(blocks != null && blocks.size() != 0) {
			entry = blocks.get(0);
		}

		// get parsed Order part
		blocks = getBlocks("Order");
		if(blocks != null && blocks.size() != 0) {			
			for(IBlock block : blocks) {
				if(!(block instanceof ActionBlock)) {
					throw new BlockDefinitionException(String.format("Block '%' in Order part is not an ActionBlock!", block.getClass().getSimpleName()));
				}
				
				order = (ActionBlock) block;
			}
		}
	
		if(entry!=null) {
			// get parsed Exit part
			blocks = getBlocks("Exit");
			if(blocks != null && blocks.size() != 0) {			
				for(IBlock block : blocks) {
					if(!(block instanceof IBlock)) {
						throw new BlockDefinitionException(String.format("Block '%s' in Exit part is not an IBlock!", block.getClass().getSimpleName()));
					}
					
					exitConditional = ((IBlock) block);
				}
			}
			
			for(Element elRulePart : elRule.getChildren("Exit")) {
				String atEndOfDay = elRulePart.getAttributeValue("atEndOfDay");
				if(atEndOfDay!=null && (!atEndOfDay.equalsIgnoreCase("false") && !atEndOfDay.equalsIgnoreCase("none"))) {					
					eod = PickerEodTypes.DayClose;
				}
				
				break;
			}
		}		
		
		StockpickerOptions options = this.Strategy.getStockpickerOptions();
		if(options != null && !this.Strategy.isAlgoWizard) {
			try {
				if(direction == Directions.Long) {
					eod = options.getEndOfDayLong(eod);
				} else {
					eod = options.getEndOfDayShort(eod);
				}
			} catch(Exception e) {
				new BlockDefinitionException(e.getMessage(), e);
			}
		}
		
		if(eod == PickerEodTypes.DayClose && Strategy.Stockpicker.exitType!=PickerTriggerTypes.OnBarClose) {
			eod = PickerEodTypes.NotDefined;
		}
		
		if(direction == Directions.Long) {
			this.Strategy.Stockpicker.eodLong = eod;
		} else {
			this.Strategy.Stockpicker.eodShort = eod;
		}
		
		if(direction == Directions.Long) {
			Strategy.Stockpicker.LongEntryExists = (entry != null);
		} else {
			Strategy.Stockpicker.ShortEntryExists = (entry != null);
		}
	}

}
