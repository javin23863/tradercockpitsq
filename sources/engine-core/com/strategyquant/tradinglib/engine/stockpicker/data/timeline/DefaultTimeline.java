package com.strategyquant.tradinglib.engine.stockpicker.data.timeline;

import com.strategyquant.datalib.basket.BasketDto;
import com.strategyquant.datalib.basket.BasketOfStocksManager;
import com.strategyquant.lib.L;
import com.strategyquant.tradinglib.project.StopPauseEngine;

public class DefaultTimeline extends PickerDataTimeline {
   private static DefaultTimeline instance;
   public boolean generated = false;

   public static synchronized DefaultTimeline getInstance() {
      if (instance == null) {
         instance = new DefaultTimeline();
      }

      return instance;
   }

   public void generate() throws Exception {
      String var1 = "[[S&P 500]]";
      Log.info("Generating default timeline ...");
      BasketDto var2 = BasketOfStocksManager.getInstance().getBasket(var1);
      if (var2 == null) {
         throw new Exception(L.t("Failed to generate default timeline. Stock group %s not found.", new Object[]{var1}));
      }

      this.init(var2, new StopPauseEngine());
      this.generated = true;
   }
}
