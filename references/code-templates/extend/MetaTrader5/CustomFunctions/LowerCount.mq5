
//+------------------------------------------------------------------+

bool sqIsLowerCount(string indicatorIdentificationLeft,string indicatorIdentificationRight, int bars,bool NotStrict,int shift) {

   	bool atLeastOnce = false;

	for(int i=0; i<bars; i++) { /// bars != Bars in mq4

		double leftIndicator = NormalizeDouble(sqGetExpressionByIdentification(indicatorIdentificationLeft,shift+i), 5); /// precision = 4. It returns more acccurate backtest synchronisation
		double rightIndicator = NormalizeDouble(sqGetExpressionByIdentification(indicatorIdentificationRight,shift+i), 5);
		
		if(leftIndicator>rightIndicator){

			return (false);
		}
		if(leftIndicator==rightIndicator && NotStrict == false){

			return (false);
		}
		if(leftIndicator<rightIndicator){

			atLeastOnce = true;
		}
	}
	return(atLeastOnce);
}






 