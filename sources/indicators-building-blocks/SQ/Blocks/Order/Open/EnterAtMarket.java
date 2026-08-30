package SQ.Blocks.Order.Open;

import SQ.ExitMethods.StopLoss;
import SQ.Functions.OrderFunctions;
import SQ.Internal.ActionBlock;
import SQ.Internal.MMFormulaBlock;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.SettingsMap;
import com.strategyquant.tradinglib.ATM;
import com.strategyquant.tradinglib.ATMExit;
import com.strategyquant.tradinglib.BlockDefinitionException;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.CategoryOrder;
import com.strategyquant.tradinglib.Editor;
import com.strategyquant.tradinglib.ExitMethod;
import com.strategyquant.tradinglib.Help;
import com.strategyquant.tradinglib.IActionEventListener;
import com.strategyquant.tradinglib.IFormula;
import com.strategyquant.tradinglib.ILiveOrder;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.Required;
import com.strategyquant.tradinglib.SortOrder;
import com.strategyquant.tradinglib.StrategyBase;
import com.strategyquant.tradinglib.atm.exits.None;
import com.strategyquant.tradinglib.atm.exits.TrailingStop;
import java.util.ArrayList;

@BuildingBlock(name = "(MKT) Enter at market", display = "EnterAtMarket", returnType = 8)
@Help("Opens order at current market price")
@SortOrder(100)
@CategoryOrder(100)
public class EnterAtMarket extends ActionBlock {
   private static final int MaxDistanceFromMarketHash = "MaxDistanceFromMarket.MaxDistanceFromMarket".hashCode();
   private static final int MaxDistanceFromMarketPctHash = "MaxDistanceFromMarket.MaxDistancePct".hashCode();
   @Parameter(defaultValue = "Any", category = "Basic", showIfDefault = false)
   @Editor(type = 70)
   public String Symbol;
   @Parameter(defaultValue = "1", category = "Basic")
   @Editor(type = 40, values = "Long=1,Short=-1")
   public int Direction;
   @Parameter(category = "Basic")
   @Editor(type = 200, formulaName = "Size")
   @Required
   public IFormula Size;
   @Parameter(defaultValue = "MagicNumber", category = "Order identification", showIfDefault = false)
   @Help("Magic number is used to identify this trade, it should be unique for every trade you open.")
   @Editor(type = 80)
   public int MagicNumber;
   @Parameter(defaultValue = "", category = "Order identification", showIfDefault = false)
   public String Comment;
   @Parameter(defaultValue = "false", category = "Order identification", showIfDefault = false)
   @Help("If set to true, it will allow to place multiple trades with the same Magic Number")
   public boolean AllowDuplicateTrades;
   public ExitMethod[] ExitMethods;

   @Override
   public void OnAction() throws TradingException {
      if (this.AllowDuplicateTrades && this.engineSupportsDuplicateTrades() || this.checkLiveOrderExists(0, true) == null) {
         if (this.Strategy == null || this.Strategy.Trader == null || this.Strategy.Trader.IsMarketOpen()) {
            int var1 = this.Direction > 0 ? 1 : 2;
            double var2 = this.computeSL(
               (byte)var1, this.Direction > 0 ? this.Strategy.MarketData.Chart(this.Symbol).Ask() : this.Strategy.MarketData.Chart(this.Symbol).Bid()
            );
            double var4 = this.computeSize((byte)var1, 0.0, var2);
            if ((this.Direction <= 0 || var2 != this.Strategy.MarketData.Chart(this.Symbol).Ask())
               && (this.Direction >= 0 || var2 != this.Strategy.MarketData.Chart(this.Symbol).Bid())) {
               ATM var6 = this.Strategy.getATM();
               if (var6 != null && var6.isApplicable(this.Strategy, var4, var2, (byte)var1)) {
                  double var7 = this.computePT(
                     (byte)var1, this.Direction > 0 ? this.Strategy.MarketData.Chart(this.Symbol).Ask() : this.Strategy.MarketData.Chart(this.Symbol).Bid()
                  );
                  this.openATMOrder(var6, -1.0, var4, var2, var7, (byte)var1, 0);
               } else {
                  this.openNormalOrder(-1.0, var4, var2, (byte)var1, 0);
               }
            }
         }
      }
   }

