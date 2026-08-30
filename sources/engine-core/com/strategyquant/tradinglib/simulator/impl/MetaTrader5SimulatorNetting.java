package com.strategyquant.tradinglib.simulator.impl;

import com.strategyquant.datalib.session.SessionStatus;
import com.strategyquant.lib.SQTime;
import com.strategyquant.tradinglib.simulator.ITradingSimulator;

public class MetaTrader5SimulatorNetting extends MetaTraderSimulatorNetting {
   private byte executionType;
   private SessionStatus marketOpenSessionStatus = new SessionStatus();

   public MetaTrader5SimulatorNetting(byte var1) {
      this.executionType = var1;
   }

   @Override
   public ITradingSimulator clone() {
      MetaTrader5SimulatorNetting var1 = new MetaTrader5SimulatorNetting(this.executionType);
      var1.setTestPrecision(this.getTestPrecision());
      return var1;
   }

   @Override
   protected boolean checkPriceLevelCorrectness(int var1, double var2, double var4) {
      if (var1 != 5 && var1 != 4) {
         if (this.executionType == 4) {
            if (var2 < var4) {
               return false;
            }
         } else if (var2 <= var4) {
            return false;
         }
      } else if (this.executionType == 4) {
         if (var2 > var4) {
            return false;
         }
      } else if (var2 >= var4) {
         return false;
      }

      return true;
   }

   @Override
   public boolean fillAtRealAskBidPrice() {
      return this.getTestPrecision() == 3 || this.getTestPrecision() == 4;
   }

   @Override
   protected double getSLPTFilledPrice(double var1, double var3) {
      return this.getTestPrecision() != 3 && this.getTestPrecision() != 4 ? var3 : var1;
   }

   @Override
   public int getEngineId() {
      return 1441180233;
   }

   @Override
   public boolean IsMarketOpen() {
      if (this.marketOpenSession == null) {
         return true;
      }

      long var1 = this.tickData.getTime();
      String var3 = SQTime.toDateMinuteString(var1);
      this.marketOpenSession.checkTimeIsInSession(var1, this.marketOpenSessionStatus, null);
      Log.debug("MT sim check marketopen - time: {}, market open: {}", var3, this.marketOpenSessionStatus.isInSession);
      return this.marketOpenSessionStatus.isInSession;
   }
}
