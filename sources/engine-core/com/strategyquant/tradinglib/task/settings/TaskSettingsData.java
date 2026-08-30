package com.strategyquant.tradinglib.task.settings;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TaskSettingsData {
   public static final Logger Log = LoggerFactory.getLogger("TaskSettingsData");
   private HashMap<String, Serializable> params = new HashMap<>();
   private ArrayList<SettingError> errors = new ArrayList<>();

   public void clear() {
      this.params.clear();
      this.errors.clear();
   }

   public void addError(SettingError var1) {
      this.errors.add(var1);
   }

   public void addError(String var1, String var2, String var3) {
      this.addError(new SettingError(var1, var2, var3));
   }

   public void addParam(String var1, Serializable var2) throws Exception {
      if (this.params.containsKey(var1)) {
         Log.debug("Duplicate key '" + var1 + "'");
      }

      this.params.put(var1, var2);
   }

   public void addParamUnchecked(String var1, Serializable var2) {
      this.params.put(var1, var2);
   }

   public boolean hasErrors() {
      return !this.errors.isEmpty();
   }

   public HashMap<String, Serializable> getParams() {
      return this.params;
   }

   public ArrayList<SettingError> getErrors() {
      return this.errors;
   }
}
