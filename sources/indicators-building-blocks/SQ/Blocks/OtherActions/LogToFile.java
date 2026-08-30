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
@Help("Log message to a given file. The file will be placed in {MT4}/tester/files (for backtest) or in {MT4}/experts/files (for real trading)")
@SortOrder(100)
@CategoryOrder(500)
@IgnoreInBuilder
@ForEngine("*,-MC,-TS,-SP,-SA")
public class LogToFile extends ActionBlock {
   @Parameter(defaultValue = "some_logfile.txt")
   @Editor(type = 30)
   public String Filename;
   @Parameter(defaultValue = "some message")
   @Editor(type = 30)
   public String Message;

   @Override
   public void OnAction() throws TradingException {
   }
}
