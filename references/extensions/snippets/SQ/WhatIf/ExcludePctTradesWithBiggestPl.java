package SQ.WhatIf;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.strategyquant.tradinglib.ClassConfig;
import com.strategyquant.tradinglib.Help;
import com.strategyquant.tradinglib.Order;
import com.strategyquant.tradinglib.OrdersList;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.PlTypes;
import com.strategyquant.tradinglib.WhatIf;

import SQ.Functions.ComparatorByProfit;
import it.unimi.dsi.fastutil.objects.ObjectListIterator;

@ClassConfig(name="Exclude % of trades with biggest profit", display="Exclude #TradesPct#% trades with biggest profit")
@Help("Exclude % of trades with biggest profit")
public class ExcludePctTradesWithBiggestPl extends WhatIf {
	public static final Logger Log = LoggerFactory.getLogger(ExcludePctTradesWithBiggestPl.class);
	
	@Parameter(name="% Trades", defaultValue="5", minValue=1, maxValue=100, step=1)
	public int TradesPct;

	@Override
	public void filter(OrdersList orders) throws Exception {
		// sort orders by profit
		orders.sort(new ComparatorByProfit(PlTypes.Money));
		
		int count = (int) Math.ceil((orders.size()) * ((TradesPct)/100d));
		int removed = 0;
			
		// remove X first orders
		for(ObjectListIterator<Order> i = orders.listIterator(); i.hasNext();) {
			Order order = i.next(); 	
			  
			if(order.isBalanceOrder()) continue;
			if(count==removed) break;
			
			i.remove();
			removed++;
		}
	}
}