package SQ.Negater;

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
public class PivotsNegater extends Negater {
   public static final Logger Log = LoggerFactory.getLogger("PivotsNegater");

   public IBlock negate(NegatersList var1, IBlock var2, int var3, int var4, StrategyBase var5) throws BlockDefinitionException {
      IBlock var6 = Blocks.getOppositeBlock(var2);
      if (!var2.getClass().getSimpleName().equals("Pivots")) {
         return null;
      }

      ParametersHelper.negateParametersInClonedBlock(var2, var6, var1, var5);
      ParametersHelper.negateDataSeries(var2, var6);
      Field var7 = this.getOutputIndexField(var2);
      Field var8 = this.getOutputIndexField(var6);
      if (var7 != null && var8 != null) {
         try {
            int var9 = var7.getInt(var2);
            byte var10 = 0;
            switch (var9) {
               case 0:
                  var10 = 0;
                  break;
               case 1:
                  var10 = 4;
                  break;
               case 2:
                  var10 = 5;
                  break;
               case 3:
                  var10 = 6;
                  break;
               case 4:
                  var10 = 1;
                  break;
               case 5:
                  var10 = 2;
                  break;
               case 6:
                  var10 = 3;
            }

            var8.set(var6, Integer.valueOf(var10));
            return var6;
         } catch (IllegalArgumentException | IllegalAccessException var11) {
            throw new BlockDefinitionException("Exception setting value of parameter OutputIndex found!", var11);
         }
      } else {
         return null;
      }
   }
}
