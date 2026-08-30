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

@ClassConfig(name="Trade only in months", display="Trade only in months")
@Help("Trade only in months")
public class ByMonths extends WhatIf {
	public static final Logger Log = LoggerFactory.getLogger(ByMonths.class);
	
	@Parameter(name="January", defaultValue="true")
	public boolean January;

	@Parameter(name="February", defaultValue="true")
	public boolean February;
	
	@Parameter(name="March", defaultValue="true")
	public boolean March;
	
	@Parameter(name="April", defaultValue="true")
	public boolean April;
	
	@Parameter(name="May", defaultValue="true")
	public boolean May;
	
	@Parameter(name="June", defaultValue="true")
	public boolean June;
	
	@Parameter(name="July", defaultValue="true")
	public boolean July;
	
	@Parameter(name="August", defaultValue="true")
	public boolean August;
	
	@Parameter(name="September", defaultValue="true")
	public boolean September;
	
	@Parameter(name="October", defaultValue="true")
	public boolean October;
	
	@Parameter(name="November", defaultValue="true")
	public boolean November;
	
	@Parameter(name="December", defaultValue="true")
	public boolean December;
	
	@Override
	public void filter(OrdersList orders) throws Exception {
		for(ObjectListIterator<Order> i = orders.listIterator(); i.hasNext();) {
			Order order = i.next(); 	
			  
			int month = SQTime.getMonthOriginal(order.OpenTime);
			if((!January && month==1) ||
				(!February && month==2) ||
				(!March && month==3) ||
				(!April && month==4) ||
				(!May && month==5) ||
				(!June && month==6) ||
				(!July && month==7) ||
				(!August && month==8) ||
				(!September && month==9) ||
				(!October && month==10) ||
				(!November && month==11) ||
				(!December && month==12)) {
				i.remove();
			}
		}
	}
}
