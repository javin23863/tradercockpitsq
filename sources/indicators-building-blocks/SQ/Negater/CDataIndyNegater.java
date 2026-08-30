package SQ.Negater;

import SQ.Internal.CDataIndy;
import com.strategyquant.tradinglib.BlockDefinitionException;
import com.strategyquant.tradinglib.IBlock;
import com.strategyquant.tradinglib.Negater;
import com.strategyquant.tradinglib.NegatersList;
import com.strategyquant.tradinglib.SortOrder;
import com.strategyquant.tradinglib.StrategyBase;

@SortOrder(1000)
public class CDataIndyNegater extends Negater {
   public IBlock negate(NegatersList var1, IBlock var2, int var3, int var4, StrategyBase var5) throws BlockDefinitionException {
      if (!(var2 instanceof CDataIndy)) {
         return null;
      }

      CDataIndy var6 = ((CDataIndy)var2).cloneCDataIndy();
      var6.negateValueIndex();
      return var6;
   }
}
