package SQ.Negater;

import com.strategyquant.tradinglib.BlockDefinitionException;
import com.strategyquant.tradinglib.ExitMethod;
import com.strategyquant.tradinglib.IBlock;
import com.strategyquant.tradinglib.Negater;
import com.strategyquant.tradinglib.NegatersList;
import com.strategyquant.tradinglib.ParametersHelper;
import com.strategyquant.tradinglib.SortOrder;
import com.strategyquant.tradinglib.StrategyBase;

@SortOrder(10000)
public class ActionsNegater extends Negater {
   public IBlock negate(NegatersList var1, IBlock var2, int var3, int var4, StrategyBase var5) throws BlockDefinitionException {
      if (var3 != 5) {
         return null;
      }

      IBlock var6 = var2.clone(true, var5);
      ParametersHelper.negateParametersInClonedBlock(var2, var6, var1, var5);
      ExitMethod[] var7 = (ExitMethod[])ParametersHelper.getFieldValue(var2, "ExitMethods");
      if (var7 != null) {
         ExitMethod[] var8 = new ExitMethod[var7.length];

         for (int var9 = 0; var9 < var7.length; var9++) {
            var8[var9] = (ExitMethod)var1.negate(var7[var9], var5);
         }

         ParametersHelper.setFieldValue(var6, "ExitMethods", var8);
      }

      try {
         int var11 = (Integer)ParametersHelper.getFieldValue(var6, "Direction");
         var11 *= -1;
         ParametersHelper.setFieldValue(var6, "Direction", var11);
      } catch (BlockDefinitionException var10) {
      }

      return var6;
   }
}
