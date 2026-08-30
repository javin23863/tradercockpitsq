//------------------------------------------------------------------

private double roundValue(double value){
    return round(value + 0.0000000001, 6);
}

//------------------------------------------------------------------

private int sqFixRanges(int value, int min, int max, int defaultVal) {
   if(value < min || value > max) {
      return defaultVal;
   }
   
   return value;
}

//------------------------------------------------------------------

private Object[] calculateIndicator(String functionName, String symbol, int timeframe, AppliedPrice applied_price, int shift, Object... params) throws JFException {
	try {
		instrument = this.getInstrument(symbol);
		period = this.getPeriod(timeframe);

		return this.indicators.calculateIndicator(instrument, period, new OfferSide[] {OfferSide.BID}, functionName, new IIndicators.AppliedPrice[] {applied_price}, params, shift);
	} catch(Exception e) {
        throw new JFException(String.format("Failed to calculate indicator '%s'. " + e.getMessage(), functionName), e);
    }
}

//------------------------------------------------------------------

private Double calculateIndicatorValue(String functionName, String symbol, int timeframe, AppliedPrice applied_price, int shift, Object... params) throws JFException {
	try {
		instrument = this.getInstrument(symbol);
		period = this.getPeriod(timeframe);

		//return (Double)this.indicators.calculateIndicator(instrument, period, new OfferSide[] {OfferSide.BID}, functionName, new IIndicators.AppliedPrice[] {applied_price}, params, shift)[0];
		return roundValue(((double[]) this.indicators.calculateIndicator(instrument, period, new OfferSide[] {OfferSide.BID}, functionName, new IIndicators.AppliedPrice[] {applied_price}, params, Filter.ALL_FLATS, shift+1, history.getStartTimeOfCurrentBar(instrument, period), 0)[0])[0]);
	} catch(Exception e) {
        throw new JFException(String.format("Failed to calculate indicator '%s'. " + e.getMessage(), functionName), e);
    }
}

//------------------------------------------------------------------

private Double calculateIndicatorLineValue(String functionName, String symbol, int timeframe, AppliedPrice applied_price, int line, int shift, Object... params) throws JFException {
	try {
		instrument = this.getInstrument(symbol);
		period = this.getPeriod(timeframe);

		//return (Double)this.indicators.calculateIndicator(instrument, period, new OfferSide[] {OfferSide.BID}, functionName, new IIndicators.AppliedPrice[] {applied_price}, params, shift)[0];
		return roundValue(((double[]) this.indicators.calculateIndicator(instrument, period, new OfferSide[] {OfferSide.BID}, functionName, new IIndicators.AppliedPrice[] {applied_price}, params, Filter.ALL_FLATS, shift+1, history.getStartTimeOfCurrentBar(instrument, period), 0)[line])[0]);
	} catch(Exception e) {
        throw new JFException(String.format("Failed to calculate indicator '%s'. " + e.getMessage(), functionName), e);
    }
}

//------------------------------------------------------------------

private Double calculateIndicatorLineValue(String functionName, String symbol, int timeframe, OfferSide[] offerSides, IIndicators.AppliedPrice[] inputTypes, int line, int shift, Object... params) throws JFException {
	try {
		instrument = this.getInstrument(symbol);
		period = this.getPeriod(timeframe);

		//return (Double)this.indicators.calculateIndicator(instrument, period, new OfferSide[] {OfferSide.BID}, functionName, new IIndicators.AppliedPrice[] {applied_price}, params, shift)[0];
		return roundValue(((double[]) this.indicators.calculateIndicator(instrument, period, offerSides, functionName, inputTypes, params, Filter.ALL_FLATS, shift+1, history.getStartTimeOfCurrentBar(instrument, period), 0)[line])[0]);
	} catch(Exception e) {
        throw new JFException(String.format("Failed to calculate indicator '%s'. " + e.getMessage(), functionName), e);
    }
}

//------------------------------------------------------------------

private IBar getBar(String symbol, int timeframe, int shift) throws JFException {
	instrument = this.getInstrument(symbol);
	period = this.getPeriod(timeframe);
	
    return history.getBars(instrument, period, OfferSide.BID, Filter.ALL_FLATS, shift+1, history.getStartTimeOfCurrentBar(instrument, period), 0).get(0);
}

//------------------------------------------------------------------

