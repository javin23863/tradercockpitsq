
// IndicatorCrossesBelowMA implementation
package SQ.Blocks.Comparisons;

import com.strategyquant.lib.*;
import com.strategyquant.datalib.*;
import com.strategyquant.tradinglib.*;

@BuildingBlock(name="(I ↘ MA) Indicator Crosses Below MA", display="#Indicator# crosses below its #MAType# MA(#Period#)", returnType = ReturnTypes.Boolean)
@OppositeBlock("IndicatorCrossesAboveMA")
public class IndicatorCrossesBelowMA extends IndicatorMAComparisonBlockAbstract {
    
    @Override
    public boolean OnEvaluateComparison() throws TradingException {
        // Get current and previous indicator values 
        double indicatorValue = SQUtils.round(Indicator.evaluateBlock(Shift), 6);
        double prevIndicatorValue = SQUtils.round(Indicator.evaluateBlock(Shift + 1), 6);
        
        // Get current and previous MA values
        double maValue = calculateMA(Period, MAType, Shift);
        double prevMaValue = calculateMA(Period, MAType, Shift + 1);
        
        // Check crossing condition: prev indicator above prev MA AND current indicator below current MA
        return prevIndicatorValue > prevMaValue && indicatorValue < maValue;
    }
}