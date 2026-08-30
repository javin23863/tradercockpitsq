bool sqIndicatorCrossesBelowMA(
    string indicator,
    int period,
    int maType,
    int shift
) {
    // Get current and previous indicator values
    double indicatorCurrent = NormalizeDouble(sqGetExpressionByIdentification(indicator, shift), 6);
    double indicatorPrevious = NormalizeDouble(sqGetExpressionByIdentification(indicator, shift + 1), 6);
    
    // Variables for current and previous MA values
    double maCurrent = 0;
    double maPrevious = 0;
    
    // Calculate current MA value based on type
    switch(maType) {
        case 1: // Simple MA
            {
                double sum = 0;
                for(int i = 0; i < period; i++) {
                    sum += NormalizeDouble(sqGetExpressionByIdentification(indicator, shift + i), 6);
                }
                maCurrent = sum / period;
                
                // Calculate previous MA value
                sum = 0;
                for(int i = 1; i < period + 1; i++) {
                    sum += NormalizeDouble(sqGetExpressionByIdentification(indicator, shift + i), 6);
                }
                maPrevious = sum / period;
            }
            break;
            
        case 2: // Exponential MA
            {
                double alpha = 2.0 / (period + 1.0);
                
                // Calculate current EMA
                double prevEMA = NormalizeDouble(sqGetExpressionByIdentification(indicator, shift + period - 1), 6);
                for(int i = period - 2; i >= 0; i--) {
                    prevEMA = NormalizeDouble(sqGetExpressionByIdentification(indicator, shift + i), 6) * alpha + 
                             prevEMA * (1 - alpha);
                }
                maCurrent = prevEMA;
                
                // Calculate previous EMA
                prevEMA = NormalizeDouble(sqGetExpressionByIdentification(indicator, shift + period), 6);
                for(int i = period - 1; i >= 1; i--) {
                    prevEMA = NormalizeDouble(sqGetExpressionByIdentification(indicator, shift + i), 6) * alpha + 
                             prevEMA * (1 - alpha);
                }
                maPrevious = prevEMA;
            }
            break;
            
        case 3: // Weighted MA
            {
                // Calculate current WMA
                double weightedSum = 0;
                double weightSum = 0;
                for(int i = 0; i < period; i++) {
                    double weight = period - i;
                    weightedSum += NormalizeDouble(sqGetExpressionByIdentification(indicator, shift + i), 6) * weight;
                    weightSum += weight;
                }
                maCurrent = weightedSum / weightSum;
                
                // Calculate previous WMA
                weightedSum = 0;
                weightSum = 0;
                for(int i = 1; i < period + 1; i++) {
                    double weight = period - i + 1;
                    weightedSum += NormalizeDouble(sqGetExpressionByIdentification(indicator, shift + i), 6) * weight;
                    weightSum += weight;
                }
                maPrevious = weightedSum / weightSum;
            }
            break;
            
        case 4: // Hull MA
            {
                // Calculate current HMA
                int halfPeriod = period / 2;
                
                // Current WMA(n/2)
                double halfWeightedSum = 0;
                double halfWeightSum = 0;
                for(int i = 0; i < halfPeriod; i++) {
                    double weight = halfPeriod - i;
                    halfWeightedSum += NormalizeDouble(sqGetExpressionByIdentification(indicator, shift + i), 6) * weight;
                    halfWeightSum += weight;
                }
                double currentHalfWMA = halfWeightedSum / halfWeightSum;
                
                // Current WMA(n)
                double fullWeightedSum = 0;
                double fullWeightSum = 0;
                for(int i = 0; i < period; i++) {
                    double weight = period - i;
                    fullWeightedSum += NormalizeDouble(sqGetExpressionByIdentification(indicator, shift + i), 6) * weight;
                    fullWeightSum += weight;
                }
                double currentFullWMA = fullWeightedSum / fullWeightSum;
                maCurrent = 2 * currentHalfWMA - currentFullWMA;
                
                // Calculate previous HMA
                // Previous WMA(n/2)
                halfWeightedSum = 0;
                halfWeightSum = 0;
                for(int i = 1; i < halfPeriod + 1; i++) {
                    double weight = halfPeriod - i + 1;
                    halfWeightedSum += NormalizeDouble(sqGetExpressionByIdentification(indicator, shift + i), 6) * weight;
                    halfWeightSum += weight;
                }
                double previousHalfWMA = halfWeightedSum / halfWeightSum;
                
                // Previous WMA(n)
                fullWeightedSum = 0;
                fullWeightSum = 0;
                for(int i = 1; i < period + 1; i++) {
                    double weight = period - i + 1;
                    fullWeightedSum += NormalizeDouble(sqGetExpressionByIdentification(indicator, shift + i), 6) * weight;
                    fullWeightSum += weight;
                }
                double previousFullWMA = fullWeightedSum / fullWeightSum;
                maPrevious = 2 * previousHalfWMA - previousFullWMA;
            }
            break;
    }
    
    // Check for crossing below condition
    return (indicatorPrevious >= maPrevious && indicatorCurrent < maCurrent);
}