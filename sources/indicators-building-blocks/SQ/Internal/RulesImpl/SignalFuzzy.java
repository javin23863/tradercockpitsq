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
import java.util.Arrays;
import java.util.List;
import org.jdom2.Element;

public class SignalFuzzy extends Rule {
   private IBlock[][] signals = null;
   private Variable[] signalVariables = null;
   private int[] minTrue = null;
   boolean everyTick = true;
   private static ArrayList<IBlock> falseBoolArr = new ArrayList<>(Arrays.asList(new Boolean(false)));

   @Override
   public void evaluateRule(int var1, ITradingOptionsEvaluator var2, String var3) throws TradingException {
      if (this.signals != null) {
         if (!this.everyTick && var1 != this.barEventType && !var3.equals("OnInit") && !var3.equals("OnDeinit")) {
            for (int var14 = 0; var14 < this.signals.length; var14++) {
               this.signalVariables[var14].setValue(false);
            }
         } else {
            for (int var4 = 0; var4 < this.signals.length; var4++) {
               Variable var5 = this.signalVariables[var4];
               IBlock[] var6 = this.signals[var4];
               if (var6 != null) {
                  int var7 = this.minTrue[var4];
                  if (var7 <= 0) {
                     var7 = 1;
                  }

                  if (var5 != null && var6 != null) {
                     boolean var8 = false;
                     int var9 = 0;

                     for (int var10 = 0; var10 < var6.length; var10++) {
                        IBlock var11 = var6[var10];
                        boolean var12 = var11.evaluateBlock() > 0.0;
                        if (var12) {
                           var9++;
                        }

                        if (var9 >= var7) {
                           var8 = true;
                           break;
                        }
                     }

                     var5.setValue(var8);
                  }
               }
            }

            boolean var13 = true;
         }
      }
   }

   @Override
   protected void parseXml(Element var1) throws BlockDefinitionException {
      ArrayList var2 = new ArrayList();
      ArrayList var3 = new ArrayList();
      ArrayList var4 = new ArrayList();
      List var5 = var1.getChildren();

      for (int var6 = 0; var6 < var5.size(); var6++) {
         Element var7 = (Element)var5.get(var6);
         if (!var7.getName().contains("Description") && var7.getName().contains("signals")) {
            for (Element var9 : var7.getChildren()) {
               this.parseSignal(var9, var2, var3, var4);
            }
         }
      }

      String var10 = var1.getAttributeValue("everyTick");
      if (var10 != null && var10.equals("false")) {
         this.everyTick = false;
      } else {
         this.everyTick = true;
      }

      if (var2.size() > 0) {
         this.signals = new IBlock[var2.size()][];
         this.signalVariables = new Variable[var2.size()];
         this.minTrue = new int[var2.size()];

         for (int var11 = 0; var11 < var2.size(); var11++) {
            ArrayList var12 = (ArrayList)var2.get(var11);
            if (var12 != null && var12.size() > 0) {
               this.signals[var11] = new IBlock[var12.size()];

               for (int var13 = 0; var13 < var12.size(); var13++) {
                  this.signals[var11][var13] = (IBlock)var12.get(var13);
               }
            }

            this.signalVariables[var11] = (Variable)var3.get(var11);
            this.minTrue[var11] = (Integer)var4.get(var11);
         }
      }

      var2.clear();
      var3.clear();
      var4.clear();
   }

   private void parseSignal(Element var1, ArrayList<ArrayList<IBlock>> var2, ArrayList<Variable> var3, ArrayList<Integer> var4) throws BlockDefinitionException {
      String var5 = var1.getAttributeValue("variable");
      Object var6 = null;
      if (this.Strategy == null) {
         throw new BlockDefinitionException("Signal rule cannot get strategy!");
      }

      var6 = this.Strategy.variables().getById(var5);
      if (var6 == null) {
         throw new BlockDefinitionException("Signal variable is not set!");
      }

      List var7 = var1.getChildren();
      var3.add(var6);
      if (var7.size() == 0) {
         var2.add(falseBoolArr);
         var4.add(1);
      } else {
         ArrayList var8 = new ArrayList();

         for (int var9 = 0; var9 < var7.size(); var9++) {
            Element var10 = (Element)var7.get(var9);
            if (!var10.getName().equals("Item")) {
               throw new BlockDefinitionException("Block has an unallowed name '" + var10.getName() + "'");
            }

            IBlock var11 = Blocks.getBlockObject(var10.getAttributeValue("key"), this.Strategy, var10);
            var8.add(var11);
         }

         int var16 = Integer.parseInt(var1.getAttributeValue("minTrue"));
         double var17 = 0.01 * var16;
         double var12 = var7.size() * var17;
         int var14 = (int)Math.round(var12);
         var4.add(var14);
         var2.add(var8);
      }
   }
}
