package com.strategyquant.tradinglib.transfomer;

import java.util.List;
import org.json.JSONArray;

public abstract class AbstractDtoToJsonTransformer<T> {
   public JSONArray toJson(List<T> var1) {
      JSONArray var2 = new JSONArray();

      for (Object var4 : var1) {
         var2.put(this.objectToJson((T)var4));
      }

      return var2;
   }

   protected abstract Object objectToJson(T var1);
}
