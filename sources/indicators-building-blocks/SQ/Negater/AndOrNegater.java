package SQ.Negater;

import com.strategyquant.tradinglib.BlockDefinitionException;
import com.strategyquant.tradinglib.Blocks;
import com.strategyquant.tradinglib.IBlock;
import com.strategyquant.tradinglib.Negater;
import com.strategyquant.tradinglib.NegatersList;
import com.strategyquant.tradinglib.ParametersHelper;
import com.strategyquant.tradinglib.SortOrder;
import com.strategyquant.tradinglib.StrategyBase;

@SortOrder(100)
public class AndOrNegater extends Negater {
   public IBlock negate(NegatersList var1, IBlock var2, int var3, int var4, StrategyBase var5) throws BlockDefinitionException {
      if (var3 != 2) {
         return null;
      }

      String var6 = var2.getClass().getSimpleName();
      if (!var6.equals("AND") && !var6.equals("OR")) {
         return null;
      }

      IBlock var7 = Blocks.get(var6);
      IBlock[] var8 = (IBlock[])ParametersHelper.getParameterValue(var2, "Children");
      IBlock[] var9 = new IBlock[var8.length];

      for (int var10 = 0; var10 < var8.length; var10++) {
         var9[var10] = var1.negate(var8[var10], var5);
      }

      ParametersHelper.setParameterValue(var7, "Children", var9);
      return var7;
   }
}