   protected void openATMOrder(ATM var1, double var2, double var4, double var6, double var8, byte var10, int var11) throws TradingException {
      ExitMethod var12 = this.getStopLossExit();
      double var13 = 0.0;
      if (this.Strategy.getEngine() != -659455871 && this.Strategy.getEngine() != 395961824) {
         ILiveOrder var21 = this.Strategy.Trader.Open(var10, this.Symbol, var2).setSize(var4).setMagicNumber(this.MagicNumber).setComment(this.Comment).Send();
         if (var21.isSuccessful()) {
            this.handleBarsValid(var21, var11);
            if (var12 != null && !var21.isClosedOrder()) {
               var12.setForOrder(var21, this.Strategy);
            }

            for (byte var22 = 0; var22 < var1.getExitsCount(); var22++) {
               ATMExit var23 = this.tryCloneATMExit(var1.getExit(var22));
               boolean var24 = var22 == var1.getExitsCount() - 1;
               double var19 = var23.computeSize(var4, var13, var24);
               if (var19 > 0.0) {
                  var13 += this.Strategy.getEngine() == 1441180233
                     ? this.openNewATMNettingOrder(var21, var23, var19, var6, var8, var22)
                     : this.openNewTSATMOrder(var21, var23, var19, var6, var8, var22);
               }
            }
         }
      } else {
         for (int var15 = 0; var15 < var1.getExitsCount(); var15++) {
            ATMExit var16 = this.tryCloneATMExit(var1.getExit(var15));
            boolean var17 = var15 == var1.getExitsCount() - 1;
            double var18 = var16.computeSize(var4, var13, var17);
            if (var18 > 0.0) {
               ILiveOrder var20 = this.Strategy
                  .Trader
                  .Open(var10, this.Symbol, var2)
                  .setSize(var18)
                  .setMagicNumber(this.MagicNumber)
                  .setComment(this.Comment)
                  .Send();
               var13 += var20.getSize();
               if (var20.isSuccessful()) {
                  this.handleBarsValid(var20, var11);
                  if (var12 != null && !var20.isClosedOrder()) {
                     var12.setForOrder(var20, this.Strategy);
                  }

                  var16.setForOrder(var20, this.Strategy, var6, var8);
               }
            }
         }
      }
   }

