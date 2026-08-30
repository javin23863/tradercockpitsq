package SQ.Negater;

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

@SortOrder(1000000)
public class DefaultNegater extends Negater {
   public static final Logger Log = LoggerFactory.getLogger("DefaultNegater");

   public IBlock negate(NegatersList var1, IBlock var2, int var3, int var4, StrategyBase var5) throws BlockDefinitionException {
      IBlock var6 = Blocks.getOppositeBlock(var2);
      ParametersHelper.negateParametersInClonedBlock(var2, var6, var1, var5);
      ParametersHelper.negateDataSeries(var2, var6);
      return var6;
   }
}
