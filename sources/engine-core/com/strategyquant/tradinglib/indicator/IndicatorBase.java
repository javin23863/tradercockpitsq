package com.strategyquant.tradinglib.indicator;

import com.strategyquant.datalib.DataSeries;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.datalib.dataseries.ComputedDataSeries;
import com.strategyquant.datalib.dataseries.DataSeriesBase;
import com.strategyquant.datalib.dataseries.IDataSeriesChangeListener;
import com.strategyquant.datalib.dataseries.IDataSeriesComputer;
import com.strategyquant.datalib.dataseries.TimeDataSeries;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.tradinglib.Buffer;
import com.strategyquant.tradinglib.ChartData;
import com.strategyquant.tradinglib.Output;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.StrategyBase;
import com.strategyquant.tradinglib.debug.DebugConsole;
import com.strategyquant.tradinglib.debug.Debugger;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class IndicatorBase implements IDataSeriesComputer, IDataSeriesChangeListener {
   public static final Logger Log = LoggerFactory.getLogger("Indicator");
   private static final Logger DebugLog = LoggerFactory.getLogger(Debugger.class);
   @Parameter
   public int Shift;
   protected IndicatorBase.DataSeriesSet[] inputDataSeries = null;
   protected IndicatorBase.DataSeriesSet[] outputDataSeries = null;
   protected int CurrentBar = 0;
   private int computingBar = -1;
   private String indicatorName;
   private boolean onInitCalled = false;
   public int Calls = 0;
   public boolean RecursionExceptionEnforced = true;
   private ArrayList<DataSeries> outputLinesForData = null;
   private int userDefinedStartingBar = 0;

   protected void Initialize() throws TradingException {
      this.OnInit();
   }

   protected void Deinitialize() throws TradingException {
      this.deinitializeDataSeries();
      this.OnDeinit();
   }

   private void deinitializeDataSeries() {
      if (this.outputDataSeries != null) {
         for (int var1 = 0; var1 < this.outputDataSeries.length; var1++) {
            this.outputDataSeries[var1].destroy();
         }
      }
   }

   protected void OnInit() throws TradingException {
   }

   protected abstract void OnBarUpdate() throws TradingException;

   protected void OnDeinit() throws TradingException {
   }

   public void callOnInit() throws TradingException {
      if (!this.onInitCalled) {
         this.onInitCalled = true;
         this.Initialize();
      }
   }

   public abstract void initialize(IndicatorsObj var1, boolean var2) throws TradingException;

   public void initializeStrategy(StrategyBase var1) {
   }

   public void setIndyStartingBar(int var1) {
      this.userDefinedStartingBar = var1;
   }

   public int getIndyStartingBar() {
      return this.userDefinedStartingBar;
   }

   protected void recognizeAndInitializeDataSeries(boolean var1) throws TradingException {
      int var2 = 0;

      try {
         Class var3 = this.getClass();
         this.indicatorName = var3.getSimpleName();

         for (Field var7 : var3.getFields()) {
            Parameter var8 = var7.getAnnotation(Parameter.class);
            if (var8 != null) {
               Object var9 = var7.get(this);
               if (var9 instanceof ComputedDataSeries) {
                  ComputedDataSeries var10 = (ComputedDataSeries)var9;
                  var2 = this.addDataSeries("Open", var10.Open, var1);
                  this.addDataSeries("High", var10.High, var1);
                  this.addDataSeries("Low", var10.Low, var1);
                  this.addDataSeries("Close", var10.Close, var1);
                  this.addDataSeries("Volume", var10.Volume, var1);
                  this.setIndyStartingBar(var10.Close.getIndyStartingBar());
               } else if (var7.getType().isAssignableFrom(DataSeries.class)) {
                  IndicatorBase.DataSeriesSet var18 = new IndicatorBase.DataSeriesSet();
                  var18.series = (DataSeries)var9;
                  var18.name = var7.getName();
                  var2 = var18.series.size() + var18.series.getShift();
                  this.addInputDataSeries(var18);
                  var18.series.addChangeListener(this.recognizeUpdateEvent(var1), this);
                  this.setIndyStartingBar(var18.series.getIndyStartingBar());
               } else if (var7.getType().isAssignableFrom(TimeDataSeries.class)) {
                  IndicatorBase.DataSeriesSet var19 = new IndicatorBase.DataSeriesSet();
                  var19.series = (DataSeriesBase)var7.get(this);
                  var19.name = var7.getName();
                  var2 = var19.series.size() + var19.series.getShift();
                  this.addInputDataSeries(var19);
                  this.setIndyStartingBar(var19.series.getIndyStartingBar());
               } else if (var7.getType().isAssignableFrom(ChartData.class)) {
                  ChartData var20 = (ChartData)var7.get(this);
                  this.addDataSeries(var7.getName(), var20.Time, var1);
                  this.addDataSeries(var7.getName(), var20.Open, var1);
                  this.addDataSeries(var7.getName(), var20.High, var1);
                  this.addDataSeries(var7.getName(), var20.Low, var1);
                  this.addDataSeries(var7.getName(), var20.Close, var1);
                  int var11 = this.addDataSeries(var7.getName(), var20.Volume, var1);
                  var2 = var11;
                  this.setIndyStartingBar(var20.getIndyStartingBar());
               }
            }

            Parameter var17 = var7.getAnnotation(Parameter.class);
            if (var17 == null) {
               Output var21 = var7.getAnnotation(Output.class);
               Buffer var22 = var7.getAnnotation(Buffer.class);
               if (var21 != null || var22 != null) {
                  if (var7.getType().isAssignableFrom(DataSeries.class)) {
                     DataSeries var12 = new DataSeries();
                     if (var2 > 0) {
                        var12.addValues(var2);
                     }

                     var7.set(this, var12);
                     var12.setDataComputer(this);
                     IndicatorBase.DataSeriesSet var13 = new IndicatorBase.DataSeriesSet();
                     var13.series = var12;
                     var13.name = var7.getName();
                     var13.type = 1;
                     this.addOutputDataSeries(var13);
                  } else if (var7.getType().isAssignableFrom(TimeDataSeries.class)) {
                     TimeDataSeries var23 = new TimeDataSeries();
                     if (var2 > 0) {
                        var23.addValues(var2);
                     }

                     var7.set(this, var23);
                     var23.setDataComputer(this);
                     IndicatorBase.DataSeriesSet var24 = new IndicatorBase.DataSeriesSet();
                     var24.series = var23;
                     var24.name = var7.getName();
                     var24.type = 2;
                     this.addOutputDataSeries(var24);
                  }
               }
            }
         }
      } catch (TradingException var14) {
         Log.error("Cannot initialize DataSeries for indicator", var14);
         throw var14;
      } catch (IllegalArgumentException var15) {
         Log.error("Cannot initialize DataSeries for indicator (2)", var15);
         throw new TradingException(var15);
      } catch (IllegalAccessException var16) {
         Log.error("Cannot initialize DataSeries for indicator (3)", var16);
         throw new TradingException(var16);
      }
   }

   private int recognizeUpdateEvent(boolean var1) {
      return var1 ? 4 : 2;
   }

   private void addInputDataSeries(IndicatorBase.DataSeriesSet var1) {
      if (this.inputDataSeries == null) {
         this.inputDataSeries = new IndicatorBase.DataSeriesSet[1];
      } else {
         IndicatorBase.DataSeriesSet[] var2 = Arrays.copyOf(this.inputDataSeries, this.inputDataSeries.length + 1);
         this.inputDataSeries = var2;
      }

      this.inputDataSeries[this.inputDataSeries.length - 1] = var1;
   }

   private void addOutputDataSeries(IndicatorBase.DataSeriesSet var1) {
      if (this.outputDataSeries == null) {
         this.outputDataSeries = new IndicatorBase.DataSeriesSet[1];
      } else {
         IndicatorBase.DataSeriesSet[] var2 = Arrays.copyOf(this.outputDataSeries, this.outputDataSeries.length + 1);
         this.outputDataSeries = var2;
      }

      this.outputDataSeries[this.outputDataSeries.length - 1] = var1;
   }

   private int addDataSeries(String var1, DataSeries var2, boolean var3) {
      IndicatorBase.DataSeriesSet var4 = new IndicatorBase.DataSeriesSet();
      var4.series = var2;
      var4.name = var1;
      this.addInputDataSeries(var4);
      var4.series.addChangeListener(this.recognizeUpdateEvent(var3), this);
      return var4.series.size() + var4.series.getShift();
   }

   private int addDataSeries(String var1, TimeDataSeries var2, boolean var3) {
      IndicatorBase.DataSeriesSet var4 = new IndicatorBase.DataSeriesSet();
      var4.series = var2;
      var4.name = var1;
      this.addInputDataSeries(var4);
      var4.series.addChangeListener(this.recognizeUpdateEvent(var3), this);
      return var4.series.size() + var4.series.getShift();
   }

   public void changed(int var1, int var2) throws TradingException {
      if (this.outputDataSeries != null && this.outputDataSeries.length != 0) {
         for (int var3 = 0; var3 < this.outputDataSeries.length; var3++) {
            IndicatorBase.DataSeriesSet var4 = this.outputDataSeries[var3];
            int var5 = var4.series.specialSize();
            if (var2 > var5) {
               int var6 = var2 - var5;
               if (var4.type == 1) {
                  var4.series.addDoubleValues(var6, 0.0);
               } else {
                  var4.series.addLongValues(var6, 0L);
               }
            }

            var4.series.updateComputedUntil(var1 + 1);
         }
      }
   }

   public void compute(int var1, int var2) throws TradingException {
      if (this.outputDataSeries == null) {
         throw new TradingException("No OutputDataSeries defined!");
      }

      int var3 = this.outputDataSeries[0].series.size() + this.outputDataSeries[0].series.getShift();
      if (var3 != 0) {
         if (this.RecursionExceptionEnforced || this.computingBar == -1) {
            if (var1 == -1) {
               var1 = var3;
            } else if (var1 == var2 && var1 + 1 < var3) {
               var1++;
            }

            for (int var4 = var1 - 1; var4 >= var2; var4--) {
               this.CurrentBar = var3 - var4 - 1;
               if (this.computingBar != -1 && this.CurrentBar <= this.computingBar) {
                  return;
               }

               this.computingBar = this.CurrentBar;
               if (this.computingBar >= this.userDefinedStartingBar) {
                  try {
                     this.rememberPreviousShiftAndSetNew(var4);
                     this.OnBarUpdate();
                     this.restorePreviousShift();
                  } catch (TradingException var6) {
                     throw var6.addCall(this.indicatorName + "(CurrentBar: " + this.CurrentBar + ", Index: " + var4 + ")");
                  }
               }

               if (this.outputDataSeries != null) {
                  for (int var5 = 0; var5 < this.outputDataSeries.length; var5++) {
                     this.outputDataSeries[var5].series.setComputedUntil(var4);
                  }
               }
            }

            this.computingBar = -1;
         }
      }
   }

   private void rememberPreviousShiftAndSetNew(int var1) {
      if (this.inputDataSeries != null) {
         for (int var2 = 0; var2 < this.inputDataSeries.length; var2++) {
            this.inputDataSeries[var2].shift = this.inputDataSeries[var2].series.getShift();
            this.inputDataSeries[var2].series.setShift(var1);
         }
      }

      if (this.outputDataSeries != null) {
         for (int var3 = 0; var3 < this.outputDataSeries.length; var3++) {
            this.outputDataSeries[var3].shift = this.outputDataSeries[var3].series.getShift();
            this.outputDataSeries[var3].series.setShift(var1);
         }
      }
   }

   private void restorePreviousShift() {
      if (this.inputDataSeries != null) {
         for (int var1 = 0; var1 < this.inputDataSeries.length; var1++) {
            this.inputDataSeries[var1].series.setShift(this.inputDataSeries[var1].shift);
         }
      }

      if (this.outputDataSeries != null) {
         for (int var2 = 0; var2 < this.outputDataSeries.length; var2++) {
            this.outputDataSeries[var2].series.setShift(this.outputDataSeries[var2].shift);
         }
      }
   }

   public void refreshShift() {
      if (this.inputDataSeries != null) {
         int var1 = this.inputDataSeries[0].series.getShift();
         if (this.outputDataSeries != null) {
            for (int var2 = 0; var2 < this.outputDataSeries.length; var2++) {
               this.outputDataSeries[var2].series.setShift(var1);
            }
         }
      }
   }

   public void destroy() {
      if (this.inputDataSeries != null) {
         for (int var1 = 0; var1 < this.inputDataSeries.length; var1++) {
            if (this.inputDataSeries[var1].series != null) {
               this.inputDataSeries[var1].series.destroy();
               this.inputDataSeries[var1] = null;
            }
         }

         this.inputDataSeries = null;
      }

      if (this.outputDataSeries != null) {
         for (int var2 = 0; var2 < this.outputDataSeries.length; var2++) {
            if (this.outputDataSeries[var2].series != null) {
               this.outputDataSeries[var2].series.destroy();
               this.outputDataSeries[var2] = null;
            }
         }

         this.outputDataSeries = null;
      }
   }

   public int getOutputsCount() {
      return this.outputDataSeries == null ? 0 : this.outputDataSeries.length;
   }

   public ArrayList<DataSeries> getOutputLines() throws IllegalArgumentException, IllegalAccessException {
      if (this.outputLinesForData == null) {
         this.outputLinesForData = new ArrayList<>();
         Class var1 = this.getClass();

         for (Field var5 : var1.getFields()) {
            Output var6 = var5.getAnnotation(Output.class);
            if (var6 != null && var5.getType().isAssignableFrom(DataSeries.class)) {
               DataSeries var7 = (DataSeries)var5.get(this);
               var7.setLineName(var5.getName());
               var7.setColor(var6.color());
               var7.setShowInChart(var6.show());
               var7.setShowZeroValues(var6.showZeroValues());
               var7.setChartType(var6.chartType());
               this.outputLinesForData.add(var7);
            }
         }
      }

      return this.outputLinesForData;
   }

   public boolean checkUsesData(ChartData var1) {
      if (this.inputDataSeries == null) {
         return false;
      }

      for (int var2 = 0; var2 < this.inputDataSeries.length; var2++) {
         IndicatorBase.DataSeriesSet var3 = this.inputDataSeries[var2];
         if (var1.Time == var3.series) {
            return true;
         }

         if (var1.Open == var3.series) {
            return true;
         }

         if (var1.High == var3.series) {
            return true;
         }

         if (var1.Low == var3.series) {
            return true;
         }

         if (var1.Close == var3.series) {
            return true;
         }

         if (var1.Volume == var3.series) {
            return true;
         }
      }

      return false;
   }

   public void getDataColumnHeader(StringBuilder var1, ArrayList<String> var2) {
      if (this.outputDataSeries != null) {
         for (int var3 = 0; var3 < this.outputDataSeries.length; var3++) {
            IndicatorBase.DataSeriesSet var4 = this.outputDataSeries[var3];
            StringBuilder var5 = new StringBuilder(this.indicatorName);
            if (!var4.name.equals("Value")) {
               var5.append(".");
               var5.append(var4.name);
            }

            String var6 = var5.toString();
            var6 = this.findUniqueColumnNames(var6, var2);
            var2.add(var6);
            var1.append("\"");
            var1.append(var6);
            var1.append("\"");
            var1.append(",");
            var4.series.setDataComputer(null);
            var4.series.setShift(0);
         }
      }
   }

   private String findUniqueColumnNames(String var1, ArrayList<String> var2) {
      if (!this.columnNamesContain(var1, var2)) {
         return var1;
      }

      int var4 = 1;

      while (true) {
         StringBuilder var5 = new StringBuilder(var1);
         var5.append("{");
         var5.append(var4);
         var5.append("}");
         String var3 = var5.toString();
         if (!this.columnNamesContain(var3, var2)) {
            return var3;
         }

         var4++;
      }
   }

   private boolean columnNamesContain(String var1, ArrayList<String> var2) {
      for (String var4 : var2) {
         if (var4.equals(var1)) {
            return true;
         }
      }

      return false;
   }

   public void setDataShift(int var1, int var2) {
      IndicatorBase.DataSeriesSet var3 = this.outputDataSeries[var1];
      var3.series.setShift(var2);
   }

   public void setName(String var1) {
      this.indicatorName = var1;
   }

   public String getName() {
      if (this.indicatorName == null) {
         this.indicatorName = this.getClass().getSimpleName();
      }

      return this.indicatorName;
   }

   public String getPseudoName() {
      StringBuilder var1 = new StringBuilder(this.getClass().getSimpleName());
      var1.append("(");
      this.addParameters(var1);
      var1.append(")");
      return var1.toString();
   }

   private void addParameters(StringBuilder var1) {
      Class var2 = this.getClass();
      int var3 = 0;

      for (Field var7 : var2.getFields()) {
         if (!var7.getName().equals("Shift")) {
            for (Annotation var11 : var7.getAnnotations()) {
               if (var11 instanceof Parameter) {
                  String var12 = var7.getType().getName();
                  if (!var12.equals("int")
                     && !var12.equals("long")
                     && !var12.equals("double")
                     && !var12.equals("boolean")
                     && !var12.equals("java.lang.String")
                     && !var12.equals("java.lang.Object")) {
                     if (var12.equals("com.strategyquant.tradinglib.IBlock")) {
                     }
                  } else {
                     if (var3 > 0) {
                        var1.append(", ");
                     }

                     var1.append(this.getParamPrimitiveValue(var12, var7));
                     var3++;
                  }
               }
            }
         }
      }
   }

   private String getParamPrimitiveValue(String var1, Field var2) {
      String var3 = "UNKWN";

      try {
         switch (var1) {
            case "int":
               var3 = Integer.toString(var2.getInt(this));
               break;
            case "long":
               var3 = Long.toString(var2.getLong(this));
               break;
            case "double":
               var3 = SQUtils.doubleToString(var2.getDouble(this));
               break;
            case "boolean":
               var3 = Boolean.toString(var2.getBoolean(this));
               break;
            case "java.lang.String":
               var3 = (String)var2.get(this);
               break;
            case "java.lang.Object":
               var3 = "Obj";
         }
      } catch (IllegalArgumentException | IllegalAccessException var6) {
      }

      return var3;
   }

   private void precomputeData() throws TradingException {
      long var1 = System.currentTimeMillis();
      long var3 = System.currentTimeMillis();
   }

   public void debug(String var1, String var2) {
      DebugConsole.log(var1, var2);
   }

   public void fdebug(String var1, String var2) {
      DebugLog.info(var1 + " - " + var2);
   }

   class DataSeriesSet {
      public static final int DATASERIES = 1;
      public static final int TIMEDATASERIES = 2;
      public int type;
      DataSeriesBase series;
      int shift = 0;
      String name;

      public void destroy() {
         this.series.destroy();
      }
   }
}
