package SQ.Internal.RulesImpl;

import SQ.Blocks.Order.Open.EnterAtLimit;
import SQ.Blocks.Order.Open.EnterAtMarket;
import SQ.Blocks.Order.Open.EnterAtStop;
import SQ.ExitMethods.ExitAfterBars;
import SQ.Internal.ActionBlock;
import SQ.Internal.ITradingOptionsEvaluator;
import SQ.Internal.Rule;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.lib.SQTime;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.tradinglib.BlockDefinitionException;
import com.strategyquant.tradinglib.ExitMethod;
import com.strategyquant.tradinglib.IBlock;
import com.strategyquant.tradinglib.IFormula;
import com.strategyquant.tradinglib.OrderTypes;
import com.strategyquant.tradinglib.engine.stockpicker.backtester.BacktestData;
import com.strategyquant.tradinglib.engine.stockpicker.signals.exit.PickerExitSignal;
import com.strategyquant.tradinglib.options.parameters.StockpickerOptions;
import java.util.ArrayList;
import java.util.Iterator;
import org.jdom2.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StockpickerEntryExit extends Rule {
   public static final Logger Log = LoggerFactory.getLogger("StockpickerEntryExit");
   private IBlock entry = null;
   private ActionBlock order = null;
   private IBlock exitConditional = null;
   private byte direction = -1;
   private int shift;
   private BacktestData data;
   private byte eod = 10;

   @Override
   public void evaluateRule(int var1, ITradingOptionsEvaluator var2, String var3) throws Exception {
      double var4 = 0.0;
      this.data = this.Strategy.Stockpicker.data;
      String var6 = this.Strategy.getSymbol();
      int var7 = this.Strategy.getSymbolHash();
      if (this.direction != 1 || this.Strategy.Stockpicker.MaxOpenPositionsLong >= 1) {
         if (this.direction != -1 || this.Strategy.Stockpicker.MaxOpenPositionsShort >= 1) {
            if (this.Strategy.Stockpicker.entryType() == this.Strategy.Stockpicker.strategyTriggeredAt()) {
               if (this.entry != null) {
                  var4 = this.entry.evaluateBlock();
               }

               if (var4 > 0.0 && this.order != null) {
                  boolean var8 = this.Strategy.Stockpicker.entryType() == 15;
                  if (var8) {
                     this.shift = -1;
                     if (!this.Strategy.Stockpicker.data.exists(this.shift)) {
                        return;
                     }
                  } else {
                     this.shift = 0;
                  }

                  byte var9 = -1;
                  double var10 = 0.0;
                  double var12 = 0.0;
                  double var14 = 0.0;
                  int var16 = 0;
                  int var17 = 0;
                  if (this.order instanceof EnterAtLimit var18) {
                     var9 = (byte)(var18.Direction > 0 ? 3 : 4);
                  }

                  if (this.order instanceof EnterAtStop var22) {
                     if (var9 == -1) {
                        var9 = (byte)(var22.Direction > 0 ? 5 : 6);
                     }

                     var14 = this.evaluateOpenPrice(var22.Price, var6, var22.Direction);
                     var16 = var22.BarsValid;
                     if (var16 > 0
                        && (this.Strategy.Stockpicker.entryType() == 10 || this.Strategy.Stockpicker.entryType() == this.Strategy.Stockpicker.exitType())) {
                        var16++;
                     }
                  }

                  if (this.order instanceof EnterAtMarket var23) {
                     if (var9 == -1) {
                        var9 = (byte)(var23.Direction > 0 ? 1 : 2);
                     }

                     if (var14 == 0.0) {
                        if (this.Strategy.Stockpicker.entryType() != 15 && this.Strategy.Stockpicker.entryType() != 5) {
                           var14 = this.data.CloseD(0, this.shift);
                        } else {
                           var14 = this.data.OpenD(0, this.shift);
                        }
                     }

                     var10 = this.computeSL((EnterAtMarket)this.order, var9, var14, var6);
                     var12 = this.computePT((EnterAtMarket)this.order, var9, var14, var6);
                     ExitMethod var19 = var23.getExitAfterBarsExit();
                     if (var19 != null) {
                        var17 = ((ExitAfterBars)var19).ExitAfterBars;
                     }
                  }

                  if (var14 <= 0.0) {
                     Log.debug(
                        String.format(
                           "SKIPPED Entry signal for %s, %s - Entry price must be greater than 0, got %s",
                           var6,
                           SQTime.toDateString(this.data.getCurrentTime()),
                           SQUtils.d2(var14)
                        )
                     );
                     return;
                  }

                  if (var10 <= 0.0 && var10 != -9.9999999E7) {
                     Log.debug(
                        String.format(
                           "SKIPPED Entry signal for %s, %s - SL price must be greater than 0, got %s",
                           var6,
                           SQTime.toDateString(this.data.getCurrentTime()),
                           SQUtils.d2(var10)
                        )
                     );
                     return;
                  }

                  if (var12 <= 0.0 && var12 != -9.9999999E7) {
                     Log.debug(
                        String.format(
                           "SKIPPED Entry signal for %s, %s - PT price must be greater than 0, got %s",
                           var6,
                           SQTime.toDateString(this.data.getCurrentTime()),
                           SQUtils.d2(var12)
                        )
                     );
                     return;
                  }

                  this.data
                     .Signals(this.data.getTime(this.shift), true)
                     .addEntrySignal(var7, var6, var9, var14, var16, var10, var12, this.slptValidFrom(var10, var12, var9), var17, this.eod);
               }
            }

            if (this.Strategy.Stockpicker.exitType() == this.Strategy.Stockpicker.strategyTriggeredAt() && this.exitConditional != null) {
               var4 = this.exitConditional.evaluateBlock();
               if (var4 > 0.0) {
                  boolean var21 = this.Strategy.Stockpicker.exitType() == 15;
                  if (var21) {
                     this.shift = -1;
                     if (!this.Strategy.Stockpicker.data.exists(this.shift)) {
                        return;
                     }
                  } else {
                     this.shift = 0;
                  }

                  this.data.Signals(this.data.getTime(this.shift), true).addExitSignal(new PickerExitSignal(var6, this.direction, (byte)22));
               }
            }
         }
      }
   }

   private int slptValidFrom(double var1, double var3, byte var5) {
      if (!OrderTypes.isMarketOrder(var5) || (!(var1 > 0.0) || !(var3 <= 0.0)) && (!(var1 <= 0.0) || !(var3 > 0.0))) {
         return OrderTypes.isLimitOrder(var5) && var1 > 0.0 && var3 <= 0.0 ? 0 : 1;
      } else {
         return 0;
      }
   }

   private double evaluateOpenPrice(IFormula var1, String var2, int var3) throws TradingException {
      double var4 = var1.evaluateFormula(this.Strategy, var2, 0.0, var3);
      if (var4 == -9.9999999E7) {
         throw new TradingException("Open price not defined");
      } else {
         return var4;
      }
   }

   protected double computeSL(EnterAtMarket var1, byte var2, double var3, String var5) throws TradingException {
      ExitMethod var6 = var1.getStopLossExit();
      return var6 == null ? -9.9999999E7 : SQUtils.fixPrice(0.01, var6.computeValue(var2, this.Strategy, var5, var3));
   }

   protected double computePT(EnterAtMarket var1, byte var2, double var3, String var5) throws TradingException {
      ExitMethod var6 = var1.getProfitTargetExit();
      return var6 == null ? -9.9999999E7 : SQUtils.fixPrice(0.01, var6.computeValue(var2, this.Strategy, var5, var3));
   }

   @Override
   protected void parseXml(Element var1) throws BlockDefinitionException {
      super.parseXml(var1);
      String var2 = var1.getAttributeValue("name");
      if (var2.equalsIgnoreCase("Long")) {
         this.direction = 1;
      } else {
         this.direction = -1;
      }

      ArrayList var3 = this.getBlocks("Entry");
      if (var3.size() > 1) {
         throw new BlockDefinitionException("Entry part cannot have more than one sub-block!");
      }

      if (var3 != null && var3.size() != 0) {
         this.entry = (IBlock)var3.get(0);
      }

      var3 = this.getBlocks("Order");
      if (var3 != null && var3.size() != 0) {
         for (IBlock var5 : var3) {
            if (!(var5 instanceof ActionBlock)) {
               throw new BlockDefinitionException(String.format("Block '%' in Order part is not an ActionBlock!", var5.getClass().getSimpleName()));
            }

            this.order = (ActionBlock)var5;
         }
      }

      if (this.entry != null) {
         var3 = this.getBlocks("Exit");
         if (var3 != null && var3.size() != 0) {
            for (IBlock var13 : var3) {
               if (!(var13 instanceof IBlock)) {
                  throw new BlockDefinitionException(String.format("Block '%s' in Exit part is not an IBlock!", var13.getClass().getSimpleName()));
               }

               this.exitConditional = var13;
            }
         }

         Iterator var11 = var1.getChildren("Exit").iterator();
         if (var11.hasNext()) {
            Element var14 = (Element)var11.next();
            String var6 = var14.getAttributeValue("atEndOfDay");
            if (var6 != null && !var6.equalsIgnoreCase("false") && !var6.equalsIgnoreCase("none")) {
               this.eod = 5;
            }
         }
      }

      StockpickerOptions var12 = this.Strategy.getStockpickerOptions();
      if (var12 != null && !this.Strategy.isAlgoWizard) {
         try {
            if (this.direction == 1) {
               this.eod = var12.getEndOfDayLong(this.eod);
            } else {
               this.eod = var12.getEndOfDayShort(this.eod);
            }
         } catch (Exception var7) {
            new BlockDefinitionException(var7.getMessage(), var7);
         }
      }

      if (this.eod == 5 && this.Strategy.Stockpicker.exitType != 10) {
         this.eod = 10;
      }

      if (this.direction == 1) {
         this.Strategy.Stockpicker.eodLong = this.eod;
      } else {
         this.Strategy.Stockpicker.eodShort = this.eod;
      }

      if (this.direction == 1) {
         this.Strategy.Stockpicker.LongEntryExists = this.entry != null;
      } else {
         this.Strategy.Stockpicker.ShortEntryExists = this.entry != null;
      }
   }
}