   private double openNewTSATMOrder(final ILiveOrder var1, final ATMExit var2, final double var3, double var5, double var7, final byte var9) throws TradingException {
      var1.registerEvent(
         2,
         new IActionEventListener() {
            private ILiveOrder exitOrder = null;
            private double actualSL = -1.0;
            private double actualPT = -1.0;
            private double lastTS = -1.0;
            private boolean filled = false;

            public void OnActionEvent(StrategyBase var1x) throws TradingException {
               if (var1.isClosedOrder()) {
                  if (this.exitOrder != null) {
                     this.exitOrder.Close((byte)8);
                     this.exitOrder = null;
                  }
               } else if (var1.isMarketOrder()) {
                  this.actualSL = EnterAtMarket.this.computeSL(var1.getOrderType(), var1.getOpenPrice());
                  this.actualPT = EnterAtMarket.this.computePT(var1.getOrderType(), var1.getOpenPrice());
                  if (var2.exitLevel instanceof None var2x) {
                     if (var2x.checkExitAfterBars(var1)) {
                        var2x.deactivate();
                        EnterAtMarket.this.Strategy
                           .Trader
                           .Open((byte)(var1.isLong() ? 2 : 1), var1.getSymbol(), 0.0)
                           .setSize(var3)
                           .setMagicNumber(EnterAtMarket.this.MagicNumber)
                           .setComment(EnterAtMarket.this.Comment)
                           .setExitIndex(var9)
                           .Send();
                     }
                  } else if (var2.exitLevel instanceof TrailingStop) {
                     if (this.filled) {
                        return;
                     }

                     double var4 = SQUtils.fixPrice(
                        EnterAtMarket.this.Strategy.getInstrumentInfo().tickStep,
                        var2.exitLevel.getNettingPrice(EnterAtMarket.this.Strategy, var1, this.actualSL, this.actualPT)
                     );
                     if (var1.isLong()) {
                        if (var4 > var1.getOpenPrice() && var4 > this.actualSL && var4 > this.lastTS) {
                           this.lastTS = var4;
                        }
                     } else if (var4 < var1.getOpenPrice() && var4 < this.actualSL && (var4 < this.lastTS || this.lastTS < 0.0)) {
                        this.lastTS = var4;
                     }

                     if (this.exitOrder != null) {
                        if (EnterAtMarket.this.exitWasFilled(this.exitOrder)) {
                           this.filled = true;
                           return;
                        }

                        this.exitOrder.Close((byte)9);
                     }

                     if (this.lastTS > 0.0) {
                        this.exitOrder = EnterAtMarket.this.Strategy
                           .Trader
                           .Open((byte)(EnterAtMarket.this.Direction > 0 ? 6 : 5), EnterAtMarket.this.Symbol, this.lastTS)
                           .setSize(var3)
                           .setMagicNumber(EnterAtMarket.this.MagicNumber)
                           .setComment(EnterAtMarket.this.Comment)
                           .setExitIndex(var9)
                           .Send();
                        this.exitOrder.registerEvent(7, new IActionEventListener() {
                           public void OnActionEvent(StrategyBase var1x) throws TradingException {
                              if (EnterAtMarket.this.exitWasFilled(exitOrder)) {
                                 filled = true;
                              }
                           }
                        });
                     }
                  } else {
                     if (this.filled) {
                        return;
                     }

                     double var5x = SQUtils.fixPrice(
                        EnterAtMarket.this.Strategy.getInstrumentInfo().tickStep,
                        var2.exitLevel.getNettingPrice(EnterAtMarket.this.Strategy, var1, this.actualSL, this.actualPT)
                     );
                     if (var5x <= 0.0) {
                        return;
                     }

                     if (this.exitOrder != null) {
                        if (EnterAtMarket.this.exitWasFilled(this.exitOrder)) {
                           return;
                        }

                        this.exitOrder.Close((byte)8);
                        this.exitOrder = null;
                     }

                     this.exitOrder = EnterAtMarket.this.Strategy
                        .Trader
                        .Open((byte)(EnterAtMarket.this.Direction > 0 ? 4 : 3), EnterAtMarket.this.Symbol, var5x)
                        .setSize(var3)
                        .setMagicNumber(EnterAtMarket.this.MagicNumber)
                        .setComment(EnterAtMarket.this.Comment)
                        .setExitIndex(var9)
                        .Send();
                     if (this.exitOrder.isSuccessful()) {
                        this.exitOrder.registerEvent(2, new IActionEventListener() {
                           public void OnActionEvent(StrategyBase var1x) throws TradingException {
                              boolean var2x = false;

                              for (int var3x = EnterAtMarket.this.Strategy.Trader.getOpenOrdersCount(false) - 1; var3x >= 0; var3x--) {
                                 ILiveOrder var4 = EnterAtMarket.this.Strategy.Trader.getOpenOrder(var3x, false);
                                 if (var4.getOrderId() == var1.getOrderId()) {
                                    var2x = true;
                                 }
                              }

                              if (!var2x) {
                                 exitOrder.Close((byte)8);
                              }
                           }
                        });
                        this.exitOrder.registerEvent(7, new IActionEventListener() {
                           public void OnActionEvent(StrategyBase var1x) throws TradingException {
                              if (EnterAtMarket.this.exitWasFilled(exitOrder)) {
                                 filled = true;
                              }
                           }
                        });
                     }
                  }
               }
            }
         }
      );
      return var3;
   }

