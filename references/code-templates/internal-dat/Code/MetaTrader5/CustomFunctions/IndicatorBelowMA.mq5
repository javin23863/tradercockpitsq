//+------------------------------------------------------------------+
//| Check if indicator is below MA                                     |
//+------------------------------------------------------------------+
bool sqIsIndicatorBelowMA(
    string indicator,
    int period,
    int maType,
    int shift
) {
    double indicatorValue = NormalizeDouble(sqGetExpressionByIdentification(indicator, shift), 6);
    double maValue = 0;
    
    // Calculate MA value based on type
    switch(maType) {
        case 1: // Simple MA
            {
                double sum = 0;
                for(int i = 0; i < period; i++) {
                    sum += NormalizeDouble(sqGetExpressionByIdentification(indicator, shift + i), 6);
                }
                maValue = sum / period;
            }
            break;
            
        case 2: // Exponential MA
            {
                double alpha = 2.0 / (period + 1.0);
                double prevEMA = NormalizeDouble(sqGetExpressionByIdentification(indicator, shift + period - 1), 6);
                
                for(int i = period - 2; i >= 0; i--) {
                    prevEMA = NormalizeDouble(sqGetExpressionByIdentification(indicator, shift + i), 6) * alpha + 
                             prevEMA * (1 - alpha);
                }
                maValue = prevEMA;
            }
            break;
            
        case 3: // Weighted MA
            {
                double weightedSum = 0;
                double weightSum = 0;
                for(int i = 0; i < period; i++) {
                    double weight = period - i;
                    weightedSum += NormalizeDouble(sqGetExpressionByIdentification(indicator, shift + i), 6) * weight;
                    weightSum += weight;
                }
                maValue = weightedSum / weightSum;
            }
            break;
            
        case 4: // Hull MA
            {
                // Calculate WMA(n/2)
                int halfPeriod = period / 2;
                double halfWeightedSum = 0;
                double halfWeightSum = 0;
                for(int i = 0; i < halfPeriod; i++) {
                    double weight = halfPeriod - i;
                    halfWeightedSum += NormalizeDouble(sqGetExpressionByIdentification(indicator, shift + i), 6) * weight;
                    halfWeightSum += weight;
                }
                double halfWMA = halfWeightedSum / halfWeightSum;
                
                // Calculate WMA(n)
                double fullWeightedSum = 0;
                double fullWeightSum = 0;
                for(int i = 0; i < period; i++) {
                    double weight = period - i;
                    fullWeightedSum += NormalizeDouble(sqGetExpressionByIdentification(indicator, shift + i), 6) * weight;
                    fullWeightSum += weight;
                }
                double fullWMA = fullWeightedSum / fullWeightSum;
                
                // Calculate 2*WMA(n/2) - WMA(n)
                maValue = 2 * halfWMA - fullWMA;
            }
            break;
    }
    
    return indicatorValue < maValue;
}