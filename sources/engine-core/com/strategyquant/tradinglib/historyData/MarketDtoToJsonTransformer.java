package com.strategyquant.tradinglib.historyData;

import com.strategyquant.model.IdName;
import com.strategyquant.tradinglib.transfomer.AbstractDtoToJsonTransformer;
import org.json.JSONObject;

public class MarketDtoToJsonTransformer extends AbstractDtoToJsonTransformer<IdName> {
   protected JSONObject objectToJson(IdName var1) {
      JSONObject var2 = new JSONObject();
      var2.put("id", var1.getId());
      var2.put("name", var1.getName());
      return var2;
   }
}