private double sqBarRange(String symbol, int timeframe, int shift) throws JFException {
	bar = getBar(symbol, timeframe, shift);
	
	return roundValue(bar.getHigh() - bar.getLow());
}

//------------------------------------------------------------------+

private double sqOpen(String symbol, int timeframe, int shift) throws JFException {
	bar = getBar(symbol, timeframe, shift);
	
	return bar.getOpen();
}   

//------------------------------------------------------------------

private double sqHigh(String symbol, int timeframe, int shift) throws JFException {
	bar = getBar(symbol, timeframe, shift);
	
	return bar.getHigh();
}

//------------------------------------------------------------------

private double sqLow(String symbol, int timeframe, int shift) throws JFException {
	bar = getBar(symbol, timeframe, shift);
	
	return bar.getLow();
}

//------------------------------------------------------------------

private double sqClose(String symbol, int timeframe, int shift) throws JFException {
	bar = getBar(symbol, timeframe, shift);
	
	return bar.getClose();
}

//------------------------------------------------------------------

private double sqTrueRange(String symbol, int timeframe, int shift) throws JFException {
   double close1 = sqClose(symbol, timeframe, shift+1);
   double high = sqHigh(symbol, timeframe, shift+1);
   double low = sqLow(symbol, timeframe, shift+1);

   double TrueHigh, TrueLow;

	if(close1 > high) {
		TrueHigh = close1;
	} else {
		TrueHigh = high;
	}

	if(close1 < low) {
		TrueLow = close1;
	} else {
		TrueLow = low;
	}

	double TrueRange = TrueHigh - TrueLow;
	
	return TrueRange;
}

//------------------------------------------------------------------

private double sqBiggestRange(String symbol, int timeframe, int period, int shift) throws JFException {
   double maxnum = -100000000;
   double range;

   for(int i=shift; i<shift+period; i++) {
      range = sqHigh(symbol, timeframe, i) - sqLow(symbol, timeframe, i);

      if(range > maxnum) {
         maxnum = range;
      }
   }

   return roundValue(maxnum);
}

//------------------------------------------------------------------

private double sqSmallestRange(String symbol, int timeframe, int period, int shift) throws JFException {
   double minnum = 100000000;
   double range;

   for(int i=shift; i<shift+period; i++) {
      range = sqHigh(symbol, timeframe, i) - sqLow(symbol, timeframe, i);

      if(range < minnum) {
         minnum = range;
      }
   }

   return roundValue(minnum);
}

//------------------------------------------------------------------

private double sqRSI(String symbol, int timeframe, int period, AppliedPrice applied_price, int shift) throws JFException {
   return this.calculateIndicatorValue("rsi", symbol, timeframe, applied_price, shift, period);
}

//------------------------------------------------------------------

private double sqROC(String symbol, int timeframe, int period, int shift) throws JFException {
   return this.calculateIndicatorValue("roc", symbol, timeframe, CLOSE, shift, period);
}

//------------------------------------------------------------------

private double sqCCI(String symbol, int timeframe, int period, AppliedPrice applied_price, int shift) throws JFException {
   return this.calculateIndicatorValue("cci", symbol, timeframe, applied_price, shift, period);
}

//------------------------------------------------------------------

private double sqMA(String symbol, int timeframe, int ma_period, int ma_shift, int ma_method, AppliedPrice applied_price, int shift) throws JFException {
   ma_method = sqFixRanges(ma_method, 0, 3, 0);
      
   switch(ma_method) {
      case MODE_SMA: return this.calculateIndicatorValue("SMA", symbol, timeframe, applied_price, shift, ma_period);
      case MODE_EMA: return this.calculateIndicatorValue("EMA", symbol, timeframe, applied_price, shift, ma_period);
      case MODE_SMMA: return this.calculateIndicatorValue("SMMA", symbol, timeframe, applied_price, shift, ma_period);
      case MODE_LWMA: return this.calculateIndicatorValue("LWMA", symbol, timeframe, applied_price, shift, ma_period);
   }

   return -1d;
}

//------------------------------------------------------------------

private double sqHullMovingAverage(String symbol, int timeframe, int period, AppliedPrice applied_price, int shift) throws JFException {
   return this.calculateIndicatorValue("hma", symbol, timeframe, applied_price, shift, period);
}

//+------------------------------------------------------------------+

private  double sqTEMA(String symbol, int timeframe, int ma_period, AppliedPrice applied_price, int shift) throws JFException {
   return this.calculateIndicatorValue("tema", symbol, timeframe, applied_price, shift, ma_period);
}

