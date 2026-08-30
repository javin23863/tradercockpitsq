package SQ.Negater;

import SQ.Blocks.Comparisons.LeftRightComparisonBlockAbstract;
import SQ.Blocks.Other.Number;
import SQ.Internal.BlockUtils;
import com.strategyquant.tradinglib.BlockDefinitionException;
import com.strategyquant.tradinglib.Blocks;
import com.strategyquant.tradinglib.IBlock;
import com.strategyquant.tradinglib.Negater;
import com.strategyquant.tradinglib.NegatersList;
import com.strategyquant.tradinglib.ParametersHelper;
import com.strategyquant.tradinglib.SortOrder;
import com.strategyquant.tradinglib.StrategyBase;

@SortOrder(1000)
public class OscillatorComparisonsNegater extends Negater {
   public IBlock negate(NegatersList var1, IBlock var2, int var3, int var4, StrategyBase var5) throws BlockDefinitionException {
      if (var3 != 2) {
         return null;
      }

      if (!BlockUtils.isLeftRightBlock(var2)) {
         return null;
      }

      IBlock var6 = Blocks.getOppositeBlock(var2);
      if (!BlockUtils.isLeftRightBlock(var6)) {
         return null;
      }

      LeftRightComparisonBlockAbstract var7 = (LeftRightComparisonBlockAbstract)var2;
      LeftRightComparisonBlockAbstract var8 = (LeftRightComparisonBlockAbstract)var6;
      if (var7.Left instanceof Number && this.isOscillatorIndicator(var7.Right)) {
         var8.Left = var7.Left.clone(true, var5);
         var8.Right = var7.Right.clone(true, var5);
         Number var15 = (Number)var8.Left;
         IBlock var16 = var7.Right;
         double var17 = this.getOscillatorMiddleValue(var16);
         double var18 = var17 - var15.Number;
         var15.Number = var17 + var18;
      } else {
         if (!this.isOscillatorIndicator(var7.Left) || !(var7.Right instanceof Number)) {
            return null;
         }

         var8.Left = var7.Left.clone(true, var5);
         var8.Right = var7.Right.clone(true, var5);
         Number var9 = (Number)var8.Right;
         IBlock var10 = var7.Left;
         double var11 = this.getOscillatorMiddleValue(var10);
         double var13 = var11 - var9.Number;
         var9.Number = var11 + var13;
      }

      ParametersHelper.negateDataSeries(var2, var8);
      return var8;
   }
}
