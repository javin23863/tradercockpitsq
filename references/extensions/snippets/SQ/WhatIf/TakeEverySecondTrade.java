package SQ.WhatIf;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.strategyquant.tradinglib.ClassConfig;
import com.strategyquant.tradinglib.Help;
import com.strategyquant.tradinglib.Order;
import com.strategyquant.tradinglib.OrdersList;
import com.strategyquant.tradinglib.WhatIf;
import com.strategyquant.tradinglib.results.stats.comparator.OrderComparatorByOpenTime;

import it.unimi.dsi.fastutil.objects.ObjectListIterator;

@ClassConfig(name="Take every second trade", display="Take every second trade")
@Help("Take every second trade")
public class TakeEverySecondTrade extends WhatIf {
	public static final Logger Log = LoggerFactory.getLogger(TakeEverySecondTrade.class);

	@Override
	public void filter(OrdersList orders) throws Exception {
		boolean remove = false;
		
		orders.sort(new OrderComparatorByOpenTime());
		
		for(ObjectListIterator<Order> i = orders.listIterator(); i.hasNext();) {
			Order order = i.next(); 	
			  
		   if(remove) {
			   i.remove();
		   }
		   
		   remove = !remove;
		}
	}
}