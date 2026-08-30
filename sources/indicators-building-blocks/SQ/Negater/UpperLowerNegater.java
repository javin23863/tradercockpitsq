package SQ.Negater;

import com.strategyquant.tradinglib.BlockDefinitionException;
import com.strategyquant.tradinglib.Blocks;
import com.strategyquant.tradinglib.IBlock;
import com.strategyquant.tradinglib.Negater;
import com.strategyquant.tradinglib.NegatersList;
import com.strategyquant.tradinglib.Output;
import com.strategyquant.tradinglib.ParametersHelper;
import com.strategyquant.tradinglib.SortOrder;
import com.strategyquant.tradinglib.StrategyBase;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;

@SortOrder(10100)
public class UpperLowerNegater extends Negater {
   public IBlock negate(NegatersList var1, IBlock var2, int var3, int var4, StrategyBase var5) throws BlockDefinitionException {
      if (!this.containsUpperLower(var2)) {
         return null;
      }

      IBlock var6 = Blocks.getOppositeBlock(var2);
      ParametersHelper.negateParametersInClonedBlock(var2, var6, var1, var5);
      Field var7 = this.getOutputIndexField(var2);
      Field var8 = this.getOutputIndexField(var6);
      if (var7 != null && var8 != null) {
         try {
            int var9 = var7.getInt(var2);
            int var10 = var9 == 0 ? 1 : 0;
            var8.set(var6, var10);
         } catch (IllegalArgumentException | IllegalAccessException var11) {
            throw new BlockDefinitionException("Exception setting value of parameter OutputIndex found!", var11);
         }

         ParametersHelper.negateDataSeries(var2, var6);
         return var6;
      } else {
         return null;
      }
   }

   public boolean containsUpperLower(IBlock var1) {
      new ArrayList();
      int var3 = 0;
      Class var4 = var1.getClass();

      for (Field var8 : var4.getFields()) {
         for (Annotation var12 : var8.getAnnotations()) {
            if (var12 instanceof Output && (var8.getName().equals("Upper") || var8.getName().equals("Lower"))) {
               var3++;
            }
         }
      }

      return var3 == 2;
   }
}
