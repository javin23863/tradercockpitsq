package SQ.Negater;

import SQ.Blocks.Indicators.Fractal.Fractal;
import com.strategyquant.tradinglib.BlockDefinitionException;
import com.strategyquant.tradinglib.Blocks;
import com.strategyquant.tradinglib.IBlock;
import com.strategyquant.tradinglib.Negater;
import com.strategyquant.tradinglib.NegatersList;
import com.strategyquant.tradinglib.ParametersHelper;
import com.strategyquant.tradinglib.SortOrder;
import com.strategyquant.tradinglib.StrategyBase;
import java.lang.reflect.Field;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SortOrder(900000)
public class FractalNegater extends Negater {
   public static final Logger Log = LoggerFactory.getLogger("FiboNegater");

   public IBlock negate(NegatersList var1, IBlock var2, int var3, int var4, StrategyBase var5) throws BlockDefinitionException {
      if (!(var2 instanceof Fractal)) {
         return null;
      }

      IBlock var6 = Blocks.getOppositeBlock(var2);
      ParametersHelper.negateParametersInClonedBlock(var2, var6, var1, var5);
      ParametersHelper.negateDataSeries(var2, var6);
      Fractal var7 = (Fractal)var6;
      Field var8 = this.getOutputIndexField(var2);
      Field var9 = this.getOutputIndexField(var6);
      if (var8 != null && var9 != null) {
         try {
            int var10 = var8.getInt(var2);
            int var11 = var10 == 1 ? 0 : 1;
            var9.set(var6, var11);
            return var7;
         } catch (IllegalArgumentException | IllegalAccessException var12) {
            throw new BlockDefinitionException("Exception setting value of parameter OutputIndex found!", var12);
         }
      } else {
         return null;
      }
   }
}
