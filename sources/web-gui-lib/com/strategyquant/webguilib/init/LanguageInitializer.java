package com.strategyquant.webguilib.init;

import com.strategyquant.lib.SQUtils;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LanguageInitializer {
   public static final Logger Log = LoggerFactory.getLogger(LanguageInitializer.class);
   private static Map<String, String> languages = new HashMap<>();

   public static void clearLanguages() {
      languages.clear();
   }

   public static void append(String var0) throws Exception {
      String var1 = var0.substring(var0.lastIndexOf("/") + 1, var0.indexOf(".lng"));
      String var2 = SQUtils.fileToString(new File(var0));
      String var3 = var2.substring(0, var2.indexOf("\n") + 1);
      String var4 = "";
      if (languages.containsKey(var1)) {
         var4 = languages.get(var1);
      }

      if (var3.contains("#flag") && var4.contains("#flag")) {
         var2 = var2.substring(var3.length());
      }

      while (var4.endsWith(" ") || var4.endsWith("\t")) {
         var4 = var4.substring(0, var4.length() - 1);
      }

      languages.put(var1, var4 + var2);
   }

   public static void createLanguageFiles(String var0) throws IOException {
      for (String var4 : languages.keySet()) {
         String var2 = var0 + "/" + var4 + ".lng";
         Log.info("Creating merged language file '" + var2 + "'");
         PrintWriter var1 = new PrintWriter(new FileWriter(var2, false));
         var1.write(languages.get(var4));
         var1.flush();
         var1.close();
      }
   }
}
