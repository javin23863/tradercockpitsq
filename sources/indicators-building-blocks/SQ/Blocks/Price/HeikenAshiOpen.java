package SQ.Blocks.Price;

import SQ.Internal.ValueBlock;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.ChartData;
import com.strategyquant.tradinglib.ForEngine;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.SortOrder;

@BuildingBlock(name = "(HA O) Heiken Ashi Open", returnType = 2, display = "Heiken Ashi Open[@Chart@#Shift#]")
@SortOrder(2100)
@ForEngine("*,-SP,-SA")
public class HeikenAshiOpen extends ValueBlock {
   @Parameter
   public ChartData Chart;
   @Parameter
   public int Shift;

   @Override
   public double OnBlockEvaluate(int var1) throws TradingException {
      return this.Strategy.Indicators.HeikenAshi(this.Chart).HAOpen.getRounded(var1 + this.Shift);
   }
}
