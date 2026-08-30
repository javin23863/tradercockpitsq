package com.strategyquant.plugin.Portfolio.impl.Composer;

import com.strategyquant.datalib.ChartDef;
import com.strategyquant.datalib.DataInfo;
import com.strategyquant.datalib.InstrumentInfo;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.datalib.data.DataManager;
import com.strategyquant.datalib.data.io.IDataLoader;
import com.strategyquant.datalib.data.io.VersatileData;
import com.strategyquant.lib.SQTime;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.XMLUtil;
import com.strategyquant.tradinglib.MoneyManagementMethod;
import com.strategyquant.tradinglib.Order;
import com.strategyquant.tradinglib.OrderCloseTypes;
import com.strategyquant.tradinglib.OrderTypes;
import com.strategyquant.tradinglib.OrdersList;
import com.strategyquant.tradinglib.PlTypes;
import com.strategyquant.tradinglib.Result;
import com.strategyquant.tradinglib.ResultsGroup;
import com.strategyquant.tradinglib.StrategyBase;
import com.strategyquant.tradinglib.TradeSizeSmallerThanOneException;
import com.strategyquant.tradinglib.correlation.CorrelationComputer;
import com.strategyquant.tradinglib.engine.stockpicker.Stockpicker;
import com.strategyquant.tradinglib.moneymanagement.MoneyManagementMethodsList;
import com.strategyquant.tradinglib.project.ProjectConfigHelper;
import com.strategyquant.tradinglib.results.SpecialValues;
import com.strategyquant.tradinglib.results.SymbolInfo;
import com.strategyquant.tradinglib.results.stats.comparator.OrderComparatorByOpenTime;
import com.strategyquant.tradinglib.results.stats.computer.OrderPLComputer;
import it.unimi.dsi.fastutil.longs.Long2ObjectAVLTreeMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectBidirectionalIterator;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.ObjectListIterator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.jdom2.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PortfolioComposer {
   private static final Logger Log = LoggerFactory.getLogger(PortfolioComposer.class);
   public boolean isRunning = false;
   private boolean stopped;

   public ResultsGroup recalculate(String var1, PortfolioComposerSettings var2, Double[] var3) throws Exception {
      Long2ObjectAVLTreeMap var4 = new Long2ObjectAVLTreeMap();
      Object2DoubleOpenHashMap var5 = new Object2DoubleOpenHashMap();
      Object2ObjectOpenHashMap var6 = new Object2ObjectOpenHashMap();
      Object2ObjectOpenHashMap var7 = new Object2ObjectOpenHashMap();
      Object2ObjectOpenHashMap var8 = new Object2ObjectOpenHashMap();
      Object2IntOpenHashMap var9 = new Object2IntOpenHashMap();
      Long2ObjectOpenHashMap var10 = new Long2ObjectOpenHashMap();
      ObjectArrayList var11 = new ObjectArrayList();
      this.stopped = false;

      try {
         double var12 = var2.initialCapital;
         int var14 = var2.leverage;
         long var27 = 0L;
         int var32 = var2.decimals;
         long var33 = -1L;
         long var35 = -1L;
         int var37 = var2.strategies.size();

         for (int var38 = 0; var38 < var37; var38++) {
            ResultsGroup var23 = var2.strategies.get(var38);
            OrdersList var20 = var23.orders();

            for (int var39 = 0; var39 < var20.size(); var39++) {
               Order var21 = var20.get(var39);
               var21.OrderId = var27++;
               if (!var23.isHodlStrategy()) {
                  if (var33 == -1L || var21.OpenTime < var33) {
                     var33 = var21.OpenTime;
                  }

                  if (var35 == -1L || var21.CloseTime > var35) {
                     var35 = var21.CloseTime;
                  }
               }
            }
         }

         if (var2.dateRangeLimited) {
            var33 = var2.startDay;
            var35 = var2.endDay;
         } else {
            var2.startDay = SQTime.getDateInMs(var33);
            var2.endDay = SQTime.getDateInMs(var35);
         }

         ResultsGroup var148 = ResultsGroup.merge(var2.strategies, new String[]{"AdditionalMarket", "CrossCheck_"}, null, var10, var11);
         var148.setName(var1);
         var148.orders().clear();
         PortfolioComposerLog var149 = new PortfolioComposerLog(var1, var2);
         long var40 = -1L;
         long var42 = -1L;

         for (int var44 = 0; var44 < var37; var44++) {
            double var30 = (double)var44 / var37 * 100.0 / 4.0;
            if (this.stopped) {
               break;
            }

            ResultsGroup var142 = var2.strategies.get(var44);
            String var19 = var142.getName();
            var6.put(var19, var142);
            Element var24 = var142.getStrategyXml();
            StrategyBase var25 = StrategyBase.createXmlStrategy(var24);
            var7.put(var19, var25);
            double var17 = var3[var44];
            var5.put(var19, var17);
            MoneyManagementMethod var26 = ProjectConfigHelper.getMoneyManagement(
               XMLUtil.stringToElement(var142.getLastSettings()).getChild("RiskMoneyManagement").getChild("MoneyManagement")
            );
            if (var26.getClass().getSimpleName().equals("ATRRiskBasedSizing")) {
               throw new Exception("Strategy '" + var19 + "' uses ATRRiskBasedSizing MM which is not supported in Portfolio Composer.");
            }

            var8.put(var19, var26);
            if (var142.isStockpickerStrategy()) {
               int var45 = 1;
               if (!var142.isHodlStrategy()) {
                  var45 = Stockpicker.getMaxOpenPosition(var24, 1);
                  var45 += Stockpicker.getMaxOpenPosition(var24, -1);
               }

               var9.put(var19, var45);
            }

            String var152 = var142.specialValues().getString(SpecialValues.PortfolioComposerHodlSymbol);
            OrdersList var138 = var142.orders();
            var138.sort(new OrderComparatorByOpenTime());

            for (int var46 = 0; var46 < var138.size(); var46++) {
               Order var139 = var138.get(var46);
               Order var22 = new Order(var139);
               var22.StrategyName = var19;
               if (var152 != null) {
                  var22.Symbol = var152;
                  var22.CloseType = 1;
                  this.modifyHodlOrder(var22, var33, var35);
               }

               long var15;
               if (OrderTypes.isMarketOrder(var139.OriginalType)) {
                  var15 = SQTime.getDateInMs(var22.OpenTime);
               } else {
                  var15 = SQTime.getDateInMs(var22.OriginalOpenTime);
               }

               if (var40 == -1L || var15 < var40) {
                  var40 = var15;
               }

               if (var42 == -1L || var15 > var42) {
                  var42 = var15;
               }

               if (!var4.containsKey(var15)) {
                  var4.put(var15, new ObjectArrayList());
               }

               ((ObjectArrayList)var4.get(var15)).add(var22);
            }
         }

         PortfolioComposer.DailyLog var150 = new PortfolioComposer.DailyLog();
         double var153 = var12;
         int var51 = 1;
         double var55 = 0.0;
         double var57 = 0.0;
         if (var2.dateRangeLimited) {
            var40 = var2.startDay;
            var42 = var2.endDay;
         }

         long var65 = SQTime.getDaysBetween(var40, var42);
         long var67 = 0L;
         long var69 = var40;
         Long2ObjectAVLTreeMap var71 = new Long2ObjectAVLTreeMap();
         Long2ObjectAVLTreeMap var72 = new Long2ObjectAVLTreeMap();
         Long2ObjectAVLTreeMap var73 = new Long2ObjectAVLTreeMap();
         Long2ObjectAVLTreeMap var74 = new Long2ObjectAVLTreeMap();

         do {
            var55 = this.calculateUsedMargin(var74, var6);
            var57 = var153 * var14 - var55;
            var150.reset();
            var150.print("\n=========================================================================================================");
            var150.print(
               String.format(
                  "%s, Account balance: %s, Free margin: %s, Used margin %s",
                  SQTime.toFullDateMinuteString(var69),
                  PlTypes.printPL(var153, (byte)10),
                  PlTypes.printPL(var57, (byte)10),
                  PlTypes.printPL(var55, (byte)10)
               )
            );
            this.printOpenTrades(var74, var150, var6, var32);
            ObjectArrayList var63 = (ObjectArrayList)var4.get(var69);
            if (var63 != null) {
               for (int var75 = 0; var75 < var63.size(); var75++) {
                  var150.tradesModified();
                  Order var140 = (Order)var63.get(var75);
                  if (var10.containsKey(var140.OrderId)) {
                     String var136 = var140.StrategyName;
                     ResultsGroup var143 = (ResultsGroup)var6.get(var136);
                     double var135 = var5.getDouble(var136);
                     MoneyManagementMethod var146 = (MoneyManagementMethod)var8.get(var136);
                     InstrumentInfo var52 = this.getInstrumentInfo(var143, var140);
                     boolean var64 = var143.isStockpickerStrategy();
                     StrategyBase var145 = (StrategyBase)var7.get(var136);
                     var145.accountBalance = var153;
                     var145.accountEquity = var153;
                     var145.initialCapital = var12;
                     var145.Symbol = var140.Symbol;
                     var145.Instrument = var52.instrument;
                     double var53;
                     if (OrderTypes.isMarketOrder(var140.OriginalType)) {
                        var53 = var140.OpenPrice;
                     } else {
                        var53 = var140.OriginalPrice;
                     }

                     if (var53 != 0.0) {
                        float var47;
                        try {
                           if (var64) {
                              var51 = var9.getInt(var136);
                              var146.setMaxOpenPositions(var51);
                           }

                           var146.setWeight(var135 / 100.0);
                           var47 = (float)var146.computeTradeSize(var145, var140.Symbol, var140.Type, var53, var140.StopLoss, var52.tickSize, var52.pointValue);
                        } catch (TradeSizeSmallerThanOneException var127) {
                           String var50 = var136
                              + "/"
                              + var140.Symbol
                              + "|OpenType="
                              + OrderTypes.toString(var140.OriginalType)
                              + "|OpenTime="
                              + SQTime.toFullDateMinuteString(var140.OpenTime)
                              + "|OpenPrice="
                              + PlTypes.printPL(var53, (byte)10, var32)
                              + "|MM=["
                              + var146.printFormatedName()
                              + ",Weight="
                              + var135
                              + "%,MaxPos="
                              + var51
                              + "]=0|CloseTime="
                              + SQTime.toFullDateMinuteString(var140.CloseTime);
                           var150.print(String.format("Order REJECTED '%s', %s (OriginalSize=%s).", var50, var127.getMessage(), var140.Size));
                           continue;
                        }

                        double var48 = this.calculateMargin(var47, var53, var52);
                        String var155;
                        if (var64) {
                           var155 = var136
                              + "/"
                              + var140.Symbol
                              + "|OpenType="
                              + OrderTypes.toString(var140.OriginalType)
                              + "|OpenTime="
                              + SQTime.toFullDateMinuteString(var140.OpenTime)
                              + "|OpenPrice="
                              + PlTypes.printPL(var53, (byte)10, var32)
                              + "|MM=["
                              + var146.printFormatedName()
                              + ",Weight="
                              + var135
                              + "%,MaxPos="
                              + var51
                              + "]="
                              + var47
                              + "|CloseTime="
                              + SQTime.toFullDateMinuteString(var140.CloseTime)
                              + "|Margin="
                              + PlTypes.printPL(var48, (byte)10);
                        } else {
                           var155 = var136
                              + "/"
                              + var140.Symbol
                              + "|OpenType="
                              + OrderTypes.toString(var140.OriginalType)
                              + "|OpenTime="
                              + SQTime.toFullDateMinuteString(var140.OpenTime)
                              + "|OpenPrice="
                              + PlTypes.printPL(var53, (byte)10, var32)
                              + "|MM=["
                              + var146.printFormatedName()
                              + ",Weight="
                              + var135
                              + "%]="
                              + var47
                              + "|CloseTime="
                              + SQTime.toFullDateMinuteString(var140.CloseTime)
                              + "|Margin="
                              + PlTypes.printPL(var48, (byte)10);
                        }

                        if (var48 > var57) {
                           var150.print(
                              String.format("Order REJECTED '%s', insufficient funds in the account. Free margin: %s", var155, PlTypes.printPL(var57, (byte)10))
                           );
                        } else {
                           var55 += var48;
                           var57 = var153 * var14 - var55;
                           if (var140.Size == var47) {
                              var150.print(
                                 String.format("Order ACCEPTED '%s', order size not changed. Free margin: %s", var155, PlTypes.printPL(var57, (byte)10))
                              );
                           } else {
                              var150.print(
                                 String.format(
                                    "Order ACCEPTED '%s', order size changed from %s to %s. Free margin: %s",
                                    var155,
                                    var140.Size,
                                    var47,
                                    PlTypes.printPL(var57, (byte)10)
                                 )
                              );
                           }

                           if (var140.Size != 0.0F) {
                              var140.CommSwap = var140.CommSwap * (var47 / var140.Size);
                           } else {
                              var140.CommSwap = 0.0F;
                           }

                           var140.Size = var47;
                           String var29 = (String)var10.get(var140.OrderId);
                           var140.SetupName = var29;
                           var148.orders().add(var140);
                           addProfit(var71, var72, var73, var69, var136, var140.PL, var153);
                           long var76 = SQTime.getDateInMs(var140.CloseTime);
                           if (!var74.containsKey(var76)) {
                              var74.put(var76, new ObjectArrayList());
                           }

                           ((ObjectArrayList)var74.get(var76)).add(var140);
                        }
                     }
                  }
               }
            }

            var150.print("-------------------------------------------");
            var63 = (ObjectArrayList)var74.get(var69);
            if (var63 != null) {
               ObjectListIterator var163 = var63.listIterator();

               while (var163.hasNext()) {
                  var150.tradesModified();
                  Order var141 = (Order)var163.next();
                  String var137 = var141.StrategyName;
                  ResultsGroup var144 = (ResultsGroup)var6.get(var137);
                  InstrumentInfo var157 = this.getInstrumentInfo(var144, var141);
                  double var59 = var141.PipsPL * var141.Size * (float)(var157.pointValue * var157.tickSize);
                  var153 += var59;
                  double var158;
                  if (OrderTypes.isMarketOrder(var141.OriginalType)) {
                     var158 = var141.OpenPrice;
                  } else {
                     var158 = var141.OriginalPrice;
                  }

                  double var154 = this.calculateMargin(var141.Size, var158, var157);
                  var55 -= var154;
                  var57 = var153 * var14 - var55;
                  String var156 = var137
                     + "/"
                     + var141.Symbol
                     + "|OpenType="
                     + OrderTypes.toString(var141.OriginalType)
                     + "|OpenTime="
                     + SQTime.toFullDateMinuteString(var141.OpenTime)
                     + "|OpenPrice="
                     + PlTypes.printPL(var141.OpenPrice, (byte)10, var32)
                     + "|Size="
                     + var141.Size
                     + "|CloseTime="
                     + SQTime.toFullDateMinuteString(var141.CloseTime)
                     + "|ClosePrice="
                     + PlTypes.printPL(var141.ClosePrice, (byte)10, var32)
                     + "|CloseType="
                     + OrderCloseTypes.toString(var141.CloseType)
                     + "|PL="
                     + PlTypes.printPL(var59, (byte)10)
                     + "|Margin="
                     + PlTypes.printPL(var154, (byte)10);
                  var150.print(String.format("Order CLOSED '%s'. Free margin: %s", var156, PlTypes.printPL(var57, (byte)10)));
                  var163.remove();
               }
            }

            if (var150.tradesModified) {
               var149.printNoNewLine(var150.toString());
            }

            var69 = SQTime.addDays(var69, 1);
            var67++;
            double var147 = 25.0 + (double)var67 / var65 * 100.0 / 2.0;
         } while (!this.stopped && var69 <= var42);

         var150.print("-------------------------------------------");
         HashSet var164 = new HashSet();
         ObjectIterator var165 = var71.values().iterator();

         while (var165.hasNext()) {
            Map var77 = (Map)var165.next();
            var164.addAll(var77.keySet());
         }

         ArrayList var166 = new ArrayList(var164);
         double[] var167 = new double[var164.size()];
         double[] var78 = new double[var164.size()];
         double[] var79 = new double[var164.size()];
         double[] var80 = new double[var164.size()];
         int var81 = 0;
         NormalDistribution var82 = new NormalDistribution();
         double var83 = var82.inverseCumulativeProbability(var2.confidenceLevel);

         for (int var85 = 0; var85 < var166.size(); var85++) {
            String var86 = (String)var166.get(var85);
            double var87 = calculateStandardDeviationLastNPeriods(var73, var86, var73.size());
            double var89 = getAverageProfitPercentage(var73, var86, -1);
            var78[var81] = var89;
            double var91 = getLastMarginUsed(var72, var86);
            double var93 = getAverageMarginUsed(var72, var86);
            var79[var81] = var89 * var93;
            var150.print("Strategy: " + var86);
            var150.print("Weight: " + SQUtils.d2String(var5.getDouble(var86), 2));
            var150.print(" ");
            var150.print("Daily expected return : " + PlTypes.printPL(var89 * 100.0, (byte)20, 3));
            var150.print("Daily expected return : " + PlTypes.printPL(var93 * var89, (byte)10, 2));
            var150.print(" ");
            var150.print("Daily Standard Deviation return : " + PlTypes.printPL(var87 * 100.0, (byte)20, 3));
            var167[var81] = var87;
            var150.print(" ");
            double var95 = -var93 * (var83 * var87);
            var150.print("VaR (with a mean of zero): " + PlTypes.printPL(var95, (byte)10, 2));
            var95 = var93 * (var89 - var83 * var87);
            var150.print("VaR : " + PlTypes.printPL(var95, (byte)10, 2));
            var80[var81] = var95;
            var150.print(" ");
            var150.print("Average Margin Used: " + PlTypes.printPL(var93, (byte)10, 2));
            var150.print(" ");
            var150.print(PlTypes.printPL(var2.confidenceLevel * 100.0, (byte)20, 0) + " confidence level");
            var150.print(
               SQUtils.d2String((1.0 - var2.confidenceLevel) * 100.0, 0)
                  + " days out of 100 days we can expect a loss of : "
                  + PlTypes.printPL(var95, (byte)10, 2)
            );
            var150.print(
               "VaR with "
                  + PlTypes.printPL((1.0 - var2.confidenceLevel) * 100.0, (byte)20, 2)
                  + " of the worse profitable return : "
                  + PlTypes.printPL(var95, (byte)10, 2)
            );
            var150.print(" ");

            for (String var98 : var164) {
               double var99 = getCorrelation(var73, var86, var98);
               var150.print("Correlation between " + var86 + " and " + var98 + " : " + SQUtils.d2String(var99, 3));
            }

            var150.print("----------------------------------------------------------------- ");
            var150.print(" ");
            var150.print(" ");
            var81++;
         }

         double var169 = 0.0;
         double var170 = 0.0;
         double var171 = 0.0;
         double var173 = 0.0;
         double var175 = 0.0;
         double var177 = 0.0;
         double var178 = 0.0;
         int var179 = var166.size();

         for (int var100 = 0; var100 < var166.size(); var100++) {
            String var101 = (String)var166.get(var100);
            var171 = var5.getDouble(var101) / 100.0;
            var169 += Math.pow(var171, 2.0) * Math.pow(var167[var100], 2.0);
            var175 += getAverageMarginUsed(var72, var101) * var171;
            var177 += var78[var100] * var171;
            var178 += var79[var100] * var171;

            for (int var102 = var100 + 1; var102 < var166.size(); var102++) {
               String var103 = (String)var166.get(var102);
               var173 = var5.getDouble(var103) / 100.0;
               double var104 = getCorrelation(var73, var101, var103);
               var170 += 2.0 * var171 * var173 * var104 * var167[var100] * var167[var102];
            }
         }

         var150.print(" ");
         var150.print(" ");
         var150.print("Daily Portfolio expected return : " + PlTypes.printPL(var177 * 100.0, (byte)20, 3));
         var150.print("Daily expected return : " + PlTypes.printPL(var178, (byte)10, 2));
         double var180 = Math.sqrt(var169 + var170);
         var150.print("StdDeviation Portfolio : " + PlTypes.printPL(var180 * 100.0, (byte)20, 3));
         double var181 = -var175 * (var83 * var180);
         var150.print("Portfolio VaR (with a mean of zero): " + PlTypes.printPL(var181, (byte)10, 2));
         var150.print(" ");
         double var182 = var2.RiskFreeRate / 36500.0;
         var150.print("Daily Risk Free Rate : " + PlTypes.printPL(var182 * 100.0, (byte)20, 5));
         var150.print(" ");
         double var106 = var177 - var182;
         double var108 = 0.0;
         if (var180 != 0.0) {
            var108 = var106 / var180;
         } else {
            var108 = -1.0;
         }

         var150.print("Daily Portfolio expected return : " + PlTypes.printPL(var177 * 100.0, (byte)20, 3));
         var150.print("Daily Portfolio ExcessReturn return : " + PlTypes.printPL(var106 * 100.0, (byte)20, 3));
         var150.print("Daily Portfolio SharpRatio : " + SQUtils.d2String(var108, 4));
         var150.print(" ");
         var150.print(" ");
         var148.specialValues().set(SpecialValues.PortfolioComposerDailyExpectedReturnPct, String.valueOf(var177));
         var148.specialValues().set(SpecialValues.PortfolioComposerDailyExpectedReturnMny, String.valueOf(var178));
         var148.specialValues().set(SpecialValues.PortfolioComposerStdDeviation, String.valueOf(var180));
         var148.specialValues().set(SpecialValues.PortfolioComposerSharpeRatio, String.valueOf(var108));
         double var110 = var2.confidenceLevel;
         var83 = var82.inverseCumulativeProbability(var110);
         double var112 = var177 - var83 * var180;
         var150.print("Portfolio VaR : " + PlTypes.printPL(var112 * 100.0, (byte)20, 3));
         double var114 = var175 * (var177 - var83 * var180);
         var150.print("Portfolio VaR : " + PlTypes.printPL(var114, (byte)10, 2));
         double var116 = calculateExpectedShortfall(var177, var180, var110);
         var150.print("Expected Shortfall (ES or CVaR) at % " + var110 * 100.0 + " confidence level: % " + PlTypes.printPL(var116 * 100.0, (byte)20, 3));
         double var118 = var175 * calculateExpectedShortfall(var177, var180, var110);
         var150.print("Expected Shortfall (ES or CVaR) at % " + var110 * 100.0 + " confidence level: % " + PlTypes.printPL(var118, (byte)10, 2));
         var148.specialValues().set(SpecialValues.PortfolioComposerWeights, Arrays.toString(var3));
         var148.specialValues().set(SpecialValues.PortfolioComposerValueAtRiskPct, String.valueOf(var112));
         var148.specialValues().set(SpecialValues.PortfolioComposerValueAtRiskMny, String.valueOf(var114));
         var148.specialValues().set(SpecialValues.PortfolioComposerConditionalValueatRiskPct, String.valueOf(var116));
         var148.specialValues().set(SpecialValues.PortfolioComposerConditionalValueatRiskMny, String.valueOf(var118));
         var149.printNoNewLine(var150.toString());
         this.updateHodlResults(var148, var11);
         var148.specialValues().set(SpecialValues.PortfolioHistoryFrom, var40);
         var148.specialValues().set(SpecialValues.PortfolioHistoryTo, var42);

         for (String var121 : var148.getResultKeys()) {
            Result var122 = var148.subResult(var121);
            var122.getSettings().set("PortfolioDataStart", var40);
            var122.getSettings().set("PortfolioDataEnd", var42);
         }

         var148.createPortfolioResult();
         var148.computeAllStats();
         var148.recalculateDailyEquity();
         var148.specialValues().set(SpecialValues.PortfolioComposerLog, var149.toString());
         this.updateMM(var148, var2.initialCapital);
         var148.setNote("PortfolioComposerWeights=" + Arrays.toString(var3));
         return var148;
      } catch (Exception | Error var128) {
         Log.error("PortfolioComposer Exc.", var128);
         throw new Exception("PortfolioComposer ERROR!!! " + var128.getMessage(), var128);
      } finally {
         new Long2ObjectAVLTreeMap();
         if (var5 != null) {
            var5.clear();
         }

         if (var6 != null) {
            var6.clear();
         }

         if (var7 != null) {
            var7.clear();
         }

         if (var8 != null) {
            var8.clear();
         }

         if (var9 != null) {
            var9.clear();
         }

         if (var10 != null) {
            var10.clear();
         }

         Object var130 = null;
         Object var131 = null;
         Object var132 = null;
         Object var133 = null;
         Object var134 = null;
      }
   }

   public static double calculateExpectedShortfall(double var0, double var2, double var4) {
      NormalDistribution var6 = new NormalDistribution();
      double var7 = var6.inverseCumulativeProbability(var4);
      double var9 = var6.density(var7);
      double var11 = 1.0 - var4;
      return var0 - var2 * (var9 / var11);
   }

   public static void addProfit(
      Long2ObjectAVLTreeMap<Map<String, Double>> var0,
      Long2ObjectAVLTreeMap<Map<String, Double>> var1,
      Long2ObjectAVLTreeMap<Map<String, Double>> var2,
      long var3,
      String var5,
      double var6,
      double var8
   ) {
      Map var10 = (Map)var0.getOrDefault(var3, new HashMap());
      var10.put(var5, var10.getOrDefault(var5, 0.0) + var6);
      var0.put(var3, var10);
      Map var11 = (Map)var1.getOrDefault(var3, new HashMap());
      var11.put(var5, var8);
      var1.put(var3, var11);
      if (var11.containsKey(var5)) {
         double var12 = (Double)var11.get(var5);
         double var14 = 0.0;
         if (var12 != 0.0) {
            var14 = (Double)var10.get(var5) / var12;
         } else {
            var14 = 0.0;
         }

         Map var16 = (Map)var2.getOrDefault(var3, new HashMap());
         var16.put(var5, var14);
         var2.put(var3, var16);
      }
   }

   public static double calculateStandardDeviationLastNPeriods(Long2ObjectAVLTreeMap<Map<String, Double>> var0, String var1, int var2) {
      double var3 = 0.0;
      double var5 = 0.0;
      int var7 = 0;
      int var8 = var0.size();
      int var9 = Math.max(0, var8 - var2);
      DescriptiveStatistics var10 = new DescriptiveStatistics();

      for (long var12 : var0.keySet().stream().skip(var9).toList()) {
         Map var14 = (Map)var0.get(var12);
         if (var14 != null) {
            if (var14.containsKey(var1)) {
               double var15 = (Double)var14.get(var1);
               if (!Double.isNaN(var15) && var15 != Double.NEGATIVE_INFINITY && var15 != Double.POSITIVE_INFINITY) {
                  var10.addValue((Double)var14.get(var1));
                  var3 += var15;
                  var7++;
               } else {
                  var10.addValue(0.0);
                  var3 += 0.0;
                  var7++;
               }
            } else {
               var10.addValue(0.0);
               var7++;
            }
         }
      }

      if (var7 != 0 && var3 != 0.0) {
         double var19 = var3 / var7;

         for (long var21 : var0.keySet().stream().skip(var9).toList()) {
            Map var16 = (Map)var0.get(var21);
            if (var16 != null) {
               if (var16.containsKey(var1)) {
                  double var17 = (Double)var16.get(var1);
                  if (!Double.isNaN(var17) && var17 != Double.NEGATIVE_INFINITY && var17 != Double.POSITIVE_INFINITY) {
                     var5 += (var17 - var19) * (var17 - var19);
                  } else {
                     var5 += (0.0 - var19) * (0.0 - var19);
                  }
               } else {
                  var5 += (0.0 - var19) * (0.0 - var19);
               }
            }
         }

         double var20 = var5 / var7;
         return Math.sqrt(var20);
      } else {
         return 0.0;
      }
   }

   public static double getAverageProfitPercentage(Long2ObjectAVLTreeMap<Map<String, Double>> var0, String var1, int var2) {
      DescriptiveStatistics var3 = new DescriptiveStatistics();
      boolean var4 = false;
      if (var2 > 0) {
         int var5 = var0.size();
         int var6 = Math.max(0, var5 - var2);

         for (long var8 : var0.keySet().stream().skip(var6).toList()) {
            Map var10 = (Map)var0.get(var8);
            if (var10 != null) {
               if (var10.containsKey(var1)) {
                  double var11 = (Double)var10.get(var1);
                  if (!Double.isNaN(var11) && var11 != Double.NEGATIVE_INFINITY && var11 != Double.POSITIVE_INFINITY) {
                     var3.addValue(var11);
                  } else {
                     var3.addValue(0.0);
                  }
               } else {
                  var3.addValue(0.0);
               }
            }
         }
      } else {
         ObjectBidirectionalIterator var13 = var0.entrySet().iterator();

         while (var13.hasNext()) {
            Entry var14 = (Entry)var13.next();
            Map var15 = (Map)var14.getValue();
            if (var15.containsKey(var1)) {
               double var16 = (Double)var15.get(var1);
               if (!Double.isNaN(var16) && var16 != Double.NEGATIVE_INFINITY && var16 != Double.POSITIVE_INFINITY) {
                  var3.addValue(var16);
               } else {
                  var3.addValue(0.0);
               }
            } else {
               var3.addValue(0.0);
            }
         }
      }

      return var3.getMean();
   }

   public static double getLastMarginUsed(Long2ObjectAVLTreeMap<Map<String, Double>> var0, String var1) {
      if (var0.isEmpty()) {
         return 0.0;
      }

      for (long var3 : var0.keySet().stream().sorted((var0x, var1x) -> Long.compare(var1x, var0x)).toList()) {
         Map var5 = (Map)var0.get(var3);
         if (var5.containsKey(var1)) {
            return (Double)var5.get(var1);
         }
      }

      return 0.0;
   }

   public static double getAverageMarginUsed(Long2ObjectAVLTreeMap<Map<String, Double>> var0, String var1) {
      DescriptiveStatistics var2 = new DescriptiveStatistics();
      ObjectBidirectionalIterator var3 = var0.entrySet().iterator();

      while (var3.hasNext()) {
         Entry var4 = (Entry)var3.next();
         Map var5 = (Map)var4.getValue();
         if (var5.containsKey(var1)) {
            var2.addValue((Double)var5.get(var1));
         }
      }

      return var2.getMean();
   }

   public static double getCorrelation(Long2ObjectAVLTreeMap<Map<String, Double>> var0, String var1, String var2) {
      ArrayList var3 = new ArrayList();
      ArrayList var4 = new ArrayList();
      ObjectBidirectionalIterator var5 = var0.entrySet().iterator();

      while (var5.hasNext()) {
         Entry var6 = (Entry)var5.next();
         Map var7 = (Map)var6.getValue();
         if (var7.containsKey(var1) && var7.containsKey(var2)) {
            var3.add((Double)var7.get(var1));
            var4.add((Double)var7.get(var2));
         }
      }

      if (var3.size() != 0 && var4.size() != 0) {
         double[] var10 = var3.stream().mapToDouble(Double::doubleValue).toArray();
         double[] var11 = var4.stream().mapToDouble(Double::doubleValue).toArray();
         CorrelationComputer var12 = new CorrelationComputer();
         double var8 = CorrelationComputer.calculateCorrelation(var10, var11);
         return !Double.isNaN(var8) && var8 != Double.NEGATIVE_INFINITY && var8 != Double.POSITIVE_INFINITY ? var8 : 0.0;
      } else {
         return 0.0;
      }
   }

   private void updateMM(ResultsGroup var1, double var2) {
      try {
         String var4 = var1.getLastSettings();
         if (var4 == null) {
            return;
         }

         Element var5 = XMLUtil.stringToElement(var4);
         Element var6 = var5.getChild("RiskMoneyManagement");
         MoneyManagementMethod var7 = MoneyManagementMethodsList.create("RiskFixedPctOfAccount", new Object[]{100, 0, 0, 0, 1000000});
         var7.setInitialCapital(var2);
         Element var8 = var7.getXML();
         var6.removeContent();
         var6.addContent(var8);
         var1.setLastSettings(XMLUtil.elementToString(var5));
      } catch (Exception var9) {
         Log.error("Updating MM failed. Exc." + var9.getMessage(), var9);
      }
   }

   private void updateHodlResults(ResultsGroup var1, ObjectArrayList<String> var2) throws Exception {
      for (int var4 = 0; var4 < var2.size(); var4++) {
         String var3 = (String)var2.get(var4);
         OrdersList var5 = var1.orders().filter(var3, (byte)0, (byte)127);
         if (var5.size() == 1) {
            Order var6 = var5.get(0);
            var1.orders().removeById(var6.OrderId);
            ChartDef var7 = new ChartDef("History", var6.Symbol, "D1", var6.OpenTime, var6.CloseTime, 0.0, "No Session");
            IDataLoader var8 = DataManager.getDataLoader(var7, 1, null);
            var8.open();
            double var9 = -1.0;
            double var17 = 0.0;

            for (int var19 = 1; var8.hasNextTick(); var19++) {
               VersatileData var20 = new VersatileData();
               var8.getNextTick(var20);
               long var11 = SQTime.getDateInMs(var20.time);
               double var13 = var20.open;
               double var15 = var20.close;
               Order var21 = new Order();
               var21.Ticket = var19;
               var21.Symbol = var6.Symbol;
               var21.SetupName = var6.SetupName;
               var21.Size = var6.Size;
               var21.Type = 1;
               var21.OpenPrice = (float)(var9 == -1.0 ? var13 : var9);
               var21.ClosePrice = (float)var15;
               var21.OpenTime = var11;
               var21.CloseTime = var11;
               var21.PL = (var21.ClosePrice - var21.OpenPrice) * var21.Size;
               var21.PctPL = (float)OrderPLComputer.getPercentagePL(var21.PL, var17);
               var21.CloseType = 1;
               var1.orders().add(var21);
               var9 = var15;
            }
         }
      }
   }

   private void modifyHodlOrder(Order var1, long var2, long var4) throws Exception {
      try {
         String var6 = var1.Symbol;
         DataInfo var7 = DataManager.getDataInfo("History", var6);
         if (var7 != null && var7.symbolInfo != null) {
            ChartDef var8 = new ChartDef("History", var6, "D1", var2, var4, 0.0, "No Session");
            IDataLoader var9 = DataManager.getDataLoader(var8, 1, null);
            var9.open();
            var1.OpenTime = -1L;
            var1.CloseTime = -1L;

            while (var9.hasNextTick()) {
               VersatileData var10 = new VersatileData();
               var9.getNextTick(var10);
               long var11 = SQTime.getDateInMs(var10.time);
               if (var1.OpenTime == -1L || var11 <= var2) {
                  var1.OpenTime = var11;
                  var1.OpenPrice = (float)var10.open;
               }

               if (var1.CloseTime == -1L || var11 <= var4) {
                  var1.CloseTime = var11;
                  var1.ClosePrice = (float)var10.close;
               }
            }

            Log.info("HODL order: " + var1.toString());
         } else {
            throw new TradingException(String.format("Instrument info for symbol '%s doesn't exist", var6));
         }
      } catch (Exception var13) {
         Log.error("Hodl order Exc.", var13);
         throw new Exception(
            String.format(
               "Error occurred while loading symbol '%s' data for the Buy & Hold strategy '%s'. Reason: %s", var1.Symbol, var1.StrategyName, var13.getMessage()
            ),
            var13
         );
      }
   }

   private double calculateMargin(float var1, double var2, InstrumentInfo var4) {
      return var1 * var2 * var4.pointValue;
   }

   private InstrumentInfo getInstrumentInfo(ResultsGroup var1, Order var2) throws Exception {
      InstrumentInfo var3 = null;
      SymbolInfo var4 = var1.symbols().get(var2.Symbol);
      if (var4 != null) {
         var3 = var4.InstrumentInfo();
      }

      if (var3 == null) {
         var3 = var1.symbols().getDefaultInstrumentInfo();
      }

      if (var3 == null) {
         throw new Exception(String.format("Instrument info not found for %s / %s", var1.getName(), var2.Symbol));
      } else {
         return var3;
      }
   }

   private double calculateUsedMargin(Long2ObjectAVLTreeMap<ObjectArrayList<Order>> var1, Object2ObjectOpenHashMap<String, ResultsGroup> var2) throws Exception {
      double var3 = 0.0;
      ObjectBidirectionalIterator var8 = var1.long2ObjectEntrySet().iterator();

      while (var8.hasNext()) {
         it.unimi.dsi.fastutil.longs.Long2ObjectMap.Entry var9 = (it.unimi.dsi.fastutil.longs.Long2ObjectMap.Entry)var8.next();
         ObjectArrayList var10 = (ObjectArrayList)var9.getValue();

         for (int var11 = 0; var11 < var10.size(); var11++) {
            Order var5 = (Order)var10.get(var11);
            double var6;
            if (OrderTypes.isMarketOrder(var5.OriginalType)) {
               var6 = var5.OpenPrice;
            } else {
               var6 = var5.OriginalPrice;
            }

            ResultsGroup var12 = (ResultsGroup)var2.get(var5.StrategyName);
            InstrumentInfo var13 = this.getInstrumentInfo(var12, var5);
            var3 += this.calculateMargin(var5.Size, var6, var13);
         }
      }

      return var3;
   }

   private void printOpenTrades(
      Long2ObjectAVLTreeMap<ObjectArrayList<Order>> var1, PortfolioComposer.DailyLog var2, Object2ObjectOpenHashMap<String, ResultsGroup> var3, int var4
   ) throws Exception {
      var2.print("Open trades (used margin)");
      int var9 = 0;
      ObjectBidirectionalIterator var12 = var1.long2ObjectEntrySet().iterator();

      while (var12.hasNext()) {
         it.unimi.dsi.fastutil.longs.Long2ObjectMap.Entry var13 = (it.unimi.dsi.fastutil.longs.Long2ObjectMap.Entry)var12.next();
         ObjectArrayList var14 = (ObjectArrayList)var13.getValue();

         for (int var15 = 0; var15 < var14.size(); var15++) {
            Order var5 = (Order)var14.get(var15);
            double var7;
            if (OrderTypes.isMarketOrder(var5.OriginalType)) {
               var7 = var5.OpenPrice;
            } else {
               var7 = var5.OriginalPrice;
            }

            var5 = (Order)var14.get(var15);
            ResultsGroup var16 = (ResultsGroup)var3.get(var5.StrategyName);
            InstrumentInfo var17 = this.getInstrumentInfo(var16, var5);
            double var10 = this.calculateMargin(var5.Size, var7, var17);
            var9++;
            String var6 = var5.StrategyName
               + "/"
               + var5.Symbol
               + "|OpenType="
               + OrderTypes.toString(var5.OriginalType)
               + "|OpenTime="
               + SQTime.toFullDateMinuteString(var5.OpenTime)
               + "|OpenPrice="
               + PlTypes.printPL(var7, (byte)10, var4)
               + "|Size="
               + var5.Size
               + "|CloseTime="
               + SQTime.toFullDateMinuteString(var5.CloseTime)
               + "|Margin="
               + SQUtils.d2(var10);
            var2.print(var9 + ": " + var6);
         }
      }

      if (var9 == 0) {
         var2.print("No open trades");
      }

      var2.print("-------------------------------------------");
   }

   public void stop() {
      this.stopped = true;
   }

   private class DailyLog {
      public boolean tradesModified = false;
      private StringBuilder b = new StringBuilder();

      private DailyLog() {
      }

      public void print(String var1) {
         this.b.append(var1 + "\n");
      }

      public void tradesModified() {
         this.tradesModified = true;
      }

      public void reset() {
         this.b = new StringBuilder();
         this.tradesModified = false;
      }

      @Override
      public String toString() {
         return this.b.toString();
      }
   }
}
