package com.strategyquant.tradinglib;

import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.TimePeriods;
import com.strategyquant.lib.utils.ISQCloneable;
import com.strategyquant.tradinglib.debug.Debugger;
import java.lang.reflect.Constructor;

public abstract class CorrelationType extends Debugger implements ISQCloneable<CorrelationType> {
   public static final int DATA_TYPE_PL = 5;
   public static final int DATA_TYPE_TRADES = 10;
   protected String name = null;
   protected int dataType = -1;

   public String getName() {
      return this.name;
   }

   public int getDataType() {
      return this.dataType;
   }

   @Override
   public String toString() {
      return this.name;
   }

   public abstract void computePeriods(OrdersList var1, int var2, TimePeriods var3) throws Exception;

   public boolean isCanceledOrder(Order var1) {
      return var1.isCanceledOrder() && var1.PL == 0.0F;
   }

   public boolean shouldSkipPeriod(double var1, double var3) {
      return false;
   }

   public CorrelationType getClone() {
      try {
         Constructor var1 = null;
         var1 = this.getClass().getConstructor();
         CorrelationType var2 = (CorrelationType)SQUtils.invokeUnchecked(var1, new Object[0]);
         var2.name = this.name;
         var2.dataType = this.dataType;
         return var2;
      } catch (NoSuchMethodException | SecurityException var3) {
         throw new IllegalArgumentException("Exception cloning CorrelationType!", var3);
      }
   }
}
