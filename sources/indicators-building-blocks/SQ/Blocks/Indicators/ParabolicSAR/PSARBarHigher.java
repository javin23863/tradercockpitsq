package SQ.Blocks.Indicators.ParabolicSAR;

import SQ.Internal.ConditionBlock;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.CategoryOrder;
import com.strategyquant.tradinglib.ChartData;
import com.strategyquant.tradinglib.OppositeBlock;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.ParameterSet;
import com.strategyquant.tradinglib.SortOrder;
import com.strategyquant.tradinglib.blocks.annotations.ParameterSets;

@BuildingBlock(name = "Price is above ParabolicSAR(@Chart@#Step#, #Maximum#)[#Shift#]", returnType = 3)
@SortOrder(300)
@CategoryOrder(1400)
@OppositeBlock("PSARBarLower")
@ParameterSets({@ParameterSet(set = "Step=0.02,Maximum=0.2"), @ParameterSet(set = "Step=0.02,Maximum=0.1"), @ParameterSet(set = "Step=0.01,Maximum=0.2")})
public class PSARBarHigher extends ConditionBlock {
   @Parameter
   public ChartData Input;
   @Parameter(defaultValue = "0.02", minValue = 0.01, maxValue = 0.6, step = 0.01, builderMinValue = 0.01, builderMaxValue = 0.4, builderStep = 0.001)
   public double Step;
   @Parameter(defaultValue = "0.2", minValue = 0.01, maxValue = 1.0, step = 0.1, builderMinValue = 0.01, builderMaxValue = 1.0, builderStep = 0.01)
   public double Maximum;
   @Parameter
   public int Shift;

   @Override
   public boolean OnBlockEvaluate() throws TradingException {
      return this.Input.Bid() > this.Strategy.Indicators.ParabolicSAR(this.Input, this.Step, this.Maximum).Value.getRounded(this.Shift);
   }
}
