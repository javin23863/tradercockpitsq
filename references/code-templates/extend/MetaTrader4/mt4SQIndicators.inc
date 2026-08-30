
double roundValue(double value){
    return NormalizeDouble(value + 0.0000000001, 6);
}

//+------------------------------------------------------------------+

int sqFixRanges(int value, int min, int max, int defaultVal) {
   if(value < min || value > max) {
      return (defaultVal);
   }
   
   return value;
}

//+------------------------------------------------------------------+

double sqBarRange(string symbol, int timeframe, int shift) {
  double range;
   if(symbol == "NULL" || symbol == "Current") {
      range = iHigh(NULL, timeframe, shift) - iLow(NULL, timeframe, shift);
   } else {
      range = iHigh(symbol, timeframe, shift) - iLow(symbol, timeframe, shift);
   }
   
   return roundValue(range);
}

//+------------------------------------------------------------------+

double sqHeikenAshi(string symbol, int timeframe, string mode, int shift) {
   if(symbol == "NULL" || symbol == "Current") {
      if(mode == "Open") {
         return(NormalizeDouble(iCustom(NULL, timeframe, "SqHeikenAshi", 0,0,0,0, 2, shift), 6));
      }
      if(mode == "Close") {
         return(NormalizeDouble(iCustom(NULL, timeframe, "SqHeikenAshi", 0,0,0,0, 3, shift), 6));
      }
      if(mode == "High") {
         return(NormalizeDouble(MathMax(iCustom( NULL, timeframe, "SqHeikenAshi", 0,0,0,0, 0, shift), iCustom( NULL, timeframe, "SqHeikenAshi", 0,0,0,0, 1, shift)), 6));
      }
      if(mode == "Low") {
         return(NormalizeDouble(MathMin(iCustom( NULL, timeframe, "SqHeikenAshi", 0,0,0,0, 0, shift), iCustom( NULL, timeframe, "SqHeikenAshi", 0,0,0,0, 1, shift)), 6));
      }

   } else {
      if(mode == "Open") {
         return(NormalizeDouble(iCustom( symbol, timeframe, "SqHeikenAshi", 0,0,0,0, 2, shift), 6));
      }
      if(mode == "Close") {
         return(NormalizeDouble(iCustom( symbol, timeframe, "SqHeikenAshi", 0,0,0,0, 3, shift), 6));
      }
      if(mode == "High") {
         return(NormalizeDouble(MathMax(iCustom( symbol, timeframe, "SqHeikenAshi", 0,0,0,0, 0, shift), iCustom( symbol, timeframe, "SqHeikenAshi", 0,0,0,0, 1, shift)), 6));
      }
      if(mode == "Low") {
         return(NormalizeDouble(MathMin(iCustom( symbol, timeframe, "SqHeikenAshi", 0,0,0,0, 0, shift), iCustom( symbol, timeframe, "SqHeikenAshi", 0,0,0,0, 1, shift)), 6));
      }
   }

   return(-1);
}

//+------------------------------------------------------------------+

double sqBiggestRange(string symbol, int timeframe, int period, int shift) {
   double maxnum = -100000000;
   double range;

   for(int i=shift; i<shift+period; i++) {
      if(symbol == "NULL" || symbol == "Current") {
         range = iHigh(NULL, timeframe, i) - iLow(NULL, timeframe, i);
      } else {
         range = iHigh(symbol, timeframe, i) - iLow(symbol, timeframe, i);
      }

      if(range > maxnum) {
         maxnum = range;
      }
   }

   return roundValue(maxnum);
}

//+------------------------------------------------------------------+

double sqSmallestRange(string symbol, int timeframe, int period, int shift) {
   double minnum = 100000000;
   double range;

   for(int i=shift; i<shift+period; i++) {
      if(symbol == "NULL" || symbol == "Current") {
         range = iHigh(NULL, timeframe, i) - iLow(NULL, timeframe, i);
      } else {
         range = iHigh(symbol, timeframe, i) - iLow(symbol, timeframe, i);
      }

      if(range < minnum) {
         minnum = range;
      }
   }

   return roundValue(minnum);
}

