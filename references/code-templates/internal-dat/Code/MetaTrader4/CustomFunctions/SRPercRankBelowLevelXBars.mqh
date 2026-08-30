bool sqSRPercRankBelowLevelXBars(int mode,int length,int ATRPeriod,double Level, int bars,int shift) {
	bool atLeastOnce = false;

	for(int i=0; i<bars; i++) {  /// bars != Bars in mq4

		if(NormalizeDouble(iCustom(NULL,0,"SqSRPercentRank",mode,length,ATRPeriod,0,shift+i),5)>Level){
			
			return (false);
		}
		if(NormalizeDouble(iCustom(NULL,0,"SqSRPercentRank",mode,length,ATRPeriod,0,shift+i),5)<Level){
			
			atLeastOnce = true;
		}
	}
	return(atLeastOnce);
}