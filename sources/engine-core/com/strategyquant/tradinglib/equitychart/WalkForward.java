package com.strategyquant.tradinglib.equitychart;

import com.strategyquant.tradinglib.Order;
import com.strategyquant.tradinglib.OrdersList;
import com.strategyquant.tradinglib.ResultsGroup;
import com.strategyquant.tradinglib.WalkForwardPeriod;
import com.strategyquant.tradinglib.WalkForwardResult;
import com.strategyquant.tradinglib.optimization.WalkForwardMatrixResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WalkForward {
   public static final Logger Log = LoggerFactory.getLogger(WalkForward.class);
   private int optFromTradeIndex = -1;
   private int optToTradeIndex = -1;
   private int runFromTradeIndex = -1;
   private int runToTradeIndex = -1;

   public void print(EquityChart var1, OrdersList var2, ResultsGroup var3, String var4, String var5, byte var6, byte var7) throws Exception {
      WalkForwardMatrixResult var8 = (WalkForwardMatrixResult)var3.mainResult().get("WalkForwardResult");
      if (var8 != null) {
         WalkForwardResult var9 = var8.getWFResult(var5, true);
         if (var9 != null) {
            MainChartDataset var10 = var1.getMainChartDatasets().get(0);
            long var11 = var10.xVals[0];
            SubChart var13 = null;
            if (var7 == 127) {
               var13 = new SubChart("WalkForward");
               var13.name = "WF Periods";
               var1.addSubChart(var13);
            }

            if (var4.equals("time")) {
               for (int var14 = 0; var14 < var9.wfPeriods.size(); var14++) {
                  WalkForwardPeriod var15 = var9.wfPeriods.get(var14);
                  if (var7 == 127) {
                     SubChartDataset var16 = new SubChartDataset("WalkForwardOpt" + var14, 2);
                     var16.addValue(var15.optimizeFrom, var14 + 1);
                     var16.addValue(var15.optimizeTo, var14 + 1);
                     var16.color = "#00ff00";
                     var16.thickness = 3;
                     var13.addDataset(var16);
                     if (var14 != var9.wfPeriods.size() - 1) {
                        SubChartDataset var17 = new SubChartDataset("WalkForwardRun" + var14, 2);
                        var17.addValue(var15.runFrom, var14 + 1);
                        var17.addValue(var15.runTo, var14 + 1);
                        var17.color = "#ff0000";
                        var17.thickness = 3;
                        var13.addDataset(var17);
                     }

                     Marker var28 = new Marker();
                     var28.x = var15.runFrom;
                     var1.addMarker(var28);
                  } else if (var7 == 20) {
                     Marker var24 = new Marker();
                     var24.x = var15.runFrom < var11 ? var11 : var15.runFrom;
                     var1.addMarker(var24);
                  }
               }
            } else {
               for (int var19 = 0; var19 < var9.wfPeriods.size(); var19++) {
                  WalkForwardPeriod var21 = var9.wfPeriods.get(var19);
                  this.computeTradeIndexes(var2, var21);
                  if (var7 == 127) {
                     SubChartDataset var25 = new SubChartDataset("WalkForwardOpt" + var19, 2);
                     var25.addValue(this.optFromTradeIndex, var19 + 1);
                     var25.addValue(this.optToTradeIndex, var19 + 1);
                     var25.color = "#00ff00";
                     var25.thickness = 3;
                     var13.addDataset(var25);
                     if (var19 != var9.wfPeriods.size() - 1) {
                        SubChartDataset var29 = new SubChartDataset("WalkForwardRun" + var19, 2);
                        var29.addValue(this.runFromTradeIndex, var19 + 1);
                        var29.addValue(this.runToTradeIndex, var19 + 1);
                        var29.color = "#ff0000";
                        var29.thickness = 3;
                        var13.addDataset(var29);
                     }

                     Marker var30 = new Marker();
                     var30.x = this.runFromTradeIndex;
                     var1.addMarker(var30);
                  } else if (var7 == 20) {
                     Marker var26 = new Marker();
                     var26.x = this.runFromTradeIndex;
                     var1.addMarker(var26);
                  }
               }
            }

            if (var4.equals("time") || var7 == 127 || var7 == 10) {
               OrdersList var20 = var3.orders().filter(var3.getMainResultKey(), var6, (byte)127, true);
               if (var7 == 10 || var7 == 20) {
                  OrdersList var22 = new OrdersList("ordersOriginal");
                  WalkForwardPeriod var27 = var9.wfPeriods.get(0);

                  for (int var31 = 0; var31 < var20.size(); var31++) {
                     Order var18 = var20.get(var31);
                     if (var7 == 10) {
                        if (var18.CloseTime > var27.optimizeTo) {
                           break;
                        }

                        var22.add(var20.get(var31));
                     } else if (var18.CloseTime > var27.optimizeTo) {
                        var22.add(var20.get(var31));
                     }
                  }

                  var20 = var22;
               }

               MainChartDataset var23 = EquityChartUtils.generateDataset(var20, var4);
               var23.name = "Original strategy";
               var23.color = "#969696";
               var23.colorDark = "#969696";
               var1.addMainChartDataset(var23);
            }
         }
      }
   }

   private void computeTradeIndexes(OrdersList var1, WalkForwardPeriod var2) {
      this.optFromTradeIndex = -1;
      this.optToTradeIndex = -1;
      this.runFromTradeIndex = -1;
      this.runToTradeIndex = -1;

      for (int var4 = 0; var4 < var1.size(); var4++) {
         Order var3 = var1.get(var4);
         if (var3.CloseTime >= var2.optimizeFrom && this.optFromTradeIndex == -1) {
            this.optFromTradeIndex = var4;
         }

         if (var3.CloseTime >= var2.optimizeTo && this.optToTradeIndex == -1) {
            this.optToTradeIndex = var4;
         }

         if (var3.CloseTime >= var2.runFrom && this.runFromTradeIndex == -1) {
            this.runFromTradeIndex = var4;
         }

         if (var3.CloseTime >= var2.runTo && this.runToTradeIndex == -1) {
            this.runToTradeIndex = var4;
            break;
         }
      }

      if (this.runToTradeIndex == -1) {
         this.runToTradeIndex = var1.size() - 1;
      }

      if (this.optToTradeIndex == -1) {
         this.optToTradeIndex = var1.size() - 1;
         this.runFromTradeIndex = var1.size() - 1;
      }
   }
}