//+------------------------------------------------------------------+

double sqMA(string symbol, int timeframe, int ma_period, int ma_shift, int ma_method, int applied_price, int shift) {
	
   ma_method = sqFixRanges(ma_method, 0, 3, 0);
   applied_price = sqFixRanges(applied_price, 0, 6, 0);

   return roundValue(iMA(symbol, timeframe, ma_period, ma_shift, ma_method, applied_price, shift));
}

//+------------------------------------------------------------------+

double sqTEMA(string symbol, int timeframe, int ma_period, int applied_price, int shift) {
   return roundValue(iCustom(symbol, timeframe, "SqTEMA", ma_period, applied_price, 0, shift));
}

//+------------------------------------------------------------------+

double sqIchimoku(string symbol, int timeframe, int tenkanPeriod, int kijunPeriod, int senkouPeriod, int line, int shift) {
   return roundValue(iCustom(symbol, timeframe, "SqIchimoku", tenkanPeriod, kijunPeriod, senkouPeriod, line, shift));
}

//+------------------------------------------------------------------+

double sqAroon(string symbol, int timeframe, int period, int line, int shift) {
   return roundValue(iCustom(symbol, timeframe, "SqAroon", period, false, false, line, shift));
}

//+------------------------------------------------------------------+

double sqBBWidthRatio(string symbol, int timeframe, int period, double deviation, int appliedPrice, int shift) {
   return roundValue(iCustom(symbol, timeframe, "SqBBWidthRatio", period, deviation, appliedPrice, 0, shift));
}

//+------------------------------------------------------------------+

double sqAvgVolume(string symbol, int timeframe, int period, int shift) {
   return roundValue(iCustom(symbol, timeframe, "SqAvgVolume", period, 1, shift));
}
            
//+------------------------------------------------------------------+

double sqFibo(string symbol, int timeframe, int fiboRange, double fiboLevel) {
   fiboRange = sqFixRanges(fiboRange, 1, 7, 1);

   return roundValue(iCustom(symbol, timeframe, "SqFibo", fiboRange, fiboLevel, 0, 0, 0));
}

//+------------------------------------------------------------------+

double sqLinReg(string symbol, int timeframe, int period, int line, int shift) {
   return roundValue(iCustom(symbol, timeframe, "SqLinReg", period, line, 0, shift));
}

//+------------------------------------------------------------------+

double sqPivots(string symbol, int timeframe, int startHour, int startMinute, int line, int shift) {
   return roundValue(iCustom(symbol, timeframe, "SqPivots", startHour, startMinute, line, shift));
}

//+------------------------------------------------------------------+

double sqQQE(string symbol, int timeframe, int rsiPeriod, int sF, double wF, int line, int shift) {
   return roundValue(iCustom(symbol, timeframe, "SqQQE", rsiPeriod, sF, wF, line, shift));
}

//+------------------------------------------------------------------+

double sqKeltnerChannel(string symbol, int timeframe, int period, double Deviation, int line, int shift) {
   return roundValue(iCustom(symbol, timeframe, "SqKeltnerChannel", period, Deviation, line, shift));
}

//+------------------------------------------------------------------+

double sqMTKeltnerChannel(string symbol, int timeframe, int period, double Deviation, int line, int shift) {
   return roundValue(iCustom(symbol, timeframe, "SqMTKeltnerChannel", period, Deviation, line, shift));
}

//+------------------------------------------------------------------+

double sqRSI(string symbol, int timeframe, int period, int applied_price, int shift) {
   return roundValue(iRSI(symbol, timeframe, period, applied_price, shift));
}

//+------------------------------------------------------------------+

double sqCCI(string symbol, int timeframe, int period, int applied_price, int shift) {
   return roundValue(iCCI(symbol, timeframe, period, applied_price, shift));
}

//+------------------------------------------------------------------+

double sqAC(string symbol, int timeframe, int shift) {
   return roundValue(iAC(symbol, timeframe, shift));
}

//+------------------------------------------------------------------+

double sqADX(string symbol, int timeframe, int period, int line, int shift) {
   return roundValue(iCustom(symbol, timeframe, "SqADX", period, line, shift));
}