   private double openNewATMNettingOrder(final ILiveOrder var1, final ATMExit var2, final double var3, double var5, double var7, final byte var9) throws TradingException {
      if (var2.exitLevel instanceof None) {
         var1.registerEvent(
            2,
            new IActionEventListener() {
               public void OnActionEvent(StrategyBase var1x) throws TradingException {
                  if (!var1.isClosedOrder() && var1.isMarketOrder()) {
                     None var2x = (None)var2.exitLevel;
                     if (var2x.checkExitAfterBars(var1)) {
                        var2x.deactivate();
                        ILiveOrder var3x = EnterAtMarket.this.Strategy
                           .Trader
                           .Open((byte)(var1.isLong() ? 2 : 1), var1.getSymbol(), 0.0)
                           .setSize(var3)
                           .setMagicNumber(EnterAtMarket.this.MagicNumber)
                           .setComment(EnterAtMarket.this.Comment)
                           .setExitIndex(var9)
                           .Send();
                     }
                  }
               }
            }
         );
         return var3;
      } else if (var2.exitLevel instanceof TrailingStop) {
         var1.registerEvent(
            2,
            new IActionEventListener() {
               private ILiveOrder exitOrder = null;
               private double lastTS = -1.0;
               private boolean filled = false;

               public void OnActionEvent(StrategyBase var1x) throws TradingException {
                  if (!this.filled) {
                     if (var1.isClosedOrder()) {
                        if (this.exitOrder != null) {
                           this.exitOrder.Close((byte)8);
                           this.exitOrder = null;
                        }
                     } else {
                        if (var1.isMarketOrder()) {
                           double var2x = SQUtils.fixPrice(
                              EnterAtMarket.this.Strategy.getInstrumentInfo().tickStep,
                              var2.exitLevel.getNettingPrice(EnterAtMarket.this.Strategy, var1, var1.getSL(), var1.getPT())
                           );
                           double var4 = this.lastTS;
                           if (var1.isLong()) {
                              if (var2x > var1.getOpenPrice() && var2x > var1.getSL() && var2x > this.lastTS) {
                                 this.lastTS = var2x;
                              }
                           } else if (var2x < var1.getOpenPrice() && var2x < var1.getSL() && (var2x < this.lastTS || this.lastTS < 0.0)) {
                              this.lastTS = var2x;
                           }

                           if (var4 != this.lastTS && this.lastTS > 0.0) {
                              if (this.exitOrder != null) {
                                 this.exitOrder.Close((byte)9);
                              }

                              this.exitOrder = EnterAtMarket.this.Strategy
                                 .Trader
                                 .Open((byte)(EnterAtMarket.this.Direction > 0 ? 6 : 5), EnterAtMarket.this.Symbol, this.lastTS)
                                 .setSize(var3)
                                 .setMagicNumber(EnterAtMarket.this.MagicNumber)
                                 .setComment(EnterAtMarket.this.Comment)
                                 .setExitIndex(var9)
                                 .Send();
                              this.exitOrder.registerEvent(7, new IActionEventListener() {
                                 public void OnActionEvent(StrategyBase var1x) throws TradingException {
                                    if (EnterAtMarket.this.exitWasFilled(exitOrder)) {
                                       filled = true;
                                    }
                                 }
                              });
                           }
                        }
                     }
                  }
               }
            }
         );
         return var3;
      } else {
         double var10 = SQUtils.fixPrice(this.Strategy.getInstrumentInfo().tickStep, var2.exitLevel.getNettingPrice(this.Strategy, var1, var5, var7));
         if (var10 <= 0.0) {
            return 0.0;
         } else {
            final ILiveOrder var12 = this.Strategy
               .Trader
               .Open((byte)(this.Direction > 0 ? 4 : 3), this.Symbol, var10)
               .setSize(var3)
               .setMagicNumber(this.MagicNumber)
               .setComment(this.Comment)
               .setExitIndex(var9)
               .Send();
            if (var12.isSuccessful()) {
               var12.registerEvent(2, new IActionEventListener() {
                  public void OnActionEvent(StrategyBase var1x) throws TradingException {
                     boolean var2 = false;

                     for (int var3 = EnterAtMarket.this.Strategy.Trader.getOpenOrdersCount(false) - 1; var3 >= 0; var3--) {
                        ILiveOrder var4 = EnterAtMarket.this.Strategy.Trader.getOpenOrder(var3, false);
                        if (var4.getOrderId() == var1.getOrderId()) {
                           var2 = true;
                        }
                     }

                     if (!var2) {
                        var12.Close((byte)8);
                     }
                  }
               });
               return var3;
            } else {
               return 0.0;
            }
         }
      }
   }

