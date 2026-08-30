package SQ.Blocks.Price;

import SQ.Internal.ValueBlock;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.tradinglib.BlockDefinitionException;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.ChartData;
import com.strategyquant.tradinglib.ForEngine;
import com.strategyquant.tradinglib.OppositeBlock;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.SortOrder;

@BuildingBlock(name = "(SO) Session Open", returnType = 2, display = "SessionOpen(@Chart@#StartHours#:#StartMinutes#)[#Shift#]")
@OppositeBlock("SessionOpen")
@SortOrder(800)
@ForEngine("*,-SP,-SA")
public class SessionOpen extends ValueBlock {
   @Parameter
   public ChartData Chart;
   @Parameter(defaultValue = "8", minValue = 0.0, maxValue = 23.0, step = 1.0)
   public int StartHours;
   @Parameter(defaultValue = "30", minValue = 0.0, maxValue = 59.0, step = 1.0)
   public int StartMinutes;
   @Parameter(defaultValue = "0", minValue = 0.0, step = 1.0)
   public int Shift;
   private SessionOHLCCalculator sessionCalculator = null;

   @Override
   protected void OnInit() throws BlockDefinitionException {
      this.sessionCalculator = new SessionOHLCCalculator(
         (byte)1, this.StartHours, this.StartMinutes, this.StartHours, this.StartMinutes, this.Shift, this.Chart
      );
   }

   @Override
   public double OnBlockEvaluate(int var1) throws TradingException {
      return this.sessionCalculator.get();
   }
}
