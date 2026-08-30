package SQ.Blocks.Functions;

import SQ.Internal.ValueBlock;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.lib.SQTime;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.Help;
import com.strategyquant.tradinglib.IgnoreInBuilder;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.SortOrder;

@BuildingBlock(name = "(DATE) GetDate", display = "GetDate(#Day#, #Month#, #Year#)", returnType = 1)
@Help("Returns day as YYYMMDD number, comparable with Bar Time or Current Time values")
@SortOrder(1200)
@IgnoreInBuilder
public class GetDate extends ValueBlock {
   @Parameter(minValue = 1.0, maxValue = 31.0, defaultValue = "1", step = 1.0)
   public int Day;
   @Parameter(minValue = 1.0, maxValue = 12.0, defaultValue = "1", step = 1.0)
   public int Month;
   @Parameter(minValue = 1900.0, maxValue = 2100.0, defaultValue = "2016", step = 1.0)
   public int Year;

   @Override
   public double OnBlockEvaluate(int var1) throws TradingException {
      return SQTime.getYmd(this.Year - 1900, this.Month, this.Day);
   }
}