//+------------------------------------------------------------------+

double sqATR(string symbol, int timeframe, int period, int shift) {
   return roundValue(iCustom(symbol, timeframe, "SqATR", period, 0, shift));
}

//+------------------------------------------------------------------+

double sqAO(string symbol, int timeframe, int shift) {
   return roundValue(iAO(symbol, timeframe, shift));
}

//+------------------------------------------------------------------+

double sqBearsPower(string symbol, int timeframe, int period, int applied_price, int shift) {
   return roundValue(iBearsPower(symbol, timeframe, period, applied_price, shift));
}

//+------------------------------------------------------------------+

double sqBullsPower(string symbol, int timeframe, int period, int applied_price, int shift) {
   return roundValue(iBullsPower(symbol, timeframe, period, applied_price, shift));
}

//+------------------------------------------------------------------+

double sqDeMarker(string symbol, int timeframe, int period, int shift) {
   return roundValue(iDeMarker(symbol, timeframe, period, shift));
}

//+------------------------------------------------------------------+

double sqMACD(string symbol, int timeframe, int fast_ema_period, int slow_ema_period, int signal_period, int applied_price, int mode, int shift) {
   return roundValue(iMACD(symbol, timeframe, fast_ema_period, slow_ema_period, signal_period, applied_price, mode, shift));
} 

//+------------------------------------------------------------------+

double sqMomentum(string symbol, int timeframe, int period, int applied_price, int shift) {
   return roundValue(iMomentum(symbol, timeframe, period, applied_price, shift));
}

//+------------------------------------------------------------------+

double sqStochastic(string symbol, int timeframe, int Kperiod, int Dperiod, int slowing, int method, int price_field, int mode, int shift) {
   price_field = sqFixRanges(price_field, 0, 1, 0);
   method = sqFixRanges(method, 0, 3, 0);
   
   return roundValue(iCustom(symbol, timeframe, "SqStochastic", Kperiod, Dperiod, slowing, method, price_field, mode, shift));
} 

//+------------------------------------------------------------------+

double sqOsMA(string symbol, int timeframe, int fast_ema_period, int slow_ema_period, int signal_period, int applied_price, int shift) {
   return roundValue(iOsMA(symbol, timeframe, fast_ema_period, slow_ema_period, signal_period, applied_price, shift));
}

//+------------------------------------------------------------------+

double sqBands(string symbol, int timeframe, int period, double deviation, int bands_shift, int applied_price, int mode, int shift) {
   return roundValue(iBands(symbol, timeframe, period, deviation, bands_shift, applied_price, mode+1, shift));
}
    
//+------------------------------------------------------------------+

double sqBBRange(string symbol, int timeframe, int period, double deviation, int applied_price, int shift) {
   return roundValue(iBands(symbol, timeframe, period, deviation, 0, applied_price, 1, shift) - iBands(symbol, timeframe, period, deviation, 0, applied_price, 2, shift));
}

//+------------------------------------------------------------------+

double sqSAR(string symbol, int timeframe, double step, double maximum, int shift) {
   return roundValue(iCustom(symbol, timeframe, "SqParabolicSAR", step, maximum, 0, shift));
   //return roundValue(iSAR(symbol, timeframe, step, maximum, shift));
}

//+------------------------------------------------------------------+

double sqStdDev(string symbol, int timeframe, int ma_period, int ma_shift, int ma_method, int applied_price, int shift) {
   return roundValue(iStdDev(symbol, timeframe, ma_period, ma_shift, ma_method, applied_price, shift));
}

//+------------------------------------------------------------------+

double sqHighestInRange(string symbol, int timeframe, int timeFrom, int timeTo, int shift) {
   return roundValue(iCustom(symbol, timeframe, "SqHighestInRange", HHMMToString(timeFrom), HHMMToString(timeTo), 0, shift));
}

//+------------------------------------------------------------------+

double sqHighestInRange(string symbol, int timeframe, string timeFrom, string timeTo, int shift) {
   return roundValue(iCustom(symbol, timeframe, "SqHighestInRange", timeFrom, timeTo, 0, shift));
}
     
