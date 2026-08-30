package SQ.Internal;

import com.strategyquant.datalib.DataSeries;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.XMLUtil;
import com.strategyquant.tradinglib.BlockDefinitionException;
import com.strategyquant.tradinglib.Blocks;
import com.strategyquant.tradinglib.ExitMethod;
import com.strategyquant.tradinglib.IBlock;
import com.strategyquant.tradinglib.IFormula;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.StrategyBase;
import com.strategyquant.tradinglib.Variable;
import com.strategyquant.tradinglib.blocks.AnnotationProcessor;
import com.strategyquant.tradinglib.debug.Debugger;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import org.jdom2.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract class StandardBlock extends Debugger implements IBlock {
   public static final Logger Log = LoggerFactory.getLogger("StandardBlock");
   public XmlStrategy Strategy;
   protected StrategyBase nonXmlStrategy = null;
   private Int2IntOpenHashMap customData = null;
   private int returnType = 0;
   private final Class<?> exitMethodsClass = ExitMethod[].class;
   public int chartIndex;
   protected DataSeries barsShiftedSeries;
   protected int reservedBars = 0;

   public IBlock newInstance(StrategyBase var1, Element var2) throws BlockDefinitionException {
      StandardBlock var3 = (StandardBlock)this.clone(true, var1);
      var3.initialize(var1, var2);
      return var3;
   }

   public IBlock clone(boolean var1, StrategyBase var2) throws BlockDefinitionException {
      try {
         Constructor var3 = null;
         var3 = this.getClass().getConstructor();
         StandardBlock var4 = (StandardBlock)SQUtils.invokeUnchecked(var3, new Object[0]);
         var4.initializeStrategy(var2);
         if (var4.nonXmlStrategy == null && this.nonXmlStrategy != null) {
            var4.nonXmlStrategy = this.nonXmlStrategy;
         }

         StrategyBase var5 = this.getStrategy();
         if (var1) {
            Class var6 = this.getClass();

            for (Field var10 : var6.getFields()) {
               for (Annotation var14 : var10.getAnnotations()) {
                  if (var14 instanceof Parameter) {
                     Object var15 = var10.get(this);
                     if (var15 instanceof IBlock) {
                        var15 = ((IBlock)var15).clone(true, var2);
                     } else if (var15 instanceof IFormula) {
                        var15 = ((IBlock)var15).clone(true, var2);
                     } else if (var5 != null) {
                        Variable var16 = var5.variables().getByField(this, var10.getName());
                        if (var16 != null) {
                           var16.registerAttachedField(var4, var10);
                           var5.variables().getByField(var4, var10.getName());
                        }
                     }

                     var10.set(var4, var15);
                  }
               }

               if (var10.getName().equals("ExitMethods")) {
                  ExitMethod[] var19 = (ExitMethod[])var10.get(this);
                  if (var19 != null) {
                     ExitMethod[] var20 = new ExitMethod[var19.length];

                     for (int var21 = 0; var21 < var19.length; var21++) {
                        ExitMethod var22 = var19[var21];
                        if (var22 instanceof IFormula) {
                           IBlock var23 = ((IBlock)var22).clone(true, var2);
                        } else if (!(var22 instanceof ExitMethod)) {
                           if (var22 instanceof IBlock) {
                              IBlock var24 = ((IBlock)var22).clone(true, var2);
                           } else if (var5 != null) {
                              Variable var25 = var5.variables().getByField(this, var10.getName());
                              if (var25 != null) {
                                 var25.registerAttachedField(var4, var10);
                                 var5.variables().getByField(var4, var10.getName());
                              }
                           }
                        }
                     }

                     var10.set(var4, var20);
                  }
               }
            }

            if (this.customData != null) {
               var4.customData = new Int2IntOpenHashMap();
               var4.customData.putAll(this.customData);
            }
         }

         return var4;
      } catch (NoSuchMethodException | SecurityException | BlockDefinitionException | IllegalArgumentException | IllegalAccessException var17) {
         throw new BlockDefinitionException("Exception cloning block! " + var17.getMessage(), var17);
      }
   }

   private void initializeStrategy(StrategyBase var1) {
      if (this.Strategy == null && this.nonXmlStrategy == null && var1 != null) {
         if (var1 instanceof XmlStrategy) {
            this.Strategy = (XmlStrategy)var1;
         } else {
            this.nonXmlStrategy = var1;
         }
      }
   }

   public boolean isTradestationEngine() {
      if (this.nonXmlStrategy != null) {
         return this.nonXmlStrategy.isTradestationEngine();
      } else {
         return this.Strategy != null ? this.Strategy.isTradestationEngine() : false;
      }
   }

   protected void initialize(StrategyBase var1, Element var2) throws BlockDefinitionException {
      this.initializeStrategy(var1);
      if (var2 != null) {
         try {
            this.parseXml(var2);
         } catch (NumberFormatException var4) {
            Log.error("NumberFormatException, XML: {}", XMLUtil.elementToString(var2));
            throw var4;
         }
      }

      this.OnInit();
   }

   public void deinitialize() {
      this.OnDeinit();
   }

   protected void OnInit() throws BlockDefinitionException {
   }

   protected void OnDeinit() {
   }

   public StrategyBase getStrategy() {
      return this.Strategy != null ? this.Strategy : this.nonXmlStrategy;
   }

   protected void parseXml(Element var1) throws BlockDefinitionException {
      Class var2 = this.getClass();

      for (Field var6 : var2.getFields()) {
         if (var6.getType() == this.exitMethodsClass) {
            AnnotationProcessor.wizardParseExitTypesXml(this, var6, var1);
            this.sortExitmethodsInArray(var6);
         } else {
            AnnotationProcessor.wizardParseXml(this, var6, var1);
         }
      }
   }

   private void sortExitmethodsInArray(Field var1) {
      Object var2 = null;

      try {
         var2 = var1.get(this);
      } catch (IllegalArgumentException var4) {
         Log.info("Cannot sort exit methods (1)", var4);
      } catch (IllegalAccessException var5) {
         Log.info("Cannot sort exit methods (2)", var5);
      }

      if (var2 != null) {
         ExitMethod[] var3 = (ExitMethod[])var2;
         Blocks.sortByOrder(var3);
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
      if (var1.equals("StockpickerChartIndex")) {
         this.chartIndex = var2;
      }
   }

   public int getCustomData(String var1) {
      return this.customData != null && this.customData.containsKey(var1.hashCode()) ? this.customData.get(var1.hashCode()) : Integer.MIN_VALUE;
   }

   public void copyCustomData(IBlock var1) {
      if (var1 instanceof StandardBlock var2 && var2.customData != null) {
         if (this.customData == null) {
            this.customData = new Int2IntOpenHashMap();
         }

         this.customData.putAll(var2.customData);
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
}
