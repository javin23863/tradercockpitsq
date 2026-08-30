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

@ClassConfig(name="Exclude overlapping trades", display="Exclude overlapping trades")
@Help("Exclude overlapping trades")
public class ExludeOverlappingTrades extends WhatIf {
	public static final Logger Log = LoggerFactory.getLogger(ExludeOverlappingTrades.class);

	@Override
	public void filter(OrdersList orders) throws Exception {
		long closeTime = -1;

		orders.sort(new OrderComparatorByOpenTime());
		
		for(ObjectListIterator<Order> i = orders.listIterator(); i.hasNext();) {
			Order order = i.next(); 	
			  
			if(closeTime==-1) {
				closeTime = order.CloseTime;
			} else {
				if(order.OpenTime>closeTime) {
					closeTime = order.CloseTime;
				   
					continue;
				} 
			   
				// when we are here it means that current trade opened before previous trade closed,
				// which means the trades are overlapping and current trade should be skipped 
				i.remove();
		   }
		}
	}
}