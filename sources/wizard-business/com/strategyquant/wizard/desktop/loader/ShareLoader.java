package com.strategyquant.wizard.desktop.loader;

import com.strategyquant.wizard.desktop.utils.JsonUtils;
import java.io.IOException;

public class ShareLoader {
   public String share(String var1, String var2, String var3, String var4) {
      String var5 = "http://wizard.strategyquant.com/backend/share";
      String var6 = "s=" + var3 + "&filename=" + var1 + "&type=" + var2 + var4;

      try {
         return JsonUtils.doPost(var5, var6);
      } catch (IOException var8) {
         return JsonUtils.getErrorResonse(var8.getMessage());
      }
   }

   public String verifyShare(String var1) {
      String var2 = "http://wizard.strategyquant.com/backend/verifyusers?" + var1;

      try {
         return JsonUtils.doGet(var2);
      } catch (IOException var4) {
         return JsonUtils.getErrorResonse(var4.getMessage());
      }
   }

   public String notifyShare(String var1, String var2) {
      String var3 = "http://wizard.strategyquant.com/backend/notifyshare?id=" + var1 + "&s=" + var2;

      try {
         return JsonUtils.doGet(var3);
      } catch (IOException var5) {
         return JsonUtils.getErrorResonse(var5.getMessage());
      }
   }

   public String loadShared(String var1, String var2) {
      String var3 = "http://wizard.strategyquant.com/backend/loadshared?id=" + var1 + "&s=" + var2;

      try {
         return JsonUtils.doGet(var3);
      } catch (IOException var5) {
         return JsonUtils.getErrorResonse(var5.getMessage());
      }
   }

   public String saveShared(String var1, String var2, String var3) {
      String var4 = "http://wizard.strategyquant.com/backend/saveshared";
      String var5 = "content=" + var2 + "&id=" + var1 + "&s=" + var3;

      try {
         return JsonUtils.doPost(var4, var5);
      } catch (IOException var7) {
         return JsonUtils.getErrorResonse(var7.getMessage());
      }
   }
}
