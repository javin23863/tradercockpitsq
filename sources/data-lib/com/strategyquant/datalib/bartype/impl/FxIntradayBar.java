package com.strategyquant.datalib.bartype.impl;

import com.strategyquant.datalib.TickEvent;
import com.strategyquant.datalib.bartype.BarType;
import com.strategyquant.datalib.bartype.BarTypeStatus;
import com.strategyquant.datalib.data.DataException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FxIntradayBar extends BarType {
   public static final Logger Log = LoggerFactory.getLogger("FxIntradayBar");

   public FxIntradayBar() {
      super(1, "Intraday");
   }

   @Override
   public void processTickImplementation(TickEvent var1, BarTypeStatus var2, int var3) throws DataException {
      long var4 = var1.getTime();
      var2.barTime = var4;
      if (var1.isBarOpen()) {
         var2.status = 1;
      } else {
         var2.status = 2;
      }
   }

   @Override
   public BarType clone(String var1) {
      return new FxIntradayBar();
   }

   @Override
   public BarType clone() {
      return new FxIntradayBar();
   }

   @Override
   public boolean isTickBar() {
      return false;
   }

   @Override
   public String checkCanBeComputedFrom(String var1) throws DataException {
      return null;
   }

   @Override
   public String getBaseTF() {
      return "Intraday";
   }

   @Override
   public String getTickTF() {
      return "Intraday";
   }

   @Override
   public long estimateStartDate(int var1, long var2) {
      return 0L;
   }

   @Override
   public boolean checkTimeframeIsSupported(String var1) {
      return var1.toUpperCase().equals("INTRADAY");
   }

   @Override
   public int getPeriodInMS() {
      return 0;
   }
}
