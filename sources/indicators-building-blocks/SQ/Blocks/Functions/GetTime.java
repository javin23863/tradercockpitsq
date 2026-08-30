package SQ.Blocks.Functions;

import SQ.Internal.ValueBlock;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.lib.SQTime;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.ForEngine;
import com.strategyquant.tradinglib.Help;
import com.strategyquant.tradinglib.IgnoreInBuilder;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.SortOrder;

@BuildingBlock(name = "(TIME) GetTime", display = "GetTime(#Hour#, #Minute#, #Second#)", returnType = 1)
@Help("Returns time as HHMMNN number, comparable with Bar Time or Current Time values")
@SortOrder(1100)
@IgnoreInBuilder
@ForEngine("*,-SP,-SA")
public class GetTime extends ValueBlock {
   @Parameter(minValue = 0.0, maxValue = 23.0, defaultValue = "0", step = 1.0)
   public int Hour;
   @Parameter(minValue = 0.0, maxValue = 59.0, defaultValue = "0", step = 1.0)
   public int Minute;
   @Parameter(minValue = 0.0, maxValue = 59.0, defaultValue = "0", step = 1.0)
   public int Second;

   @Override
   public double OnBlockEvaluate(int var1) throws TradingException {
      return SQTime.getHms(this.Hour, this.Minute, this.Second);
   }
}
