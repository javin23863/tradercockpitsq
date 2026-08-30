package com.strategyquant.tradinglib.project.websocket;

import com.strategyquant.tradinglib.project.SQProject;
import com.strategyquant.tradinglib.project.websocket.channels.IProjectChannel;
import com.strategyquant.tradinglib.project.websocket.channels.ProjectChannels;
import java.util.ArrayList;
import java.util.HashMap;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProjectDataPublisher extends SynchronizedWebSocketPublisher {
   private static final Logger Log = LoggerFactory.getLogger(ProjectDataPublisher.class);
   public static final String PROJECT_DATA = "projectData";
   public static final String CHANNELS_DATA = "channels";
   public static final int ERROR_TYPE_GENERAL = 10;
   public static final int ERROR_TYPE_FATAL = 50;
   private SQProject project;
   private DataToSend data = new DataToSend("projectData");
   private JSONObject dataObj = new JSONObject();
   private HashMap<String, Object> lastChannelData = new HashMap<>();

   public ProjectDataPublisher(SQProject var1) {
      this.project = var1;
      this.data.setDataObject(this.dataObj);
   }

   @Override
   public DataToSend getData() {
      try {
         if (this.hasNewData()) {
            return this.data;
         }
      } catch (Exception var2) {
         Log.error("Project data publisher error: ", var2);
         this.project.printToLog("Project data publisher error - " + var2.getMessage());
      }

      return null;
   }

   @Override
   public void resetLastData() {
      this.lastChannelData.clear();
   }

   public void resetLastData(String var1) {
      this.lastChannelData.remove(var1);
   }

   private boolean hasNewData() {
      boolean var1 = false;
      this.dataObj.put("name", this.project.getName());
      this.dataObj.remove("channels");
      JSONArray var2 = null;
      ArrayList var3 = ProjectChannels.getChannels();
      if (var3 != null && var3.size() != 0) {
         boolean var4 = false;

         for (IProjectChannel var6 : var3) {
            String var7 = var6.getChannelName();
            if (ProjectFeeds.isSubscribed(this.project.getName(), var7)) {
               try {
                  Object var8 = var6.getData(this.project);
                  if (var8 != null && this.dataChanged(var7, var8)) {
                     JSONObject var9 = new JSONObject();
                     var9.put("name", var7);
                     var9.put("data", var8);
                     if (var2 == null) {
                        var2 = new JSONArray();
                     }

                     var2.put(var9);
                     var1 = true;
                     this.lastChannelData.put(var7, var8);
                  }
               } catch (Exception var10) {
                  Log.error(String.format("Error while getting data from '%s-%s'. Exc.", this.project.getName(), var7), var10);
               }
            }
         }

         if (var1) {
            this.dataObj.put("channels", var2);
         }

         return var1;
      } else {
         return false;
      }
   }

   private boolean dataChanged(String var1, Object var2) {
      Object var3 = this.lastChannelData.get(var1);
      if (var3 == null) {
         return true;
      } else if (var2 instanceof JSONObject && var3 instanceof JSONObject) {
         return !((JSONObject)var2).similar((JSONObject)var3);
      } else if (var2 instanceof JSONArray && var3 instanceof JSONArray) {
         return !((JSONArray)var2).similar((JSONArray)var3);
      } else {
         return var2 instanceof String && var3 instanceof String ? !var2.equals(var3) : true;
      }
   }
}
