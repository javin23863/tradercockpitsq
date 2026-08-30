package com.strategyquant.tradinglib.robustnesstests;

import com.strategyquant.datalib.TickEvent;
import com.strategyquant.lib.IRandomGenerator;
import com.strategyquant.lib.SettingsMap;
import com.strategyquant.tradinglib.OrdersList;
import com.strategyquant.tradinglib.propertygrid.ParametersTableItem;

public abstract class RobustnessTestMethod extends ParametersTableItem {
   private final int type;

   public RobustnessTestMethod(int var1) {
      this.type = var1;
   }

   public int getType() {
      return this.type;
   }

   public void modifyTrades(IRandomGenerator var1, OrdersList var2) throws Exception {
   }

   public void modifySettings(IRandomGenerator var1, SettingsMap var2) throws Exception {
   }

   public void modifyData(IRandomGenerator var1, TickEvent var2) {
   }
}
