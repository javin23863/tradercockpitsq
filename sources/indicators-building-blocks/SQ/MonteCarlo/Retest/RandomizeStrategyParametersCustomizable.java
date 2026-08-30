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

@ClassConfig(
   name = "Randomize strategy parameters Customizable",
   display = "Customizable Randomize strategy parameters, with probability #Probability# % and max change #MaxChange# %"
)
public class RandomizeStrategyParametersCustomizable extends MonteCarloRetest {
   public static final Logger Log = LoggerFactory.getLogger(RandomizeStrategyParametersCustomizable.class);
   @Parameter(name = "Probability", defaultValue = "100", minValue = 1.0, maxValue = 100.0, step = 1.0)
   @Help("% Probability of parameter change")
   public int Probability;
   @Parameter(name = "Max change", defaultValue = "50", minValue = 1.0, maxValue = 500.0, step = 1.0)
   @Help("Max % change of parameter")
   public int MaxChange;
   @Parameter(name = "Symmetric parameters", defaultValue = "true")
   @Help(
      "If true, it uses symmetric parameters - the parameters will be shared for long and short side. Otherwise, the parameters for long and short side will be independent."
   )
   public boolean Symmetric;
   @Parameter(name = "Period", defaultValue = "true")
   @Help("Randomize Period")
   public boolean Period;
   @Parameter(name = "Shift", defaultValue = "false")
   @Help("Randomize Shift")
   public boolean Shift;
   @Parameter(name = "Constant", defaultValue = "false")
   @Help("Randomize Constant")
   public boolean Constant;
   @Parameter(name = "Other Param", defaultValue = "false")
   @Help("Randomize Other Param")
   public boolean OtherParam;
   @Parameter(name = "Exit Used", defaultValue = "false")
   @Help("Randomize Exit Used")
   public boolean ExitUsed;
   @Parameter(name = "Exit Unused", defaultValue = "false")
   @Help("Randomize Exit Unused")
   public boolean ExitUnused;
   @Parameter(name = "Boolean", defaultValue = "false")
   @Help("Randomize Boolean")
   public boolean Boolean;
   @Parameter(name = "Trading Options", defaultValue = "false")
   @Help("Randomize Trading Options")
   public boolean TradingOptions;
   private ValuesMap paramTypes = new ValuesMap();

   public RandomizeStrategyParametersCustomizable() {
      super(1);
   }

   public void modifySettings(IRandomGenerator var1, SettingsMap var2) throws Exception {
      StrategyBase var3 = StrategyBase.getStrategy(var2);
      this.paramTypes.set("ParamTypePeriod", this.Period);
      this.paramTypes.set("ParamTypeShift", this.Shift);
      this.paramTypes.set("ParamTypeConstant", this.Constant);
      this.paramTypes.set("ParamTypeOtherParam", this.OtherParam);
      this.paramTypes.set("ParamTypeExitUsed", this.ExitUsed);
      this.paramTypes.set("ParamTypeExitUnused", this.ExitUnused);
      this.paramTypes.set("ParamTypeBoolean", this.Boolean);
      this.paramTypes.set("ParamTypeTradingOptions", this.TradingOptions);
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

   public RandomizeStrategyParametersCustomizable getClone() throws Exception {
      RandomizeStrategyParametersCustomizable var1 = new RandomizeStrategyParametersCustomizable();
      var1.Probability = this.Probability;
      var1.MaxChange = this.MaxChange;
      var1.Symmetric = this.Symmetric;
      var1.Period = this.Period;
      var1.Shift = this.Shift;
      var1.Constant = this.Constant;
      var1.OtherParam = this.OtherParam;
      var1.ExitUsed = this.ExitUsed;
      var1.ExitUnused = this.ExitUnused;
      var1.Boolean = this.Boolean;
      var1.TradingOptions = this.TradingOptions;
      var1.setParams(this.getParams());
      return var1;
   }
}
