package com.strategyquant.tradinglib.project.websocket;

import org.json.JSONArray;
import org.json.JSONObject;

public class DataToSend {
   private String name;
   private JSONObject dataObject;
   private JSONArray dataArray;

   public DataToSend() {
   }

   public DataToSend(String var1) {
      this.name = var1;
   }

   public DataToSend(String var1, JSONObject var2) {
      this.name = var1;
      this.dataObject = var2;
   }

   public DataToSend(String var1, JSONArray var2) {
      this.name = var1;
      this.dataArray = var2;
   }

   public JSONObject getDataObject() {
      return this.dataObject;
   }

   public void setDataObject(JSONObject var1) {
      this.dataObject = var1;
   }

   public JSONArray getDataArray() {
      return this.dataArray;
   }

   public void setDataObject(JSONArray var1) {
      this.dataArray = var1;
   }

   public String getName() {
      return this.name;
   }

   public void setName(String var1) {
      this.name = var1;
   }
}
