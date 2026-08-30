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

@ClassConfig(name="Trade only in days", display="Trade only in days")
@Help("Trade only in days")
public class ByDays extends WhatIf {
	public static final Logger Log = LoggerFactory.getLogger(ByDays.class);
	
	@Parameter(name="Monday", defaultValue="true")
	public boolean Monday;

	@Parameter(name="Tuesday", defaultValue="true")
	public boolean Tuesday;
	
	@Parameter(name="Wednesday", defaultValue="true")
	public boolean Wednesday;
	
	@Parameter(name="Thursday", defaultValue="true")
	public boolean Thursday;
	
	@Parameter(name="Friday", defaultValue="true")
	public boolean Friday;
	
	@Parameter(name="Saturday", defaultValue="true")
	public boolean Saturday;
	
	@Parameter(name="Sunday", defaultValue="true")
	public boolean Sunday;
	
	@Override
	public void filter(OrdersList orders) throws Exception {
		for(ObjectListIterator<Order> i = orders.listIterator(); i.hasNext();) {
			Order order = i.next(); 	
			  
			int day = SQTime.getDayOfWeek(order.OpenTime);
			if((!Monday && day==1) ||
				(!Tuesday && day==2) ||
				(!Wednesday && day==3) ||
				(!Thursday && day==4) ||
				(!Friday && day==5) ||
				(!Saturday && day==6) ||
				(!Sunday && day==7)) {
				i.remove();
			}
		}
	}
}