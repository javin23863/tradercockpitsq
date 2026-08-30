
//+------------------------------------------------------------------+

bool sqSRPercRankBelowLevelXBars(int mode,int length,int ATRPeriod,double Level, int bars,int shift) {
	
	bool atLeastOnce = false;
	int SRPCIndyHandle = iCustom(NULL,0, "SqSRPercentRank", mode,length ,ATRPeriod);
	
	for(int count=0; count<bars; count++) {  /// bars != Bars in mq4

		if(NormalizeDouble((double) getSRPCIndicatorValue(SRPCIndyHandle, 0, shift+count), 5)>Level){
			
			return (false);
		}
		if(NormalizeDouble((double) getSRPCIndicatorValue(SRPCIndyHandle, 0, shift+count), 5)<Level){
			
			atLeastOnce = true;
		}
	}
	return(atLeastOnce);
}