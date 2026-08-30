package SQ.Negater;

import SQ.Blocks.Comparisons.LeftRightComparisonBlockAbstract;
import SQ.Blocks.Other.DoubleVariable;
import SQ.Blocks.Other.IntVariable;
import SQ.Blocks.Other.Number;
import SQ.Internal.BlockUtils;
import SQ.Internal.CBlock;
import com.strategyquant.tradinglib.BlockDefinitionException;
import com.strategyquant.tradinglib.IBlock;
import com.strategyquant.tradinglib.Negater;
import com.strategyquant.tradinglib.NegatersList;
import com.strategyquant.tradinglib.SortOrder;
import com.strategyquant.tradinglib.StrategyBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SortOrder(1000)
public class CBlockNegater extends Negater {
   private static final Logger Log = LoggerFactory.getLogger("CBlockNegater");

   public IBlock negate(NegatersList var1, IBlock var2, int var3, int var4, StrategyBase var5) throws BlockDefinitionException {
      if (!(var2 instanceof CBlock var6)) {
         return null;
      } else {
         CBlock var7 = var6.getOppositeBlock();
         IBlock var8 = var6.getContents();
         IBlock var9 = var7.getContents();
         if (BlockUtils.isLeftRightBlock(var8) && BlockUtils.isLeftRightBlock(var9)) {
            LeftRightComparisonBlockAbstract var10 = (LeftRightComparisonBlockAbstract)var9;
            if (this.isNumber(var10.Left) && this.isOscillatorIndicator(var10.Right)) {
               double var22 = this.getNumber(var10.Left);
               IBlock var23 = var10.Right;
               double var24 = this.getOscillatorMiddleValue(var23);
               double var25 = var24 - var22;
               double var26 = var22;
               double var27 = var24 + var25;
               this.applyBlockObjChange(var10.Left, var27);
               var7.applyParamChange(var26, var27);
            } else if (this.isOscillatorIndicator(var10.Left) && this.isNumber(var10.Right)) {
               double var11 = this.getNumber(var10.Right);
               IBlock var13 = var10.Left;
               double var14 = this.getOscillatorMiddleValue(var13);
               double var16 = var14 - var11;
               double var18 = var11;
               double var20 = var14 + var16;
               this.applyBlockObjChange(var10.Right, var20);
               var7.applyParamChange(var18, var20);
            }
         }

         return var7;
      }
   }

   private void applyBlockObjChange(IBlock var1, double var2) {
      if (var1 instanceof Number var4) {
         var4.Number = var2;
      }

      if (var1 instanceof IntVariable var5) {
         var5.Variable = (int)var2;
      }

      if (var1 instanceof DoubleVariable var6) {
         var6.Variable = var2;
      }
   }

   private double getNumber(IBlock var1) {
      if (var1 instanceof Number var4) {
         return var4.Number;
      } else if (var1 instanceof IntVariable var3) {
         return var3.Variable;
      } else {
         return var1 instanceof DoubleVariable var2 ? var2.Variable : -1.0;
      }
   }

   private boolean isNumber(IBlock var1) {
      return var1 instanceof Number || var1 instanceof IntVariable || var1 instanceof DoubleVariable;
   }
}
