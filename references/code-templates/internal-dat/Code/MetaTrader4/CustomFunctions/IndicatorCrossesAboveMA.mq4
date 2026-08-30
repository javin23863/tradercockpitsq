//+------------------------------------------------------------------+
//| Checks if indicator crosses above its Moving Average                |
//+------------------------------------------------------------------+
bool sqIndicatorCrossesAboveMA(
    string indicatorIdentification,  // Indicator to check
    int period,                     // MA period
    int maType,                     // 1=SMA, 2=EMA, 3=WMA, 4=HullMA
    int shift                       // Current shift
) {
    // Get current and previous indicator values
    double currentValue = NormalizeDouble(
        sqGetIndicatorByIdentification(indicatorIdentification, shift),
        6
    );
    double previousValue = NormalizeDouble(
        sqGetIndicatorByIdentification(indicatorIdentification, shift + 1),
        6
    );
    
    // Calculate current and previous MA values
    double currentMA = 0;
    double previousMA = 0;
    
    switch(maType) {
        case 1: { // Simple MA
            // Current MA
            double sum = 0;
            for(int i = 0; i < period; i++) {
                sum += NormalizeDouble(
                    sqGetIndicatorByIdentification(indicatorIdentification, shift + i),
                    6
                );
            }
            currentMA = sum / period;
            
            // Previous MA
            sum = 0;
            for(int i = 1; i <= period; i++) {
                sum += NormalizeDouble(
                    sqGetIndicatorByIdentification(indicatorIdentification, shift + i),
                    6
                );
            }
            previousMA = sum / period;
            break;
        }
        
        case 2: { // Exponential MA
            double alpha = 2.0 / (period + 1.0);
            
            // Current MA
            currentMA = NormalizeDouble(
                sqGetIndicatorByIdentification(indicatorIdentification, shift + period - 1),
                6
            );
            for(int i = period - 2; i >= 0; i--) {
                double curValue = NormalizeDouble(
                    sqGetIndicatorByIdentification(indicatorIdentification, shift + i),
                    6
                );
                currentMA = curValue * alpha + currentMA * (1 - alpha);
            }
            
            // Previous MA
            previousMA = NormalizeDouble(
                sqGetIndicatorByIdentification(indicatorIdentification, shift + period),
                6
            );
            for(int i = period - 1; i >= 1; i--) {
                double curValue = NormalizeDouble(
                    sqGetIndicatorByIdentification(indicatorIdentification, shift + i),
                    6
                );
                previousMA = curValue * alpha + previousMA * (1 - alpha);
            }
            break;
        }
        
        case 3: { // Weighted MA
            // Current MA
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
            currentMA = weightedSum / weightSum;
            
            // Previous MA
            weightedSum = 0;
            weightSum = 0;
            for(int i = 1; i <= period; i++) {
                double weight = period - i + 1;
                double value = NormalizeDouble(
                    sqGetIndicatorByIdentification(indicatorIdentification, shift + i),
                    6
                );
                weightedSum += value * weight;
                weightSum += weight;
            }
            previousMA = weightedSum / weightSum;
            break;
        }
        
        case 4: { // Hull MA
            int halfPeriod = period / 2;
            int sqrtPeriod = (int)MathSqrt(period);
            
            // Calculate current Hull MA
            // First calculate WMA(n/2) - current
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
            double currentHalfWMA = weightedHalfSum / weightHalfSum;
            
            // Then calculate WMA(n) - current
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
            double currentFullWMA = weightedFullSum / weightFullSum;
            
            // Calculate 2*WMA(n/2) - WMA(n) - current
            double currentRawHull = 2 * currentHalfWMA - currentFullWMA;
            
            // Final current WMA with sqrt period
            weightedFullSum = 0;
            weightFullSum = 0;
            for(int i = 0; i < sqrtPeriod; i++) {
                double weight = sqrtPeriod - i;
                weightedFullSum += currentRawHull * weight;
                weightFullSum += weight;
            }
            currentMA = weightedFullSum / weightFullSum;
            
            // Calculate previous Hull MA
            // First calculate WMA(n/2) - previous
            weightedHalfSum = 0;
            weightHalfSum = 0;
            for(int i = 1; i <= halfPeriod; i++) {
                double weight = halfPeriod - i + 1;
                double value = NormalizeDouble(
                    sqGetIndicatorByIdentification(indicatorIdentification, shift + i),
                    6
                );
                weightedHalfSum += value * weight;
                weightHalfSum += weight;
            }
            double previousHalfWMA = weightedHalfSum / weightHalfSum;
            
            // Then calculate WMA(n) - previous
            weightedFullSum = 0;
            weightFullSum = 0;
            for(int i = 1; i <= period; i++) {
                double weight = period - i + 1;
                double value = NormalizeDouble(
                    sqGetIndicatorByIdentification(indicatorIdentification, shift + i),
                    6
                );
                weightedFullSum += value * weight;
                weightFullSum += weight;
            }
            double previousFullWMA = weightedFullSum / weightFullSum;
            
            // Calculate 2*WMA(n/2) - WMA(n) - previous
            double previousRawHull = 2 * previousHalfWMA - previousFullWMA;
            
            // Final previous WMA with sqrt period
            weightedFullSum = 0;
            weightFullSum = 0;
            for(int i = 1; i <= sqrtPeriod; i++) {
                double weight = sqrtPeriod - i + 1;
                weightedFullSum += previousRawHull * weight;
                weightFullSum += weight;
            }
            previousMA = weightedFullSum / weightFullSum;
            break;
        }
    }
    
    // Check for crossover: previous value below MA and current value above MA
    return (previousValue <= previousMA && currentValue > currentMA);
}
