package com.strategyquant.wizard.desktop.loader;

import com.strategyquant.lib.app.MainApp;
import com.strategyquant.wizard.desktop.utils.JsonUtils;
import java.awt.Component;
import java.io.IOException;

public class ErrorReportSender {
   private Component parent;

   public ErrorReportSender(Component var1) {
      this.parent = var1;
   }

   public boolean send(String var1, String var2) {
      String var3 = "http://wizard.strategyquant.com/backend/errorReport";
      String var4 = "email=" + var1 + "&strategy=" + var2;

      try {
         JsonUtils.doPost(var3, var4);
         return true;
      } catch (IOException var6) {
         MainApp.showErrorDialog("Cannot connect to internet");
         return false;
      }
   }

   public void dispose() {
      this.parent = null;
   }
}
