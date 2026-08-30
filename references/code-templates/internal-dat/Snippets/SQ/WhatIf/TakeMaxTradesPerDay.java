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
import com.strategyquant.tradinglib.results.stats.comparator.OrderComparatorByOpenTime;

import it.unimi.dsi.fastutil.objects.ObjectListIterator;

@ClassConfig(name="Take maximum trades per day", display="Take maximum #Trades# trades per day")
@Help("Take maximum trades per day")
public class TakeMaxTradesPerDay extends WhatIf {
	public static final Logger Log = LoggerFactory.getLogger(TakeMaxTradesPerDay.class);

	@Parameter(name="Trades", defaultValue="2", minValue=1, maxValue=10000, step=1)
	public int Trades;
	
	@Override
	public void filter(OrdersList orders) throws Exception {		
		long lastDate = -1;
		long date = -1;
		int count = 1;
		
		orders.sort(new OrderComparatorByOpenTime());
		
		for(ObjectListIterator<Order> i = orders.listIterator(); i.hasNext();) {
			Order order = i.next(); 	
			 
			if(order.isBalanceOrder()) {
				continue;
			}
			
			date = SQTime.getDateInMs(order.OpenTime);
			   
			if(date==lastDate) {
				if(count==Trades) { // reached max allowed trades per day, remove the order
					i.remove();                         
					
					continue;
				}

				// the trade was opened at the same day, increase counter
				count++;
			} else {
				// new day, reset the counter
				count = 1;
			}
			   
			lastDate = date;
		}
		
	}
}