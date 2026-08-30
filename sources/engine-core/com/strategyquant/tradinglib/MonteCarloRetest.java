package com.strategyquant.tradinglib;

import com.strategyquant.datalib.TickEvent;
import com.strategyquant.datalib.data.io.VersatileData;
import com.strategyquant.lib.IRandomGenerator;
import com.strategyquant.lib.SettingsMap;
import com.strategyquant.lib.utils.ISQCloneable;
import com.strategyquant.tradinglib.propertygrid.ParametersTableItem;

public abstract class MonteCarloRetest extends ParametersTableItem implements ISQCloneable<MonteCarloRetest> {
   private final int type;
   protected SettingsMap settings;

   public MonteCarloRetest(int var1) {
      this.type = var1;
   }

   public int getType() {
      return this.type;
   }

   public void modifySettings(IRandomGenerator var1, SettingsMap var2) throws Exception {
   }

   public void modifyData(IRandomGenerator var1, TickEvent var2, double var3) {
   }

   public abstract MonteCarloRetest getClone() throws Exception;

   public void initSettings(SettingsMap var1) {
      this.settings = this.settings;
   }

   public void modifyOHLCData(IRandomGenerator var1, VersatileData var2, double var3) {
   }
}
