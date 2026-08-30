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
@Help("Sends email with specified text. It relies on correct SMTP settings in MT4 in Tools : Options : Email")
@SortOrder(300)
@CategoryOrder(500)
@IgnoreInBuilder
@ForEngine("*,-MC,-TS,-SP,-SA")
public class SendEmail extends ActionBlock {
   @Parameter(defaultValue = "Some subject")
   @Editor(type = 30)
   public String EmailSubject;
   @Parameter(defaultValue = "some message")
   @Editor(type = 30)
   public String Message;

   @Override
   public void OnAction() throws TradingException {
   }
}
