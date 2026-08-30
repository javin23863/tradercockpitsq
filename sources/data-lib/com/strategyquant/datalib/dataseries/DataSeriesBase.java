package com.strategyquant.datalib.dataseries;

import com.strategyquant.datalib.ChartDef;
import com.strategyquant.datalib.InstrumentInfo;
import com.strategyquant.datalib.TradingException;
import java.security.InvalidParameterException;
import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class DataSeriesBase {
   public static final Logger Log = LoggerFactory.getLogger("DataSeriesBase");
   protected static final int DEFAULT_ALLOCATION_COUNT = 10000;
   protected static final int DEFAULT_GROW = 10000;
   protected IDoubleValuesList doubleValues = null;
   protected IDataSeriesChangeListener[] onTickChangeListeners = null;
   protected IDataSeriesChangeListener[] onBarChangeListeners = null;
   protected String dataSeriesName = null;
   protected int shift = 0;
   protected IDataSeriesComputer linkedDataComputer = null;
   protected int computedUntil = -1;
   private String lineName;
   protected int allocationSize;
   protected int growStep;
   protected String lineColor;
   private ChartDef chartDef;
   private int indyStartingBar = 0;
   private boolean showInChart = true;
   private boolean showZeroValues = true;
   private String chartType;

   public DataSeriesBase() {
      this(10000, 10000, 0);
   }

   public DataSeriesBase(int var1) {
      this(var1, 10000, 0);
   }

   public DataSeriesBase(int var1, String var2) {
      this(var1, 10000, 0);
      this.dataSeriesName = var2;
   }

   public DataSeriesBase(int var1, String var2, ChartDef var3) {
      this(var1, 10000, 0);
      this.dataSeriesName = var2;
      this.chartDef = var3;
   }

   public DataSeriesBase(int var1, int var2) {
      this(var1, 10000, 0);
   }

   private DataSeriesBase(int var1, int var2, int var3) {
      this.allocationSize = var1;
      this.growStep = var2;
      if (var1 != 0) {
         this.doubleValues = DoubleListCache.get(var1);
      }
   }

   public int size() {
      return this.doubleValues.size() - this.shift;
   }

   public int specialSize() {
      return this.doubleValues.size();
   }

   protected int getRealIndex(int var1) throws TradingException {
      return this.size() - var1 - 1;
   }

   public double _getDouble(int var1) throws TradingException {
      if (var1 >= this.size()) {
         return 0.0;
      }

      int var2 = this.getRealIndex(var1);
      this.computePotentialMissingValues(var1);
      return this.doubleValues.getDouble(var2);
   }

   protected void computePotentialMissingValues(int var1) throws TradingException {
      int var2 = var1 > 0 ? var1 - 1 : var1;
      if (this.hasLinkedDataComputer() && (this.computedUntil == -1 || var2 + this.shift < this.computedUntil || var2 + this.shift == 0)) {
         this.linkedDataComputer.compute(this.computedUntil, var2 + this.shift);
      }
   }

   protected void computePotentialMissingValuesOldVersion(int var1) throws TradingException {
      if (this.hasLinkedDataComputer() && (this.computedUntil == -1 || var1 + this.shift < this.computedUntil || var1 + this.shift == 0)) {
         this.linkedDataComputer.compute(this.computedUntil, var1 + this.shift);
      }
   }

   protected boolean hasLinkedDataComputer() {
      return this.linkedDataComputer != null;
   }

   protected void _setDouble(int var1, double var2, boolean var4) throws TradingException {
      this._writeDoubleToOffheap(var1, var2);
      if (this.hasLinkedDataComputer() && (this.computedUntil == var1 + this.shift + 1 || this.computedUntil == -1 && var1 == this.size() - 1)) {
         this.computedUntil = var1;
      }

      if (this.onBarChangeListeners != null && !var4) {
         this.callOnBarChangeListeners(var1 + this.shift, this.size() + this.shift);
      }

      if (this.onTickChangeListeners != null) {
         this.callOnTickChangeListeners(var1 + this.shift, this.size() + this.shift);
      }
   }

   protected void _writeDoubleToOffheap(int var1, double var2) {
      if (var2 > -1.0E-17 && var2 < 1.0E-17) {
         var2 = 0.0;
      }

      int var4 = this.doubleValues.size() - this.shift;
      if (var1 < var4) {
         if (var1 < 0) {
            throw new InvalidParameterException("Index cannot be smaller than zero!");
         }

         int var5 = var4 - var1 - 1;
         this.doubleValues.setDouble(var5, var2);
      }
   }

   public void _writeDoubleToOffheapRealIndex(int var1, double var2) {
      this.doubleValues.setDouble(var1, var2);
   }

   public void addDoubleValues(int var1, double var2) throws TradingException {
      if (this.shift != 0) {
      }

      this.doubleValues.addDoubleValues(var1, var2);
      if (this.hasLinkedDataComputer() && this.computedUntil > -1) {
         this.computedUntil += var1;
      }

      if (this.onBarChangeListeners != null) {
         this.callOnBarChangeListeners(var1 - 1, this.size());
      }

      if (this.onTickChangeListeners != null) {
         this.callOnTickChangeListeners(var1 - 1, this.size());
      }
   }

   public void addLongValues(int var1, long var2) throws TradingException {
      if (this.shift != 0) {
      }

      this.doubleValues.addDoubleValues(var1, var2);
      if (this.hasLinkedDataComputer() && this.computedUntil > -1) {
         this.computedUntil += var1;
      }

      if (this.onBarChangeListeners != null) {
         this.callOnBarChangeListeners(var1 - 1, this.size());
      }

      if (this.onTickChangeListeners != null) {
         this.callOnTickChangeListeners(var1 - 1, this.size());
      }
   }

   protected void _addDouble(double var1) throws TradingException {
      if (this.shift != 0) {
         throw new TradingException("Shift must be 0 when calling addValues() !");
      }

      this.doubleValues.addDouble(var1);
      if (this.onBarChangeListeners != null) {
         this.callOnBarChangeListeners(1, this.size());
      }

      if (this.onTickChangeListeners != null) {
         this.callOnTickChangeListeners(1, this.size());
      }
   }

   protected void _addLong(long var1) throws TradingException {
      if (this.shift != 0) {
         throw new TradingException("Shift must be 0 when calling addValues() !");
      }

      this.doubleValues.addDouble(var1);
      if (this.onBarChangeListeners != null) {
         this.callOnBarChangeListeners(1, this.size());
      }

      if (this.onTickChangeListeners != null) {
         this.callOnTickChangeListeners(1, this.size());
      }
   }

   public void addChangeListener(int var1, IDataSeriesChangeListener var2) {
      if (var1 == 4) {
         if (this.onTickChangeListeners == null) {
            this.onTickChangeListeners = new IDataSeriesChangeListener[1];
         } else {
            IDataSeriesChangeListener[] var3 = Arrays.copyOf(this.onTickChangeListeners, this.onTickChangeListeners.length + 1);
            this.onTickChangeListeners = var3;
         }

         this.onTickChangeListeners[this.onTickChangeListeners.length - 1] = var2;
      } else {
         if (this.onBarChangeListeners == null) {
            this.onBarChangeListeners = new IDataSeriesChangeListener[1];
         } else {
            IDataSeriesChangeListener[] var4 = Arrays.copyOf(this.onBarChangeListeners, this.onBarChangeListeners.length + 1);
            this.onBarChangeListeners = var4;
         }

         this.onBarChangeListeners[this.onBarChangeListeners.length - 1] = var2;
      }
   }

   protected void callOnTickChangeListeners(int var1, int var2) throws TradingException {
      if (this.onTickChangeListeners != null) {
         for (int var3 = 0; var3 < this.onTickChangeListeners.length; var3++) {
            this.onTickChangeListeners[var3].changed(var1, var2);
         }
      }
   }

   protected void callOnBarChangeListeners(int var1, int var2) throws TradingException {
      if (this.onBarChangeListeners != null) {
         for (int var3 = 0; var3 < this.onBarChangeListeners.length; var3++) {
            this.onBarChangeListeners[var3].changed(var1, var2);
         }
      }
   }

   public void setShift(int var1) {
      this.shift = var1;
   }

   public int getShift() {
      return this.shift;
   }

   public void setDataComputer(IDataSeriesComputer var1) {
      this.linkedDataComputer = var1;
   }

   public void setComputedUntil(int var1) {
      this.computedUntil = var1;
   }

   public void updateComputedUntil(int var1) {
      if (this.computedUntil != -1 && this.computedUntil < var1) {
         this.computedUntil = var1;
      }
   }

   public int getComputedUntilIndex() {
      return this.computedUntil;
   }

   public void setName(String var1) {
      this.dataSeriesName = var1;
   }

   public void setName(String var1, ChartDef var2) {
      this.dataSeriesName = var1;
      if (this.chartDef == null && var2 != null) {
         this.chartDef = var2;
      }
   }

   public String getName() {
      return this.dataSeriesName;
   }

   public void callDataChangeListeners() throws TradingException {
   }

   public void destroy() {
      if (this.doubleValues != null) {
         DoubleListCache.clear(this.doubleValues);
         this.doubleValues = null;
      }
   }

   public void setLineName(String var1) {
      this.lineName = var1;
   }

   public String getLineName() {
      return this.lineName;
   }

   public void setColor(String var1) {
      this.lineColor = var1;
   }

   public String getColor() {
      return this.lineColor;
   }

   public InstrumentInfo getInstrumentInfo() {
      return this.chartDef != null ? this.chartDef.getInstrumentInfo() : null;
   }

   public String Symbol() {
      return this.chartDef != null ? this.chartDef.getSymbol() : null;
   }

   public int getIndyStartingBar() {
      return this.indyStartingBar;
   }

   public void setIndyStartingBar(int var1) {
      this.indyStartingBar = var1;
   }

   public boolean isShowInChart() {
      return this.showInChart;
   }

   public void setShowInChart(boolean var1) {
      this.showInChart = var1;
   }

   public boolean isShowZeroValues() {
      return this.showZeroValues;
   }

   public void setShowZeroValues(boolean var1) {
      this.showZeroValues = var1;
   }

   public String getChartType() {
      return this.chartType;
   }

   public void setChartType(String var1) {
      this.chartType = var1;
   }
}
