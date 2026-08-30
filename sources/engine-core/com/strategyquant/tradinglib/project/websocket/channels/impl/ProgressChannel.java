package com.strategyquant.tradinglib.project.websocket.channels.impl;

import com.strategyquant.tradinglib.project.ProjectProgressInfo;
import com.strategyquant.tradinglib.project.SQProject;
import com.strategyquant.tradinglib.project.websocket.channels.IProjectChannel;
import org.json.JSONObject;

public class ProgressChannel implements IProjectChannel {
   @Override
   public Object getData(SQProject var1) {
      JSONObject var2 = null;
      ProjectProgressInfo var3 = var1.getProgress().get();
      if (var3 != null) {
         var2 = new JSONObject();
         var2.put("action", var3.action);
         var2.put("percent", var3.percent);
         var2.put("info", var3.info);
         if (var3.hasExtra) {
            var2.put(var3.extraName, var3.extraValue);
         }

         if (var3.error != null) {
            var2.put("error", var3.error);
         }
      }

      return var2;
   }

   @Override
   public String getChannelName() {
      return "progress-channel";
   }
}