   private boolean exitWasFilled(ILiveOrder var1) {
      return var1.isMarketOrder() && var1.getCloseTime() > 0L;
   }

   protected void openNormalOrder(double var1, double var3, double var5, byte var7, int var8) throws TradingException {
      ILiveOrder var9 = this.Strategy.Trader.Open(var7, this.Symbol, var1).setSize(var3).setMagicNumber(this.MagicNumber).setComment(this.Comment).Send();
      if (var9.isSuccessful()) {
         this.handleBarsValid(var9, var8);

         for (ExitMethod var13 : this.ExitMethods) {
            if (!var9.isClosedOrder()) {
               if (this.AllowDuplicateTrades) {
                  try {
                     ExitMethod var14 = (ExitMethod)var13.clone(true, this.Strategy);
                     var14.setForOrder(var9, this.Strategy);
                  } catch (BlockDefinitionException var15) {
                     Log.error("Cannot clone exit method '" + var13.getClass().getName() + "' for order #" + var9.getOrderId(), var15);
                  }
               } else {
                  var13.setForOrder(var9, this.Strategy);
               }
            }
         }
      }
   }

   protected void handleBarsValid(final ILiveOrder var1, final int var2) throws TradingException {
      if (!var1.isClosedOrder() && !var1.isMarketOrder()) {
         if (var2 != 0) {
            var1.registerEvent(2, new IActionEventListener() {
               public void OnActionEvent(StrategyBase var1x) throws TradingException {
                  EnterAtMarket.this.checkBarsValid(var1, var2);
               }
            });
         }
      }
   }

   protected void checkBarsValid(ILiveOrder var1, int var2) throws TradingException {
      if (!var1.isClosedOrder()) {
         if (var1.isPendingOrder() && var1.getBarsInTrade() >= var2) {
            var1.Close((byte)6);
         }
      }
   }

   protected double computeSL(byte var1, double var2) throws TradingException {
      ExitMethod var4 = this.getStopLossExit();
      return var4 != null && var2 != 0.0
         ? SQUtils.fixPrice(this.Strategy.getInstrumentInfo().tickStep, var4.computeValue(var1, this.Strategy, this.Symbol, var2))
         : -9.9999999E7;
   }

   protected double computePT(byte var1, double var2) throws TradingException {
      ExitMethod var4 = this.getProfitTargetExit();
      if (var4 == null) {
         return -9.9999999E7;
      }

      double var5 = this.Strategy.getInstrumentInfo().tickStep;
      return SQUtils.fixPrice(var5, var4.computeValue(var1, this.Strategy, this.Symbol, var2));
   }

   public ExitMethod getStopLossExit() {
      ExitMethod var1 = null;

      for (ExitMethod var5 : this.ExitMethods) {
         if (var5.getExitType() == 2) {
            var1 = var5;
            break;
         }
      }

      return var1;
   }

   public ExitMethod getProfitTargetExit() {
      ExitMethod var1 = null;

      for (ExitMethod var5 : this.ExitMethods) {
         if (var5.getExitType() == 3) {
            var1 = var5;
            break;
         }
      }

      return var1;
   }

   public ExitMethod getExitAfterBarsExit() {
      ExitMethod var1 = null;

      for (ExitMethod var5 : this.ExitMethods) {
         if (var5.getExitType() == 4) {
            var1 = var5;
            break;
         }
      }

      return var1;
   }