//+------------------------------------------------------------------+

double sqLowestInRange(string symbol, int timeframe, int timeFrom, int timeTo, int shift) {
   return roundValue(iCustom(symbol, timeframe, "SqLowestInRange", HHMMToString(timeFrom), HHMMToString(timeTo), 0, shift));
}
     
//+------------------------------------------------------------------+

double sqLowestInRange(string symbol, int timeframe, string timeFrom, string timeTo, int shift) {
   return roundValue(iCustom(symbol, timeframe, "SqLowestInRange", timeFrom, timeTo, 0, shift));
}

//+------------------------------------------------------------------+

double sqFractal(string symbol, int timeframe, int fractal, int bufferIndex, int shift) {
   return roundValue(iCustom(symbol, timeframe, "SqFractal", fractal, bufferIndex, shift));
}

double sqWaveTrend(string symbol, int timeframe, int ChannelLength, int AverageLength, int line, int shift) {
   return roundValue(iCustom(symbol,timeframe,"SqWaveTrend",ChannelLength,AverageLength, line, shift));
}

//+------------------------------------------------------------------+

bool sqIchimokuChikouSpanCross(int bullishOrBearish, string symbol, int timeframe, int tenkanPeriod, int kijunPeriod, int senkouPeriod, int shift, int signalStrength) {
   double chikouSpan1 = sqIchimoku(symbol, timeframe, tenkanPeriod, kijunPeriod, senkouPeriod, 4, shift+1);
	double chikouSpan0 = sqIchimoku(symbol, timeframe, tenkanPeriod, kijunPeriod, senkouPeriod, 4, shift);
	double c = iClose(symbol, timeframe, shift);
	double kumoTop = sqIchimoku(symbol, timeframe, tenkanPeriod, kijunPeriod, senkouPeriod, 2, shift);
	double kumoBottom = sqIchimoku(symbol, timeframe, tenkanPeriod, kijunPeriod, senkouPeriod, 3, shift);
	if(kumoBottom > kumoTop) {
		double temp = kumoBottom;
		kumoBottom = kumoTop;
		kumoTop = temp;
	}
	
    signalStrength = sqFixRanges(signalStrength, 0, 2, 1);
    	
	bool signal;
	
	if(bullishOrBearish == -1) {
	   // bearish;
	   signal = (chikouSpan1 >= c) && (chikouSpan0 < c) && (chikouSpan1 > chikouSpan0);
		
		if(signalStrength == 2) {
			// for strong signal the cross should happen below kumo cloud
			signal = signal && (c < kumoBottom);
			
		} else if(signalStrength == 1) {
			// for neutral signal the cross should happen in kumo cloud
			signal = signal && (c < kumoTop);
			
		} else if(signalStrength == 0) {
			// do nothing, if there is cross signal is always at least weak
			
		} else {
			return false;;
		}
		
		return signal;
		
	} else if(bullishOrBearish == 1) { 
	   // bullish
	   signal = (chikouSpan1 <= c) && (chikouSpan0 > c) && (chikouSpan1 < chikouSpan0);
	   
	   if(signalStrength == 2) {
			// for strong signal the cross should happen above kumo cloud
			signal = signal && (c > kumoTop);
			
		} else if(signalStrength == 1) {
			// for neutral signal the cross should happen in kumo cloud
			signal = signal && (c > kumoBottom);
			
		} else if(signalStrength == 0) {
			// do nothing, if there is cross signal is always at least weak
			
		} else {
			return false;
		}

		return signal;
		
	} else {
			return false;
	}
}


//+------------------------------------------------------------------+

