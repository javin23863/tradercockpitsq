package com.strategyquant.tradinglib;

import com.strategyquant.lib.IRandomGenerator;
import com.strategyquant.lib.utils.ISQCloneable;
import com.strategyquant.tradinglib.propertygrid.ParametersTableItem;

public abstract class MonteCarloManipulation extends ParametersTableItem implements ISQCloneable<MonteCarloManipulation> {
   public abstract void modifyTrades(IRandomGenerator var1, OrdersList var2) throws Exception;

   public MonteCarloManipulation getClone() throws Exception {
      return this;
   }
}
