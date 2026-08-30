package SQ.Internal;

import com.strategyquant.datalib.DataSeries;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.tradinglib.BlockDefinitionException;
import com.strategyquant.tradinglib.ChartData;
import com.strategyquant.tradinglib.IBlock;
import com.strategyquant.tradinglib.IParametersHelperModifier;
import com.strategyquant.tradinglib.NoShift;
import com.strategyquant.tradinglib.Output;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.ParametersHelper;
import com.strategyquant.tradinglib.StrategyBase;
import com.strategyquant.tradinglib.blocks.AnnotationProcessor;
import com.strategyquant.tradinglib.indicator.IndicatorBase;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import org.jdom2.Element;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

public abstract class IndicatorBlock extends Indicator implements IBlock {
   public XmlStrategy Strategy = null;
   private StrategyBase nonXmlStrategy = null;
   public int OutputIndex = 0;
   private Int2IntOpenHashMap customData = null;
   private PrintWriter writer;
   private DateTimeFormatter timeFormatter;
   private int returnType = 0;
   IndicatorBase indicatorObj = null;
   private DataSeries[] outputs;
   private int noShiftRecognized = -1;

   public double OnBlockEvaluate(int var1) throws TradingException {
      try {
         if (this.indicatorObj == null) {
            this.initIndicatorObject(this.Strategy);
         } else {
            this.indicatorObj.refreshShift();
         }

         if (this.OutputIndex >= this.outputs.length) {
            throw new Exception("Invalid output index: " + this.OutputIndex + " in block " + this.getName());
         }

         int var2 = var1 + this.Shift;
         if (this.noShiftRecognized == -1) {
            if (this.indicatorObj.getClass().isAnnotationPresent(NoShift.class)) {
               this.noShiftRecognized = 1;
            } else {
               this.noShiftRecognized = 0;
            }
         }

         if (this.noShiftRecognized == 1) {
            var2 = 0;
         }

         return this.outputs[this.OutputIndex].get(var2);
      } catch (Exception var3) {
         if (var3.getMessage() != null && !var3.getMessage().contains("Invalid output index:")) {
            Log.error("Indicator[" + this.getClass().getSimpleName() + "] - Indicator.OnBlockEvaluate() failed. Exc.", var3);
         }

         throw new TradingException("Indicator[" + this.getClass().getSimpleName() + "] - " + var3.getMessage());
      }
   }

   private void initIndicatorObject(StrategyBase var1) throws Exception {
      ObjectArrayList var2 = new ObjectArrayList();
      ObjectArrayList var3 = new ObjectArrayList();

      for (Field var7 : this.getClass().getFields()) {
         for (Annotation var11 : var7.getAnnotations()) {
            if (var11 instanceof Parameter && !var7.getName().equals("Shift")) {
               var2.add(var7);
            } else if (var11 instanceof Output) {
               var3.add(var7.getName());
            }
         }
      }

      Class[] var13 = new Class[var2.size()];

      for (int var14 = 0; var14 < var2.size(); var14++) {
         var13[var14] = ((Field)var2.get(var14)).getType();
      }

      Method var15 = this.Indicators.getClass().getMethod(this.getClass().getSimpleName(), var13);
      Object[] var16 = new Object[var2.size()];

      for (int var17 = 0; var17 < var2.size(); var17++) {
         var16[var17] = ((Field)var2.get(var17)).get(this);
      }

      Object var18 = null;

      try {
         var18 = var15.invoke(this.Indicators, var16);
         this.indicatorObj = (IndicatorBase)var18;
      } catch (InvocationTargetException var12) {
         if (var12.getTargetException() != null) {
            String var21 = this.createIndicatorDescription(var16);
            throw new Exception("Cannot create indicator " + var21, var12.getTargetException());
         }

         throw var12;
      }

      this.outputs = new DataSeries[var3.size()];

      for (int var20 = 0; var20 < var3.size(); var20++) {
         this.outputs[var20] = (DataSeries)var18.getClass().getField((String)var3.get(var20)).get(var18);
      }

      this.indicatorObj.initializeStrategy(var1);
   }

   private String createIndicatorDescription(Object[] var1) {
      StringBuffer var2 = new StringBuffer(this.getClass().getSimpleName());
      var2.append("(");

      for (int var3 = 0; var3 < var1.length; var3++) {
         Object var4 = var1[var3];
         if (var3 > 0) {
            var2.append(",");
         }

         if (var4 instanceof DataSeries) {
            var2.append("[DataSeries]");
         } else if (var4 instanceof ChartData) {
            var2.append("[ChartData]");
         } else if (var4 instanceof Integer) {
            var2.append(((Integer)var4).toString());
         } else if (var4 instanceof Double) {
            var2.append(((Double)var4).toString());
         } else {
            var2.append("[");
            var2.append(var4.getClass().getSimpleName());
            var2.append("]");
         }
      }

      var2.append(")");
      return var2.toString();
   }