bool sqIchimokuSenkouSpanCross(int bullishOrBearish, string symbol, int timeframe, int tenkanPeriod, int kijunPeriod, int senkouPeriod, int shift, int signalStrength) {
	double senkouSpanA1 = sqIchimoku(symbol, timeframe, tenkanPeriod, kijunPeriod, senkouPeriod, 2, shift+1);
	double senkouSpanA0 = sqIchimoku(symbol, timeframe, tenkanPeriod, kijunPeriod, senkouPeriod, 2, shift);
	double senkouSpanB1 = sqIchimoku(symbol, timeframe, tenkanPeriod, kijunPeriod, senkouPeriod, 3, shift+1);
	double senkouSpanB0 = sqIchimoku(symbol, timeframe, tenkanPeriod, kijunPeriod, senkouPeriod, 3, shift);
	double c = iClose(symbol, timeframe, shift);
			
	double kumoTop = MathMax(senkouSpanA0,  senkouSpanB0); 
	double kumoBottom = MathMin(senkouSpanA0,  senkouSpanB0); 
		
	bool signal;
	
    signalStrength = sqFixRanges(signalStrength, 0, 2, 1);
	
	if(bullishOrBearish == -1) {
	   // bearish;
	   signal = (senkouSpanA1 > senkouSpanB1) && (senkouSpanA0 < senkouSpanB0);
		
		if(signalStrength == 2) {
			// for strong signal the cross should happen below kumo cloud
			signal = signal && (c < kumoBottom);
			
		} else if(signalStrength == 1) {
			// for neutral signal the cross should happen in kumo cloud
			signal = signal && (c < kumoTop);
			
		} else if(signalStrength == 0) {
			// do nothing, if there is cross signal is always at least weak
			
		} else {
			return false;;
		}
		
		return signal;
		
	} else if(bullishOrBearish == 1) { 
	   // bullish
	   signal = (senkouSpanA1 < senkouSpanB1) && (senkouSpanA0 > senkouSpanB0);
	   
	   if(signalStrength == 2) {
			// for strong signal the cross should happen above kumo cloud
			signal = signal && (c > kumoTop);
			
		} else if(signalStrength == 1) {
			// for neutral signal the cross should happen in kumo cloud
			signal = signal && (c > kumoBottom);
			
		} else if(signalStrength == 0) {
			// do nothing, if there is cross signal is always at least weak
			
		} else {
			return false;
		}

		return signal;
		
	} else {
			return false;
	}
}

//+------------------------------------------------------------------+

bool sqIchimokuKijunSenCross(int bullishOrBearish, string symbol, int timeframe, int tenkanPeriod, int kijunPeriod, int senkouPeriod, int shift, int signalStrength) {
	double o = iOpen(symbol, timeframe, shift);
	double c = iClose(symbol, timeframe, shift);
	double kijun = sqIchimoku(symbol, timeframe, tenkanPeriod, kijunPeriod, senkouPeriod, 1, shift);

	double kumoTop = sqIchimoku(symbol, timeframe, tenkanPeriod, kijunPeriod, senkouPeriod, 2, shift);
	double kumoBottom = sqIchimoku(symbol, timeframe, tenkanPeriod, kijunPeriod, senkouPeriod, 3, shift);
	if(kumoBottom > kumoTop) {
		double temp = kumoBottom;
		kumoBottom = kumoTop;
		kumoTop = temp;
	}
		
	bool signal;
	
    signalStrength = sqFixRanges(signalStrength, 0, 2, 1);
	
	if(bullishOrBearish == -1) {
	   // bearish;
	   signal = (o > kijun) && c < kijun;
		
		if(signalStrength == 2) {
			// for strong signal the cross should happen below kumo cloud
			signal = signal && (kijun < kumoBottom);
			
		} else if(signalStrength == 1) {
			// for neutral signal the cross should happen in kumo cloud
			signal = signal && (kijun < kumoTop);
			
		} else if(signalStrength == 0) {
			// do nothing, if there is cross signal is always at least weak
			
		} else {
			return false;;
		}
		
		return signal;
		
	} else if(bullishOrBearish == 1) { 
	   // bullish
	   signal = (o < kijun) && c > kijun;
	   
		if(signalStrength == 2) {
			// for strong signal the cross should happen above kumo cloud
			signal = signal && (kijun > kumoTop);
			
		} else if(signalStrength == 1) {
			// for neutral signal the cross should happen in kumo cloud
			signal = signal && (kijun > kumoBottom);
			
		} else if(signalStrength == 0) {
			// do nothing, if there is cross signal is always at least weak
			
		} else {
			return false;
		}

		return signal;
		
	} else {
			return false;
	}
}

