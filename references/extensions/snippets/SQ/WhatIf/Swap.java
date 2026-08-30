package SQ.WhatIf;

import java.util.HashMap;

import com.strategyquant.tradinglib.*;
import com.strategyquant.tradinglib.swap.SwapCalculator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.strategyquant.datalib.DataInfo;
import com.strategyquant.datalib.InstrumentInfo;
import com.strategyquant.datalib.data.DataException;
import com.strategyquant.datalib.data.DataManager;
import com.strategyquant.datalib.instrument.InstrumentManager;
import com.strategyquant.lib.SQTime;

import it.unimi.dsi.fastutil.objects.ObjectListIterator;

@ClassConfig(name="Swap", display="Swap (Long: #swapLongInput#,Short: #swapShortInput#)")
@Help("Simulate swap")
public class Swap extends WhatIf {
	public static final Logger Log = LoggerFactory.getLogger(Swap.class);

	@Parameter(name="SwapDev Long", defaultValue="10", minValue=-1000, maxValue=1000, step=0.01)
	public double swapLongInput;

	@Parameter(name="SwapDev Short", defaultValue="-10", minValue=-1000, maxValue=1000, step=0.01)
	public double swapShortInput;

	private SwapMethod swapMethod;

	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	
	@Override
	public void filter(OrdersList orders) throws Exception {
		if(swapMethod == null){
			swapMethod = new SwapMethod(true, SwapTypes.MONEY, swapLongInput, swapShortInput, TripleSwapOptions.WEDNESDAY, "23:00");
		}

		for(ObjectListIterator<Order> i = orders.listIterator(); i.hasNext();) {
			Order order = i.next();
			order.CommSwap += SwapCalculator.calculate(order, swapMethod);
		}
	}
}