package SQ.Blocks.OtherActions;

import SQ.Internal.ActionBlock;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.ForEngine;
import com.strategyquant.tradinglib.Help;
import com.strategyquant.tradinglib.IgnoreInBuilder;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.SortOrder;

@BuildingBlock(name = "(|) Draw Vertical Line", display = "Draw Vertical Line", returnType = 4)
@Help("Draws Vertical Line")
@SortOrder(400)
@IgnoreInBuilder
@ForEngine("*,-MC,-TS,-SP,-SA")
public class DrawVerticalLine extends ActionBlock {
   @Parameter
   public int Shift;

   @Override
   public void OnAction() throws TradingException {
   }
}