   public int getSuperType() {
      return 4;
   }

   public double evaluateBlock() throws TradingException {
      double var1 = this.OnBlockEvaluate(0);
      return SQUtils.round6(var1);
   }

   public double evaluateBlock(int var1) throws TradingException {
      return SQUtils.round6(this.OnBlockEvaluate(var1));
   }

   public IBlock newInstance(StrategyBase var1, Element var2) throws BlockDefinitionException {
      IndicatorBlock var3 = (IndicatorBlock)this.clone(true, var1);
      var3.initialize(var1, var2);
      return var3;
   }

   public IBlock clone(boolean var1, final StrategyBase var2) throws BlockDefinitionException {
      try {
         Constructor var3 = null;
         var3 = this.getClass().getConstructor();
         IndicatorBlock var4 = (IndicatorBlock)SQUtils.invokeUnchecked(var3, new Object[0]);
         var4.initializeStrategy(var2);
         if (var1) {
            ParametersHelper.copyParametersFromBlockToBlock(this, var4, new IParametersHelperModifier() {
               public Object modifyParameterValue(String var1, Object var2x) throws BlockDefinitionException {
                  return var2x instanceof IBlock ? ((IBlock)var2x).clone(true, var2) : var2x;
               }
            });
            if (this.customData != null) {
               var4.customData = new Int2IntOpenHashMap();
               var4.customData.putAll(this.customData);
               var4.OutputIndex = this.OutputIndex;
            }
         }

         return var4;
      } catch (NoSuchMethodException | SecurityException | BlockDefinitionException var5) {
         throw new BlockDefinitionException("Exception cloning block! " + var5.getMessage(), var5);
      }
   }

   private void initialize(StrategyBase var1, Element var2) throws BlockDefinitionException {
      this.initializeStrategy(var1);
      if (this.Strategy != null) {
         this.Indicators = this.Strategy.Indicators;
      }

      if (var2 != null) {
         this.parseXml(var2);
      }
   }

   public void initializeStrategy(StrategyBase var1) {
      if (var1 != null) {
         if (this.Strategy == null && this.nonXmlStrategy == null && var1 != null) {
            if (var1 instanceof XmlStrategy) {
               this.Strategy = (XmlStrategy)var1;
            } else {
               this.nonXmlStrategy = var1;
            }
         }

         super.initializeStrategy(var1);
      }
   }

   public StrategyBase getStrategy() {
      return this.Strategy != null ? this.Strategy : this.nonXmlStrategy;
   }

   protected void parseXml(Element var1) throws BlockDefinitionException {
      Class var2 = this.getClass();
      Annotation var3 = var2.getAnnotation(NoShift.class);

      for (Field var7 : var2.getFields()) {
         if (!var7.getName().equals("Shift") || var3 == null) {
            AnnotationProcessor.wizardParseXml(this, var7, var1);
         }
      }
   }

   public IBlock negate() {
      return this;
   }

   public void setCustomData(String var1, int var2) {
      if (this.customData == null) {
         this.customData = new Int2IntOpenHashMap();
      }

      this.customData.put(var1.hashCode(), var2);
   }

   public int getCustomData(String var1) {
      return this.customData != null && this.customData.containsKey(var1.hashCode()) ? this.customData.get(var1.hashCode()) : Integer.MIN_VALUE;
   }

   public void copyCustomData(IBlock var1) {
      if (var1 instanceof IndicatorBlock var2) {
         if (var2.customData != null) {
            if (this.customData == null) {
               this.customData = new Int2IntOpenHashMap();
            }

            this.customData.putAll(var2.customData);
         }

         this.OutputIndex = var2.OutputIndex;
      }
   }

   protected void initLogging(String var1) throws TradingException {
      this.timeFormatter = DateTimeFormat.forPattern("yyyy.MM.dd,HH:mm");

      try {
         this.writer = new PrintWriter(new BufferedWriter(new FileWriter(new File(var1), StandardCharsets.UTF_8)));
      } catch (IOException var3) {
         throw new TradingException(var3.getMessage());
      }
   }

   protected void logToFile(String var1) {
      if (this.writer != null) {
         this.writer.write(var1);
      }
   }

   protected void finishLogging() {
      if (this.writer != null) {
         this.writer.close();
      }
   }

   public int getReturnType() {
      return this.returnType;
   }

   public void setReturnType(int var1) {
      this.returnType = var1;
   }

   public Element getCustomBlockXml(int var1) throws BlockDefinitionException {
      throw new BlockDefinitionException("Not implemented for this!");
   }

   public int getCurrentBar() {
      return this.Strategy.isTradestationEngine() ? this.CurrentBar - this.getIndyStartingBar() : this.CurrentBar;
   }
}
