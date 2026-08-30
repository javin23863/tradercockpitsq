package SQ.Blocks.OtherActions;

import SQ.Internal.ActionBlock;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.CategoryOrder;
import com.strategyquant.tradinglib.Editor;
import com.strategyquant.tradinglib.Help;
import com.strategyquant.tradinglib.IFormula;
import com.strategyquant.tradinglib.IgnoreInBuilder;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.SortOrder;
import com.strategyquant.tradinglib.Variable;

@BuildingBlock(name = "(VAR) Assign variable", display = "Assign variable", returnType = 4)
@Help("Assigns a value to the variable")
@SortOrder(100)
@CategoryOrder(400)
@IgnoreInBuilder
public class AssignVariable extends ActionBlock {
   @Parameter
   @Help("Choose variable to which you want to assign some value")
   @Editor(type = 80)
   public Variable Variable;
   @Parameter
   @Editor(type = 200, formulaName = "Price")
   public IFormula Value;

   @Override
   public void OnAction() throws TradingException {
      if (this.Value.isBooleanValue()) {
         double var1 = this.Value.evaluateFormula(this.Strategy, this.Strategy.Symbol, 0.0, 0);
         this.Variable.setValue(var1 != 0.0 && var1 != -9.9999999E7);
      } else {
         this.Variable.setValue(this.Value.evaluateFormula(this.Strategy, this.Strategy.Symbol, 0.0, 0));
      }
   }
}
