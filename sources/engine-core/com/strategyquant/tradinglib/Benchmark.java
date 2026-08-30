package com.strategyquant.tradinglib;

import com.strategyquant.datalib.ChartDef;
import com.strategyquant.datalib.DataInfo;
import com.strategyquant.datalib.data.DataManager;
import com.strategyquant.datalib.data.io.IDataLoader;
import com.strategyquant.datalib.data.io.VersatileData;
import com.strategyquant.lib.SQTime;
import com.strategyquant.lib.XMLUtil;
import com.strategyquant.tradinglib.project.ProjectConfigHelper;
import org.jdom2.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Benchmark {
   public static final Logger Log = LoggerFactory.getLogger(Benchmark.class);

   public BenchmarkResult calculate(ResultsGroup var1, long var2, long var4) throws Exception {
      try {
         BenchmarkResult var6 = new BenchmarkResult();
         Element var7 = XMLUtil.stringToElement(var1.getLastSettings());
         Element var8 = var7.getChild("RiskMoneyManagement");
         MoneyManagementMethod var9 = ProjectConfigHelper.getMoneyManagement(var8.getChild("MoneyManagement"));
         double var10 = var9.getInitialCapital();
         double var16 = 9.9999999E7;
         double var18 = 9.9999999E7;
         double var20 = var10;
         double var24 = 0.0;
         double var26 = 0.0;
         long var28 = 0L;
         double var30 = 0.0;
         int var32 = 0;
         String var33 = "SPY_benchmark.D";
         DataInfo var34 = DataManager.getDataInfo("History", var33);
         if (var34 == null) {
            throw new Exception("Cannot get data info for symbol " + var33);
         }

         Log.info(String.format("SPY benchmark from %s to %s ...", SQTime.toDateString(var2), SQTime.toDateString(var4)));
         ChartDef var35 = new ChartDef("History", var33, "D1", var2, var4, 2.5, "No Session");
         IDataLoader var36 = DataManager.getDataLoader(var35, 1, null);
         var36.open();

         for (; var36.hasNextTick(); var32++) {
            VersatileData var37 = new VersatileData();
            var36.getNextTick(var37);
            long var38 = var37.time;
            double var40 = var37.close;
            if (var32 == 0) {
               var26 = var40;

               try {
                  if (var9 != null) {
                     StrategyBase var42 = StrategyBase.createXmlStrategy(var1.getStrategyXml());
                     var42.accountBalance = var10;
                     var42.accountEquity = var10;
                     var30 = var9.computeTradeSize(var42, var34.symbol, (byte)1, var26, 0.0, var34.symbolInfo.tickSize, var34.symbolInfo.pointValue);
                  } else {
                     var30 = var10 / var26;
                  }
               } catch (Exception var43) {
                  throw new Exception("Failed to calculate initial buy amount from MoneyManagement method - " + var43.getMessage(), var43);
               }
            }

            var24 = (var40 - var26) * var30;
            var6.equity.put(var38, var24);
            double var22 = var10 + var24;
            double var12 = (float)this.specialSubtraction(var20, var22);
            double var14 = (float)this.getPercentageDD(var12, var20);
            var6.ddMapMoney.put(SQTime.getDateInMs(var38), var12);
            var6.ddMapPct.put(SQTime.getDateInMs(var38), var14);
            if (var14 < var16) {
               var16 = var14;
            }

            if (var12 < var18) {
               var18 = var12;
            }

            if (var22 > var20) {
               var20 = var22;
            }
         }

         return var6;
      } catch (Exception var44) {
         Log.error("Cannot calculate benchmark performance. ", var44);
         throw new Exception("Cannot calculate benchmark performance. ", var44);
      }
   }

   private double specialSubtraction(double var1, double var3) {
      double var5 = var1 - var3;
      if (var5 < 0.0) {
         var5 = 0.0;
      }

      return -1.0 * var5;
   }

   private double getPercentageDD(double var1, double var3) {
      return !(var3 <= 0.0) && !(var1 > var3) ? var1 / (var3 / 100.0) : -1.0;
   }
}
