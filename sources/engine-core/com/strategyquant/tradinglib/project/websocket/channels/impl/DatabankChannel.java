package com.strategyquant.tradinglib.project.websocket.channels.impl;

import com.strategyquant.tradinglib.Databank;
import com.strategyquant.tradinglib.project.SQProject;
import com.strategyquant.tradinglib.project.websocket.channels.IProjectChannel;
import java.util.ArrayList;
import org.json.JSONArray;
import org.json.JSONObject;

public class DatabankChannel implements IProjectChannel {
   @Override
   public Object getData(SQProject var1) {
      ArrayList var2 = var1.getDatabanks().list();
      JSONArray var3 = null;

      for (int var4 = 0; var4 < var2.size(); var4++) {
         Databank var5 = (Databank)var2.get(var4);
         JSONArray var6 = var5.getChanges();
         if (var6 != null) {
            if (var3 == null) {
               var3 = new JSONArray();
            }

            JSONObject var7 = new JSONObject();
            var7.put("name", var5.getName());
            var7.put("records", var5.size());
            var7.put("changes", var6);
            var3.put(var7);
         }
      }

      return var3;
   }

   @Override
   public String getChannelName() {
      return "databanks-channel";
   }
}
