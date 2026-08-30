package com.strategyquant.plugin.Connection.impl.MT4;

import com.jfx.ErrHistoryWillUpdated;
import com.jfx.ErrUnknownSymbol;
import com.jfx.Timeframe;
import com.strategyquant.datalib.ChartDef;
import com.strategyquant.datalib.data.io.VersatileData;
import com.strategyquant.tradinglib.connection.HistoryData;
import com.strategyquant.tradinglib.connection.IDataFeed;
import java.util.ArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MT4DataFeed implements IDataFeed {
   public static final Logger Log = LoggerFactory.getLogger("MT4DataFeed");
   private ArrayList<String> availableSymbols;
   private MT4Connection mt4Connection;
   private MT4Bridge mt4Bridge;

   public MT4DataFeed(MT4Connection var1, MT4Bridge var2) {
      this.mt4Connection = var1;
      this.mt4Bridge = var2;
   }

   public void registerSymbol(String var1) throws Exception {
      if (!this.checkSymbolIsSupported(var1)) {
         throw new Exception("Symbol " + var1 + " is not supported.");
      }

      this.mt4Bridge.registerSymbol(var1);
   }

   public boolean checkSymbolIsSupported(String var1) {
      this.loadAvailableSymbols();
      if (this.availableSymbols == null) {
         Log.error("Cannot load available symbols.");
         return false;
      }

      for (int var2 = 0; var2 < this.availableSymbols.size(); var2++) {
         if (this.availableSymbols.get(var2).equals(var1)) {
            return true;
         }
      }

      return false;
   }

   public HistoryData getHistoryData(ChartDef var1, int var2, long var3, long var5) {
      HistoryData var7 = new HistoryData(var1, var2, var3, var5);
      ArrayList var8 = new ArrayList();
      int var9 = -1;

      while (true) {
         try {
            long var10 = this.mt4Bridge.iTime(var1.getSymbol(), Timeframe.PERIOD_M1, ++var9).getTime();
            if (var10 <= var5) {
               if (var10 < var3) {
                  break;
               }

               VersatileData var12 = new VersatileData();
               var12.time = var10;
               var12.open = this.mt4Bridge.iOpen(var1.getSymbol(), Timeframe.PERIOD_M1, var9);
               var12.high = this.mt4Bridge.iHigh(var1.getSymbol(), Timeframe.PERIOD_M1, var9);
               var12.low = this.mt4Bridge.iLow(var1.getSymbol(), Timeframe.PERIOD_M1, var9);
               var12.close = this.mt4Bridge.iClose(var1.getSymbol(), Timeframe.PERIOD_M1, var9);
               var12.volume = this.mt4Bridge.iVolume(var1.getSymbol(), Timeframe.PERIOD_M1, var9);
               var8.add(var12);
            }
         } catch (ErrHistoryWillUpdated var13) {
            var8.remove(var8.size() - 1);
            Log.error("Cannot get historical bar with shift: " + var9, var13);
            break;
         } catch (ErrUnknownSymbol var14) {
            var8.remove(var8.size() - 1);
            Log.error("Cannot get historical bar. Unknown symbol: " + var1.getSymbol(), var14);
            break;
         }
      }

      for (int var15 = var8.size() - 1; var15 >= 0; var15--) {
         var7.addMinuteData(
            ((VersatileData)var8.get(var15)).time,
            ((VersatileData)var8.get(var15)).open,
            ((VersatileData)var8.get(var15)).high,
            ((VersatileData)var8.get(var15)).low,
            ((VersatileData)var8.get(var15)).close,
            ((VersatileData)var8.get(var15)).volume
         );
      }

      return var7;
   }

   private void loadAvailableSymbols() {
      if (this.mt4Bridge.isConnected() && this.availableSymbols == null) {
         this.availableSymbols = this.mt4Bridge.getSymbols();
      }
   }
}
