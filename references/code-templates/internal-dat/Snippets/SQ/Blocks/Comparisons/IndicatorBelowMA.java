package SQ.Blocks.Comparisons;

import com.strategyquant.lib.*;
import com.strategyquant.datalib.*;
import com.strategyquant.tradinglib.*;

@BuildingBlock(name="(I < MA) Indicator Below MA", display="#Indicator# is below its #MAType# MA(#Period#)", returnType = ReturnTypes.Boolean)
@OppositeBlock("IndicatorAboveMA")
public class IndicatorBelowMA extends IndicatorMAComparisonBlockAbstract {
    @Override
    public boolean OnEvaluateComparison() throws TradingException {
        double indicatorValue = SQUtils.round(Indicator.evaluateBlock(Shift), 6);
        double maValue = calculateMA(Period, MAType, Shift);
        return indicatorValue < maValue;
    }
}