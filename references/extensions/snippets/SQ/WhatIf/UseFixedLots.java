package SQ.WhatIf;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.strategyquant.tradinglib.ClassConfig;
import com.strategyquant.tradinglib.Help;
import com.strategyquant.tradinglib.Order;
import com.strategyquant.tradinglib.OrdersList;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.WhatIf;

import it.unimi.dsi.fastutil.objects.ObjectListIterator;

@ClassConfig(name="Use fixed lots", display="Use #Size# fixed lots")
@Help("Use fixed lots")
public class UseFixedLots extends WhatIf {
	public static final Logger Log = LoggerFactory.getLogger(UseFixedLots.class);
	
	@Parameter(name="Size", defaultValue="0.1", minValue=0.01, maxValue=100, step=0.1)
	public double Size;
	
	@Override
	public void filter(OrdersList orders) throws Exception {
		for(ObjectListIterator<Order> i = orders.listIterator(); i.hasNext();) {
			Order order = i.next(); 	
			  
			// recompute P/L for the new size
            order.PL = (order.PL/order.Size)*(float)Size;

			// set order size to the configured value			
			order.Size = (float)Size;
		}
	}
}