package SQ.Functions;

import SQ.Internal.Strategy;
import SQ.Internal.XmlStrategy;

import com.strategyquant.lib.*;
import com.strategyquant.datalib.*;
import com.strategyquant.tradinglib.*;

public class OrderFunctions {

	public static boolean identify(ILiveOrder order, Strategy Strategy, String Symbol, int Direction, int MagicNumber, String Comment) {

		if(!order.getStrategyName().equals(Strategy.getStrategyName())) {
			return false;
		}
		
		if(Symbol.equals("Any")) {
			
		} else if(Symbol.equals("Current")) {
			if(!order.getSymbol().equals(Strategy.MarketData.Chart(0).Symbol)) return false;
		} else {
			if(!order.getSymbol().equals(Symbol)) return false;
		}
		
		if(Direction != 0 && order.getDirection() != Direction) return false;

		if(MagicNumber != 0 && order.getMagicNumber() != MagicNumber) return false;
		
		if(Comment != null && !Comment.equals("") && !order.getComment().contains(Comment)) return false;

		return true;
	}

	//------------------------------------------------------------------------
	
	public static boolean identify(Order order, Strategy Strategy, String Symbol, int Direction, int MagicNumber, String Comment) {
		if(!order.StrategyName.equals(Strategy.getStrategyName())) {
			return false;
		}
		
		if(Symbol.equals("Any")) {

		} else if(Symbol.equals("Current")) {
			if(!order.Symbol.equals(Strategy.MarketData.Chart(0).Symbol)) return false;
		} else {
			if(!order.Symbol.equals(Symbol)) return false;
		}
		
		if(Direction != 0 && order.getDirection() != Direction) return false;

		if(MagicNumber != 0 && order.MagicNumber != MagicNumber) return false;
		
		if(Comment != null && !Comment.equals("") && !order.Comment.contains(Comment)) return false;

		return true;
	}

	//------------------------------------------------------------------------

	public static ILiveOrder findLiveOrder(Strategy Strategy, String Symbol, int Direction, int MagicNumber, String Comment) {
		for(int i=0; i<Strategy.Trader.getOpenOrdersCount(true); i++) {
			ILiveOrder order = Strategy.Trader.getOpenOrder(i, true);
			
			if(OrderFunctions.identify(order, Strategy, Symbol, Direction, MagicNumber, Comment)) {
				return order;
			}
		}
		
		return null;
	}

	//------------------------------------------------------------------------

	/**
	 * Returns first order in history that meets criteria specified in arguments
	 * 
	 * @param Strategy
	 * @param Symbol
	 * @param Direction
	 * @param MagicNumber
	 * @param Comment
	 * @return
	 */
	public static Order findHistoryOrder(Strategy Strategy, String Symbol, int Direction, int MagicNumber, String Comment) {
		for(int i=0; i<Strategy.Trader.getHistoryOrdersCount(); i++) {
			Order order = Strategy.Trader.getHistoryOrder(i);
			
			if(OrderFunctions.identify(order, Strategy, Symbol, Direction, MagicNumber, Comment)) {
				return order;
			}
		}
		
		return null;
	}	

	//------------------------------------------------------------------------
	
	/**
	 * Returns last order in history that meets criteria specified in arguments
	 * 
	 * @param Strategy
	 * @param Symbol
	 * @param Direction
	 * @param MagicNumber
	 * @param Comment
	 * @return
	 */
	public static Order findLastHistoryOrder(Strategy Strategy, String Symbol, int Direction, int MagicNumber, String Comment) {
		for(int i=Strategy.Trader.getHistoryOrdersCount() - 1; i>=0; i--) {
			Order order = Strategy.Trader.getHistoryOrder(i);
			
			if(OrderFunctions.identify(order, Strategy, Symbol, Direction, MagicNumber, Comment)) {
				return order;
			}
		}
		
		return null;
	}	
}
