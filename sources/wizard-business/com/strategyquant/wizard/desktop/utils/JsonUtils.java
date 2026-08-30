package com.strategyquant.wizard.desktop.utils;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class JsonUtils {
   public static String getErrorResonse(String var0) {
      return "\"result\":\"error\",\"errors\":{\"message\":" + var0 + "}";
   }

   public static String doPost(String var0, String var1) throws IOException {
      URL var2 = new URL(var0);
      HttpURLConnection var3 = (HttpURLConnection)var2.openConnection();
      var3.setRequestMethod("POST");
      var3.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
      var3.setRequestProperty("Content-Length", String.valueOf(var1.length()));
      var3.setDoOutput(true);
      DataOutputStream var4 = new DataOutputStream(var3.getOutputStream());
      var4.writeBytes(var1);
      var4.flush();
      var4.close();
      BufferedReader var5 = new BufferedReader(new InputStreamReader(var3.getInputStream(), StandardCharsets.UTF_8));
      StringBuffer var7 = new StringBuffer();

      String var6;
      while ((var6 = var5.readLine()) != null) {
         var7.append(var6);
      }

      var5.close();
      return var7.toString();
   }

   public static String doGet(String var0) throws IOException {
      URL var1 = new URL(var0);
      HttpURLConnection var2 = (HttpURLConnection)var1.openConnection();
      var2.setRequestMethod("GET");
      var2.setRequestProperty("User-Agent", "");
      BufferedReader var3 = new BufferedReader(new InputStreamReader(var2.getInputStream()));
      StringBuffer var5 = new StringBuffer();

      String var4;
      while ((var4 = var3.readLine()) != null) {
         var5.append(var4);
      }

      var3.close();
      return var5.toString();
   }
}
