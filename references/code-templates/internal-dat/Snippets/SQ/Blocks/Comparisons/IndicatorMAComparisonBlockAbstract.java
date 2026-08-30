// Abstract base class
package SQ.Blocks.Comparisons;

import SQ.Internal.ComparisonBlock;
import com.strategyquant.lib.*;
import com.strategyquant.datalib.*;
import com.strategyquant.tradinglib.*;

abstract public class IndicatorMAComparisonBlockAbstract extends ComparisonBlock {
    @Parameter
    public IBlock Indicator;
    
    @Parameter(name="Period", defaultValue="14", minValue=2, builderMaxValue=250, maxValue=250)
    @Help("Moving Average Period")
    public int Period;
    
    @Parameter(name="MA Type", defaultValue="1")
    @Editor(type=Editors.Selection, values="Simple=1,Exponential=2,Weighted=3,Hull=4")
    @Help("Simple=1,Exponential=2,Weighted=3,Hull=4")
    public int MAType;
    
    @Parameter(category="Properties", builderMinValue=1, builderMaxValue=1)
    public int Shift;

    protected double calculateWMA(int period, int shift) throws TradingException {
        double weightedSum = 0;
        double weightSum = 0;
        for(int i = 0; i < period; i++) {
            double weight = period - i;
            weightedSum += SQUtils.round(Indicator.evaluateBlock(shift + i), 6) * weight;
            weightSum += weight;
        }
        return weightedSum / weightSum;
    }

    protected double calculateMA(int period, int maType, int shift) throws TradingException {
        double result = 0;
        
        switch(maType) {
            case 1: // Simple MA
                double sum = 0;
                for(int i = 0; i < period; i++) {
                    sum += SQUtils.round(Indicator.evaluateBlock(shift + i), 6);
                }
                result = sum / period;
                break;
                
            case 2: // Exponential MA
                double alpha = 2.0 / (period + 1.0);
                double prevEMA = SQUtils.round(Indicator.evaluateBlock(shift + period - 1), 6);
                
                for(int i = period - 2; i >= 0; i--) {
                    prevEMA = SQUtils.round(Indicator.evaluateBlock(shift + i), 6) * alpha + prevEMA * (1 - alpha);
                }
                result = prevEMA;
                break;
                
            case 3: // Weighted MA
                result = calculateWMA(period, shift);
                break;
                
            case 4: // Hull MA
                int halfPeriod = period / 2;
                int sqrtPeriod = (int)Math.sqrt(period);
                
                // Calculate WMA(n/2)
                double halfWMA = calculateWMA(halfPeriod, shift);
                
                // Calculate WMA(n)
                double fullWMA = calculateWMA(period, shift);
                
                // Calculate 2*WMA(n/2) - WMA(n)
                result = 2 * halfWMA - fullWMA;
                break;
        }
        
        return result;
    }
}