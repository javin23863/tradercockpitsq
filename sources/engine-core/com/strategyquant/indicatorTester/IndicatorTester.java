package com.strategyquant.indicatorTester;

import com.strategyquant.datalib.DataSeries;
import com.strategyquant.datalib.InstrumentInfo;
import com.strategyquant.datalib.data.io.TimeframeRecognizer;
import com.strategyquant.datalib.dataseries.MedianDataSeries;
import com.strategyquant.datalib.dataseries.TimeDataSeries;
import com.strategyquant.datalib.dataseries.TypicalDataSeries;
import com.strategyquant.datalib.dataseries.WeightedDataSeries;
import com.strategyquant.lib.L;
import com.strategyquant.lib.SQTime;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.constants.SQPaths;
import com.strategyquant.tradinglib.ChartData;
import com.strategyquant.tradinglib.StrategyBase;
import com.strategyquant.tradinglib.indicator.IndicatorsObj;
import com.strategyquant.tradinglib.simulator.Engines;
import java.io.File;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.locks.ReentrantLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IndicatorTester {
   public static final Logger Log = LoggerFactory.getLogger("IndicatorTester");
   private SimpleDateFormat df = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss");
   private IndicatorsObj indicators;
   public static final int SRC_CHART = 0;
   public static final int SRC_OPEN = 1;
   public static final int SRC_HIGH = 2;
   public static final int SRC_LOW = 3;
   public static final int SRC_CLOSE = 4;
   public static final int SRC_VOLUME = 5;
   public static final int SRC_TYPICAL = 6;
   public static final int SRC_MEDIAN = 7;
   public static final int SRC_WEIGHTED = 8;
   private int startCompareIndex = 400;
   private int maxInvalidLines = 50;
   private String timeframe = "H1";
   private int digits = 5;
   private double tickSize = 1.0E-4;
   private double tickStep = 1.0E-5;
   public String engine = "MetaTrader5 (hedged)";
   private ReentrantLock lock = new ReentrantLock();
   private String errorMsg;
   private ArrayList<String> dataErrors = new ArrayList<>();
   private boolean showGrid = false;
   private TimeframeRecognizer tfRecognizer = new TimeframeRecognizer();
   private String indicatorTestsPath;

   public IndicatorTester() throws Exception {
   }

   public void init(String var1) throws Exception {
      this.init(var1, SQPaths.indicatorTestsPath, this.digits, this.tickSize, this.tickStep);
   }

   public void init(String var1, int var2, double var3, double var5) throws Exception {
      this.init(var1, SQPaths.indicatorTestsPath, var2, var3, var5);
   }

   public void init(String var1, String var2, int var3, double var4, double var6) throws Exception {
      try {
         this.engine = var1;
         int var8 = Engines.getEngine(var1);
         this.indicators = StrategyBase.createIndicatorsObj(var8);
         this.indicators.Engine = var8;
         this.indicatorTestsPath = var2;
         this.digits = var3;
         this.tickSize = var4;
         this.tickStep = var6;
      } catch (Exception var9) {
         throw new Exception("Initialization of IndicatorTester failed. ", var9);
      }
   }

   public boolean checkFileExists(String var1) {
      return new File(this.getFilePath(var1)).exists();
   }

   public void runTest(String var1, int var2, String var3, String var4, Object[] var5, int var6, int var7) throws IndicatorTestException {
      if (this.indicators == null) {
         throw new IndicatorTestException(2, new Exception("First init(engine) must be called."));
      }

      this.lock.lock();
      Log.info("--------------------------------------------------------------------------------------------------------------------");
      Log.info("Testing indicator '" + var3 + "' using source file '" + var1 + "', engine=" + this.engine);
      DataSeries var8 = new DataSeries();
      DataSeries var9 = new DataSeries();
      ChartData var10 = null;

      try {
         String var11 = this.getFilePath(var1);
         File var12 = new File(var11);

         try {
            var10 = this.loadSourceData(var11, var9);
         } catch (Exception var26) {
            throw new IndicatorTestException(1, var26);
         }

         if (var9.size() == 0) {
            throw new IndicatorTestException(5, new Exception(var12.getAbsolutePath()));
         }

         try {
            var5 = var5 == null ? new Object[0] : var5;
            Object[] var13 = new Object[var5.length + 1];
            var13[0] = this.getSourceData(var10, var2);
            System.arraycopy(var5, 0, var13, 1, var5.length);
            Object var14 = this.callMethod(this.indicators, var3, var13);
            var8 = (DataSeries)var14.getClass().getField(var4).get(var14);
            var8.setShift(var7);
         } catch (Exception var25) {
            throw new Exception(L.t("Cannot instantiate indicator '%s'!", new Object[]{var3}), var25);
         }

         int var31 = var10.Time.size() - this.startCompareIndex - 1;
         int var32 = 0;
         var8.get(0);

         for (int var15 = var31; var15 >= 0; var15--) {
            double var16 = var8.get(var15);
            if (!this.checkPrecision(var16, var9.get(var15), var6)) {
               String var18 = this.df.format(new Date(var10.Time(var15))) + "," + var16 + "," + var9.get(var15);
               this.showGrid = true;
               this.dataErrors.add(var18);
               Log.debug(var12.getName() + " - " + var18);
               if (++var32 > 150) {
                  this.errorMsg = "more than 150 differences";
                  throw new IndicatorTestException(3, new Exception(this.errorMsg));
               }
            }
         }

         if (var32 > 0) {
            this.errorMsg = var32 + " differences";
         }
      } catch (IndicatorTestException var27) {
         throw var27;
      } catch (Exception var28) {
         this.errorMsg = var28.getMessage();
         this.dataErrors.add(SQUtils.stackTraceToString(var28));
         throw new IndicatorTestException(4, var28);
      } finally {
         var8.destroy();
         var9.destroy();
         if (var10 != null) {
            var10.destroy();
         }

         this.lock.unlock();
      }
   }

   public void runTest(
      String var1,
      int var2,
      int var3,
      int var4,
      int var5,
      String var6,
      String var7,
      String var8,
      String var9,
      String var10,
      String var11,
      String var12,
      String var13,
      Object[] var14,
      Object[] var15,
      Object[] var16,
      Object[] var17,
      String var18,
      int var19,
      int var20,
      int var21,
      int var22,
      boolean var23
   ) throws IndicatorTestException {
      if (this.indicators == null) {
         throw new IndicatorTestException(2, new Exception("First init(engine) must be called."));
      }

      this.lock.lock();
      Log.info("--------------------------------------------------------------------------------------------------------------------");
      Log.info("Testing indicator '" + var6 + "' using source file '" + var1 + "', engine=" + this.engine);
      DataSeries var24 = new DataSeries();
      DataSeries var25 = new DataSeries();
      DataSeries var26 = new DataSeries();
      DataSeries var27 = new DataSeries();
      DataSeries var28 = new DataSeries();
      ChartData var29 = null;

      try {
         String var30 = this.getFilePath(var1);
         File var31 = new File(var30);

         try {
            var29 = this.loadSourceData(var30, var28, var23);
         } catch (Exception var60) {
            throw new IndicatorTestException(1, var60);
         }

         if (var28.size() == 0) {
            throw new IndicatorTestException(5, new Exception(var31.getAbsolutePath()));
         }

         int var32 = Engines.getEngine(this.engine);
         IndicatorsObj var33 = StrategyBase.createIndicatorsObj(var32);
         var33.Engine = var32;

         try {
            if (var6.length() >= 6 && var6.equals("Volume")) {
               var24 = var29.Volume;
            } else if (!var6.equals("Open") && (!var6.startsWith("Open") || var6.length() != 5)) {
               if (!var6.equals("High") && (!var6.startsWith("High") || var6.length() != 5)) {
                  if (!var6.equals("Low") && (!var6.startsWith("Low") || var6.length() != 4)) {
                     if (!var6.equals("Close") && (!var6.startsWith("Close") || var6.length() != 6)) {
                        var14 = var14 == null ? new Object[0] : var14;
                        Object[] var34 = new Object[var14.length + 1];
                        var34[0] = this.getSourceData(var29, var2);
                        System.arraycopy(var14, 0, var34, 1, var14.length);
                        Object var35 = this.callMethod(var33, var6, var34);
                        var24 = (DataSeries)var35.getClass().getField(var10).get(var35);
                     } else {
                        var24 = var29.Close;
                     }
                  } else {
                     var24 = var29.Low;
                  }
               } else {
                  var24 = var29.High;
               }
            } else {
               var24 = var29.Open;
            }

            if (!var7.equals("") && !var11.equals("")) {
               if (var7.length() >= 6 && var7.equals("Volume")) {
                  var25 = var29.Volume;
               } else if (!var7.equals("Open") && (!var7.startsWith("Open") || var7.length() != 5)) {
                  if (!var7.equals("High") && (!var7.startsWith("High") || var7.length() != 5)) {
                     if (!var7.equals("Low") && (!var7.startsWith("Low") || var7.length() != 4)) {
                        if (!var7.equals("Close") && (!var7.startsWith("Close") || var7.length() != 6)) {
                           var15 = var15 == null ? new Object[0] : var15;
                           Object[] var69 = new Object[var15.length + 1];
                           var69[0] = this.getSourceData(var29, var3);
                           System.arraycopy(var15, 0, var69, 1, var15.length);
                           Object var74 = this.callMethod(var33, var7, var69);
                           var25 = (DataSeries)var74.getClass().getField(var11).get(var74);
                        } else {
                           var25 = var29.Close;
                        }
                     } else {
                        var25 = var29.Low;
                     }
                  } else {
                     var25 = var29.High;
                  }
               } else {
                  var25 = var29.Open;
               }
            } else {
               var25 = var24;
            }

            if (!var8.equals("") && !var12.equals("")) {
               if (var8.length() >= 6 && var8.equals("Volume")) {
                  var26 = var29.Volume;
               } else if (!var8.equals("Open") && (!var8.startsWith("Open") || var8.length() != 5)) {
                  if (!var8.equals("High") && (!var8.startsWith("High") || var8.length() != 5)) {
                     if (!var8.equals("Low") && (!var8.startsWith("Low") || var8.length() != 4)) {
                        if (!var8.equals("Close") && (!var8.startsWith("Close") || var8.length() != 6)) {
                           var16 = var16 == null ? new Object[0] : var16;
                           Object[] var70 = new Object[var16.length + 1];
                           var70[0] = this.getSourceData(var29, var4);
                           System.arraycopy(var16, 0, var70, 1, var16.length);
                           Object var75 = this.callMethod(var33, var8, var70);
                           var26 = (DataSeries)var75.getClass().getField(var12).get(var75);
                        } else {
                           var26 = var29.Close;
                        }
                     } else {
                        var26 = var29.Low;
                     }
                  } else {
                     var26 = var29.High;
                  }
               } else {
                  var26 = var29.Open;
               }
            } else {
               var26 = var24;
            }

            if (!var9.equals("") && !var13.equals("")) {
               if (var9.length() >= 6 && var9.equals("Volume")) {
                  var27 = var29.Volume;
               } else if (!var9.equals("Open") && (!var9.startsWith("Open") || var9.length() != 5)) {
                  if (!var9.equals("High") && (!var9.startsWith("High") || var9.length() != 5)) {
                     if (!var9.equals("Low") && (!var9.startsWith("Low") || var9.length() != 4)) {
                        if (!var9.equals("Close") && (!var9.startsWith("Close") || var9.length() != 6)) {
                           var17 = var17 == null ? new Object[0] : var17;
                           Object[] var71 = new Object[var17.length + 1];
                           var71[0] = this.getSourceData(var29, var5);
                           System.arraycopy(var17, 0, var71, 1, var17.length);
                           Object var76 = this.callMethod(var33, var9, var71);
                           var27 = (DataSeries)var76.getClass().getField(var13).get(var76);
                        } else {
                           var27 = var29.Close;
                        }
                     } else {
                        var27 = var29.Low;
                     }
                  } else {
                     var27 = var29.High;
                  }
               } else {
                  var27 = var29.Open;
               }
            } else {
               var27 = var24;
            }

            var24.setShift(var20);
            var25.setShift(var20);
            var26.setShift(var20);
            var27.setShift(var20);
         } catch (Exception var61) {
            throw new Exception(L.t("Cannot instantiate indicator '%s'!", new Object[]{var6}), var61);
         }

         int var72 = var29.Time.size() - 1 - 1;
         int var77 = 0;
         int var36 = 0;
         int var37 = 0;
         int var38 = 0;
         int var39 = 0;
         int var40 = 0;
         int var41 = 0;
         boolean var42 = false;
         boolean var43 = false;
         boolean var44 = false;
         boolean var45 = false;
         boolean var46 = false;
         boolean var47 = false;
         boolean var48 = false;
         var24.get(0);
         if (!var11.equals("")) {
            var25.get(0);
         }

         for (int var49 = var72; var49 >= 0; var49--) {
            double var50 = var24.get(var49);
            if (var6.equals("OpenD")) {
               var50 = var29.OpenD(var49);
            }

            if (var6.equals("HighD")) {
               var50 = var29.HighD(var49);
            }

            if (var6.equals("LowD")) {
               var50 = var29.LowD(var49);
            }

            if (var6.equals("CloseD")) {
               var50 = var29.CloseD(var49);
            }

            if (var6.equals("OpenW")) {
               var50 = var29.OpenW(var49);
            }

            if (var6.equals("HighW")) {
               var50 = var29.HighW(var49);
            }

            if (var6.equals("LowW")) {
               var50 = var29.LowW(var49);
            }

            if (var6.equals("CloseW")) {
               var50 = var29.CloseW(var49);
            }

            if (var6.equals("OpenM")) {
               var50 = var29.OpenM(var49);
            }

            if (var6.equals("HighM")) {
               var50 = var29.HighM(var49);
            }

            if (var6.equals("LowM")) {
               var50 = var29.LowM(var49);
            }

            if (var6.equals("CloseM")) {
               var50 = var29.CloseM(var49);
            }

            if (!var18.equals("")) {
               if (this.isConditionMet(var29, var6, var18, var24, var25, var26, var27, var49)) {
                  var50 = 1.0;
               } else {
                  var50 = 0.0;
               }
            } else {
               if (!var42 && this.checkPrecision(var50, var28.get(var49), 1)) {
                  var42 = true;
                  var77 = var72 - var49;
               }

               if (!this.checkPrecision(var50, var28.get(var49), 1)) {
                  var42 = false;
               }

               if (!var43 && this.checkPrecision(var50, var28.get(var49), 2)) {
                  var43 = true;
                  var36 = var72 - var49;
               }

               if (!this.checkPrecision(var50, var28.get(var49), 2)) {
                  var43 = false;
               }

               if (!var44 && this.checkPrecision(var50, var28.get(var49), 3)) {
                  var44 = true;
                  var37 = var72 - var49;
               }

               if (!this.checkPrecision(var50, var28.get(var49), 3)) {
                  var44 = false;
               }

               if (!var45 && this.checkPrecision(var50, var28.get(var49), 4)) {
                  var45 = true;
                  var38 = var72 - var49;
               }

               if (!this.checkPrecision(var50, var28.get(var49), 4)) {
                  var45 = false;
               }

               if (!var46 && this.checkPrecision(var50, var28.get(var49), 5)) {
                  var46 = true;
                  var39 = var72 - var49;
               }

               if (!this.checkPrecision(var50, var28.get(var49), 5)) {
                  var46 = false;
               }

               if (!var47 && this.checkPrecision(var50, var28.get(var49), 6)) {
                  var47 = true;
                  var40 = var72 - var49;
               }

               if (!this.checkPrecision(var50, var28.get(var49), 6)) {
                  var47 = false;
               }

               if (!var48 && this.checkPrecision(var50, var28.get(var49), 7)) {
                  var48 = true;
                  var41 = var72 - var49;
               }

               if (!this.checkPrecision(var50, var28.get(var49), 7)) {
                  var48 = false;
               }
            }
         }

         if (var18.equals("")) {
            Log.info(this.engine + "   " + var1 + "  :  " + var77 + " reserved bars " + this.timeframe + " are necessary for a precision 1 decimals ");
            Log.info(this.engine + "   " + var1 + "  :  " + var36 + " reserved bars " + this.timeframe + " are necessary for a precision 2 decimals ");
            Log.info(this.engine + "   " + var1 + "  :  " + var37 + " reserved bars " + this.timeframe + " are necessary for a precision 3 decimals ");
            Log.info(this.engine + "   " + var1 + "  :  " + var38 + " reserved bars " + this.timeframe + " are necessary for a precision 4 decimals ");
            Log.info(this.engine + "   " + var1 + "  :  " + var39 + " reserved bars " + this.timeframe + " are necessary for a precision 5 decimals ");
            Log.info(this.engine + "   " + var1 + "  :  " + var40 + " reserved bars " + this.timeframe + " are necessary for a precision 6 decimals ");
            Log.info(this.engine + "   " + var1 + "  :  " + var41 + " reserved bars " + this.timeframe + " are necessary for a precision 7 decimals ");
         }

         var72 = var29.Time.size() - var21 - 1;
         int var78 = 0;
         var24.get(0);
         if (!var11.equals("")) {
            var25.get(0);
         }

         for (int var81 = var72; var81 >= 0; var81--) {
            double var51 = var24.get(var81);
            if (var6.equals("OpenD")) {
               var51 = var29.OpenD(var81);
            }

            if (var6.equals("HighD")) {
               var51 = var29.HighD(var81);
            }

            if (var6.equals("LowD")) {
               var51 = var29.LowD(var81);
            }

            if (var6.equals("CloseD")) {
               var51 = var29.CloseD(var81);
            }

            if (var6.equals("OpenW")) {
               var51 = var29.OpenW(var81);
            }

            if (var6.equals("HighW")) {
               var51 = var29.HighW(var81);
            }

            if (var6.equals("LowW")) {
               var51 = var29.LowW(var81);
            }

            if (var6.equals("CloseW")) {
               var51 = var29.CloseW(var81);
            }

            if (var6.equals("OpenM")) {
               var51 = var29.OpenM(var81);
            }

            if (var6.equals("HighM")) {
               var51 = var29.HighM(var81);
            }

            if (var6.equals("LowM")) {
               var51 = var29.LowM(var81);
            }

            if (var6.equals("CloseM")) {
               var51 = var29.CloseM(var81);
            }

            if (!var18.equals("")) {
               if (this.isConditionMet(var29, var6, var18, var24, var25, var26, var27, var81)) {
                  var51 = 1.0;
               } else {
                  var51 = 0.0;
               }
            }

            if (!this.checkPrecision(var51, var28.get(var81), var19)) {
               String var53 = this.df.format(new Date(var29.Time(var81))) + "," + var51 + "," + var28.get(var81);
               this.showGrid = true;
               this.dataErrors.add(var53);
               Log.error(var31.getName() + " - " + var53);
               var78++;
            }

            if (var78 >= var22) {
               this.errorMsg = this.engine
                  + "   "
                  + var1
                  + "  :  Failed with "
                  + var21
                  + " "
                  + this.timeframe
                  + " reserved bars and "
                  + var19
                  + " decimals   with more than  "
                  + var78
                  + " differences";
               throw new IndicatorTestException(3, new Exception(this.errorMsg));
            }
         }

         if (var78 > 0) {
            this.errorMsg = var78 + " differences";
         }
      } catch (IndicatorTestException var62) {
         throw var62;
      } catch (Exception var63) {
         this.errorMsg = var63.getMessage();
         this.dataErrors.add(SQUtils.stackTraceToString(var63));
         throw new IndicatorTestException(4, var63);
      } finally {
         var24.destroy();
         var25.destroy();
         var26.destroy();
         var27.destroy();
         var28.destroy();
         if (var29 != null) {
            var29.destroy();
         }

         this.lock.unlock();
      }
   }

   public boolean isConditionMet(ChartData var1, String var2, String var3, DataSeries var4, DataSeries var5, DataSeries var6, DataSeries var7, int var8) throws Exception {
      if (var8 + 1 < var4.size() && var8 + 1 < var5.size() && var8 + 1 < var6.size() && var8 + 1 < var7.size()) {
         double var9 = var4.get(var8);
         double var11 = var4.get(var8 + 1);
         double var13 = var4.get(var8 + 2);
         double var15 = var5.get(var8);
         double var17 = var5.get(var8 + 1);
         double var19 = var5.get(var8 + 2);
         double var21 = var6.get(var8);
         double var23 = var6.get(var8 + 1);
         double var25 = var6.get(var8 + 2);
         Object var27 = null;
         switch (var3) {
            case "ADXRising":
               return var4.getRounded(var8 + 1) < var4.getRounded(var8);
            case "ADXFalling":
               return var4.getRounded(var8 + 1) > var4.getRounded(var8);
            case "ADXSup50":
               return var4.getRounded(var8) > 50.0;
            case "ADXInf50":
               return var4.getRounded(var8) < 50.0;
            case "ADXCrossAbove50":
               return var4.getRounded(var8 + 1) < 50.0 && var4.getRounded(var8) > 50.0;
            case "ADXCrossbelow50":
               return var4.getRounded(var8 + 1) > 50.0 && var4.getRounded(var8) < 50.0;
            case "ADXChangeDirectionUpward":
               return var4.getRounded(var8 + 2) > var4.getRounded(var8 + 1) && var4.getRounded(var8 + 1) < var4.getRounded(var8);
            case "ADXChangeDirectionDownward":
               return var4.getRounded(var8 + 2) < var4.getRounded(var8 + 1) && var4.getRounded(var8 + 1) > var4.getRounded(var8);
            case "AwesomeOscillatorRising":
               return var4.getRounded(var8 + 1) < var4.getRounded(var8);
            case "AwesomeOscillatorFalling":
               return var4.getRounded(var8 + 1) > var4.getRounded(var8);
            case "AwesomeOscillatorSup50":
               return var4.getRounded(var8) > 50.0;
            case "AwesomeOscillatorInf50":
               return var4.getRounded(var8) < 50.0;
            case "AwesomeOscillatorCrossAbove50":
               return var4.getRounded(var8 + 1) < 50.0 && var4.getRounded(var8) > 50.0;
            case "AwesomeOscillatorCrossbelow50":
               return var4.getRounded(var8 + 1) > 50.0 && var4.getRounded(var8) < 50.0;
            case "AwesomeOscillatorChangeDirectionUpward":
               return var4.getRounded(var8 + 2) > var4.getRounded(var8 + 1) && var4.getRounded(var8 + 1) < var4.getRounded(var8);
            case "AwesomeOscillatorChangeDirectionDownward":
               return var4.getRounded(var8 + 2) < var4.getRounded(var8 + 1) && var4.getRounded(var8 + 1) > var4.getRounded(var8);
            case "BearsPowerRising":
               return var4.getRounded(var8 + 1) < var4.getRounded(var8);
            case "BearsPowerFalling":
               return var4.getRounded(var8 + 1) > var4.getRounded(var8);
            case "BearsPowerSup50":
               return var4.getRounded(var8) > 50.0;
            case "BearsPowerInf50":
               return var4.getRounded(var8) < 50.0;
            case "BearsPowerCrossAbove50":
               return var4.getRounded(var8 + 1) < 50.0 && var4.getRounded(var8) > 50.0;
            case "BearsPowerCrossbelow50":
               return var4.getRounded(var8 + 1) > 50.0 && var4.getRounded(var8) < 50.0;
            case "BearsPowerChangeDirectionUpward":
               return var4.getRounded(var8 + 2) > var4.getRounded(var8 + 1) && var4.getRounded(var8 + 1) < var4.getRounded(var8);
            case "BearsPowerChangeDirectionDownward":
               return var4.getRounded(var8 + 2) < var4.getRounded(var8 + 1) && var4.getRounded(var8 + 1) > var4.getRounded(var8);
            case "BullsPowerRising":
               return var4.getRounded(var8 + 1) < var4.getRounded(var8);
            case "BullsPowerFalling":
               return var4.getRounded(var8 + 1) > var4.getRounded(var8);
            case "BullsPowerSup50":
               return var4.getRounded(var8) > 50.0;
            case "BullsPowerInf50":
               return var4.getRounded(var8) < 50.0;
            case "BullsPowerCrossAbove50":
               return var4.getRounded(var8 + 1) < 50.0 && var4.getRounded(var8) > 50.0;
            case "BullsPowerCrossbelow50":
               return var4.getRounded(var8 + 1) > 50.0 && var4.getRounded(var8) < 50.0;
            case "BullsPowerChangeDirectionUpward":
               return var4.getRounded(var8 + 2) > var4.getRounded(var8 + 1) && var4.getRounded(var8 + 1) < var4.getRounded(var8);
            case "BullsPowerChangeDirectionDownward":
               return var4.getRounded(var8 + 2) < var4.getRounded(var8 + 1) && var4.getRounded(var8 + 1) > var4.getRounded(var8);
            case "ATR14_Rising":
               return var4.getRounded(var8 + 1) < var4.getRounded(var8);
            case "ATR14_Falling":
               return var4.getRounded(var8 + 1) > var4.getRounded(var8);
            case "ATR14_Sup50":
               return var4.getRounded(var8) > 50.0;
            case "ATR14_Inf50":
               return var4.getRounded(var8) < 50.0;
            case "ATR14_CrossAbove50":
               return var4.getRounded(var8 + 1) < 50.0 && var4.getRounded(var8) > 50.0;
            case "ATR14_Crossbelow50":
               return var4.getRounded(var8 + 1) > 50.0 && var4.getRounded(var8) < 50.0;
            case "ATR14_ChangeDirectionUpward":
               return var4.getRounded(var8 + 2) > var4.getRounded(var8 + 1) && var4.getRounded(var8 + 1) < var4.getRounded(var8);
            case "ATR14_ChangeDirectionDownward":
               return var4.getRounded(var8 + 2) < var4.getRounded(var8 + 1) && var4.getRounded(var8 + 1) > var4.getRounded(var8);
            case "AROON14_AroonDownFallsFromTop":
               return var5.getRounded(var8 + 1) == 100.0 && var5.getRounded(var8) < 100.0 && var4.getRounded(var8) < 70.0;
            case "AROON14_AroonDownRisesFromBottom":
               return var5.getRounded(var8 + 1) <= 8.0 && var5.getRounded(var8) > 8.0 && var4.getRounded(var8) > 30.0;
            case "AROON14_AroonUpCrossesAboveAroonDown":
               return var4.getRounded(var8 + 1) < var5.getRounded(var8 + 1) && var4.getRounded(var8) > var5.getRounded(var8);
            case "AROON14_AroonUpCrossesBelowAroonDown":
               return var4.getRounded(var8 + 1) > var5.getRounded(var8 + 1) && var4.getRounded(var8) < var5.getRounded(var8);
            case "AROON14_AroonUpFallsFromTop":
               return var4.getRounded(var8 + 1) == 100.0 && var4.getRounded(var8) < 100.0 && var5.getRounded(var8) < 70.0;
            case "AROON14_AroonUpRisesFromBottom":
               return var4.getRounded(var8 + 1) <= 8.0 && var4.getRounded(var8) > 8.0 && var5.getRounded(var8) > 30.0;
            case "AVGVOLUME_14_VolumeIsFalling":
               double var401 = var4.getRounded(var8 + 1, 4);
               double var496 = var4.getRounded(var8, 4);
               return var401 > var496;
            case "AVGVOLUME_14_VolumeIsRising":
               double var400 = var4.getRounded(var8 + 1, 4);
               double var495 = var4.getRounded(var8, 4);
               return var400 < var495;
            case "VolumeIsFalling":
               double var399 = var4.getRounded(var8 + 1);
               double var494 = var4.getRounded(var8);
               return var399 > var494;
            case "VolumeIsRising":
               double var398 = var4.getRounded(var8 + 1);
               double var493 = var4.getRounded(var8);
               return var398 < var493;
            case "BB_CLOSE_20_0.75_LowerBandIsFalling":
               return var5.getRounded(var8 + 1) > var5.getRounded(var8);
            case "BB_CLOSE_20_0.75_LowerBandIsRising":
               return var5.getRounded(var8 + 1) < var5.getRounded(var8);
            case "BB_CLOSE_20_0.75_UpperBandIsFalling":
               return var4.getRounded(var8 + 1) > var4.getRounded(var8);
            case "BB_CLOSE_20_0.75_UpperBandIsRising":
               return var4.getRounded(var8 + 1) < var4.getRounded(var8);
            case "BB_CLOSE_20_0.75_BarClosesAboveLowerBand":
               return var1.Close(var8) > var5.getRounded(var8);
            case "BB_CLOSE_20_0.75_BarClosesAboveUpperBand":
               return var1.Close(var8) > var4.getRounded(var8);
            case "BB_CLOSE_20_0.75_BarClosesBelowLowerBand":
               return var1.Close(var8) < var5.getRounded(var8);
            case "BB_CLOSE_20_0.75_BarClosesBelowUpperBand":
               return var1.Close(var8) < var4.getRounded(var8);
            case "BB_CLOSE_20_0.75_BarOpensAboveLowerBand":
               return var1.Open(var8) > var5.getRounded(var8 + 1);
            case "BB_CLOSE_20_0.75_BarOpensAboveUpperBand":
               return var1.Open(var8) > var4.getRounded(var8 + 1);
            case "BB_CLOSE_20_0.75_BarOpensBelowLowerBand":
               return var1.Open(var8) < var5.getRounded(var8 + 1);
            case "BB_CLOSE_20_0.75_BarOpensBelowUpperBand":
               return var1.Open(var8) < var4.getRounded(var8 + 1);
            case "BB_CLOSE_20_0.75_BarOpensAboveLowerBandAfterOpenedBelow":
               return var1.Open(var8 + 1) < var5.getRounded(var8 + 1) && var1.Open(var8) > var5.getRounded(var8 + 1);
            case "BB_CLOSE_20_0.75_BarOpensAboveUpperBandAfterOpenedBelow":
               return var1.Open(var8 + 1) < var4.getRounded(var8 + 1) && var1.Open(var8) > var4.getRounded(var8 + 1);
            case "BB_CLOSE_20_0.75_BarOpensBelowLowerBandAfterOpenedAbove":
               return var1.Open(var8 + 1) > var5.getRounded(var8 + 1) && var1.Open(var8) < var5.getRounded(var8 + 1);
            case "BB_CLOSE_20_0.75_BarOpensBelowUpperBandAfterOpenedAbove":
               return var1.Open(var8 + 1) > var4.getRounded(var8 + 1) && var1.Open(var8) < var4.getRounded(var8 + 1);
            case "CCI_Rising":
               return var4.getRounded(var8 + 1, 4) < var4.getRounded(var8, 4);
            case "CCI_Falling":
               return var4.getRounded(var8 + 1, 4) > var4.getRounded(var8, 4);
            case "CCI_Sup50":
               return var4.getRounded(var8, 4) > 50.0;
            case "CCI_Inf50":
               return var4.getRounded(var8, 4) < 50.0;
            case "CCI_CrossAbove50":
               return var4.getRounded(var8 + 1, 4) < 50.0 && var4.getRounded(var8, 4) > 50.0;
            case "CCI_Crossbelow50":
               return var4.getRounded(var8 + 1, 4) > 50.0 && var4.getRounded(var8, 4) < 50.0;
            case "CCI_30_CrossAbove0":
               return var4.getRounded(var8 + 1, 4) < 0.0 && var4.getRounded(var8, 4) > 0.0;
            case "CCI_30_Crossbelow0":
               return var4.getRounded(var8 + 1, 4) > 0.0 && var4.getRounded(var8, 4) < 0.0;
            case "CCI_ChangeDirectionUpward":
               return var4.getRounded(var8 + 2, 4) > var4.getRounded(var8 + 1, 4) && var4.getRounded(var8 + 1, 4) < var4.getRounded(var8, 4);
            case "CCI_ChangeDirectionDownward":
               return var4.getRounded(var8 + 2, 4) < var4.getRounded(var8 + 1, 4) && var4.getRounded(var8 + 1, 4) > var4.getRounded(var8, 4);
            case "WoodiesCCIZeroLineBreakDown":
               return var4.getRounded(var8 + 1) > 0.0 && var4.getRounded(var8) < 0.0;
            case "WoodiesCCIZeroLineBreakUP":
               return var4.getRounded(var8 + 1) < 0.0 && var4.getRounded(var8) > 0.0;
            case "WoodiesFamirDown":
               double var503 = var4.getRounded(var8 + 1);
               double var510 = var4.getRounded(var8 + 2);
               double var517 = var4.getRounded(var8 + 3);
               double var524 = var4.getRounded(var8 + 4);
               double var531 = var4.getRounded(var8 + 5);
               double var534 = var4.getRounded(var8 + 6);
               double var541 = var4.getRounded(var8);
               double var546 = 5.0;
               boolean var50 = var503 > var510 && var517 > var510 && var541 < var503;
               boolean var551 = var503 - var510 > var546 && var517 - var510 > var546;
               return var541 < 50.0 && var503 > 0.0 && var510 > 0.0 && var517 > 0.0 && var524 > 0.0 && var531 > 0.0 && var534 > 0.0 && var50 && var551;
            case "WoodiesFamirUP":
               double var502 = var4.getRounded(var8 + 1);
               double var509 = var4.getRounded(var8 + 2);
               double var516 = var4.getRounded(var8 + 3);
               double var523 = var4.getRounded(var8 + 4);
               double var530 = var4.getRounded(var8 + 5);
               double var533 = var4.getRounded(var8 + 6);
               double var540 = var4.getRounded(var8);
               double var545 = 5.0;
               boolean var52 = var502 < var509 && var516 < var509 && var540 > var502;
               boolean var550 = var509 - var502 > var545 && var509 - var516 > var545;
               return var540 > -50.0 && var502 < 0.0 && var509 < 0.0 && var516 < 0.0 && var523 < 0.0 && var530 < 0.0 && var533 < 0.0 && var52 && var550;
            case "WoodiesTrendDown":
               double var501 = var4.getRounded(var8 + 1);
               double var508 = var4.getRounded(var8 + 2);
               double var515 = var4.getRounded(var8 + 3);
               double var522 = var4.getRounded(var8 + 4);
               double var529 = var4.getRounded(var8 + 5);
               double var539 = var4.getRounded(var8);
               return var501 < 0.0 && var508 < 0.0 && var515 < 0.0 && var522 < 0.0 && var529 < 0.0 && var539 < 0.0;
            case "WoodiesTrendUP":
               double var500 = var4.getRounded(var8 + 1);
               double var507 = var4.getRounded(var8 + 2);
               double var514 = var4.getRounded(var8 + 3);
               double var521 = var4.getRounded(var8 + 4);
               double var528 = var4.getRounded(var8 + 5);
               double var538 = var4.getRounded(var8);
               return var500 > 0.0 && var507 > 0.0 && var514 > 0.0 && var521 > 0.0 && var528 > 0.0 && var538 > 0.0;
            case "WoodiesVegasTradeDown":
               double var499 = var4.getRounded(var8 + 1);
               double var506 = var4.getRounded(var8 + 2);
               double var513 = var4.getRounded(var8 + 3);
               double var520 = var4.getRounded(var8 + 4);
               double var527 = var4.getRounded(var8 + 5);
               double var532 = var4.getRounded(var8 + 6);
               double var537 = var4.getRounded(var8);
               double var544 = 5.0;
               boolean var549 = var520 - var513 > var544 && var499 - var506 > var544;
               boolean var53 = var499 > 0.0 && var506 > 0.0 && var513 > 0.0 && var520 > 0.0 && var527 > 0.0 && var532 > 0.0;
               boolean var54 = var499 > var506 && var499 > var513 && var520 > var506 && var520 > var513;
               boolean var552 = var499 > 200.0 || var506 > 200.0 || var513 > 200.0 || var520 > 200.0 || var527 > 200.0;
               return var552 && var54 && var549 && var53 && var537 < (var513 + var506) / 2.0;
            case "WoodiesVegasTradeUP":
               double var498 = var4.getRounded(var8 + 1);
               double var505 = var4.getRounded(var8 + 2);
               double var512 = var4.getRounded(var8 + 3);
               double var519 = var4.getRounded(var8 + 4);
               double var526 = var4.getRounded(var8 + 5);
               double var44 = var4.getRounded(var8 + 6);
               double var536 = var4.getRounded(var8);
               double var543 = 5.0;
               boolean var548 = var505 - var498 > var543 && var512 - var519 > var543;
               boolean var56 = var498 < 0.0 && var505 < 0.0 && var512 < 0.0 && var519 < 0.0 && var526 < 0.0 && var44 < 0.0;
               boolean var57 = var498 < var505 && var498 < var512 && var519 < var505 && var519 < var512;
               boolean var55 = var498 < -200.0 || var505 < -200.0 || var512 < -200.0 || var519 < -200.0 || var526 < -200.0;
               return var55 && var57 && var548 && var56 && var536 > (var512 + var505) / 2.0;
            case "WoodiesZLRDown":
               double var497 = var4.getRounded(var8 + 1);
               double var504 = var4.getRounded(var8 + 2);
               double var511 = var4.getRounded(var8 + 3);
               double var518 = var4.getRounded(var8 + 4);
               double var525 = var4.getRounded(var8 + 5);
               double var535 = var4.getRounded(var8);
               double var542 = 5.0;
               boolean var58 = var535 < var497 && var504 < var497;
               boolean var547 = var497 - var535 > var542 && var497 - var504 > var542;
               boolean var553 = var535 < 100.0 && var497 < 100.0 && var504 < 100.0;
               boolean var554 = var535 > -100.0 && var497 > -100.0 && var504 > -100.0;
               return var497 < 0.0 && var504 < 0.0 && var511 < 0.0 && var518 < 0.0 && var525 < 0.0 && var535 < 0.0 && var58 && var547 && var553 && var554;
            case "WoodiesZLRUP":
               double var34 = var4.getRounded(var8 + 1);
               double var36 = var4.getRounded(var8 + 2);
               double var38 = var4.getRounded(var8 + 3);
               double var40 = var4.getRounded(var8 + 4);
               double var42 = var4.getRounded(var8 + 5);
               double var46 = var4.getRounded(var8);
               double var48 = 5.0;
               boolean var61 = var46 > var34 && var36 > var34;
               boolean var51 = var46 - var34 > var48 && var36 - var34 > var48;
               boolean var59 = var46 < 100.0 && var34 < 100.0 && var36 < 100.0;
               boolean var60 = var46 > -100.0 && var34 > -100.0 && var36 > -100.0;
               return var34 > 0.0 && var36 > 0.0 && var38 > 0.0 && var40 > 0.0 && var42 > 0.0 && var46 > 0.0 && var61 && var51 && var59 && var60;
            case "DeMarker_Rising":
               return var4.getRounded(var8 + 1) < var4.getRounded(var8);
            case "DeMarker_Falling":
               return var4.getRounded(var8 + 1) > var4.getRounded(var8);
            case "DeMarker_Sup05":
               return var4.getRounded(var8) > 0.5;
            case "DeMarker_Inf05":
               return var4.getRounded(var8) < 0.5;
            case "DeMarker_CrossAbove05":
               return var4.getRounded(var8 + 1) < 0.5 && var4.getRounded(var8) > 0.5;
            case "DeMarker_Crossbelow05":
               return var4.getRounded(var8 + 1) > 0.5 && var4.getRounded(var8) < 0.5;
            case "DeMarker_ChangeDirectionUpward":
               return var4.getRounded(var8 + 2) > var4.getRounded(var8 + 1) && var4.getRounded(var8 + 1) < var4.getRounded(var8);
            case "DeMarker_ChangeDirectionDownward":
               return var4.getRounded(var8 + 2) < var4.getRounded(var8 + 1) && var4.getRounded(var8 + 1) > var4.getRounded(var8);
            case "DICrossDown":
               double var397 = var4.getRounded(var8 + 1);
               double var492 = var5.getRounded(var8 + 1);
               double var575 = var4.getRounded(var8);
               double var576 = var5.getRounded(var8);
               return var397 > var492 && var575 < var576;
            case "DICrossUp":
               double var396 = var4.getRounded(var8 + 1);
               double var491 = var5.getRounded(var8 + 1);
               double var574 = var4.getRounded(var8);
               double var64 = var5.getRounded(var8);
               return var396 < var491 && var574 > var64;
            case "DIM_ChangeDirectionDownward":
               return var4.getRounded(var8 + 2) < var4.getRounded(var8 + 1) && var4.getRounded(var8 + 1) > var4.getRounded(var8);
            case "DIM_ChangeDirectionUpward":
               return var4.getRounded(var8 + 2) > var4.getRounded(var8 + 1) && var4.getRounded(var8 + 1) < var4.getRounded(var8);
            case "DIM_Falling":
               return var4.getRounded(var8 + 1) > var4.getRounded(var8);
            case "DIM_Rising":
               return var4.getRounded(var8 + 1) < var4.getRounded(var8);
            case "DIP_ChangeDirectionDownward":
               return var4.getRounded(var8 + 2) < var4.getRounded(var8 + 1) && var4.getRounded(var8 + 1) > var4.getRounded(var8);
            case "DIP_ChangeDirectionUpward":
               return var4.getRounded(var8 + 2) > var4.getRounded(var8 + 1) && var4.getRounded(var8 + 1) < var4.getRounded(var8);
            case "DIP_Falling":
               return var4.getRounded(var8 + 1) > var4.getRounded(var8);
            case "DIPlusHigher":
               double var395 = var4.getRounded(var8);
               double var490 = var5.getRounded(var8);
               return var395 > var490;
            case "DIPlusLower":
               double var394 = var4.getRounded(var8);
               double var489 = var5.getRounded(var8);
               return var394 < var489;
            case "DIP_Rising":
               return var4.getRounded(var8 + 1) < var4.getRounded(var8);
            case "BearishEngulfing":
               double var581 = var1.Open(var8);
               double var584 = var1.Open(var8 + 1);
               double var589 = var1.Close(var8);
               double var592 = var1.Close(var8 + 1);
               if (var592 > var584 && var581 > var589 && var581 >= var592 && var584 >= var589 && var581 - var589 > var592 - var584) {
                  return true;
               }

               return false;
            case "BullishEngulfing":
               double var580 = var1.Open(var8);
               double var583 = var1.Open(var8 + 1);
               double var588 = var1.Close(var8);
               double var591 = var1.Close(var8 + 1);
               if (var583 > var591 && var588 > var580 && var588 >= var583 && var591 >= var580 && var588 - var580 > var583 - var591) {
                  return true;
               }

               return false;
            case "DarkCloud":
               double var579 = var1.Open(var8);
               double var582 = var1.Open(var8 + 1);
               double var587 = var1.Close(var8);
               double var590 = var1.Close(var8 + 1);
               double var595 = var1.High(var8);
               double var598 = var1.Low(var8);
               double var599 = 0.5;
               double var600 = 10.0;
               double var601 = SQUtils.round(var595 - var598, this.digits);
               double var84 = SQUtils.round(var579 - var587, this.digits);
               double var86 = var601 != 0.0 ? SQUtils.round(var84 / var601, 6) : 0.0;
               double var602 = SQUtils.round((var582 + var590) / 2.0, this.digits);
               double var603 = SQUtils.round(var600 * this.tickSize, this.digits);
               if (var590 > var582 && var602 > var587 && var579 > var587 && var587 > var582 && var86 > var599 && var601 >= var603) {
                  return true;
               }

               return false;
            case "Doji":
               double var92 = SQUtils.round(Math.abs(var1.Open(var8) - var1.Close(var8)), this.digits);
               double var94 = SQUtils.round(0.6 * this.tickSize, this.digits);
               if (var92 < var94) {
                  return true;
               }

               return false;
            case "Hammer":
               double var594 = var1.High(var8);
               double var597 = var1.Low(var8);
               double var96 = var1.Low(var8 + 1);
               double var98 = var1.Low(var8 + 2);
               double var100 = var1.Low(var8 + 3);
               double var578 = var1.Open(var8);
               double var586 = var1.Close(var8);
               double var604 = SQUtils.round(var594 - var597, this.digits);
               double var607 = 0.9;
               double var608 = 12.0;
               double var605;
               double var606;
               if (var578 > var586) {
                  var606 = var578;
                  var605 = var586;
               } else {
                  var606 = var586;
                  var605 = var578;
               }

               double var609 = SQUtils.round(var605 - var597, this.digits);
               double var610 = SQUtils.round(var594 - var606, this.digits);
               double var611 = SQUtils.round(Math.abs(var578 - var586), this.digits);
               double var612 = SQUtils.round(var611 * var607, this.digits);
               double var120 = SQUtils.round(var609 / 2.0, this.digits);
               double var122 = SQUtils.round(var609 / 3.0, this.digits);
               double var124 = SQUtils.round(var609 / 4.0, this.digits);
               double var613 = SQUtils.round(2.0 * var612, this.digits);
               double var614 = SQUtils.round(var608 * this.tickSize, this.digits);
               if (var597 <= var96 && var597 < var98 && var597 < var100) {
                  if (var120 > var610 && var609 > var613 && var604 >= var614 && var578 != var586 && var122 <= var610 && var124 <= var610) {
                     return true;
                  }

                  if (var122 > var610 && var609 > var613 && var604 >= var614 && var578 != var586 && var124 <= var610) {
                     return true;
                  }

                  if (var124 > var610 && var609 > var613 && var604 >= var614 && var578 != var586) {
                     return true;
                  }
               }

               return false;
            case "PiercingLine":
               double var577 = var1.Open(var8);
               double var68 = var1.Open(var8 + 1);
               double var585 = var1.Close(var8);
               double var72 = var1.Close(var8 + 1);
               double var593 = var1.High(var8);
               double var596 = var1.Low(var8);
               double var78 = 0.5;
               double var80 = 10.0;
               double var82 = SQUtils.round(var593 - var596, this.digits);
               double var130 = SQUtils.round(var585 - var577, this.digits);
               double var132 = var82 != 0.0 ? SQUtils.round(var130 / var82, 6) : 0.0;
               double var88 = SQUtils.round((var68 + var72) / 2.0, this.digits);
               double var90 = SQUtils.round(var80 * this.tickSize, this.digits);
               if (var72 < var68 && var88 < var585 && var577 < var585 && var585 < var68 && var132 > var78 && var82 >= var90) {
                  return true;
               }

               return false;
            case "ShootingStar":
               double var74 = var1.High(var8);
               double var76 = var1.Low(var8);
               double var134 = var1.High(var8 + 1);
               double var136 = var1.High(var8 + 2);
               double var138 = var1.High(var8 + 3);
               double var66 = var1.Open(var8);
               double var70 = var1.Close(var8);
               double var102 = SQUtils.round(var74 - var76, this.digits);
               double var108 = 0.9;
               double var110 = 12.0;
               double var104;
               double var106;
               if (var66 > var70) {
                  var106 = var66;
                  var104 = var70;
               } else {
                  var106 = var70;
                  var104 = var66;
               }

               double var112 = SQUtils.round(var104 - var76, this.digits);
               double var114 = SQUtils.round(var74 - var106, this.digits);
               double var116 = SQUtils.round(Math.abs(var66 - var70), this.digits);
               double var118 = SQUtils.round(var116 * var108, this.digits);
               double var140 = SQUtils.round(var114 / 2.0, this.digits);
               double var142 = SQUtils.round(var114 / 3.0, this.digits);
               double var144 = SQUtils.round(var114 / 4.0, this.digits);
               double var126 = SQUtils.round(2.0 * var118, this.digits);
               double var128 = SQUtils.round(var110 * this.tickSize, this.digits);
               if (var74 >= var134 && var74 > var136 && var74 > var138) {
                  if (var140 > var112 && var114 > var126 && var102 >= var128 && var66 != var70 && var142 <= var112 && var144 <= var112) {
                     return true;
                  }

                  if (var142 > var112 && var114 > var126 && var102 >= var128 && var66 != var70 && var144 <= var112) {
                     return true;
                  }

                  if (var144 > var112 && var114 > var126 && var102 >= var128 && var66 != var70) {
                     return true;
                  }
               }

               return false;
            case "IsBearishFractalUp3":
               return var4.getRounded(var8) > 0.0;
            case "IsBullishFractalDown3":
               return var4.getRounded(var8) > 0.0;
            case "IsBearishFractalUp5":
               return var4.getRounded(var8) > 0.0;
            case "IsBullishFractalDown5":
               return var4.getRounded(var8) > 0.0;
            case "GannHiLoisinDownTrend":
               double var615 = var4.getRounded(var8);
               double var616 = var1.Close(var8);
               return var616 < var615;
            case "GannHiLoisinUpTrend":
               double var146 = var4.getRounded(var8);
               double var148 = var1.Close(var8);
               return var148 > var146;
            case "BarOpenAboveHighestAfterOpenedBelow":
               double var617 = var4.getRounded(var8 + 1);
               return var1.Open(var8 + 1) < var617 && var1.Open(var8) > var617;
            case "BarOpenAboveLowestAfterOpenedBelow":
               double var618 = var4.getRounded(var8 + 1);
               return var1.Open(var8 + 1) < var618 && var1.Open(var8) > var618;
            case "BarOpenBelowHighestAfterOpenedAbove":
               double var150 = var4.getRounded(var8 + 1);
               return var1.Open(var8 + 1) > var150 && var1.Open(var8) < var150;
            case "BarOpenBelowLowestAfterOpenedAbove":
               double var152 = var4.getRounded(var8 + 1);
               return var1.Open(var8 + 1) > var152 && var1.Open(var8) < var152;
            case "FastHMAisAboveSlowHMA":
               double var393 = var4.getRounded(var8);
               double var488 = var5.getRounded(var8);
               return var393 > var488;
            case "FastHMAisBelowSlowHMA":
               double var392 = var4.getRounded(var8);
               double var487 = var5.getRounded(var8);
               return var392 < var487;
            case "HMAChangedirectionDown":
               double var391 = var4.getRounded(var8 + 2);
               double var486 = var4.getRounded(var8 + 1);
               double var573 = var4.getRounded(var8);
               return var391 < var486 && var486 > var573;
            case "HMAChangedirectionUp":
               double var390 = var4.getRounded(var8 + 2);
               double var485 = var4.getRounded(var8 + 1);
               double var572 = var4.getRounded(var8);
               return var390 > var485 && var485 < var572;
            case "HMAIsFalling":
               double var389 = var4.getRounded(var8 + 1);
               double var484 = var4.getRounded(var8);
               return var389 > var484;
            case "HMAIsRising":
               double var388 = var4.getRounded(var8 + 1);
               double var483 = var4.getRounded(var8);
               return var388 < var483;
            case "IchimokuKijunSenCrossBearish":
               double var621 = var1.Open(var8);
               double var624 = var1.Close(var8);
               double var625 = var4.getRounded(var8);
               boolean var630 = var621 > var625 && var624 < var625;
               double var637 = var5.getRounded(var8);
               double var644 = var6.getRounded(var8);
               if (var644 > var637) {
                  double var654 = var644;
                  var644 = var637;
                  var637 = var654;
               }

               int var655 = 1;
               var655 = SQUtils.fixAllowedRange(var655, 0, 2, 1);
               if (var655 == 2) {
                  var630 = var630 && var625 < var644;
               } else if (var655 == 1) {
                  var630 = var630 && var625 < var637;
               } else if (var655 == 0) {
               }

               return var630;
            case "IchimokuKijunSenCrossBullish":
               double var620 = var1.Open(var8);
               double var623 = var1.Close(var8);
               double var158 = var4.getRounded(var8);
               boolean var629 = var620 < var158 && var623 > var158;
               double var636 = var5.getRounded(var8);
               double var643 = var6.getRounded(var8);
               if (var643 > var636) {
                  double var659 = var643;
                  var643 = var636;
                  var636 = var659;
               }

               int var652 = 1;
               var652 = SQUtils.fixAllowedRange(var652, 0, 2, 1);
               if (var652 == 2) {
                  var629 = var629 && var158 > var636;
               } else if (var652 == 1) {
                  var629 = var629 && var158 > var643;
               } else if (var652 == 0) {
               }

               return var629;
            case "IchimokuKumoBreakoutBearish":
               double var619 = var1.Open(var8);
               double var622 = var1.Close(var8);
               double var635 = var4.getRounded(var8);
               double var642 = var5.getRounded(var8);
               if (var642 > var635) {
                  var642 = var635;
               }

               return var619 > var642 && var622 < var642;
            case "IchimokuKumoBreakoutBullish":
               double var154 = var1.Open(var8);
               double var156 = var1.Close(var8);
               double var634 = var4.getRounded(var8);
               double var641 = var5.getRounded(var8);
               if (var641 > var634) {
                  double var658 = var641;
                  var634 = var658;
               }

               return var154 < var634 && var156 > var634;
            case "IchimokuSenkouSpanCrossBearish":
               double var657 = var4.getRounded(var8 + 1);
               double var660 = var4.getRounded(var8);
               double var661 = var5.getRounded(var8 + 1);
               double var662 = var5.getRounded(var8);
               double var633 = Math.max(var660, var662);
               double var640 = Math.min(var660, var662);
               boolean var628 = var657 > var661 && var660 < var662;
               int var650 = 1;
               var650 = SQUtils.fixAllowedRange(var650, 0, 2, 1);
               if (var650 == 2) {
                  var628 = var628 && var1.Close(var8) < var640;
               } else if (var650 == 1) {
                  var628 = var628 && var1.Close(var8) < var633;
               } else if (var650 == 0) {
               }

               return var628;
            case "IchimokuSenkouSpanCrossBullish":
               double var166 = var4.getRounded(var8 + 1);
               double var168 = var4.getRounded(var8);
               double var170 = var5.getRounded(var8 + 1);
               double var172 = var5.getRounded(var8);
               double var632 = Math.max(var168, var172);
               double var639 = Math.min(var168, var172);
               boolean var627 = var166 < var170 && var168 > var172;
               int var648 = 1;
               var648 = SQUtils.fixAllowedRange(var648, 0, 2, 1);
               if (var648 == 2) {
                  var627 = var627 && var1.Close(var8) > var632;
               } else if (var648 == 1) {
                  var627 = var627 && var1.Close(var8) > var639;
               } else if (var648 == 0) {
               }

               return var627;
            case "IchimokuTenkanKijunCrossBearish":
               double var663 = var4.getRounded(var8 + 1);
               double var664 = var4.getRounded(var8);
               double var665 = var5.getRounded(var8 + 1);
               double var666 = var5.getRounded(var8);
               double var631 = var6.getRounded(var8);
               double var638 = var7.getRounded(var8);
               if (var638 > var631) {
                  double var669 = var638;
                  var638 = var631;
                  var631 = var669;
               }

               boolean var626 = var663 > var665 && var664 < var666;
               int var646 = 1;
               var646 = SQUtils.fixAllowedRange(var646, 0, 2, 1);
               if (var646 == 2) {
                  var626 = var626 && var664 < var638;
               } else if (var646 == 1) {
                  var626 = var626 && var664 < var631;
               } else if (var646 == 0) {
               }

               return var626;
            case "IchimokuTenkanKijunCrossBullish":
               double var174 = var4.getRounded(var8 + 1);
               double var176 = var4.getRounded(var8);
               double var178 = var5.getRounded(var8 + 1);
               double var180 = var5.getRounded(var8);
               double var161 = var6.getRounded(var8);
               double var163 = var7.getRounded(var8);
               if (var163 > var161) {
                  double var668 = var163;
                  var163 = var161;
                  var161 = var668;
               }

               boolean var160 = var174 < var178 && var176 > var180;
               int var165 = 1;
               var165 = SQUtils.fixAllowedRange(var165, 0, 2, 1);
               if (var165 == 2) {
                  var160 = var160 && var176 > var161;
               } else if (var165 == 1) {
                  var160 = var160 && var176 > var163;
               } else if (var165 == 0) {
               }

               return var160;
            case "BarCrossesAboveKama":
               double var667 = var4.getRounded(var8);
               double var670 = var4.getRounded(var8 + 1);
               double var671 = var1.Close(var8);
               double var672 = var1.Close(var8 + 1);
               return var667 < var671 && var670 > var672;
            case "BarCrossesBelowKama":
               double var182 = var4.getRounded(var8);
               double var184 = var4.getRounded(var8 + 1);
               double var186 = var1.Close(var8);
               double var188 = var1.Close(var8 + 1);
               return var182 > var186 && var184 < var188;
            case "FastKamaIsAboveSlowKama":
               double var673 = var4.getRounded(var8);
               double var674 = var5.getRounded(var8);
               return var673 > var674;
            case "FastKamaIsBelowSlowKama":
               double var190 = var4.getRounded(var8);
               double var192 = var5.getRounded(var8);
               return var190 < var192;
            case "KamaIsFalling":
               double var387 = var4.getRounded(var8 + 1);
               double var482 = var4.getRounded(var8);
               return var387 > var482;
            case "KamaIsRising":
               double var386 = var4.getRounded(var8 + 1);
               double var481 = var4.getRounded(var8);
               return var386 < var481;
            case "KERIsAbove05":
               double var681 = var4.getRounded(var8);
               return var681 > 0.5;
            case "KERIsBelow05":
               double var680 = var4.getRounded(var8);
               return var680 < 0.5;
            case "CloseAboveLowerKC":
               double var385 = var4.getRounded(var8);
               return var1.Close(var8) > var385;
            case "CloseAboveUpperKC":
               double var384 = var4.getRounded(var8);
               return var1.Close(var8) > var384;
            case "CloseBelowLowerKC":
               double var383 = var4.getRounded(var8);
               return var1.Close(var8) < var383;
            case "CloseBelowUpperKC":
               double var382 = var4.getRounded(var8);
               return var1.Close(var8) < var382;
            case "OpenAboveLowerKC":
               double var381 = var4.getRounded(var8 + 1);
               return var1.Open(var8) > var381;
            case "OpenAboveUpperKC":
               double var380 = var4.getRounded(var8 + 1);
               return var1.Open(var8) > var380;
            case "OpenBelowLowerKC":
               double var379 = var4.getRounded(var8 + 1);
               return var1.Open(var8) < var379;
            case "OpenBelowUpperKC":
               double var378 = var4.getRounded(var8 + 1);
               return var1.Open(var8) < var378;
            case "OpenAboveLowerKCAfterOpenedBelow":
               double var377 = var4.getRounded(var8 + 1);
               return var1.Open(var8 + 1) < var377 && var1.Open(var8) > var377;
            case "OpenAboveUpperKCAfterOpenedBelow":
               double var376 = var4.getRounded(var8 + 1);
               return var1.Open(var8 + 1) < var376 && var1.Open(var8) > var376;
            case "OpenBelowLowerKCAfterOpenedAbove":
               double var375 = var4.getRounded(var8 + 1);
               return var1.Open(var8 + 1) > var375 && var1.Open(var8) < var375;
            case "OpenBelowUpperKCAfterOpenedAbove":
               double var374 = var4.getRounded(var8 + 1);
               return var1.Open(var8 + 1) > var374 && var1.Open(var8) < var374;
            case "KCLowerIsFalling":
               double var373 = var4.getRounded(var8 + 1);
               double var480 = var4.getRounded(var8);
               return var373 > var480;
            case "KCLowerIsRising":
               double var372 = var4.getRounded(var8 + 1);
               double var479 = var4.getRounded(var8);
               return var372 < var479;
            case "KCUpperIsFalling":
               double var371 = var4.getRounded(var8 + 1);
               double var478 = var4.getRounded(var8);
               return var371 > var478;
            case "KCUpperIsRising":
               double var370 = var4.getRounded(var8 + 1);
               double var477 = var4.getRounded(var8);
               return var370 < var477;
            case "LaguerreRSI05ChangeDirectionDown":
               double var686 = var4.getRounded(var8);
               double var691 = var4.getRounded(var8 + 1);
               double var692 = var4.getRounded(var8 + 2);
               return var686 < var691 && var692 < var691;
            case "LaguerreRSI05ChangeDirectionUp":
               double var685 = var4.getRounded(var8);
               double var690 = var4.getRounded(var8 + 1);
               double var200 = var4.getRounded(var8 + 2);
               return var685 > var690 && var200 > var690;
            case "LaguerreRSI05CrossAbove05":
               double var684 = var4.getRounded(var8);
               double var689 = var4.getRounded(var8 + 1);
               return var684 > 0.5 && var689 < 0.5;
            case "LaguerreRSI05CrossBelow05":
               double var683 = var4.getRounded(var8);
               double var688 = var4.getRounded(var8 + 1);
               return var683 < 0.5 && var688 > 0.5;
            case "LaguerreRSI05IsFalling":
               double var682 = var4.getRounded(var8);
               double var687 = var4.getRounded(var8 + 1);
               return var682 < var687;
            case "LaguerreRSI05IsRising":
               double var196 = var4.getRounded(var8);
               double var198 = var4.getRounded(var8 + 1);
               return var196 > var198;
            case "LinRegCloseAbove":
               double var369 = var4.getRounded(var8);
               return var1.Close(var8) > var369;
            case "LinRegCloseBelow":
               double var368 = var4.getRounded(var8);
               return var1.Close(var8) < var368;
            case "LinRegOpenAbove":
               double var367 = var4.getRounded(var8 + 1);
               return var1.Open(var8) > var367;
            case "LinRegOpenBelow":
               double var366 = var4.getRounded(var8 + 1);
               return var1.Open(var8) < var366;
            case "LinRegOpenAboveAfterOpenedBelow":
               double var365 = var4.getRounded(var8 + 1);
               return var1.Open(var8 + 1) < var365 && var1.Open(var8) > var365;
            case "LinRegOpenBelowAfterOpenedAbove":
               double var364 = var4.getRounded(var8 + 1);
               return var1.Open(var8 + 1) > var364 && var1.Open(var8) < var364;
            case "LinRegIsFalling":
               double var363 = var4.getRounded(var8 + 1);
               double var476 = var4.getRounded(var8);
               return var363 > var476;
            case "LinRegIsRising":
               double var362 = var4.getRounded(var8 + 1);
               double var475 = var4.getRounded(var8);
               return var362 < var475;
            case "MACDChangeDirectionDown":
               double var361 = var4.getRounded(var8 + 2);
               double var474 = var4.getRounded(var8 + 1);
               double var571 = var4.getRounded(var8);
               return var361 < var474 && var474 > var571;
            case "MACDChangeDirectionUP":
               double var360 = var4.getRounded(var8 + 2);
               double var473 = var4.getRounded(var8 + 1);
               double var570 = var4.getRounded(var8);
               return var360 > var473 && var473 < var570;
            case "MACDCrossAbove0":
               double var359 = var4.getRounded(var8 + 1);
               double var472 = var4.getRounded(var8);
               return var359 < 0.0 && var472 > 0.0;
            case "MACDCrossAbove001":
               double var358 = var4.getRounded(var8 + 1);
               double var471 = var4.getRounded(var8);
               return var358 < 0.01 && var471 > 0.01;
            case "MACDCrossAboveSignal":
               double var697 = var4.getRounded(var8 + 1);
               double var702 = var4.getRounded(var8);
               double var707 = var5.getRounded(var8 + 1);
               double var712 = var5.getRounded(var8);
               return var697 < var707 && var702 > var712;
            case "MACDCrossbelow0":
               double var357 = var4.getRounded(var8 + 1);
               double var470 = var4.getRounded(var8);
               return var357 > 0.0 && var470 < 0.0;
            case "MACDCrossbelow001":
               double var356 = var4.getRounded(var8 + 1);
               double var469 = var4.getRounded(var8);
               return var356 > 0.01 && var469 < 0.01;
            case "MACDCrossbelowSignal":
               double var696 = var4.getRounded(var8 + 1);
               double var701 = var4.getRounded(var8);
               double var706 = var5.getRounded(var8 + 1);
               double var711 = var5.getRounded(var8);
               return var696 > var706 && var701 < var711;
            case "MACDMainIsFalling":
               double var355 = var4.getRounded(var8 + 1);
               double var468 = var4.getRounded(var8);
               return var355 > var468;
            case "MACDMainIsRising":
               double var354 = var4.getRounded(var8 + 1);
               double var467 = var4.getRounded(var8);
               return var354 < var467;
            case "MACDSignalIsFalling":
               double var353 = var5.getRounded(var8 + 1);
               double var466 = var5.getRounded(var8);
               return var353 > var466;
            case "MACDSignalIsRising":
               double var352 = var5.getRounded(var8 + 1);
               double var465 = var5.getRounded(var8);
               return var352 < var465;
            case "MACDMainIsHigherThan0":
               double var351 = var4.getRounded(var8);
               return var351 > 0.0;
            case "MACDMainIsHigherThanLevel001":
               double var350 = var4.getRounded(var8);
               return var350 > 0.01;
            case "MACDMainIsHigherThanSignal":
               double var349 = var4.getRounded(var8);
               double var464 = var5.getRounded(var8);
               return var349 > var464;
            case "MACDMainIsLowerThan0":
               double var348 = var4.getRounded(var8);
               return var348 < 0.0;
            case "MACDMainIsLowerThanLevel001":
               double var347 = var4.getRounded(var8);
               return var347 < 0.01;
            case "MACDMainIsLowerThanSignal":
               double var346 = var4.getRounded(var8);
               double var463 = var5.getRounded(var8);
               return var346 < var463;
            case "MomentumChangeDirectionDown":
               double var345 = var4.getRounded(var8 + 2);
               double var462 = var4.getRounded(var8 + 1);
               double var569 = var4.getRounded(var8);
               return var345 < var462 && var462 > var569;
            case "MomentumChangeDirectionUp":
               double var344 = var4.getRounded(var8 + 2);
               double var461 = var4.getRounded(var8 + 1);
               double var568 = var4.getRounded(var8);
               return var344 > var461 && var461 < var568;
            case "MomentumCrossAbove0":
               double var343 = var4.getRounded(var8 + 1);
               double var460 = var4.getRounded(var8);
               return var343 < 0.0 && var460 > 0.0;
            case "MomentumCrossBelow0":
               double var342 = var4.getRounded(var8 + 1);
               double var459 = var4.getRounded(var8);
               return var342 > 0.0 && var459 < 0.0;
            case "MomentumIsFalling":
               double var341 = var4.getRounded(var8 + 1);
               double var458 = var4.getRounded(var8);
               return var341 > var458;
            case "MomentumIsRising":
               double var340 = var4.getRounded(var8 + 1);
               double var457 = var4.getRounded(var8);
               return var340 < var457;
            case "MomentumIsGreater":
               double var679 = var4.getRounded(var8);
               return var679 > 0.0;
            case "MomentumIsLower":
               double var678 = var4.getRounded(var8);
               return var678 < 0.0;
            case "BarClosesAboveMovingAverage":
               return var4.getRounded(var8) < var1.Close(var8);
            case "BarClosesBelowMovingAverage":
               return var4.getRounded(var8) > var1.Close(var8);
            case "BarOpenAboveMovingAverage":
               return var4.getRounded(var8 + 1) < var1.Open(var8);
            case "BarOpenBelowMovingAverage":
               return var4.getRounded(var8 + 1) > var1.Open(var8);
            case "BarOpensAboveAfterOpenBelowMovingAverage":
               return var1.Open(var8 + 1) < var4.getRounded(var8 + 1) && var1.Open(var8) > var4.getRounded(var8 + 1);
            case "BarOpensBelowAfterOpenAboveMovingAverage":
               return var1.Open(var8 + 1) > var4.getRounded(var8 + 1) && var1.Open(var8) < var4.getRounded(var8 + 1);
            case "MovingAverageFalling":
               return var4.getRounded(var8) < var4.getRounded(var8 + 1);
            case "MovingAverageRising":
               return var4.getRounded(var8) > var4.getRounded(var8 + 1);
            case "OSMAChangeDirectionDown":
               double var339 = var4.getRounded(var8 + 2);
               double var456 = var4.getRounded(var8 + 1);
               double var567 = var4.getRounded(var8);
               return var339 < var456 && var456 > var567;
            case "OSMAChangeDirectionUp":
               double var338 = var4.getRounded(var8 + 2);
               double var455 = var4.getRounded(var8 + 1);
               double var566 = var4.getRounded(var8);
               return var338 > var455 && var455 < var566;
            case "OSMACrossAbove0":
               double var337 = var4.getRounded(var8 + 1);
               double var454 = var4.getRounded(var8);
               return var337 < 0.0 && var454 > 0.0;
            case "OSMACrossBelow0":
               double var336 = var4.getRounded(var8 + 1);
               double var453 = var4.getRounded(var8);
               return var336 > 0.0 && var453 < 0.0;
            case "OSMAIsFalling":
               double var335 = var4.getRounded(var8 + 1);
               double var452 = var4.getRounded(var8);
               return var335 > var452;
            case "OSMAIsRising":
               double var334 = var4.getRounded(var8 + 1);
               double var451 = var4.getRounded(var8);
               return var334 < var451;
            case "OSMAIsGreater0":
               double var333 = var4.getRounded(var8);
               return var333 > 0.0;
            case "OSMAIsLower0":
               double var332 = var4.getRounded(var8);
               return var332 < 0.0;
            case "PriceAbovePSAR":
               return var1.Close(var8) > var4.getRounded(var8 + 1);
            case "PriceBelowPSAR":
               return var1.Close(var8) < var4.getRounded(var8 + 1);
            case "QQEChangeDirectionDown":
               double var331 = var4.getRounded(var8 + 2);
               double var450 = var4.getRounded(var8 + 1);
               double var565 = var4.getRounded(var8);
               return var331 < var450 && var450 > var565;
            case "QQEChangeDirectionUP":
               double var330 = var4.getRounded(var8 + 2);
               double var449 = var4.getRounded(var8 + 1);
               double var564 = var4.getRounded(var8);
               return var330 > var449 && var449 < var564;
            case "QQEIsGreater0":
               double var329 = var4.getRounded(var8);
               return var329 > 0.0;
            case "QQEIsLower0":
               double var328 = var4.getRounded(var8);
               return var328 < 0.0;
            case "QQEVal1CrossAbove0":
               double var327 = var4.getRounded(var8 + 1);
               double var448 = var4.getRounded(var8);
               return var327 < 0.0 && var448 > 0.0;
            case "QQEVal1CrossBelow0":
               double var326 = var4.getRounded(var8 + 1);
               double var447 = var4.getRounded(var8);
               return var326 > 0.0 && var447 < 0.0;
            case "QQEVal1CrossAboveVal2":
               double var695 = var4.getRounded(var8 + 1);
               double var700 = var4.getRounded(var8);
               double var705 = var5.getRounded(var8 + 1);
               double var710 = var5.getRounded(var8);
               return var695 < var705 && var700 > var710;
            case "QQEVal1CrossBelowVal2":
               double var694 = var4.getRounded(var8 + 1);
               double var699 = var4.getRounded(var8);
               double var704 = var5.getRounded(var8 + 1);
               double var709 = var5.getRounded(var8);
               return var694 > var704 && var699 < var709;
            case "QQEVal1IsFalling":
               double var325 = var4.getRounded(var8 + 1);
               double var446 = var4.getRounded(var8);
               return var325 > var446;
            case "QQEVal1IsRising":
               double var324 = var4.getRounded(var8 + 1);
               double var445 = var4.getRounded(var8);
               return var324 < var445;
            case "QQEVal2IsFalling":
               double var323 = var4.getRounded(var8 + 1);
               double var444 = var4.getRounded(var8);
               return var323 > var444;
            case "QQEVal2IsRising":
               double var322 = var4.getRounded(var8 + 1);
               double var443 = var4.getRounded(var8);
               return var322 < var443;
            case "QQEVal2IsGreaterVal1":
               double var321 = var4.getRounded(var8);
               double var442 = var5.getRounded(var8);
               return var321 < var442;
            case "QQEVal1IsGreaterVal2":
               double var320 = var4.getRounded(var8);
               double var441 = var5.getRounded(var8);
               return var320 > var441;
            case "FastReflexCrossDownSlowReflex":
               double var713 = var4.getRounded(var8);
               double var714 = var5.getRounded(var8);
               double var715 = var4.getRounded(var8 + 1);
               double var716 = var5.getRounded(var8 + 1);
               return var713 < var714 & var715 > var716;
            case "FastReflexCrossUpSlowReflex":
               double var210 = var4.getRounded(var8);
               double var212 = var5.getRounded(var8);
               double var214 = var4.getRounded(var8 + 1);
               double var216 = var5.getRounded(var8 + 1);
               return var210 > var212 & var214 < var216;
            case "ReflexChangeDirectionDown":
               double var717 = var4.getRounded(var8);
               double var718 = var4.getRounded(var8 + 1);
               double var719 = var4.getRounded(var8 + 2);
               double var720 = var4.getRounded(var8 + 3);
               return var717 < var718 && var719 < var718 || var717 < var718 && var720 < var719;
            case "ReflexChangeDirectionUp":
               double var218 = var4.getRounded(var8);
               double var220 = var4.getRounded(var8 + 1);
               double var222 = var4.getRounded(var8 + 2);
               double var224 = var4.getRounded(var8 + 3);
               return var218 > var220 && var222 > var220 || var218 > var220 && var224 > var222;
            case "ReflexIsFalling":
               double var723 = var4.getRounded(var8 + 1);
               double var724 = var4.getRounded(var8);
               return var724 < var723;
            case "ReflexIsRising":
               double var722 = var4.getRounded(var8 + 1);
               double var228 = var4.getRounded(var8);
               return var228 > var722;
            case "ROCCrossAbove0":
               double var733 = var4.getRounded(var8);
               double var738 = var4.getRounded(var8 + 1);
               return var733 > 0.0 && var738 < 0.0;
            case "ROCCrossBelow0":
               double var732 = var4.getRounded(var8);
               double var737 = var4.getRounded(var8 + 1);
               return var732 < 0.0 && var737 > 0.0;
            case "ROCIsAbove0":
               double var731 = var4.getRounded(var8);
               return var731 > 0.0;
            case "ROCIsBelow0":
               double var730 = var4.getRounded(var8);
               return var730 < 0.0;
            case "ROCIsFalling":
               double var729 = var4.getRounded(var8);
               double var736 = var4.getRounded(var8 + 1);
               return var729 < var736;
            case "ROCIsRising":
               double var728 = var4.getRounded(var8);
               double var735 = var4.getRounded(var8 + 1);
               return var728 > var735;
            case "RSIChangeDirectionDown":
               double var319 = var4.getRounded(var8 + 2);
               double var440 = var4.getRounded(var8 + 1);
               double var563 = var4.getRounded(var8);
               return var319 < var440 && var440 > var563;
            case "RSIChangeDirectionUp":
               double var318 = var4.getRounded(var8 + 2);
               double var439 = var4.getRounded(var8 + 1);
               double var562 = var4.getRounded(var8);
               return var318 > var439 && var439 < var562;
            case "RSICrossAbove0":
               double var317 = var4.getRounded(var8 + 1);
               double var438 = var4.getRounded(var8);
               return var317 < 0.0 && var438 > 0.0;
            case "RSICrossBelow0":
               double var316 = var4.getRounded(var8 + 1);
               double var437 = var4.getRounded(var8);
               return var316 > 0.0 && var437 < 0.0;
            case "RSIGreater0":
               double var315 = var4.getRounded(var8);
               return var315 > 0.0;
            case "RSILower0":
               double var314 = var4.getRounded(var8);
               return var314 < 0.0;
            case "RSIIsFalling":
               double var313 = var4.getRounded(var8 + 1);
               double var436 = var4.getRounded(var8);
               return var313 > var436;
            case "RSIIsRising":
               double var312 = var4.getRounded(var8 + 1);
               double var435 = var4.getRounded(var8);
               return var312 < var435;
            case "SchaffTrendCycleAbove80":
               double var741 = var4.getRounded(var8);
               double var742 = 80.0;
               return var741 > var742;
            case "SchaffTrendCycleBelow20":
               double var740 = var4.getRounded(var8);
               double var236 = 20.0;
               return var740 < var236;
            case "SchaffTrendCycleCrossAbove80":
               double var739 = var4.getRounded(var8);
               double var743 = var4.getRounded(var8 + 1);
               return var739 > 80.0 && var743 < 80.0;
            case "SchaffTrendCycleCrossBelow20":
               double var234 = var4.getRounded(var8);
               double var238 = var4.getRounded(var8 + 1);
               return var234 < 20.0 && var238 > 20.0;
            case "SRPercentRankAbove80":
               double var746 = var4.getRounded(var8);
               return var746 > 80.0;
            case "SRPercentRankBelow20":
               double var745 = var4.getRounded(var8);
               return var745 < 20.0;
            case "SRPercentRankAbove80for2bars":
               boolean var747 = false;

               for (byte var753 = 0; var753 < 2; var753 += 1) {
                  if (var4.getRounded(var8 + var753, 5) < 80.0) {
                     return false;
                  }

                  if (var4.getRounded(var8 + var753, 5) > 80.0) {
                     var747 = true;
                  }
               }

               return var747;
            case "SRPercentRankBelow20for2bars":
               boolean var242 = false;

               for (byte var752 = 0; var752 < 2; var752 += 1) {
                  if (var4.getRounded(var8 + var752, 5) > 20.0) {
                     return false;
                  }

                  if (var4.getRounded(var8 + var752, 5) < 20.0) {
                     var242 = true;
                  }
               }

               return var242;
            case "StdDevChangeDirectionDown":
               double var311 = var4.getRounded(var8 + 2);
               double var434 = var4.getRounded(var8 + 1);
               double var561 = var4.getRounded(var8);
               return var311 < var434 && var434 > var561;
            case "StdDevChangeDirectionUp":
               double var310 = var4.getRounded(var8 + 2);
               double var433 = var4.getRounded(var8 + 1);
               double var560 = var4.getRounded(var8);
               return var310 > var433 && var433 < var560;
            case "StdDevCrossAbove":
               double var309 = var4.getRounded(var8 + 1);
               double var432 = var4.getRounded(var8);
               return var309 < 1.0E-4 && var432 > 1.0E-4;
            case "StdDevCrossBelow":
               double var308 = var4.getRounded(var8 + 1);
               double var431 = var4.getRounded(var8);
               return var308 > 1.0E-4 && var431 < 1.0E-4;
            case "StdDevIsFalling":
               double var307 = var4.getRounded(var8 + 1);
               double var430 = var4.getRounded(var8);
               return var307 > var430;
            case "StdDevIsRising":
               double var306 = var4.getRounded(var8 + 1);
               double var429 = var4.getRounded(var8);
               return var306 < var429;
            case "StdDevIsHigher":
               double var677 = var4.getRounded(var8);
               return var677 > 1.0E-4;
            case "StdDevIsLower":
               double var676 = var4.getRounded(var8);
               return var676 < 1.0E-4;
            case "StochasticFastKCrossAbove":
               double var305 = var4.getRounded(var8 + 1);
               double var428 = var4.getRounded(var8);
               return var305 < 50.0 && var428 > 50.0;
            case "StochasticFastKCrossBelow":
               double var304 = var4.getRounded(var8 + 1);
               double var427 = var4.getRounded(var8);
               return var304 > 50.0 && var427 < 50.0;
            case "StochasticFastKHigherSlowD":
               double var303 = var5.getRounded(var8);
               double var426 = var4.getRounded(var8);
               return var303 > var426;
            case "StochasticFastKLowerSlowD":
               double var302 = var5.getRounded(var8);
               double var425 = var4.getRounded(var8);
               return var302 < var425;
            case "StochasticSlowDChangeDownwards":
               double var301 = var4.getRounded(var8 + 2);
               double var424 = var4.getRounded(var8 + 1);
               double var559 = var4.getRounded(var8);
               return var301 < var424 && var424 > var559;
            case "StochasticSlowDChangeUpwards":
               double var300 = var4.getRounded(var8 + 2);
               double var423 = var4.getRounded(var8 + 1);
               double var558 = var4.getRounded(var8);
               return var300 > var423 && var423 < var558;
            case "StochasticSlowDCrossAbove":
               double var299 = var4.getRounded(var8 + 1);
               double var422 = var4.getRounded(var8);
               return var299 < 50.0 && var422 > 50.0;
            case "StochasticSlowDCrossBelow":
               double var298 = var4.getRounded(var8 + 1);
               double var421 = var4.getRounded(var8);
               return var298 > 50.0 && var421 < 50.0;
            case "StochasticSlowDIsFalling":
               double var297 = var4.getRounded(var8 + 1);
               double var420 = var4.getRounded(var8);
               return var297 > var420;
            case "StochasticSlowDIsRising":
               double var296 = var4.getRounded(var8 + 1);
               double var419 = var4.getRounded(var8);
               return var296 < var419;
            case "StochasticSlowDIsHigher":
               double var295 = var4.getRounded(var8);
               return var295 > 50.0;
            case "StochasticSlowDIsLower":
               double var294 = var4.getRounded(var8);
               return var294 < 50.0;
            case "SuperTrendCloseAbove":
               double var751 = var4.getRounded(var8);
               double var754 = var1.Close.get(var8);
               return var754 > var751;
            case "SuperTrendCloseBelow":
               double var750 = var4.getRounded(var8);
               double var245 = var1.Close.get(var8);
               return var245 < var750;
            case "SuperTrendIsFalling":
               double var749 = var4.getRounded(var8);
               double var756 = var4.getRounded(var8 + 1);
               return var749 < var756;
            case "SuperTrendIsRising":
               double var748 = var4.getRounded(var8);
               double var755 = var4.getRounded(var8 + 1);
               return var748 > var755;
            case "SuperTrendIsInRange":
               double var243 = var4.getRounded(var8, 5);
               double var247 = var4.getRounded(var8 + 1, 5);
               return var243 == var247;
            case "IsDownTrend":
               double var757 = var4.getRounded(var8);
               double var758 = var1.Close(var8);
               return var758 < var757;
            case "IsUpTrend":
               double var249 = var4.getRounded(var8);
               double var251 = var1.Close(var8);
               return var251 > var249;
            case "UlcerIndexDownIsBelow":
               double var759 = var4.getRounded(var8);
               return var759 < 0.2;
            case "UlcerIndexUpIsBelow":
               double var253 = var4.getRounded(var8);
               return var253 < 0.2;
            case "UlcerIndexDownIsRising":
               double var744 = var4.getRounded(var8);
               double var721 = var4.getRounded(var8 + 1);
               return var744 > var721;
            case "UlcerIndexUpIsRising":
               double var240 = var4.getRounded(var8);
               double var226 = var4.getRounded(var8 + 1);
               return var240 > var226;
            case "VortexIsDownTrend":
               double var760 = var4.getRounded(var8);
               double var761 = var5.getRounded(var8);
               return var760 < var761;
            case "VortexIsUpTrend":
               double var255 = var4.getRounded(var8);
               double var257 = var5.getRounded(var8);
               return var255 > var257;
            case "VortexChangeTrendDown":
               double var762 = var4.getRounded(var8);
               double var763 = var4.getRounded(var8 + 1);
               double var764 = var5.getRounded(var8);
               double var765 = var5.getRounded(var8 + 1);
               return var762 < var764 && var763 > var765;
            case "VortexChangeTrendUp":
               double var259 = var4.getRounded(var8);
               double var261 = var4.getRounded(var8 + 1);
               double var263 = var5.getRounded(var8);
               double var265 = var5.getRounded(var8 + 1);
               return var259 > var263 && var261 < var265;
            case "CloseIsAboveVWAP":
               double var727 = var4.getRounded(var8);
               double var766 = var1.Close.get(var8);
               return var727 < var766;
            case "CloseIsBelowVWAP":
               double var726 = var4.getRounded(var8);
               double var267 = var1.Close.get(var8);
               return var267 < var726;
            case "FasterVWAPIsAboveSlowerVWAP":
               double var767 = var4.getRounded(var8);
               double var768 = var5.getRounded(var8);
               return var767 > var768;
            case "FasterVWAPIsBelowSlowerVWAP":
               double var269 = var4.getRounded(var8);
               double var271 = var5.getRounded(var8);
               return var269 < var271;
            case "VWAPIsFalling":
               double var725 = var4.getRounded(var8);
               double var734 = var4.getRounded(var8 + 1);
               return var725 < var734;
            case "VWAPIsRising":
               double var230 = var4.getRounded(var8);
               double var232 = var4.getRounded(var8 + 1);
               return var230 > var232;
            case "WilliamPRChangeDown":
               double var293 = var4.getRounded(var8 + 2);
               double var418 = var4.getRounded(var8 + 1);
               double var557 = var4.getRounded(var8);
               return var293 < var418 && var418 > var557;
            case "WilliamPRChangeUP":
               double var292 = var4.getRounded(var8 + 2);
               double var417 = var4.getRounded(var8 + 1);
               double var556 = var4.getRounded(var8);
               return var292 > var417 && var417 < var556;
            case "WilliamPRCrossAbove-50":
               double var291 = var4.getRounded(var8 + 1);
               double var416 = var4.getRounded(var8);
               return var291 < -50.0 && var416 > -50.0;
            case "WilliamPRCrossBelow-50":
               double var290 = var4.getRounded(var8 + 1);
               double var415 = var4.getRounded(var8);
               return var290 > -50.0 && var415 < -50.0;
            case "WilliamPRIsAbove-50":
               double var675 = var4.getRounded(var8);
               return var675 > -50.0;
            case "WilliamPRIsBelow-50":
               double var194 = var4.getRounded(var8);
               return var194 < -50.0;
            case "WilliamPRIsFalling":
               double var289 = var4.getRounded(var8 + 1);
               double var414 = var4.getRounded(var8);
               return var289 > var414;
            case "WilliamPRIsRising":
               double var288 = var4.getRounded(var8 + 1);
               double var413 = var4.getRounded(var8);
               return var288 < var413;
            case "WaveTrendChangeDirectionDown":
               double var287 = var4.getRounded(var8 + 2);
               double var412 = var4.getRounded(var8 + 1);
               double var555 = var4.getRounded(var8);
               return var287 < var412 && var412 > var555;
            case "WaveTrendChangeDirectionUP":
               double var286 = var4.getRounded(var8 + 2);
               double var411 = var4.getRounded(var8 + 1);
               double var62 = var4.getRounded(var8);
               return var286 > var411 && var411 < var62;
            case "WaveTrendCrossAbove0":
               double var285 = var4.getRounded(var8 + 1);
               double var410 = var4.getRounded(var8);
               return var285 < 0.0 && var410 > 0.0;
            case "WaveTrendCrossAbove001":
               double var284 = var4.getRounded(var8 + 1);
               double var409 = var4.getRounded(var8);
               return var284 < 0.01 && var409 > 0.01;
            case "WaveTrendCrossAboveSignal":
               double var693 = var4.getRounded(var8 + 1);
               double var698 = var4.getRounded(var8);
               double var703 = var5.getRounded(var8 + 1);
               double var708 = var5.getRounded(var8);
               return var693 < var703 && var698 > var708;
            case "WaveTrendCrossbelow0":
               double var283 = var4.getRounded(var8 + 1);
               double var408 = var4.getRounded(var8);
               return var283 > 0.0 && var408 < 0.0;
            case "WaveTrendCrossbelow001":
               double var282 = var4.getRounded(var8 + 1);
               double var407 = var4.getRounded(var8);
               return var282 > 0.01 && var407 < 0.01;
            case "WaveTrendCrossbelowSignal":
               double var202 = var4.getRounded(var8 + 1);
               double var204 = var4.getRounded(var8);
               double var206 = var5.getRounded(var8 + 1);
               double var208 = var5.getRounded(var8);
               return var202 > var206 && var204 < var208;
            case "WaveTrendMainIsFalling":
               double var281 = var4.getRounded(var8 + 1);
               double var406 = var4.getRounded(var8);
               return var281 > var406;
            case "WaveTrendMainIsRising":
               double var280 = var4.getRounded(var8 + 1);
               double var405 = var4.getRounded(var8);
               return var280 < var405;
            case "WaveTrendSignalIsFalling":
               double var279 = var5.getRounded(var8 + 1);
               double var404 = var5.getRounded(var8);
               return var279 > var404;
            case "WaveTrendSignalIsRising":
               double var278 = var5.getRounded(var8 + 1);
               double var403 = var5.getRounded(var8);
               return var278 < var403;
            case "WaveTrendMainIsHigherThan0":
               double var277 = var4.getRounded(var8);
               return var277 > 0.0;
            case "WaveTrendMainIsHigherThanLevel001":
               double var276 = var4.getRounded(var8);
               return var276 > 0.01;
            case "WaveTrendMainIsHigherThanSignal":
               double var275 = var4.getRounded(var8);
               double var402 = var5.getRounded(var8);
               return var275 > var402;
            case "WaveTrendMainIsLowerThan0":
               double var274 = var4.getRounded(var8);
               return var274 < 0.0;
            case "WaveTrendMainIsLowerThanLevel001":
               double var273 = var4.getRounded(var8);
               return var273 < 0.01;
            case "WaveTrendMainIsLowerThanSignal":
               double var30 = var4.getRounded(var8);
               double var32 = var5.getRounded(var8);
               return var30 < var32;
            default:
               return false;
         }
      } else {
         return false;
      }
   }

   public String getFilePath(String var1) {
      return this.indicatorTestsPath + "/" + this.engine + "/" + var1;
   }

   private boolean checkPrecision(double var1, double var3, int var5) {
      double var6 = Math.pow(10.0, -var5);
      return Math.abs(var1 - var3) < var6;
   }

   private ChartData loadSourceData(String var1, DataSeries var2) throws Exception {
      ArrayList var3 = SQUtils.loadLines(var1, ";");
      TimeDataSeries var4 = new TimeDataSeries();
      DataSeries var5 = new DataSeries();
      DataSeries var6 = new DataSeries();
      DataSeries var7 = new DataSeries();
      DataSeries var8 = new DataSeries();
      DataSeries var9 = new DataSeries();
      this.tfRecognizer.reset();
      int var10 = 0;

      for (int var11 = 0; var11 < var3.size(); var11++) {
         String[] var12 = (String[])var3.get(var11);
         if (var12.length < 7) {
            if (++var10 < this.maxInvalidLines) {
               String var18 = "Parse error - Invalid line no." + (var11 + 1) + ". Expected 7 values, got " + var12.length + ".";
               this.dataErrors.add(var18);
               Log.error(var18);
            }
         } else {
            try {
               long var13 = this.df.parse(var12[0]).getTime();
               var4.add(var13);
               var5.add(Double.parseDouble(var12[1].trim()));
               var6.add(Double.parseDouble(var12[2].trim()));
               var7.add(Double.parseDouble(var12[3].trim()));
               var8.add(Double.parseDouble(var12[4].trim()));
               var9.add(Double.parseDouble(var12[5].trim()));
               var2.add(Double.parseDouble(var12[6].trim()));
               this.tfRecognizer.processTime(var13);
            } catch (Exception var15) {
               if (++var10 < this.maxInvalidLines) {
                  String var14 = "Parse error - Invalid line no." + (var11 + 1) + ". " + var15.getMessage();
                  this.dataErrors.add(var14);
                  Log.error(var14);
               }
            }
         }
      }

      if (var10 > 0) {
         this.errorMsg = "File contains invalid lines (" + var10 + ")";
         throw new IndicatorTestException(1, new Exception(var1));
      } else {
         this.timeframe = this.tfRecognizer.getTimeframe();
         Log.info("timeframe is : {} ", this.timeframe);
         ChartData var16 = new ChartData(
            this.timeframe, var4, var5, var6, var7, var8, var9, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null
         );
         InstrumentInfo var17 = new InstrumentInfo();
         var17.tickSize = this.tickSize;
         var17.tickStep = Math.pow(10.0, -this.digits);
         var17.decimals = this.digits;
         var16.setInstrumentInfo(var17);
         return var16;
      }
   }

   private ChartData loadSourceData(String var1, DataSeries var2, boolean var3) throws Exception {
      ArrayList var4 = SQUtils.loadLines(var1, ";");
      TimeDataSeries var5 = new TimeDataSeries();
      DataSeries var6 = new DataSeries();
      DataSeries var7 = new DataSeries();
      DataSeries var8 = new DataSeries();
      DataSeries var9 = new DataSeries();
      DataSeries var10 = new DataSeries();
      TimeDataSeries var11 = new TimeDataSeries();
      DataSeries var12 = new DataSeries();
      DataSeries var13 = new DataSeries();
      DataSeries var14 = new DataSeries();
      DataSeries var15 = new DataSeries();
      new DataSeries();
      TimeDataSeries var17 = new TimeDataSeries();
      DataSeries var18 = new DataSeries();
      DataSeries var19 = new DataSeries();
      DataSeries var20 = new DataSeries();
      DataSeries var21 = new DataSeries();
      new DataSeries();
      TimeDataSeries var23 = new TimeDataSeries();
      DataSeries var24 = new DataSeries();
      DataSeries var25 = new DataSeries();
      DataSeries var26 = new DataSeries();
      DataSeries var27 = new DataSeries();
      new DataSeries();
      this.tfRecognizer.reset();
      int var29 = 0;
      boolean var30 = true;
      long var31 = 0L;
      long var33 = 0L;
      long var35 = 0L;
      SimpleDateFormat var37 = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss");
      double var38 = 0.0;
      double var40 = 0.0;
      double var42 = 0.0;
      double var44 = 0.0;
      double var46 = 0.0;
      double var48 = 0.0;

      for (int var50 = 0; var50 < var4.size(); var50++) {
         String[] var51 = (String[])var4.get(var50);
         if (var51.length < 7) {
            if (++var29 < this.maxInvalidLines) {
               String var73 = "Parse error - Invalid line no." + (var50 + 1) + ". Expected 7 values, got " + var51.length + ".";
               Log.error(var73);
            }
         } else {
            try {
               String var52 = var51[0];
               Date var75 = var37.parse(var52);
               long var54 = var75.getTime();
               var5.add(var54);
               var6.add(Double.parseDouble(var51[1].trim()));
               var7.add(Double.parseDouble(var51[2].trim()));
               var8.add(Double.parseDouble(var51[3].trim()));
               var9.add(Double.parseDouble(var51[4].trim()));
               var10.add(Double.parseDouble(var51[5].trim()));
               var2.add(Double.parseDouble(var51[6].trim()));
               this.tfRecognizer.processTime(var54);
               var11.add(var54);
               var12.add(Double.parseDouble(var51[1].trim()));
               var13.add(Double.parseDouble(var51[2].trim()));
               var14.add(Double.parseDouble(var51[3].trim()));
               var15.add(Double.parseDouble(var51[4].trim()));
               var17.add(var54);
               var18.add(Double.parseDouble(var51[1].trim()));
               var19.add(Double.parseDouble(var51[2].trim()));
               var20.add(Double.parseDouble(var51[3].trim()));
               var21.add(Double.parseDouble(var51[4].trim()));
               var23.add(var54);
               var24.add(Double.parseDouble(var51[1].trim()));
               var25.add(Double.parseDouble(var51[2].trim()));
               var26.add(Double.parseDouble(var51[3].trim()));
               var27.add(Double.parseDouble(var51[4].trim()));
               if (var30) {
                  var30 = false;
                  var31 = var54;
                  var33 = var54;
                  var35 = var54;
                  var38 = Double.parseDouble(var51[2].trim());
                  var40 = Double.parseDouble(var51[3].trim());
                  var42 = Double.parseDouble(var51[2].trim());
                  var44 = Double.parseDouble(var51[3].trim());
                  var46 = Double.parseDouble(var51[2].trim());
                  var48 = Double.parseDouble(var51[3].trim());
               } else {
                  if (!SQTime.isSameDay(var31, var54)) {
                     var31 = var54;
                     var38 = Double.parseDouble(var51[2].trim());
                     var40 = Double.parseDouble(var51[3].trim());
                  } else if (SQTime.isSameDay(var31, var54)) {
                     var12.set(0, var12.get(1));
                     if (Double.parseDouble(var51[2].trim()) > var38) {
                        var13.set(0, Double.parseDouble(var51[2].trim()));
                        var38 = Double.parseDouble(var51[2].trim());
                     } else {
                        var13.set(0, var13.get(1));
                     }

                     if (Double.parseDouble(var51[3].trim()) < var40) {
                        var14.set(0, Double.parseDouble(var51[3].trim()));
                        var40 = Double.parseDouble(var51[3].trim());
                     } else {
                        var14.set(0, var14.get(1));
                     }

                     var15.set(0, Double.parseDouble(var51[4].trim()));
                  }

                  if (SQTime.isSameWeek(var33, var54) == 1) {
                     var33 = var54;
                     var42 = Double.parseDouble(var51[2].trim());
                     var44 = Double.parseDouble(var51[3].trim());
                  } else if (SQTime.isSameWeek(var33, var54) == 2) {
                     var18.set(0, var18.get(1));
                     if (Double.parseDouble(var51[2].trim()) > var42) {
                        var19.set(0, Double.parseDouble(var51[2].trim()));
                        var42 = Double.parseDouble(var51[2].trim());
                     } else {
                        var19.set(0, var19.get(1));
                     }

                     if (Double.parseDouble(var51[3].trim()) < var44) {
                        var20.set(0, Double.parseDouble(var51[3].trim()));
                        var44 = Double.parseDouble(var51[3].trim());
                     } else {
                        var20.set(0, var20.get(1));
                     }

                     var21.set(0, Double.parseDouble(var51[4].trim()));
                  }

                  if (!SQTime.isSameMonth(var35, var54)) {
                     var35 = var54;
                     var46 = Double.parseDouble(var51[2].trim());
                     var48 = Double.parseDouble(var51[3].trim());
                  } else if (SQTime.isSameMonth(var35, var54)) {
                     var24.set(0, var24.get(1));
                     if (Double.parseDouble(var51[2].trim()) > var46) {
                        var25.set(0, Double.parseDouble(var51[2].trim()));
                        var46 = Double.parseDouble(var51[2].trim());
                     } else {
                        var25.set(0, var25.get(1));
                     }

                     if (Double.parseDouble(var51[3].trim()) < var48) {
                        var26.set(0, Double.parseDouble(var51[3].trim()));
                        var48 = Double.parseDouble(var51[3].trim());
                     } else {
                        var26.set(0, var26.get(1));
                     }

                     var27.set(0, Double.parseDouble(var51[4].trim()));
                  }
               }
            } catch (Exception var70) {
               if (++var29 < this.maxInvalidLines) {
                  String var53 = "Parse error - Invalid line no." + (var50 + 1) + ". " + var70.getMessage();
                  Log.error(var53);
               }
            }
         }
      }

      var30 = true;
      double var72 = 0.0;
      double var74 = 0.0;
      double var76 = 0.0;
      double var56 = 0.0;
      double var58 = 0.0;
      double var60 = 0.0;
      double var62 = 0.0;
      double var64 = 0.0;
      double var66 = 0.0;

      for (int var68 = 0; var68 < var12.size(); var68++) {
         if (var30) {
            var30 = false;
            var31 = var11.get(var68);
            var33 = var17.get(var68);
            var35 = var23.get(var68);
            var72 = var15.get(var68);
            var74 = var21.get(var68);
            var76 = var27.get(var68);
            var56 = var13.get(var68);
            var58 = var19.get(var68);
            var60 = var25.get(var68);
            var62 = var14.get(var68);
            var64 = var20.get(var68);
            var66 = var26.get(var68);
         } else {
            if (!SQTime.isSameDay(var31, var11.get(var68))) {
               var31 = var11.get(var68);
               var72 = var15.get(var68);
               var56 = var13.get(var68);
               var62 = var14.get(var68);
            } else if (SQTime.isSameDay(var31, var11.get(var68))) {
               if (var3) {
                  var15.set(var68, var72);
               }

               if (var3) {
                  var13.set(var68, var56);
               }

               if (var3) {
                  var14.set(var68, var62);
               }
            }

            if (SQTime.isSameWeek(var33, var17.get(var68)) == 1) {
               var33 = var17.get(var68);
               var74 = var21.get(var68);
               var58 = var19.get(var68);
               var64 = var20.get(var68);
            } else if (SQTime.isSameWeek(var33, var17.get(var68)) == 2) {
               if (var3) {
                  var21.set(var68, var74);
               }

               if (var3) {
                  var19.set(var68, var58);
               }

               if (var3) {
                  var20.set(var68, var64);
               }
            }

            if (!SQTime.isSameMonth(var35, var23.get(var68))) {
               var35 = var23.get(var68);
               var76 = var27.get(var68);
               var60 = var25.get(var68);
               var66 = var26.get(var68);
            } else if (SQTime.isSameMonth(var35, var23.get(var68))) {
               if (var3) {
                  var27.set(var68, var76);
               }

               if (var3) {
                  var25.set(var68, var60);
               }

               if (var3) {
                  var26.set(var68, var66);
               }
            }
         }
      }

      if (var29 > 0) {
         throw new IndicatorTestException(1, new Exception(var1));
      }

      this.timeframe = this.tfRecognizer.getTimeframe();
      Log.info("timeframe is : {} ", this.timeframe);
      ChartData var77 = new ChartData(
         this.timeframe,
         var5,
         var6,
         var7,
         var8,
         var9,
         var10,
         var11,
         var12,
         var13,
         var14,
         var15,
         var17,
         var18,
         var19,
         var20,
         var21,
         var23,
         var24,
         var25,
         var26,
         var27
      );
      InstrumentInfo var69 = new InstrumentInfo();
      var69.tickSize = this.tickSize;
      var69.tickStep = this.tickStep;
      var69.decimals = this.digits;
      var77.setInstrumentInfo(var69);
      return var77;
   }

   private Object getSourceData(ChartData var1, int var2) throws Exception {
      switch (var2) {
         case 0:
            return var1;
         case 1:
            return var1.Open;
         case 2:
            return var1.High;
         case 3:
            return var1.Low;
         case 4:
            return var1.Close;
         case 5:
            return var1.Volume;
         case 6:
            return var1.Typical;
         case 7:
            return var1.Median;
         case 8:
            return var1.Weighted;
         default:
            throw new Exception("Invalid input data source '" + var2 + "'");
      }
   }

   public void setstartCompareIndex(int var1) throws Exception {
      this.startCompareIndex = var1;
   }

   public void setmaxInvalidLines(int var1) throws Exception {
      this.maxInvalidLines = var1;
   }

   public void setTimeFrame(String var1) throws Exception {
      this.timeframe = var1;
   }

   public void setdigits(int var1) throws Exception {
      this.digits = var1;
   }

   public void settickSize(double var1) throws Exception {
      this.tickSize = var1;
   }

   private Object callMethod(Object var1, String var2, Object[] var3) throws Exception {
      Method var4 = getMethod(var1, var2, var3);
      return var4.invoke(var1, var3);
   }

   public static Method getMethod(Object var0, String var1, Object[] var2) throws Exception {
      Class var3 = var0.getClass();
      Class[] var4 = new Class[var2.length];

      for (int var5 = 0; var5 < var2.length; var5++) {
         var4[var5] = getParamClass(var2[var5]);
      }

      return var3.getMethod(var1, var4);
   }

   private static Class<?> getParamClass(Object var0) {
      Class var1 = var0.getClass();
      if (var1.getSimpleName().equals(Integer.class.getSimpleName())) {
         return int.class;
      } else if (var1.getSimpleName().equals(Double.class.getSimpleName())) {
         return double.class;
      } else if (var1.getSimpleName().equals(Boolean.class.getSimpleName())) {
         return boolean.class;
      } else {
         return !var1.getSimpleName().equals(TypicalDataSeries.class.getSimpleName())
               && !var1.getSimpleName().equals(MedianDataSeries.class.getSimpleName())
               && !var1.getSimpleName().equals(WeightedDataSeries.class.getSimpleName())
            ? var1
            : DataSeries.class;
      }
   }

   public String getEngine() {
      return this.engine;
   }

   public String getErrorMsg() {
      this.lock.lock();

      try {
         return this.errorMsg;
      } finally {
         this.lock.unlock();
      }
   }

   public void setStartCompareIndex(int var1) {
      this.lock.lock();

      try {
         this.startCompareIndex = var1;
      } finally {
         this.lock.unlock();
      }
   }

   public void setTimeframe(String var1) {
      this.lock.lock();

      try {
         this.timeframe = var1;
      } finally {
         this.lock.unlock();
      }
   }

   public void setMaxInvalidLines(int var1) {
      this.lock.lock();

      try {
         this.maxInvalidLines = var1;
      } finally {
         this.lock.unlock();
      }
   }

   public ArrayList<String> getDataErrors() {
      this.lock.lock();

      try {
         return this.dataErrors;
      } finally {
         this.lock.unlock();
      }
   }

   public boolean shouldShowGrid() {
      this.lock.lock();

      try {
         return this.showGrid;
      } finally {
         this.lock.unlock();
      }
   }

   public void reset() {
      this.lock.lock();

      try {
         this.showGrid = false;
         this.dataErrors.clear();
      } finally {
         this.lock.unlock();
      }
   }
}
