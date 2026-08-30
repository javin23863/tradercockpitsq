package SQ.Internal.RulesImpl;

import SQ.Blocks.Other.Boolean;
import SQ.Internal.ITradingOptionsEvaluator;
import SQ.Internal.Rule;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.tradinglib.BlockDefinitionException;
import com.strategyquant.tradinglib.Blocks;
import com.strategyquant.tradinglib.IBlock;
import com.strategyquant.tradinglib.Variable;
import java.util.ArrayList;
import java.util.List;
import org.jdom2.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Signal extends Rule {
   public static final Logger Log = LoggerFactory.getLogger("Signal");
   private IBlock[] signals = null;
   private Variable[] signalVariables = null;
   boolean everyTick = true;
   private static Boolean falseBool = new Boolean(false);

   @Override
   public void evaluateRule(int var1, ITradingOptionsEvaluator var2, String var3) throws TradingException {
      if (this.signals != null) {
         if (!this.everyTick && var1 != this.barEventType) {
            for (int var8 = 0; var8 < this.signals.length; var8++) {
               this.signalVariables[var8].setValue(false);
            }
         } else {
            for (int var4 = 0; var4 < this.signals.length; var4++) {
               Variable var5 = this.signalVariables[var4];
               IBlock var6 = this.signals[var4];
               if (var5 != null && var6 != null) {
                  boolean var7 = var6.evaluateBlock() > 0.0;
                  var5.setValue(var7);
               }
            }
         }
      }
   }

   @Override
   protected void parseXml(Element var1) throws BlockDefinitionException {
      ArrayList var2 = new ArrayList();
      ArrayList var3 = new ArrayList();
      List var4 = var1.getChildren();

      for (int var5 = 0; var5 < var4.size(); var5++) {
         Element var6 = (Element)var4.get(var5);
         if (!var6.getName().contains("Description") && var6.getName().contains("signals")) {
            for (Element var8 : var6.getChildren()) {
               this.parseSignal(var8, var2, var3);
            }
         }
      }

      String var9 = var1.getAttributeValue("everyTick");
      if (var9 != null && var9.equals("false")) {
         this.everyTick = false;
      } else {
         this.everyTick = true;
      }

      if (var2.size() > 0) {
         this.signals = new IBlock[var2.size()];
         this.signalVariables = new Variable[var2.size()];

         for (int var10 = 0; var10 < var2.size(); var10++) {
            this.signals[var10] = (IBlock)var2.get(var10);
            this.signalVariables[var10] = (Variable)var3.get(var10);
         }
      }

      var2.clear();
      var3.clear();
   }

   private void parseSignal(Element var1, ArrayList<IBlock> var2, ArrayList<Variable> var3) throws BlockDefinitionException {
      String var4 = var1.getAttributeValue("variable");
      Object var5 = null;
      falseBool.Value = false;
      if (this.Strategy == null) {
         throw new BlockDefinitionException("Signal rule cannot get strategy!");
      }

      var5 = this.Strategy.variables().getById(var4);
      if (var5 == null) {
         throw new BlockDefinitionException("Signal variable is not set!");
      }

      List var6 = var1.getChildren();
      if (var6.size() > 1) {
         throw new BlockDefinitionException("Signal cannot have more than one child block!");
      }

      if (var6.size() == 0) {
         var3.add(var5);
         var2.add(falseBool);
      } else {
         Element var7 = (Element)var6.get(0);
         if (!var7.getName().equals("Item")) {
            throw new BlockDefinitionException("Block has an unallowed name '" + var7.getName() + "'");
         }

         IBlock var8 = Blocks.getBlockObject(var7.getAttributeValue("key"), this.Strategy, var7);
         var3.add(var5);
         var2.add(var8);
      }
   }
}
