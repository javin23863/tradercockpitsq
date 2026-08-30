package SQ.Negater;

import com.strategyquant.tradinglib.BlockDefinitionException;
import com.strategyquant.tradinglib.IBlock;
import com.strategyquant.tradinglib.Negater;
import com.strategyquant.tradinglib.NegatersList;
import com.strategyquant.tradinglib.ParametersHelper;
import com.strategyquant.tradinglib.SortOrder;
import com.strategyquant.tradinglib.StrategyBase;

@SortOrder(8000)
public class FormulaNegater extends Negater {
   public IBlock negate(NegatersList var1, IBlock var2, int var3, int var4, StrategyBase var5) throws BlockDefinitionException {
      if (var3 != 6) {
         return null;
      }

      IBlock var6 = var2.clone(false, var5);
      ParametersHelper.negateParametersInClonedBlock(var2, var6, var1, var5);
      return var6;
   }
}