//+------------------------------------------------------------------+

private  double sqKama(String symbol, int timeframe, int timePeriod, int fastMAPeriod, int slowMAPeriod, int shift) throws JFException {
   return this.calculateIndicatorValue("kama", symbol, timeframe, MEDIAN_PRICE, shift, timePeriod, fastMAPeriod, slowMAPeriod);
}

//+------------------------------------------------------------------+

private boolean sqIchimokuSenkouSpanCross(int bullishOrBearish, String symbol, int timeframe, int tenkanPeriod, int kijunPeriod, int senkouPeriod, int shift, int signalStrength) throws JFException{
    double senkouSpanA1 = sqIchimoku(symbol, timeframe, tenkanPeriod, kijunPeriod, senkouPeriod, 2, shift+1);
    double senkouSpanA0 = sqIchimoku(symbol, timeframe, tenkanPeriod, kijunPeriod, senkouPeriod, 2, shift);
    double senkouSpanB1 = sqIchimoku(symbol, timeframe, tenkanPeriod, kijunPeriod, senkouPeriod, 3, shift+1);
    double senkouSpanB0 = sqIchimoku(symbol, timeframe, tenkanPeriod, kijunPeriod, senkouPeriod, 3, shift);
    double c = sqClose(symbol, timeframe, shift);
            
    double kumoTop = Math.max(senkouSpanA0,  senkouSpanB0); 
    double kumoBottom = Math.min(senkouSpanA0,  senkouSpanB0); 
        
    boolean signal;
    
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
            return false;
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

private  double sqIchimoku(String symbol, int timeframe, int tenkanPeriod, int kijunPeriod, int senkouPeriod, int line, int shift) throws JFException {
   //dukas doc - outputs are returned in the following order: 'Tenkan Sen', 'Ki-jun Sen', 'Chinkou Span', 'Senkou A', 'Senkou B', 'Cloud(Senkou A)', 'Cloud(Senkou B)'
   if(line==2) {
		line = 3;
   } else if(line==3) {
		line = 4;
   }	
   
   return this.calculateIndicatorLineValue("ichimoku", symbol, timeframe, null, line, shift, tenkanPeriod, kijunPeriod, senkouPeriod);
}

//+------------------------------------------------------------------+

private  double sqAroon(String symbol, int timeframe, int period, int line, int shift) throws JFException {
   //dukas doc - outputs are returned in the following order: 'Aroon Down', 'Aroon Up'
   if(line==0) {
		line = 1;
   } else if(line==1) {
		line = 0;
   }
   return this.calculateIndicatorLineValue("aroon", symbol, timeframe, null, line, shift, period);
}

//------------------------------------------------------------------

private double sqADX(String symbol, int timeframe, int period, int line, int shift) throws JFException {
	return this.calculateIndicatorLineValue("dmi", symbol, timeframe, null, line, shift, period);
}

private double sqATR(String symbol, int timeframe, int period, int shift) throws JFException {
	return this.calculateIndicatorValue("atr", symbol, timeframe, null, shift, period);
}

//------------------------------------------------------------------

private double sqAO(String symbol, int timeframe, int shift) throws JFException {
   return this.calculateIndicatorLineValue("awesome2", symbol, timeframe, MEDIAN_PRICE, 0, shift, 5, 0, 34, 0);
}

//------------------------------------------------------------------

private double sqBearsPower(String symbol, int timeframe, int period, AppliedPrice applied_price, int shift) throws JFException {
   return this.calculateIndicatorValue("bearp", symbol, timeframe, applied_price, shift, period);
}

//+------------------------------------------------------------------

private double sqBullsPower(String symbol, int timeframe, int period, AppliedPrice applied_price, int shift) throws JFException {
   return this.calculateIndicatorValue("bullp", symbol, timeframe, applied_price, shift, period);
}

//------------------------------------------------------------------

private double sqDeMarker(String symbol, int timeframe, int period, int shift) throws JFException {
   return this.calculateIndicatorLineValue("td_i", symbol, timeframe, null, 0, shift, period);
}

//------------------------------------------------------------------

private double sqMACD(String symbol, int timeframe, int fast_ema_period, int slow_ema_period, int signal_period, AppliedPrice applied_price, int mode, int shift) throws JFException {
   return this.calculateIndicatorLineValue("macd", symbol, timeframe, applied_price, mode, shift, fast_ema_period, slow_ema_period, signal_period);
} 

//+------------------------------------------------------------------+

private double sqMomentum(String symbol, int timeframe, int period, AppliedPrice applied_price, int shift) throws JFException {
   return this.calculateIndicatorValue("mom", symbol, timeframe, applied_price, shift, period);
}

//----------------------------------------------------------------------------

private double sqLinReg(String symbol, int timeframe, int period, AppliedPrice applied_price, int shift) throws JFException {
   return this.calculateIndicatorValue("linearReg", symbol, timeframe, applied_price, shift, period);
}

//------------------------------------------------------------------

private double sqPivots(String symbol, int timeframe, int startHour, int startMinute, int line, int shift) throws JFException {
   //SQ    P, R1, R2, R3, S1, S2, S3 
   //jforex P, R1, S1, R2, S2, R3, S3
	
   switch(line) {
      case 2: line = 3; break; //R2
      case 3: line = 5; break; //R3
	  case 4: line = 2; break; //S1
	  case 5: line = 4; break; //S2
   }
	
   return this.calculateIndicatorLineValue("pivot2", symbol, timeframe, null, line, shift);
}

//------------------------------------------------------------------

private double sqFibo(String symbol, int timeframe, int fiboRange, double fiboLevel) throws JFException {
   int line = 0;  //jforex P, R1, S1, R2, S2, R3, S3
   //todo - compute line from fiboLevel parameter

   return this.calculateIndicatorLineValue("fibPivot2", symbol, timeframe, null, line, 0);
}

//------------------------------------------------------------------

private double sqKeltnerChannel(String symbol, int timeframe, int period, double deviation, int line, int shift) throws JFException {
   if(line==1) line = 2; //dukas doc - outputs are returned in the following order: 'Keltner Channel Up', 'Keltner Channel Middle', 'Keltner Channel Low'
   //return this.calculateIndicatorLineValue("keltner", symbol, timeframe, null, line, shift, period);
   return this.calculateIndicatorLineValue("kbands", symbol, timeframe, new OfferSide[] {OfferSide.BID, OfferSide.BID}, new IIndicators.AppliedPrice[] {TYPICAL_PRICE, TYPICAL_PRICE}, line, shift, period, period, deviation);
}

//----------------------------------------------------------------------------

private double sqWPR(String symbol, int timeframe, int period, int shift) throws JFException {
   return this.calculateIndicatorValue("willr", symbol, timeframe, null, shift, period);
}

//------------------------------------------------------------------

private double sqStochastic(String symbol, int timeframe, int Kperiod, int Dperiod, int slowing, int method, int price_field, int mode, int shift) throws JFException {
   price_field = sqFixRanges(price_field, 0, 1, 0);
   method = sqFixRanges(method, 0, 3, 0);
   
   //return this.calculateIndicatorLineValue("stochP", symbol, timeframe, null, mode, shift, Kperiod, slowing, method, Dperiod, method, price_field);
   return this.calculateIndicatorLineValue("stoch", symbol, timeframe, null, mode, shift, Kperiod, slowing, method, Dperiod, method);
} 

private double sqOsMA(String symbol, int timeframe, int fast_ema_period, int slow_ema_period, int signal_period, AppliedPrice applied_price, int shift) throws JFException {
   return this.calculateIndicatorValue("osma", symbol, timeframe, applied_price, shift, fast_ema_period, slow_ema_period, signal_period);
}

//------------------------------------------------------------------

private double sqBands(String symbol, int timeframe, int period, double deviation, int bands_shift, AppliedPrice applied_price, int mode, int shift) throws JFException {
   if(mode==1) mode = 2; //dukas doc - outputs are returned in the following order: 'Upper Band', 'Middle Band', 'Lower Band'
   return this.calculateIndicatorLineValue("bbands", symbol, timeframe, applied_price, mode, shift, period, deviation, deviation, 0);
}

//------------------------------------------------------------------

private double sqBBWidthRatio(String symbol, int timeframe, int period, double deviation, AppliedPrice applied_price, int shift) throws JFException {
   return this.calculateIndicatorLineValue("bbandwidth", symbol, timeframe, applied_price, 0, shift, period, deviation);
}

//------------------------------------------------------------------

private  double sqBBRange(String symbol, int timeframe, int period, double deviation, AppliedPrice applied_price, int shift) throws JFException {
   return roundValue(sqBands(symbol, timeframe, period, deviation, 0, applied_price, 1, shift) - sqBands(symbol, timeframe, period, deviation, 0, applied_price, 2, shift));
}

//------------------------------------------------------------------

private double sqSAR(String symbol, int timeframe, double step, double maximum, int shift) throws JFException {
   return this.calculateIndicatorValue("sar", symbol, timeframe, null, shift, step, maximum);
}

//------------------------------------------------------------------

private double sqStdDev(String symbol, int timeframe, int ma_period, int ma_shift, int ma_method, AppliedPrice applied_price, int shift) throws JFException {
   return this.calculateIndicatorValue("stdDev", symbol, timeframe, applied_price, shift, ma_period, 1d);
}

//------------------------------------------------------------------


private double sqFractal(String symbol, int timeframe, int fractal, int bufferIndex, int shift) throws JFException {
    double high1 = sqHigh("Current", timeframe, 1 + shift);
    double high2 = sqHigh("Current", timeframe, 2 + shift);
    double high3 = sqHigh("Current", timeframe, 3 + shift);
    double high4 = sqHigh("Current", timeframe, 4 + shift);
    double high5 = sqHigh("Current", timeframe, 5 + shift);
    double low1 = sqLow("Current", timeframe, 1 + shift);
    double low2 = sqLow("Current", timeframe, 2 + shift);
    double low3 = sqLow("Current", timeframe, 3 + shift);
    double low4 = sqLow("Current", timeframe, 4 + shift);
    double low5 = sqLow("Current", timeframe, 5 + shift);
    
    if(fractal == 3 && bufferIndex == 1 && low2 < low1 && low2 < low3){
        return low2;
    } else if(fractal == 5 && bufferIndex == 1 && low3 < low1 && low3 < low2 && low3 < low4 && low3 < low5){
        return low3;
    } else if(fractal == 3 && bufferIndex == 0 && high2 > high1 && high2 > high3){
        return high2;
    } else if(fractal == 5 && bufferIndex == 0 && high3 > high1 && high3 > high2 && high3 > high4 && high3 > high5){
        return high3;
    }
    return 0;
}

//----------------------------------------------------------------------------

private double sqIndicatorHighest(int period, int nthValue, String indicatorIdentification) throws JFException {
   if(period > 1000) {
        print("Period used for sqIndicatorHighest function is too high. Max value is 1000");
        period = 1000;
   }
   
   if(nthValue < 0 || nthValue >= period) {
	   return(-1);
   }
   
   Double[] indicatorValues = new Double[1000];
   int i;

   for(i=0; i<1000; i++) {
      indicatorValues[i] = -2147483647d;
   }

   for(i=0; i<period; i++) {
      indicatorValues[i] = sqGetIndicatorByIdentification(indicatorIdentification, i);
   }

   Arrays.sort(indicatorValues, Comparator.reverseOrder());

   if(nthValue < 0 || nthValue >= period) {
      return(-1);
   }

   return indicatorValues[nthValue];
}

//----------------------------------------------------------------------------

private double sqIndicatorLowest(int period, int nthValue, String indicatorIdentification) throws JFException {      
   if(period > 1000) {
        print("Period used for sqIndicatorLowest function is too high. Max value is 1000");
        period = 1000;
   }
   
   if(nthValue < 0 || nthValue >= period) {
	   return(-1);
   }
   
   Double[] indicatorValues = new Double[1000];
   int i;

   for(i=0; i<1000; i++) {
      indicatorValues[i] = 2147483647d;
   }

   for(i=0; i<period; i++) {
      indicatorValues[i] = sqGetIndicatorByIdentification(indicatorIdentification, i);
   }

   Arrays.sort(indicatorValues);

   if(nthValue < 0 || nthValue >= period) {
      return(-1);
   }

   return indicatorValues[nthValue];
}

//----------------------------------------------------------------------------

private double sqIndicatorAverage(int period, int maMethod, String indicatorIdentification) throws JFException {
	//todo Moving Average method (SMA, EMA, SMMA, LWMA)
   double sum = 0;
	
   for(int i=0; i<period; i++) {
      sum+= sqGetIndicatorByIdentification(indicatorIdentification, i);
   }

   return roundValue(sum / period);
}

//----------------------------------------------------------------------------

private boolean sqIsRising(String indicatorIdentification, int bars, boolean allowSameValues, int shift) throws JFException {
   boolean atLeastOnce = false;

   double previousValue = round(sqGetIndicatorByIdentification(indicatorIdentification, bars+shift-1), 6);

   for(int i=1; i<bars; i++) {
      double currentValue = round(sqGetIndicatorByIdentification(indicatorIdentification, bars+shift-1-i), 6);

      if(currentValue < previousValue) {
         // indicator was falling
         return false;
      }
      if(currentValue == previousValue && allowSameValues == false) {
         // indicator was the same, not allowed
         return false;
      }
      if(currentValue > previousValue) {
         atLeastOnce = true;
      }

      previousValue = currentValue;
   }

   // indicator was not rising once
   return atLeastOnce;
}

//----------------------------------------------------------------------------

private boolean sqIsFalling(String indicatorIdentification, int bars, boolean allowSameValues, int shift) throws JFException {
   boolean atLeastOnce = false;

   double previousValue = round(sqGetIndicatorByIdentification(indicatorIdentification, bars+shift-1), 6);

   for(int i=1; i<bars; i++) {
      double currentValue = round(sqGetIndicatorByIdentification(indicatorIdentification, bars+shift-1-i), 6);

      if(currentValue > previousValue) {
         // indicator was rising
         return false;
      }
      if(currentValue == previousValue && allowSameValues == false) {
         // indicator was the same, not allowed
         return false;
      }
      if(currentValue < previousValue) {
         atLeastOnce = true;
      }

      previousValue = currentValue;
   }

   // indicator was not falling once
   return atLeastOnce;
}

//------------------------------------------------------------------

private double sqHeikenAshi(String symbol, int timeframe, AppliedPrice mode, int shift) throws JFException {
	if(mode == OPEN) 	return roundValue(sqHeikenAshi(symbol, timeframe, 0, shift));
	if(mode == CLOSE)	return roundValue(sqHeikenAshi(symbol, timeframe, 1, shift));
	if(mode == HIGH)	return roundValue(Math.max(sqHeikenAshi(symbol, timeframe, 2, shift), sqHeikenAshi(symbol, timeframe, 3, shift)));
	if(mode == LOW)		return roundValue(Math.min(sqHeikenAshi(symbol, timeframe, 2, shift), sqHeikenAshi(symbol, timeframe, 3, shift)));

   return -1;
}

//------------------------------------------------------------------

private double sqHeikenAshi(String symbol, int timeframe, int line, int shift) throws JFException {
	 return this.calculateIndicatorLineValue("heikinAshiLines", symbol, timeframe, null, line, shift);
}

//------------------------------------------------------------------

private double sqAvgVolume(String symbol, int timeframe, int period, int shift) throws JFException {
   return this.calculateIndicatorValue("sqAvgVolume", symbol, timeframe, null, shift, period);
}

//------------------------------------------------------------------

private double sqVortex(String symbol, int timeframe, int period, int line, int shift) throws JFException {
	 return this.calculateIndicatorLineValue("vortex", symbol, timeframe, null, line, shift, period);
}

//------------------------------------------------------------------

private double sqGannHiLo(String symbol, int timeframe, int period, int line, int shift) throws JFException {
	return this.calculateIndicatorLineValue("gann_hilo", symbol, timeframe, null, line, shift, period);
}

//------------------------------------------------------------------
// Candle Pattern functions
//------------------------------------------------------------------

private boolean sqBearishEngulfing(String symbol, int timeframe, int shift) throws JFException {
   double O = sqOpen(symbol, timeframe, shift);
   double O1 = sqOpen(symbol, timeframe, shift+1);
   double C = sqClose(symbol, timeframe, shift);
   double C1 = sqClose(symbol, timeframe, shift+1);

   double ocDiff = roundValue(O-C);
   double o1c1Diff = roundValue(C1-O1);

   if ((C1>O1)&&(O>C)&&(O>=C1)&&(O1>=C)&&(ocDiff>o1c1Diff)) {
      return true;
   }

   return false;
}

//------------------------------------------------------------------

private boolean sqBullishEngulfing(String symbol, int timeframe, int shift) throws JFException {
   double O = sqOpen(symbol, timeframe, shift);
   double O1 = sqOpen(symbol, timeframe, shift+1);
   double C = sqClose(symbol, timeframe, shift);
   double C1 = sqClose(symbol, timeframe, shift+1);

   double coDiff = roundValue(C-O);
   double o1c1Diff = roundValue(O1-C1);
   
   if ((O1>C1)&&(C>O)&&(C>=O1)&&(C1>=O)&&(coDiff>o1c1Diff)) {
      return true;
   }

   return false;
}

//------------------------------------------------------------------

private boolean sqDarkCloudCover(String symbol, int timeframe, int shift) throws JFException {
   instrument = this.getInstrument(symbol);
   
   double L = sqLow(symbol, timeframe, shift);
   double H = sqHigh(symbol, timeframe, shift);

   double O = sqOpen(symbol, timeframe, shift);
   double O1 = sqOpen(symbol, timeframe, shift+1);
   double C = sqClose(symbol, timeframe, shift);
   double C1 = sqClose(symbol, timeframe, shift+1);
   
 	double tickSize = instrument.getPipValue();

 	double Piercing_Line_Ratio = 0.5f;
 	double Piercing_Candle_Length = 10.0f;
 	
 	double HL = roundValue(H-L);
 	double OC = roundValue(O-C);
 	double OC_HL = HL != 0 ? roundValue(OC/HL) : 0;
 	double O1C1_D2 = roundValue((O1+C1)/2);
 	double PCL_MTS = roundValue(Piercing_Candle_Length*tickSize);
 			
 	if(C1 > O1 && O1C1_D2 > C && O > C && C > O1 && OC_HL > Piercing_Line_Ratio && HL >= PCL_MTS) {
 		return true;
 	}

   return false;
}

//------------------------------------------------------------------

private boolean sqDoji(String symbol, int timeframe, int shift) throws JFException {
   instrument = this.getInstrument(symbol);
   
   double diff = roundValue(Math.abs(sqOpen(symbol, timeframe, shift) - sqClose(symbol, timeframe, shift)));
   double coef = roundValue(instrument.getPipValue() * 0.6); 
   
   if(diff < coef) {
      return true;
   }
   
   return false;
}

//------------------------------------------------------------------

private boolean sqHammer(String symbol, int timeframe, int shift) throws JFException {
   instrument = this.getInstrument(symbol);
   
   double H = sqHigh(symbol, timeframe, shift);
   double L = sqLow(symbol, timeframe, shift);
   double L1 = sqLow(symbol, timeframe, shift+1);
   double L2 = sqLow(symbol, timeframe, shift+2);
   double L3 = sqLow(symbol, timeframe, shift+3);

   double O = sqOpen(symbol, timeframe, shift);
   double C = sqClose(symbol, timeframe, shift);
   double CL = H-L;

   double BodyLow, BodyHigh;
   double Candle_WickBody_Percent = 0.9;
   double CandleLength = 12;

   if (O > C) {
      BodyHigh = O;
      BodyLow = C;
   } else {
      BodyHigh = C;
      BodyLow = O;
   }

   double LW = roundValue(BodyLow - L);
   double UW = roundValue(H - BodyHigh);
   double BLa = roundValue(Math.abs(O - C));
   double BL90 = roundValue(BLa * Candle_WickBody_Percent);
   
   double pipValue = instrument.getPipValue();
   
   double LW_D2 = roundValue(LW / 2);
   double LW_D3 = roundValue(LW / 3);
   double LW_D4 = roundValue(LW / 4);
   double BL90_M2 = roundValue(2 * BL90);
   double CL_MPV = roundValue(CandleLength * pipValue);
     
   if(L <= L1 && L < L2 && L < L3)  {
 		if(LW_D2 > UW && LW > BL90_M2 && CL >= CL_MPV && O != C && LW_D3 <= UW && LW_D4 <= UW)  {
    	  	return true;
      }
      if(LW_D3 > UW && LW > BL90_M2 && CL >= CL_MPV && O != C && LW_D4 <= UW)  {
      	return true;
      }
      if(LW_D4 > UW && LW > BL90_M2 && CL >= CL_MPV && O != C)  {
    	  	return true;
      }
   }
   
   return false;
}

//------------------------------------------------------------------

private boolean sqPiercingLine(String symbol, int timeframe, int shift) throws JFException {
   instrument = this.getInstrument(symbol);
   
   double L = sqLow(symbol, timeframe, shift);
   double H = sqHigh(symbol, timeframe, shift);

   double O = sqOpen(symbol, timeframe, shift);
   double O1 = sqOpen(symbol, timeframe, shift+1);
   double C = sqClose(symbol, timeframe, shift);
   double C1 = sqClose(symbol, timeframe, shift+1);
   
 	double tickSize = instrument.getPipValue();

 	double Piercing_Line_Ratio = 0.5f;
 	double Piercing_Candle_Length = 10.0f;
 	
 	double HL = roundValue(H-L);
 	double CO = roundValue(C-O);
 	double CO_HL = HL != 0 ? roundValue(CO/HL) : 0;
 	double O1C1_D2 = roundValue((O1+C1)/2);
 	double PCL_MTS = roundValue(Piercing_Candle_Length*tickSize);
 			
 	if(C1 < O1 && O1C1_D2 < C && O < C && C < O1 && CO_HL > Piercing_Line_Ratio && HL >= PCL_MTS) {
 		return true;
 	}

   return false;
}

//------------------------------------------------------------------

private boolean sqShootingStar(String symbol, int timeframe, int shift) throws JFException {
   instrument = this.getInstrument(symbol);
   
   double L = sqLow(symbol, timeframe, shift);
   double H = sqHigh(symbol, timeframe, shift);
   double H1 = sqHigh(symbol, timeframe, shift + 1);
   double H2 = sqHigh(symbol, timeframe, shift + 2);
   double H3 = sqHigh(symbol, timeframe, shift + 3);

   double O = sqOpen(symbol, timeframe, shift);
   double C = sqClose(symbol, timeframe, shift);
   double CL = roundValue(H - L);

   double BodyLow, BodyHigh;
   double Candle_WickBody_Percent = 0.9;
   double CandleLength = 12;

   if (O > C) {
      BodyHigh = O;
      BodyLow = C;
   } else {
      BodyHigh = C;
      BodyLow = O;
   }

   double LW = roundValue(BodyLow - L);
   double UW = roundValue(H - BodyHigh);
   double BLa = roundValue(Math.abs(O - C));
   double BL90 = roundValue(BLa * Candle_WickBody_Percent);
   
   double pipValue = instrument.getPipValue();
   
   double UW_D2 = roundValue(UW / 2);
   double UW_D3 = roundValue(UW / 3);
   double UW_D4 = roundValue(UW / 4);
   double BL90_M2 = roundValue(2 * BL90);
   double CL_MPV = roundValue(CandleLength * pipValue);

   if(H >= H1 && H > H2 && H > H3)  {
      if(UW_D2 > LW && UW > BL90_M2 && CL >= CL_MPV && O != C && UW_D3 <= LW && UW_D4 <= LW)  {
         return true;
      }
      if(UW_D3 > LW && UW > BL90_M2 && CL >= CL_MPV && O != C && UW_D4 <= LW)  {
         return true;
      }
      if(UW_D4 > LW && UW > BL90_M2 && CL >= CL_MPV && O != C)  {
         return true;
      }
   }

   return false;
} 

//----------------------------------------------------------------------------

private boolean sqIsGreaterCount(String indicatorIdentificationLeft, String indicatorIdentificationRight, int bars, boolean NotStrict,int shift) throws JFException {

   	boolean atLeastOnce = false;

	for(int i=0; i<bars; i++) {

		double leftIndicator = round(sqGetIndicatorByIdentification(indicatorIdentificationLeft,shift+i), 5); /// precision = 5. It returns more acccurate backtest synchronisation
		double rightIndicator = round(sqGetIndicatorByIdentification(indicatorIdentificationRight,shift+i), 5);
		
		if(leftIndicator<rightIndicator){
			return false;
		}
		if(leftIndicator==rightIndicator && NotStrict == false){
			return false;
		}
		if(leftIndicator>rightIndicator){
			atLeastOnce = true;
		}
	}
	return atLeastOnce;
}

//----------------------------------------------------------------------------

private boolean sqIsLowerCount(String indicatorIdentificationLeft, String indicatorIdentificationRight, int bars, boolean NotStrict,int shift) throws JFException {

   	boolean atLeastOnce = false;

	for(int i=0; i<bars; i++) {

		double leftIndicator = round(sqGetIndicatorByIdentification(indicatorIdentificationLeft,shift+i), 5); /// precision = 5. It returns more acccurate backtest synchronisation
		double rightIndicator = round(sqGetIndicatorByIdentification(indicatorIdentificationRight,shift+i), 5);
		
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
	return atLeastOnce;
}