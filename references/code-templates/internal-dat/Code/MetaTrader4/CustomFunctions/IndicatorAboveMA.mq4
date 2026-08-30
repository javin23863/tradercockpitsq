//+------------------------------------------------------------------+
//| Checks if indicator value is above its Moving Average              |
//+------------------------------------------------------------------+
bool sqIsIndicatorAboveMA(
    string indicatorIdentification,  // Indicator to check
    int period,                     // MA period
    int maType,                     // 1=SMA, 2=EMA, 3=WMA, 4=HullMA
    int shift                       // Current shift
) {
    // Get current indicator value with required precision
    double indicatorValue = NormalizeDouble(
        sqGetIndicatorByIdentification(indicatorIdentification, shift), 
        6
    );
    
    // Calculate MA value based on type
    double maValue = 0;
    
    switch(maType) {
        case 1: { // Simple MA
            double sum = 0;
            for(int i = 0; i < period; i++) {
                sum += NormalizeDouble(
                    sqGetIndicatorByIdentification(indicatorIdentification, shift + i),
                    6
                );
            }
            maValue = sum / period;
            break;
        }
        
        case 2: { // Exponential MA
            double alpha = 2.0 / (period + 1.0);
            maValue = NormalizeDouble(
                sqGetIndicatorByIdentification(indicatorIdentification, shift + period - 1),
                6
            );
            
            for(int i = period - 2; i >= 0; i--) {
                double curValue = NormalizeDouble(
                    sqGetIndicatorByIdentification(indicatorIdentification, shift + i),
                    6
                );
                maValue = curValue * alpha + maValue * (1 - alpha);
            }
            break;
        }
        
        case 3: { // Weighted MA
            double weightedSum = 0;
            double weightSum = 0;
            for(int i = 0; i < period; i++) {
                double weight = period - i;
                double value = NormalizeDouble(
                    sqGetIndicatorByIdentification(indicatorIdentification, shift + i),
                    6
                );
                weightedSum += value * weight;
                weightSum += weight;
            }
            maValue = weightedSum / weightSum;
            break;
        }
        
        case 4: { // Hull MA
            int halfPeriod = period / 2;
            int sqrtPeriod = (int)MathSqrt(period);
            
            // First calculate WMA(n/2)
            double weightedHalfSum = 0;
            double weightHalfSum = 0;
            for(int i = 0; i < halfPeriod; i++) {
                double weight = halfPeriod - i;
                double value = NormalizeDouble(
                    sqGetIndicatorByIdentification(indicatorIdentification, shift + i),
                    6
                );
                weightedHalfSum += value * weight;
                weightHalfSum += weight;
            }
            double halfWMA = weightedHalfSum / weightHalfSum;
            
            // Then calculate WMA(n)
            double weightedFullSum = 0;
            double weightFullSum = 0;
            for(int i = 0; i < period; i++) {
                double weight = period - i;
                double value = NormalizeDouble(
                    sqGetIndicatorByIdentification(indicatorIdentification, shift + i),
                    6
                );
                weightedFullSum += value * weight;
                weightFullSum += weight;
            }
            double fullWMA = weightedFullSum / weightFullSum;
            
            // Calculate 2*WMA(n/2) - WMA(n)
            double rawHull = 2 * halfWMA - fullWMA;
            
            // Final WMA with sqrt period
            double weightedFinalSum = 0;
            double weightFinalSum = 0;
            for(int i = 0; i < sqrtPeriod; i++) {
                double weight = sqrtPeriod - i;
                weightedFinalSum += rawHull * weight;
                weightFinalSum += weight;
            }
            maValue = weightedFinalSum / weightFinalSum;
            break;
        }
    }
    
    return indicatorValue > maValue;
}