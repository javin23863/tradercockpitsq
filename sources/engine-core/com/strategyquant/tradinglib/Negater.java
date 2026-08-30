package com.strategyquant.tradinglib;

import com.strategyquant.tradinglib.debug.Debugger;
import java.lang.reflect.Field;

public abstract class Negater extends Debugger {
   public abstract IBlock negate(NegatersList var1, IBlock var2, int var3, int var4, StrategyBase var5) throws BlockDefinitionException;

   protected double getOscillatorMiddleValue(IBlock var1) throws BlockDefinitionException {
      Indicator var2 = var1.getClass().getAnnotation(Indicator.class);
      if (var2 == null) {
         throw new BlockDefinitionException("Block '" + var1.getClass().getSimpleName() + "' is not an oscillator");
      } else {
         return var2.middleValue();
      }
   }

   protected boolean isOscillatorIndicator(IBlock var1) {
      Indicator var2 = var1.getClass().getAnnotation(Indicator.class);
      return var2 != null ? var2.oscillator() : false;
   }

   protected Field getOutputIndexField(IBlock var1) {
      Class var2 = var1.getClass();

      for (Field var6 : var2.getFields()) {
         if (var6.getName().equals("OutputIndex")) {
            return var6;
         }
      }

      return null;
   }
}
