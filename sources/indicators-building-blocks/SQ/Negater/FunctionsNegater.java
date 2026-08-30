package SQ.Negater;

import com.strategyquant.tradinglib.BlockDefinitionException;
import com.strategyquant.tradinglib.Blocks;
import com.strategyquant.tradinglib.IBlock;
import com.strategyquant.tradinglib.Negater;
import com.strategyquant.tradinglib.NegatersList;
import com.strategyquant.tradinglib.ParametersHelper;
import com.strategyquant.tradinglib.SortOrder;
import com.strategyquant.tradinglib.StrategyBase;

@SortOrder(9000)
public class FunctionsNegater extends Negater {
   public IBlock negate(NegatersList var1, IBlock var2, int var3, int var4, StrategyBase var5) throws BlockDefinitionException {
      if (var3 == 4 && var4 == 6) {
         IBlock var6 = Blocks.getOppositeBlock(var2);
         ParametersHelper.negateParametersInClonedBlock(var2, var6, var1, var5);
         ParametersHelper.negateDataSeries(var2, var6);
         return var6;
      } else {
         return null;
      }
   }
}
