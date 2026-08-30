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

@ClassConfig(name="Trade only in hours", display="Trade only in hours")
@Help("Trade only in hours")
public class ByHours extends WhatIf {
	public static final Logger Log = LoggerFactory.getLogger(ByHours.class);
	
	@Parameter(name="Hour 0", defaultValue="true")
	public boolean H0;

	@Parameter(name="Hour 1", defaultValue="true")
	public boolean H1;
	
	@Parameter(name="Hour 2", defaultValue="true")
	public boolean H2;
	
	@Parameter(name="Hour 3", defaultValue="true")
	public boolean H3;
	
	@Parameter(name="Hour 4", defaultValue="true")
	public boolean H4;
	
	@Parameter(name="Hour 5", defaultValue="true")
	public boolean H5;
	
	@Parameter(name="Hour 6", defaultValue="true")
	public boolean H6;
	
	@Parameter(name="Hour 7", defaultValue="true")
	public boolean H7;

	@Parameter(name="Hour 8", defaultValue="true")
	public boolean H8;
	
	@Parameter(name="Hour 9", defaultValue="true")
	public boolean H9;
	
	@Parameter(name="Hour 10", defaultValue="true")
	public boolean H10;
	
	@Parameter(name="Hour 11", defaultValue="true")
	public boolean H11;
	
	@Parameter(name="Hour 12", defaultValue="true")
	public boolean H12;
	
	@Parameter(name="Hour 13", defaultValue="true")
	public boolean H13;
	
	@Parameter(name="Hour 14", defaultValue="true")
	public boolean H14;

	@Parameter(name="Hour 15", defaultValue="true")
	public boolean H15;
	
	@Parameter(name="Hour 16", defaultValue="true")
	public boolean H16;
	
	@Parameter(name="Hour 17", defaultValue="true")
	public boolean H17;
	
	@Parameter(name="Hour 18", defaultValue="true")
	public boolean H18;
	
	@Parameter(name="Hour 19", defaultValue="true")
	public boolean H19;
	
	@Parameter(name="Hour 20", defaultValue="true")
	public boolean H20;
	
	@Parameter(name="Hour 21", defaultValue="true")
	public boolean H21;

	@Parameter(name="Hour 22", defaultValue="true")
	public boolean H22;
	
	@Parameter(name="Hour 23", defaultValue="true")
	public boolean H23;
	
	@Override
	public void filter(OrdersList orders) throws Exception {
		for(ObjectListIterator<Order> i = orders.listIterator(); i.hasNext();) {
			Order order = i.next(); 	
			  
			int hour = SQTime.getHour(order.OpenTime);
			if((!H0 && hour==0) ||
				(!H1 && hour==1) ||
				(!H2 && hour==2) ||
				(!H3 && hour==3) ||
				(!H4 && hour==4) ||
				(!H5 && hour==5) ||
				(!H6 && hour==6) ||
				(!H7 && hour==7) ||
				(!H8 && hour==8) ||
				(!H9 && hour==9) ||
				(!H10 && hour==10) ||
				(!H11 && hour==11) ||
				(!H12 && hour==12) ||
				(!H13 && hour==13) ||
				(!H14 && hour==14) ||
				(!H15 && hour==15) ||
				(!H16 && hour==16) ||
				(!H17 && hour==17) ||
				(!H18 && hour==18) ||
				(!H19 && hour==19) ||
				(!H20 && hour==20) ||
				(!H21 && hour==21) ||
				(!H22 && hour==22) ||
				(!H23 && hour==23)) {
				i.remove();
			}
		}
	}
}