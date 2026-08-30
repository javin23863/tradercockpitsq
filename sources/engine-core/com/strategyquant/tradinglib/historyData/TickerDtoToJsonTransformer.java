package com.strategyquant.tradinglib.historyData;

import com.strategyquant.datalib.historyData.dto.TickerDto;
import com.strategyquant.datalib.historyData.dto.TickerKind;
import com.strategyquant.lib.SQTime;
import com.strategyquant.lib.historyData.HistoryDataSubscription;
import com.strategyquant.tradinglib.transfomer.AbstractDtoToJsonTransformer;
import org.json.JSONArray;

public class TickerDtoToJsonTransformer extends AbstractDtoToJsonTransformer<TickerDto> {
   private TickerKind tickerKind;

   public TickerDtoToJsonTransformer(TickerKind var1) {
      this.tickerKind = var1;
   }

   protected Object objectToJson(TickerDto var1) {
      JSONArray var2 = new JSONArray();
      var2.put(var1.getTicker());
      var2.put(var1.getName() == null ? "" : var1.getName());
      var2.put(var1.getMarketName());
      if (this.tickerKind == TickerKind.STOCK) {
         var2.put(var1.getType());
      }

      var2.put(var1.getDateFrom() == null ? "" : SQTime.formatDateQuick(var1.getDateFrom().getTime()));
      boolean var3 = this.tickerKind == TickerKind.STOCK
         ? HistoryDataSubscription.getInstance().isAllowedEquity(var1.getTicker(), var1.isEod())
         : this.isAllowedFutures(var1);
      var2.put(var3);
      return var2;
   }

   private boolean isAllowedFutures(TickerDto var1) {
      return HistoryDataSubscription.getInstance().isAllowedFuture(var1.getTicker(), var1.getMarketName(), var1.isEod());
   }
}