//+------------------------------------------------------------------+

bool sqIchimokuTenkanKijunCross(int bullishOrBearish, string symbol, int timeframe, int tenkanPeriod, int kijunPeriod, int senkouPeriod, int shift, int signalStrength) {
	double tenkan1 = sqIchimoku(symbol, timeframe, tenkanPeriod, kijunPeriod, senkouPeriod, 0, shift+1);
	double tenkan0 = sqIchimoku(symbol, timeframe, tenkanPeriod, kijunPeriod, senkouPeriod, 0, shift);

	double kijun1 = sqIchimoku(symbol, timeframe, tenkanPeriod, kijunPeriod, senkouPeriod, 1, shift+1);
	double kijun0 = sqIchimoku(symbol, timeframe, tenkanPeriod, kijunPeriod, senkouPeriod, 1, shift);

	double kumoTop = sqIchimoku(symbol, timeframe, tenkanPeriod, kijunPeriod, senkouPeriod, 2, shift);
	double kumoBottom = sqIchimoku(symbol, timeframe, tenkanPeriod, kijunPeriod, senkouPeriod, 3, shift);
	if(kumoBottom > kumoTop) {
		double temp = kumoBottom;
		kumoBottom = kumoTop;
		kumoTop = temp;
	}
		
	bool signal;
	
    signalStrength = sqFixRanges(signalStrength, 0, 2, 1);
	
	if(bullishOrBearish == -1) {
	   // bearish;
	   signal = (tenkan1 > kijun1) && tenkan0 < kijun0;
		
		if(signalStrength == 2) {
			// for strong signal the cross should happen below kumo cloud
			signal = signal && (tenkan0 < kumoBottom);
			
		} else if(signalStrength == 1) {
			// for neutral signal the cross should happen in kumo cloud
			signal = signal && (tenkan0 < kumoTop);
			
		} else if(signalStrength == 0) {
			// do nothing, if there is cross signal is always at least weak
			
		} else {
			return false;;
		}
		
		return signal;
		
	} else if(bullishOrBearish == 1) { 
	   // bullish
	   signal = (tenkan1 < kijun1) && tenkan0 > kijun0;
	   
		if(signalStrength == 2) {
			// for strong signal the cross should happen above kumo cloud
			signal = signal && (tenkan0 > kumoTop);
			
		} else if(signalStrength == 1) {
			// for neutral signal the cross should happen in kumo cloud
			signal = signal && (tenkan0 > kumoBottom);
			
		} else if(signalStrength == 0) {
			// do nothing, if there is cross signal is always at least weak
			
		} else {
			return false;
		}

		return signal;
		
	} else {
			return false;
	}
}

//+------------------------------------------------------------------+

bool sqIchimokuKumoBreakout(int bullishOrBearish, string symbol, int timeframe, int tenkanPeriod, int kijunPeriod, int senkouPeriod, int shift) {
	double o = iOpen(symbol, timeframe, shift);
	double c = iClose(symbol, timeframe, shift);

	double kumoTop = sqIchimoku(symbol, timeframe, tenkanPeriod, kijunPeriod, senkouPeriod, 2, shift);
	double kumoBottom = sqIchimoku(symbol, timeframe, tenkanPeriod, kijunPeriod, senkouPeriod, 3, shift);
	if(kumoBottom > kumoTop) {
		double temp = kumoBottom;
		kumoBottom = kumoTop;
		kumoTop = temp;
	}
		
	bool signal;
	
	if(bullishOrBearish == -1) {
	   // bearish;
	   signal = (o > kumoBottom) && c < kumoBottom;
		
		return signal;
		
	} else if(bullishOrBearish == 1) { 
	   // bullish
	   signal = (o < kumoTop) && c > kumoTop;
		
		return signal;
		
	} else {
			return false;
	}
}

//+------------------------------------------------------------------+

double sqROC(string symbol, int timeframe, int period, int shift) {
   return roundValue(iCustom(symbol, timeframe, "SqROC", period, 0, shift));
}
