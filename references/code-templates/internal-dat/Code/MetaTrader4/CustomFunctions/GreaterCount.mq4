
//+------------------------------------------------------------------+


bool sqIsGreaterCount(string indicatorIdentificationLeft,string indicatorIdentificationRight, int bars,bool NotStrict,int shift) {

   	bool atLeastOnce = false;

	for(int i=0; i<bars; i++) { /// bars != Bars in mq4

		double leftIndicator = NormalizeDouble(sqGetIndicatorByIdentification(indicatorIdentificationLeft,shift+i), 5); /// precision = 5. It returns more acccurate backtest synchronisation
		double rightIndicator = NormalizeDouble(sqGetIndicatorByIdentification(indicatorIdentificationRight,shift+i), 5);
		
		if(leftIndicator<rightIndicator){

			return (false);
		}
		if(leftIndicator==rightIndicator && NotStrict == false){

			return (false);
		}
		if(leftIndicator>rightIndicator){

			atLeastOnce = true;
		}
	}
	return(atLeastOnce);
}









 