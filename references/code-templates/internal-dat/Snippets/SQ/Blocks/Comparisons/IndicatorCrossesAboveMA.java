// IndicatorCrossesAboveMA implementation
package SQ.Blocks.Comparisons;

import com.strategyquant.lib.*;
import com.strategyquant.datalib.*;
import com.strategyquant.tradinglib.*;

@BuildingBlock(name="(I ↗ MA) Indicator Crosses Above MA", display="#Indicator# crosses above its #MAType# MA(#Period#)", returnType = ReturnTypes.Boolean)
@OppositeBlock("IndicatorCrossesBelowMA")
public class IndicatorCrossesAboveMA extends IndicatorMAComparisonBlockAbstract {
    
    @Override
    public boolean OnEvaluateComparison() throws TradingException {
        // Get current and previous indicator values
        double indicatorValue = SQUtils.round(Indicator.evaluateBlock(Shift), 6);
        double prevIndicatorValue = SQUtils.round(Indicator.evaluateBlock(Shift + 1), 6);
        
        // Get current and previous MA values
        double maValue = calculateMA(Period, MAType, Shift);
        double prevMaValue = calculateMA(Period, MAType, Shift + 1);
        
        // Check crossing condition: prev indicator below prev MA AND current indicator above current MA
        return prevIndicatorValue < prevMaValue && indicatorValue > maValue;
    }
}