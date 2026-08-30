package com.strategyquant.tradinglib.order;

import com.strategyquant.tradinglib.ILiveOrder;
import java.util.Iterator;

public interface ILiveOrdersList extends Iterable<ILiveOrder> {
   @Override
   Iterator<ILiveOrder> iterator();

   int size();
}
