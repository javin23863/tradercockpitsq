package SQ.Internal;

import com.strategyquant.datalib.TradingException;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.tradinglib.BlockDefinitionException;
import com.strategyquant.tradinglib.Formula;
import com.strategyquant.tradinglib.IBlock;
import com.strategyquant.tradinglib.IFormula;
import com.strategyquant.tradinglib.IParametersHelperModifier;
import com.strategyquant.tradinglib.ParametersHelper;
import com.strategyquant.tradinglib.StrategyBase;
import com.strategyquant.tradinglib.blocks.AnnotationProcessor;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import org.jdom2.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class FormulaBlock extends StandardBlock implements IFormula {
   public static final Logger Log = LoggerFactory.getLogger("FormulaBlock");

   public double evaluateBlock() throws TradingException {
      throw new TradingException("This shouldn't be called for FormulaBlock!");
   }

   public double evaluateBlock(int var1) throws TradingException {
      throw new TradingException("This shouldn't be called for FormulaBlock!");
   }

   public IFormula newFormulaInstance(final StrategyBase var1, Element var2) throws BlockDefinitionException {
      try {
         Constructor var3 = null;
         var3 = this.getClass().getConstructor();
         FormulaBlock var4 = (FormulaBlock)SQUtils.invokeUnchecked(var3, new Object[0]);
         ParametersHelper.copyParametersFromBlockToBlock(this, var4, new IParametersHelperModifier() {
            public Object modifyParameterValue(String var1x, Object var2x) throws BlockDefinitionException {
               return var2x instanceof IBlock ? ((IBlock)var2x).clone(true, var1) : var2x;
            }
         });
         var4.initialize(var1, var2);
         return var4;
      } catch (NoSuchMethodException | SecurityException | BlockDefinitionException var5) {
         throw new BlockDefinitionException("Exception cloning block! " + var5.getMessage(), var5);
      }
   }

   @Override
   protected void initialize(StrategyBase var1, Element var2) throws BlockDefinitionException {
      this.initializeStrategy(var1);
      Class var3 = this.getClass();

      for (Field var7 : var3.getFields()) {
         AnnotationProcessor.wizardParseXml(this, var7, var2);
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

   public boolean isNoneValue() {
      Class var1 = this.getClass();
      Formula var2 = var1.getAnnotation(Formula.class);
      return var2 != null ? var2.noneValue() : false;
   }

   public boolean isBooleanValue() {
      return false;
   }
}
