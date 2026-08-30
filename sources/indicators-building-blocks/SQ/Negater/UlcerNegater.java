package SQ.Negater;

import SQ.Blocks.Indicators.UlcerIndex.UlcerIndex;
import com.strategyquant.tradinglib.BlockDefinitionException;
import com.strategyquant.tradinglib.Blocks;
import com.strategyquant.tradinglib.IBlock;
import com.strategyquant.tradinglib.Negater;
import com.strategyquant.tradinglib.NegatersList;
import com.strategyquant.tradinglib.ParametersHelper;
import com.strategyquant.tradinglib.SortOrder;
import com.strategyquant.tradinglib.StrategyBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SortOrder(900000)
public class UlcerNegater extends Negater {
   public static final Logger Log = LoggerFactory.getLogger("UlcerNegater");

   public IBlock negate(NegatersList var1, IBlock var2, int var3, int var4, StrategyBase var5) throws BlockDefinitionException {
      if (!(var2 instanceof UlcerIndex)) {
         return null;
      }

      IBlock var6 = Blocks.getOppositeBlock(var2);
      ParametersHelper.negateParametersInClonedBlock(var2, var6, var1, var5);
      ParametersHelper.negateDataSeries(var2, var6);
      UlcerIndex var7 = (UlcerIndex)var2;
      UlcerIndex var8 = (UlcerIndex)var6;
      var8.Mode = var7.Mode == 1 ? 2 : 1;
      return var6;
   }
}
