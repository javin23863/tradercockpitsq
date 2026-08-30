package SQ.Blocks.Indicators.LinReg;

import SQ.Internal.ConditionBlock;
import com.strategyquant.datalib.DataSeries;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.CategoryOrder;
import com.strategyquant.tradinglib.Help;
import com.strategyquant.tradinglib.OppositeBlock;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.ParameterSet;
import com.strategyquant.tradinglib.SortOrder;
import com.strategyquant.tradinglib.blocks.annotations.ParameterSets;

@BuildingBlock(name = "LinReg is rising", display = "LinReg(@Chart@#Period#)[#Shift#] is rising", returnType = 3, mainIndicator = "LinearRegression")
@Help("Is triggered if LinReg is rising")
@OppositeBlock("LinRegFalling")
@SortOrder(100)
@CategoryOrder(1200)
@ParameterSets(
   {
         @ParameterSet(set = "Period=13,ComputedFrom=0"),
         @ParameterSet(set = "Period=14,ComputedFrom=0"),
         @ParameterSet(set = "Period=20,ComputedFrom=0"),
         @ParameterSet(set = "Period=30,ComputedFrom=0"),
         @ParameterSet(set = "Period=40,ComputedFrom=0"),
         @ParameterSet(set = "Period=50,ComputedFrom=0")
   }
)
public class LinRegRising extends ConditionBlock {
   @Parameter
   public DataSeries Input;
   @Parameter(defaultValue = "14")
   public int Period;
   @Parameter
   public int Shift;

   @Override
   public boolean OnBlockEvaluate() throws TradingException {
      LinearRegression var1 = this.Strategy.Indicators.LinearRegression(this.Input, this.Period);
      double var2 = var1.Value.getRounded(this.Shift + 1);
      double var4 = var1.Value.getRounded(this.Shift);
      return var2 < var4;
   }
}
