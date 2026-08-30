package SQ.Blocks.OtherActions;

import SQ.Internal.ActionBlock;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.CategoryOrder;
import com.strategyquant.tradinglib.Editor;
import com.strategyquant.tradinglib.ForEngine;
import com.strategyquant.tradinglib.Help;
import com.strategyquant.tradinglib.IgnoreInBuilder;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.SortOrder;

@BuildingBlock(returnType = 4)
@Help("Logs the specified text to a journal. The text is written to Strategy Tester : Journal during backtest, and to Terminal : Experts during live trading.")
@SortOrder(200)
@CategoryOrder(500)
@IgnoreInBuilder
@ForEngine("*,-MC,-TS,-SP,-SA")
public class LogToJournal extends ActionBlock {
   @Parameter(defaultValue = "some message")
   @Editor(type = 30)
   public String Message;

   @Override
   public void OnAction() throws TradingException {
   }
}
