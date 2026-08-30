package com.strategyquant.datalib.data;

import com.strategyquant.datalib.historyData.dto.TickerDto;

public interface InstrumentValueEvaluator {
   double getPointValue(TickerDto var1);

   double getTickStep(TickerDto var1);

   double getTickSize(TickerDto var1);

   byte getInstrumentType(TickerDto var1);

   String getDescriptions(TickerDto var1);

   double getOrderSizeMultiplier(TickerDto var1);

   double getOrderSizeStep(TickerDto var1);
}
