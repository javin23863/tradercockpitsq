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

@ClassConfig(name="Exclude trades with lowest profit", display="Exclude #Trades# trades with lowest profit")
@Help("Exclude trades with lowest profit")
public class ExcludeTradesWithLowestPl extends WhatIf {
	public static final Logger Log = LoggerFactory.getLogger(ExcludeTradesWithLowestPl.class);
	
	@Parameter(name="Trades", defaultValue="2", minValue=1, maxValue=10000, step=1)
	public int Trades;

	@Override
	public void filter(OrdersList orders) throws Exception {
		// sort orders by profit
		orders.sort(new ComparatorByProfit(PlTypes.Money));
		
		int count = 0;

		// remove X first orders
		for(ObjectListIterator<Order> i = orders.listIterator(orders.size()); i.hasPrevious();) {
			Order order = i.previous(); 	
			  
			if(order.isBalanceOrder()) continue;
			if(count==Trades) break;
			
			i.remove();
			count++;
		}
	}
}