   public double computeSize(byte var1, double var2, double var4) throws TradingException {
      MMFormulaBlock var6 = (MMFormulaBlock)this.Size;
      return var6.computeSize(this.Strategy, this.Symbol, var1, var2, var4);
   }

   protected ILiveOrder checkLiveOrderExists(int var1, boolean var2) {
      int var3 = this.Strategy.Trader.getOpenOrdersCount(var2) - 1;

      for (int var4 = var3; var4 >= 0; var4--) {
         ILiveOrder var5 = this.Strategy.Trader.getOpenOrder(var4, var2);
         if (OrderFunctions.identify(var5, this.Strategy, this.Symbol, var1, this.MagicNumber, this.Comment) && var5.isMarketOrder()) {
            return var5;
         }
      }

      return null;
   }

   @Override
   public void OnApplyExits() throws TradingException {
      ArrayList var1 = this.getOpenOrders(this.Direction);
      ATM var2 = this.Strategy.getATM();
      boolean var3 = false;
      if (var1 != null) {
         for (int var4 = 0; var4 < var1.size(); var4++) {
            ILiveOrder var5 = (ILiveOrder)var1.get(var4);
            boolean var6 = var2 != null && var2.isApplicable(this.Strategy, var5.getSize(), var5.getSL(), var5.getOrderType());

            for (ExitMethod var10 : this.ExitMethods) {
               if ((!var6 || var10 instanceof StopLoss) && var10.setExit(var5, this.Strategy)) {
                  var3 = true;
               }
            }

            if (!var3 && var5.getSL() != -9.9999999E7) {
               int var11 = var5.isLong() ? -1 : 1;
               int var12 = var11 > 0 ? 100 : 102;
               ILiveOrder var13 = this.Strategy
                  .Trader
                  .Open((byte)var12, var5.getSymbol(), var5.getSL())
                  .setComment("SL")
                  .setMagicNumber(var5.getMagicNumber())
                  .Send();
            }
         }
      }
   }

   private ArrayList<ILiveOrder> getOpenOrders(int var1) {
      ArrayList var2 = null;

      for (int var3 = 0; var3 < this.Strategy.Trader.getOpenOrdersCount(false); var3++) {
         ILiveOrder var4 = this.Strategy.Trader.getOpenOrder(var3, false);
         if (!var4.isPendingOrder() && OrderFunctions.identify(var4, this.Strategy, this.Symbol, var1, this.MagicNumber, this.Comment) && var4.isMarketOrder()) {
            if (var2 == null) {
               var2 = new ArrayList();
            }

            var2.add(var4);
         }
      }

      return var2;
   }

   protected boolean engineSupportsDuplicateTrades() {
      return this.Strategy.Trader.supportsDuplicateTrades();
   }

   private ATMExit tryCloneATMExit(ATMExit var1) throws TradingException {
      ATMExit var2 = var1.clone();
      if (var2 == null) {
         throw new TradingException("Unable to create ATMExit object");
      } else {
         return var2;
      }
   }

   protected boolean checkOpenPriceWithinRange(double var1) {
      try {
         SettingsMap var3 = this.Strategy.getSettings();
         if (var3.containsKey(MaxDistanceFromMarketHash) && var3.containsKey(MaxDistanceFromMarketPctHash) && (Boolean)var3.get(MaxDistanceFromMarketHash)) {
            double var4 = SQUtils.round2((Double)var3.get(MaxDistanceFromMarketPctHash));
            double var6 = this.Direction > 0 ? this.Strategy.MarketData.Chart(this.Symbol).Ask() : this.Strategy.MarketData.Chart(this.Symbol).Bid();
            double var8 = SQUtils.round2(Math.abs(var6 - var1) / var6 * 100.0);
            if (var8 > var4) {
               Log.debug("Order skipped - too far from market. Open price: {}, Market price: {}, Max distance: {}%", new Object[]{var1, var6, var4});
               return false;
            }
         }
      } catch (Throwable var10) {
         Log.error("Error while checking open price max distance", var10);
      }

      return true;
   }
}
