package SQ.Blocks.OtherActions;

import SQ.Internal.ActionBlock;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.Editor;
import com.strategyquant.tradinglib.ForEngine;
import com.strategyquant.tradinglib.Help;
import com.strategyquant.tradinglib.IgnoreInBuilder;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.SortOrder;

@BuildingBlock(display = "Custom action", returnType = 4)
@Help(
   "Define your own action. You can specify any valid MQL command here, or call a custom MQL function defined in /user/extend/Code/{Platform}/CustomFunctions folder (only desktop version)"
)
@SortOrder(300)
@IgnoreInBuilder
@ForEngine("*,-SP,-SA")
public class CustomAction extends ActionBlock {
   @Parameter(defaultValue = "")
   @Editor(type = 30)
   public String Command;

   @Override
   public void OnAction() throws TradingException {
   }
}
