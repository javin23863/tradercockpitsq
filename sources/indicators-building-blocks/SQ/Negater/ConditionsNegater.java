package SQ.Negater;

import com.strategyquant.tradinglib.BlockDefinitionException;
import com.strategyquant.tradinglib.Blocks;
import com.strategyquant.tradinglib.IBlock;
import com.strategyquant.tradinglib.Negater;
import com.strategyquant.tradinglib.NegatersList;
import com.strategyquant.tradinglib.OppositeBlock;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.ParametersHelper;
import com.strategyquant.tradinglib.SortOrder;
import com.strategyquant.tradinglib.StrategyBase;
import com.strategyquant.tradinglib.Variable;
import com.strategyquant.tradinglib.Variables;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SortOrder(10000)
public class ConditionsNegater extends Negater {
   private static final Logger Log = LoggerFactory.getLogger("ConditionsNegater");

   public IBlock negate(NegatersList var1, IBlock var2, int var3, int var4, StrategyBase var5) throws BlockDefinitionException {
      if (var3 != 3) {
         return null;
      }

      IBlock var6 = Blocks.getOppositeBlock(var2);
      String var7 = null;
      Number var8 = 0;
      double var10 = 0.0;
      OppositeBlock var12 = var2.getClass().getAnnotation(OppositeBlock.class);
      if (var12 != null) {
         String var13 = var12.value();
         if (var12.oscillator() && var13.equals(var6.getClass().getSimpleName())) {
            double var14 = var12.middleValue();

            try {
               Object var9 = ParametersHelper.getParameterValue(var2, var12.field());
               if (var9 instanceof Double) {
                  var10 = (Double)var9;
                  var7 = var12.field();
               } else if (var9 instanceof Integer) {
                  var10 = ((Integer)var9).intValue();
                  var7 = var12.field();
               }

               if (var7 != null) {
                  double var16 = var14 - var10;
                  var10 = var14 + var16;
                  if (var9 instanceof Double) {
                     var8 = new Double(var10);
                  } else if (var9 instanceof Integer) {
                     var8 = new Integer((int)var10);
                  }
               }
            } catch (BlockDefinitionException var27) {
               Log.error("Cannot find field {} in block {}", var12.field(), var2.getClass().getSimpleName());
            }
         }
      }

      Class var30 = var6.getClass();

      for (Field var17 : var30.getFields()) {
         for (Annotation var21 : var17.getAnnotations()) {
            if (var21 instanceof Parameter) {
               String var22 = var17.getName();
               Object var28;
               if (var7 != null && var22.equals(var7)) {
                  var28 = var8;
               } else {
                  var28 = ParametersHelper.getParameterValue(var2, var22);
                  if (var28 instanceof IBlock) {
                     var28 = ((IBlock)var28).clone(true, var5);
                  }
               }

               try {
                  var17.set(var6, var28);
                  StrategyBase var23 = var2.getStrategy();
                  if (var23 != null) {
                     Variables var24 = var23.variables();
                     if (var24 != null) {
                        Variable var25 = var24.getByField(var2, var22);
                        if (var25 != null) {
                           var25.registerAttachedField(var6, var17);
                        }
                     }
                  }
               } catch (IllegalArgumentException | IllegalAccessException var26) {
                  throw new BlockDefinitionException("Exception getting value of parameter '" + var17.getName() + "' found!", var26);
               }
            }
         }
      }

      ParametersHelper.negateDataSeries(var2, var6);
      return var6;
   }
}
