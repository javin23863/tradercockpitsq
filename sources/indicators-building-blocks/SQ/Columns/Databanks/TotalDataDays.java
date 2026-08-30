package SQ.Columns.Databanks;

import com.strategyquant.datalib.ChartDef;
import com.strategyquant.lib.L;
import com.strategyquant.lib.SQTime;
import com.strategyquant.lib.SettingsMap;
import com.strategyquant.tradinglib.ChartSetup;
import com.strategyquant.tradinglib.DatabankColumn;
import com.strategyquant.tradinglib.OrdersList;
import com.strategyquant.tradinglib.SQStats;
import com.strategyquant.tradinglib.SampleTypes;
import com.strategyquant.tradinglib.StatsTypeCombination;
import com.strategyquant.tradinglib.strategy.OutOfSample;

public class TotalDataDays extends DatabankColumn {
   public TotalDataDays() {
      super(L.tsq("Total Data Days"), "Integer", (byte)2, 0.0, 10.0, 1000.0);
      this.setPLTypeRestrictions(new byte[]{10});
      this.setDirectionRestrictions(new byte[]{0});
   }

   public double compute(SQStats var1, StatsTypeCombination var2, OrdersList var3, SettingsMap var4, SQStats var5, SQStats var6) throws Exception {
      int var7 = 0;
      int var8 = 0;
      int var9 = 0;
      ChartSetup var10 = (ChartSetup)var4.get("BacktestChart");
      if (var10 != null) {
         ChartDef var11 = var10.getMainChart();
         if (var11 != null) {
            long var12 = var11.getHistoryFrom();
            long var14 = var11.getHistoryTo();
            OutOfSample var16 = (OutOfSample)var4.get("OutOfSample");
            var7 = this.getDaysBySampleType(var16, var2.getSampleType(), var12, var14);
            var8 = (int)(var7 / 30.41);
            var9 = var7 / 365;
            if (var16 == null || var2.getSampleType() == 127) {
               var7 = SQTime.getDaysBetween(var12, var14);
               var8 = (int)(var7 / 30.41);
               var9 = var7 / 365;
            }

            if (var7 > 0) {
               if (var8 == 0) {
                  var8 = 1;
               }

               if (var9 == 0) {
                  var9 = 1;
               }
            }
         }
      } else {
         long var17 = var4.getLong("PortfolioDataStart", Long.MAX_VALUE);
         long var13 = var4.getLong("PortfolioDataEnd", Long.MIN_VALUE);
         if (var17 != Long.MAX_VALUE && var13 != Long.MIN_VALUE) {
            var7 = SQTime.getDaysBetween(var17, var13);
            var8 = SQTime.getMonthsBetween(var17, var13);
            var9 = SQTime.getYearsBetween(var17, var13);
            if (var7 > 0) {
               if (var8 == 0) {
                  var8 = 1;
               }

               if (var9 == 0) {
                  var9 = 1;
               }
            }
         }
      }

      var1.set("TotalDataMonths", var8);
      var1.set("TotalDataYears", var9);
      return var7;
   }

   private int getDaysBySampleType(OutOfSample var1, byte var2, long var3, long var5) {
      if (var1 != null && var2 != 127) {
         int var7 = 0;

         for (int var8 = 0; var8 < var1.getRangesCount(); var8++) {
            byte var9 = var1.getSampleType(var8);
            if (var2 == 40) {
               if (var9 < 40 || var9 > 50) {
                  continue;
               }
            } else if (var2 == 20) {
               if (var9 < 20 || var9 > 30) {
                  continue;
               }
            } else if (var2 == 10) {
               if (SampleTypes.isISV(var9) || var9 == 11 || var9 == 40 || var9 == 10) {
                  continue;
               }
            } else if (var2 == 11) {
               if (SampleTypes.isISV(var9) || SampleTypes.isOOS(var9)) {
                  long var10 = var1.getDateFrom(var8);
                  long var12 = var1.getDateTo(var8);
                  var7 += SQTime.getDaysBetween(var10, var12);
                  continue;
               }
            } else if (var2 != var9) {
               continue;
            }

            long var15 = var1.getDateFrom(var8);
            long var16 = var1.getDateTo(var8);
            var7 += SQTime.getDaysBetween(var15, var16);
         }

         if (var2 != 10 && var2 != 11) {
            return var7;
         }

         int var14 = SQTime.getDaysBetween(var3, var5);
         return var14 - var7;
      } else {
         return SQTime.getDaysBetween(var3, var5);
      }
   }
}
