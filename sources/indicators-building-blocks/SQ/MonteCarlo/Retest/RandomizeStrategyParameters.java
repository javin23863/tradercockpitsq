package SQ.MonteCarlo.Retest;

import com.strategyquant.lib.IRandomGenerator;
import com.strategyquant.lib.SettingsMap;
import com.strategyquant.lib.ValuesMap;
import com.strategyquant.tradinglib.ClassConfig;
import com.strategyquant.tradinglib.Help;
import com.strategyquant.tradinglib.MonteCarloRetest;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.StrategyBase;
import com.strategyquant.tradinglib.Variable;
import com.strategyquant.tradinglib.Variables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ClassConfig(name = "Randomize strategy parameters", display = "Randomize strategy parameters, with probability #Probability# % and max change #MaxChange# %")
public class RandomizeStrategyParameters extends MonteCarloRetest {
   public static final Logger Log = LoggerFactory.getLogger(RandomizeStrategyParameters.class);
   @Parameter(name = "Probability", defaultValue = "10", minValue = 1.0, maxValue = 100.0, step = 1.0)
   public int Probability;
   @Parameter(name = "Max change", defaultValue = "20", minValue = 1.0, maxValue = 100.0, step = 1.0)
   public int MaxChange;
   @Parameter(name = "Symmetric parameters", defaultValue = "true")
   @Help(
      "If true, it uses symmetric parameters - the parameters will be shared for long and short side. Otherwise, the parameters for long and short side will be independent."
   )
   public boolean Symmetric;
   private ValuesMap paramTypes = new ValuesMap();

   public RandomizeStrategyParameters() {
      super(1);
   }

   public void modifySettings(IRandomGenerator var1, SettingsMap var2) throws Exception {
      StrategyBase var3 = StrategyBase.getStrategy(var2);
      this.paramTypes.set("ParamTypePeriod", true);
      this.paramTypes.set("ParamTypeShift", false);
      this.paramTypes.set("ParamTypeConstant", true);
      this.paramTypes.set("ParamTypeOtherParam", true);
      this.paramTypes.set("ParamTypeExitUsed", true);
      this.paramTypes.set("ParamTypeExitUnused", true);
      this.paramTypes.set("ParamTypeBoolean", false);
      this.paramTypes.set("ParamTypeTradingOptions", true);
      var3.transformToVariables(this.Symmetric, this.paramTypes);
      Variables var4 = var3.variables();
      var4.sortByName();
      if (var4.size() != 0) {
         double var5 = this.Probability / 100.0;
         int var7 = 0;

         int var8;
         do {
            var8 = this.modifyParameters(var4, var5, var1);
         } while (var8 <= 0 && ++var7 <= 10);
      }
   }

   private int modifyParameters(Variables var1, double var2, IRandomGenerator var4) {
      int var5 = 0;
      int var6 = 0;

      for (int var7 = 0; var7 < var1.size(); var7++) {
         Variable var8 = (Variable)var1.get(var7);
         if (this.isCorrectType(var8) && !var8.getName().contains("Magic")) {
            var6++;
         }
      }

      if (var6 > 0) {
         byte var9 = 0;

         do {
            var5 = this.changeSomeVars(var1, var2, var4);
         } while (var5 <= 0 && var9 <= 100);
      }

      return var5;
   }

   private int changeSomeVars(Variables var1, double var2, IRandomGenerator var4) {
      int var5 = 0;

      for (int var6 = 0; var6 < var1.size(); var6++) {
         Variable var7 = (Variable)var1.get(var6);
         if (this.isCorrectType(var7) && !var7.getName().contains("Magic") && var4.probability(var2)) {
            byte var8 = var7.getInternalType();
            if (var8 == 2) {
               var7.setValue(!var7.getValueAsBoolean());
               var5++;
            } else if (var8 == 1 || var8 == 4) {
               double var9 = var7.getValueAsDouble();
               double var11 = (1 + var4.nextInt(this.MaxChange)) / 100.0;
               double var13 = var9 * var11;
               double var15 = var4.nextInt(2) == 0 ? var9 + var13 : var9 - var13;
               if (var8 == 1) {
                  var15 = (int)Math.round(var15);
               }

               if (var9 != var15) {
                  var5++;
               }

               if (var8 == 1) {
                  var7.setValue((int)var15);
               } else {
                  var7.setValue(var15);
               }
            }
         }
      }

      return var5;
   }

   private void printVars(String var1, Variables var2, int var3) {
      Log.info("------------------------------------");
      Log.info(var1 + ", Changed: " + var3);
      Log.info("------------------------------------");

      for (int var4 = 0; var4 < var2.size(); var4++) {
         Variable var5 = (Variable)var2.get(var4);
         Log.info("Var #{} : {}", var4, var5.toString());
      }
   }

   private boolean isCorrectType(Variable var1) {
      return var1 != null && var1.getParamType() != null ? this.paramTypes.getBoolean(var1.getParamType(), false) : false;
   }

   public RandomizeStrategyParameters getClone() throws Exception {
      RandomizeStrategyParameters var1 = new RandomizeStrategyParameters();
      var1.Probability = this.Probability;
      var1.MaxChange = this.MaxChange;
      var1.Symmetric = this.Symmetric;
      var1.setParams(this.getParams());
      return var1;
   }
}
