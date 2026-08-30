package SQ.WhatIf;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.strategyquant.tradinglib.ClassConfig;
import com.strategyquant.tradinglib.Help;
import com.strategyquant.tradinglib.Order;
import com.strategyquant.tradinglib.OrdersList;
import com.strategyquant.tradinglib.WhatIf;
import it.unimi.dsi.fastutil.objects.ObjectListIterator;

@ClassConfig(name="Remove balance transactions", display="Remove balance transactions")
@Help("Remove balance transactions")
public class RemoveBalanceTransactions extends WhatIf {
	public static final Logger Log = LoggerFactory.getLogger(RemoveBalanceTransactions.class);

	@Override
	public void filter(OrdersList orders) throws Exception {
		for(ObjectListIterator<Order> i = orders.listIterator(); i.hasNext();) {
			Order order = i.next(); 	
			  
			if(order.isBalanceOrder()) {
				i.remove();
			}
		}
	}
}