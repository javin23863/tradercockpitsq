package com.strategyquant.tradinglib.task.settings;

public class SettingError {
   public String settingName;
   public String fieldName;
   public String errorMsg;

   public SettingError(String var1, String var2, String var3) {
      this.settingName = var1;
      this.fieldName = var2;
      this.errorMsg = var3;
   }
}
