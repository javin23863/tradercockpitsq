package SQ.WhatIf;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.strategyquant.lib.SQTime;
import com.strategyquant.tradinglib.ClassConfig;
import com.strategyquant.tradinglib.Help;
import com.strategyquant.tradinglib.Order;
import com.strategyquant.tradinglib.OrdersList;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.WhatIf;

import it.unimi.dsi.fastutil.objects.ObjectListIterator;

@ClassConfig(name="Exclude short trades", display="Exclude #TradesPct#% trades which takes less than #Minutes# minutes")
@Help("Exclude short trades")
public class ExcludeShortTrades extends WhatIf {
	public static final Logger Log = LoggerFactory.getLogger(ExcludeShortTrades.class);
	
	@Parameter(name="% Trades", defaultValue="5", minValue=1, maxValue=100, step=1)
	public int TradesPct;
	
	@Parameter(name="Minutes", defaultValue="10", minValue=1, maxValue=100000, step=1)
	public int Minutes;


	@Override
	public void filter(OrdersList orders) throws Exception {
		int count = (int) Math.ceil((orders.size()) * ((TradesPct)/100d));
		int removed = 0;

		// remove X first orders
		for(ObjectListIterator<Order> i = orders.listIterator(); i.hasNext();) {
			Order order = i.next(); 	
					
			if(order.isBalanceOrder()) continue;
			if(count==removed) break;
			
			int minutes = SQTime.getMinutesBetween(order.OpenTime, order.CloseTime);
			if(minutes < Minutes) {			
				i.remove();
				removed++;
			}
		}
	}
}