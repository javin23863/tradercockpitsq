package com.strategyquant.tradinglib;

import com.strategyquant.datalib.TickEvent;
import com.strategyquant.lib.utils.ISQCloneable;
import com.strategyquant.tradinglib.options.TradingOptionsList;
import com.strategyquant.tradinglib.propertygrid.ParametersTableItem;
import java.util.List;

public abstract class TradingOption extends ParametersTableItem implements ISQCloneable<TradingOption> {
   protected boolean inbuild = false;

   public boolean isInbuild() {
      return this.inbuild;
   }

   public abstract boolean OnBarUpdate(StrategyBase var1) throws Exception;

   public abstract boolean isUsedInTrading();

   public void OnTick(StrategyBase var1, TickEvent var2, boolean var3) throws Exception {
   }

   public TradingOption getClone() throws CloneNotSupportedException {
      try {
         for (TradingOption var3 : TradingOptionsList.getInstance().getAvailableClasses()) {
            if (var3.getClass().equals(this.getClass())) {
               TradingOption var4 = (TradingOption)var3.getClass().newInstance();
               List var5 = var4.getParametersList();

               for (int var6 = 0; var6 < var5.size(); var6++) {
                  String var7 = (String)var5.get(var6);
                  var4.setParameterValue(var7, "" + this.getParameterValue(var7));
               }

               return var4;
            }
         }
      } catch (Exception var8) {
         throw new CloneNotSupportedException("Cloning TradingOption " + this.name + " failed. " + var8.getMessage());
      }

      throw new CloneNotSupportedException("Cloning failed. Cannot find TradingOption " + this.name);
   }
}
