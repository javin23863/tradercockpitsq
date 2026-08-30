
//+------------------------------------------------------------------+


bool sqIsLowerPercentile(string indicatorIdentification,double Percentile, int bars,int shift) {
        int count = 1;
        double percrank = 0;
        for(int i=0; i<bars; i++) {
            double currVal = NormalizeDouble(sqGetExpressionByIdentification(indicatorIdentification,shift), 5); /// precision = 5. It returns more acccurate backtest synchronisation
            double prevVal = NormalizeDouble(sqGetExpressionByIdentification(indicatorIdentification,shift+i), 5);
            if (currVal < prevVal){
                    count++;
                }
            }
            percrank = (double)count/bars*100;
        double RB = NormalizeDouble(Percentile, 5);
        return percrank >= RB;
    }







 