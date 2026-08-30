package com.strategyquant.webguilib.init;

import com.strategyquant.lib.SQUtils;
import com.strategyquant.webguilib.WebAppManager;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.LinkedHashMap;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PortChecker {
   private static final Logger Log = LoggerFactory.getLogger(PortChecker.class);
   public static final String UNKNOWN_APP = "Unknown";
   public static final String NO_APP = "Free";

   public static LinkedHashMap<Integer, String> mapPorts(int var0, int var1) {
      LinkedHashMap var2 = new LinkedHashMap();

      for (int var3 = var0; var3 <= var1; var3++) {
         if (!portAvailable(var3)) {
            var2.put(var3, tryGetAppCode(var3));
         } else {
            var2.put(var3, "Free");
         }
      }

      return var2;
   }

   public static boolean portAvailable(int var0) {
      Log.debug("-------------- Testing port " + var0);
      Socket var1 = null;

      try {
         var1 = new Socket();
         var1.setSoTimeout(50);
         var1.connect(new InetSocketAddress("localhost", var0), 50);
         Log.debug("--------------Port " + var0 + " is not available");
         return false;
      } catch (IOException var13) {
         return true;
      } finally {
         if (var1 != null) {
            try {
               var1.close();
            } catch (IOException var12) {
            }
         }
      }
   }

   private static String tryGetAppCode(int var0) {
      String var1 = WebAppManager.getBaseURL(var0) + "/main/getappcode";
      String var2 = null;

      try {
         var2 = SQUtils.httpGet(var1);
      } catch (Exception var6) {
      }

      if (var2 != null) {
         try {
            JSONObject var3 = new JSONObject(var2);
            return var3.get("appCode").toString();
         } catch (Exception var5) {
            return "Unknown";
         }
      } else {
         return "Unknown";
      }
   }
}
