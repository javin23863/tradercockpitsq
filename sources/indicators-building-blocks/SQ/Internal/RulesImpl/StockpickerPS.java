package SQ.Internal.RulesImpl;

import SQ.Internal.ITradingOptionsEvaluator;
import SQ.Internal.Rule;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.tradinglib.BlockDefinitionException;
import com.strategyquant.tradinglib.IFormula;
import com.strategyquant.tradinglib.Variable;
import com.strategyquant.tradinglib.engine.stockpicker.backtester.BacktestData;
import com.strategyquant.tradinglib.engine.stockpicker.signals.Signals;
import com.strategyquant.tradinglib.engine.stockpicker.signals.entry.PickerEntrySignal;
import com.strategyquant.tradinglib.formulas.Formulas;
import com.strategyquant.tradinglib.options.parameters.StockpickerOptions;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.List;
import org.jdom2.Element;

public class StockpickerPS extends Rule {
   public IFormula Value;
   private BacktestData data;
   private byte direction = -1;

   @Override
   public void evaluateRule(int var1, ITradingOptionsEvaluator var2, String var3) throws Exception {
      if (!this.Strategy.Stockpicker.isSingleAssetStrategy()) {
         if (this.Strategy.Stockpicker.entryType() == this.Strategy.Stockpicker.strategyTriggeredAt()) {
            if (this.Value != null) {
               this.data = this.Strategy.Stockpicker.data;
               boolean var5 = this.Strategy.Stockpicker.entryType == 15;
               Signals var4;
               if (var5) {
                  if (!this.Strategy.Stockpicker.data.exists(-1)) {
                     return;
                  }

                  var4 = this.data.Signals(this.data.getTime(-1), false);
               } else {
                  var4 = this.data.Signals(this.data.getTime(0), false);
               }

               if (var4 != null) {
                  int var6 = this.Strategy.getSymbolHash();
                  ObjectArrayList var7;
                  if (this.direction == 1) {
                     var7 = var4.getLongEntrySignals(var6);
                  } else {
                     var7 = var4.getShortEntrySignals(var6);
                  }

                  if (var7 != null && !var7.isEmpty()) {
                     String var8 = this.Strategy.getSymbol();
                     double var9 = SQUtils.round(this.Value.evaluateFormula(this.Strategy, var8, 0.0, 0), 5);

                     for (int var11 = 0; var11 < var7.size(); var11++) {
                        PickerEntrySignal var12 = (PickerEntrySignal)var7.get(var11);
                        var12.posScore = var9;
                     }
                  }
               }
            }
         }
      }
   }

   @Override
   protected void parseXml(Element var1) throws BlockDefinitionException {
      if (!this.Strategy.Stockpicker.isSingleAssetStrategy()) {
         String var2 = var1.getAttributeValue("name");
         if (var2.equalsIgnoreCase("Position Score Long")) {
            this.direction = 1;
         } else {
            if (!var2.equalsIgnoreCase("Position Score Short")) {
               throw new BlockDefinitionException(String.format("Invalid name of rule '%s'.", var2));
            }

            this.direction = -1;
         }

         Element var3 = var1.getChild("Value");

         for (Element var5 : var3.getChildren("Item")) {
            if (var5 == null || !var5.getAttributeValue("key").equals("AssignVariable")) {
               throw new BlockDefinitionException("Invalid StockpickerPS rule definition.");
            }

            List var6 = var5.getChildren("Param");
            Element var7 = (Element)var6.get(0);
            Element var8 = (Element)var6.get(1);
            String var9 = var7.getAttributeValue("name");
            if (!var9.equals("PositionScore")) {
               if (var9.equals("MaxOpenPositions")) {
                  String var14 = var8.getText().trim();
                  String var15 = var8.getAttributeValue("variable");
                  if (var15 != null && var15.equals("true")) {
                     Variable var16 = this.Strategy.variables().getById(var14);
                     if (var16 != null) {
                        var14 = var16.getValue();
                     }
                  }

                  int var17 = Integer.valueOf(var14);
                  if (this.direction == 1) {
                     this.Strategy.Stockpicker.MaxOpenPositionsLong = var17;
                  } else {
                     this.Strategy.Stockpicker.MaxOpenPositionsShort = var17;
                  }

                  StockpickerOptions var18 = this.Strategy.getStockpickerOptions();
                  if (var18 != null && !this.Strategy.isAlgoWizard) {
                     if (this.direction == 1) {
                        this.Strategy.Stockpicker.MaxOpenPositionsLong = var18.getMaxOpenPositionsLong(this.Strategy.Stockpicker.MaxOpenPositionsLong);
                     } else {
                        this.Strategy.Stockpicker.MaxOpenPositionsShort = var18.getMaxOpenPositionsShort(this.Strategy.Stockpicker.MaxOpenPositionsShort);
                     }
                  }
               }
            } else {
               String var10 = var8.getAttributeValue("isFormula");
               if (var10 != null && var10.equals("true")) {
                  Element var11 = var8.getChild("Formula");
                  String var12 = var11.getAttributeValue("key");
                  IFormula var13 = Formulas.get(var12);
                  if (var11.getChild("Block").getChild("Item") != null) {
                     this.Value = var13.newFormulaInstance(this.Strategy, var11);
                     continue;
                  }
                  break;
               }
            }
         }

         if (this.Value == null) {
            if (this.direction == 1) {
               this.Strategy.Stockpicker.MaxOpenPositionsLong = 0;
            } else {
               this.Strategy.Stockpicker.MaxOpenPositionsShort = 0;
            }
         }

         return;
      }
   }
}
