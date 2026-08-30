package SQ.ExitMethods;

import com.strategyquant.datalib.TradingException;
import com.strategyquant.lib.SQTime;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.tradinglib.BlockDefinitionException;
import com.strategyquant.tradinglib.ClassConfig;
import com.strategyquant.tradinglib.Editor;
import com.strategyquant.tradinglib.ExitMethod;
import com.strategyquant.tradinglib.ForEngine;
import com.strategyquant.tradinglib.Help;
import com.strategyquant.tradinglib.IActionEventListener;
import com.strategyquant.tradinglib.IBlock;
import com.strategyquant.tradinglib.IFormula;
import com.strategyquant.tradinglib.ILiveOrder;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.SortOrder;
import com.strategyquant.tradinglib.StrategyBase;

@ClassConfig(name = "Move SL to BE")
@SortOrder(300)
@ForEngine("*,-SP,-SA")
public class MoveSL2BE extends ExitMethod {
   @Parameter(defaultValue = "SQ.Formulas.RangeLevel.None", name = "Move SL to BE", category = "Advanced", showIfDefault = false)
   @Help("Move StopLoss to Break Even")
   @Editor(type = 200, formulaName = "RangeLevel")
   public IFormula MoveSL2BE;
   @Parameter(name = "SL to BE - Add pips", category = "Advanced", showIfDefault = false)
   @Editor(type = 200, formulaName = "Range")
   public IFormula SL2BEAddPips;
   private double move2BESL = -9.9999999E7;
   private boolean debugTrailingStops = false;

   public void setForOrder(final ILiveOrder var1, final StrategyBase var2) throws TradingException {
      if (this.MoveSL2BE != null && !this.MoveSL2BE.isNoneValue()) {
         this.move2BESL = -9.9999999E7;
         var1.registerEvent(2, new IActionEventListener() {
            public void OnActionEvent(StrategyBase var1x) throws TradingException {
               if (var1.isMarketOpen()) {
                  MoveSL2BE.this.checkSL2BE(var1, MoveSL2BE.this.MoveSL2BE, MoveSL2BE.this.SL2BEAddPips, var2);
               }
            }
         });
      }
   }

   public double computeValue(byte var1, StrategyBase var2, String var3, double var4) throws TradingException {
      throw new TradingException("This method shouldn't be called!");
   }

   protected void checkSL2BE(ILiveOrder var1, IFormula var2, IFormula var3, StrategyBase var4) throws TradingException {
      if (!var1.isClosedOrder()) {
         if (var1.isMarketOrder()) {
            int var5 = var4.MarketData.getInstrumentInfo(var1.getSymbol()).decimals;
            double var6;
            if (var1.getDirection() == 1) {
               var6 = var4.MarketData.Chart(var1.getSymbol()).Bid();
            } else {
               var6 = var4.MarketData.Chart(var1.getSymbol()).Ask();
            }

            double var8 = var2.evaluateFormula(var4, var1.getSymbol(), var6, var1.getDirection());
            if (var8 != -9.9999999E7) {
               var8 = SQUtils.fixPrice(var1.getInstrumentInfo().tickStep, var8, var5);
               double var10 = 0.0;
               if (var3 != null) {
                  var10 = var3.evaluateFormula(var4, var1.getSymbol(), 0.0, 1);
                  var10 = var10 == -9.9999999E7 ? 0.0 : var10;
               }

               double var12 = var1.getSL() == -9.9999999E7 ? -9.9999999E7 : SQUtils.round(var1.getSL(), var5);
               double var14 = var1.isNettingMode() ? var1.getLastOpenPrice() : var1.getOpenPrice();
               double var18 = SQUtils.round(var4.getMinDistance() * var1.getInstrumentInfo().tickSize, var5);
               if (var1.getDirection() == 1) {
                  double var16 = SQUtils.round(var14 + var10, var5);
                  if (var14 <= var8 && var6 >= var16 && (var12 == -9.9999999E7 || var12 < var16) && Math.abs(var6 - var16) >= var18) {
                     var16 = SQUtils.fixPrice(var1.getInstrumentInfo().tickStep, var16, var5);
                     var1.setSL((byte)20, var16).Send();
                     if (this.debugTrailingStops) {
                        Log.info(SQTime.toDateMinuteString(var4.Time(0)) + " - Order " + var1.getOrderId() + " - Moving Long SL2BE to: {}", var16);
                     }

                     this.move2BESL = var16;
                  }
               } else {
                  double var23 = SQUtils.round(var14 - var10, var5);
                  if (var14 >= var8 && var6 <= var23 && (var12 == -9.9999999E7 || var12 > var23) && Math.abs(var6 - var23) >= var18) {
                     var23 = SQUtils.fixPrice(var1.getInstrumentInfo().tickStep, var23, var5);
                     var1.setSL((byte)20, var23).Send();
                     if (this.debugTrailingStops) {
                        Log.info("{}, Order " + var1.getOrderId() + " - Moving Short SL2BE to: {}", SQTime.toDateMinuteString(var4.Time(0)), var23);
                     }

                     this.move2BESL = var23;
                  }
               }
            }
         }
      }
   }

   public boolean setExit(ILiveOrder var1, StrategyBase var2) throws TradingException {
      if (this.move2BESL != -9.9999999E7 && !var1.usesATM()) {
         int var3 = var1.isLong() ? -1 : 1;
         int var4 = var3 > 0 ? 100 : 102;
         if (this.debugTrailingStops) {
            Log.info("{} - Creating order for move2BESL at: {}", SQTime.toDateMinuteString(this.Strategy.Time(0)), this.move2BESL);
         }

         ILiveOrder var5 = this.Strategy
            .Trader
            .Open((byte)var4, var1.getSymbol(), this.move2BESL)
            .setMagicNumber(var1.getMagicNumber())
            .setSLType((byte)20)
            .Send();
         return true;
      } else {
         return false;
      }
   }

   public IBlock clone(boolean var1, StrategyBase var2) throws BlockDefinitionException {
      MoveSL2BE var3 = new MoveSL2BE();
      var3.MoveSL2BE = this.MoveSL2BE;
      var3.SL2BEAddPips = this.SL2BEAddPips;
      return var3;
   }
}
