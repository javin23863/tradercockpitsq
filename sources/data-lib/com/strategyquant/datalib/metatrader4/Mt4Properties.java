package com.strategyquant.datalib.metatrader4;

import com.strategyquant.lib.app.MainApp;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.json.JSONArray;
import org.json.JSONObject;

public class Mt4Properties {
   private Map<String, Mt4SymbolProperties> values = new HashMap<>();
   private static final String PropertiesPath = MainApp.getDataPath() + "//user//settings//DataConfig//mt4.properties";

   public Mt4Properties() throws IOException {
      this(PropertiesPath);
   }

   public Mt4Properties(String var1) throws IOException {
      try {
         this.readPropetiesFileCommon(var1);
      } catch (UncheckedIOException | IOException var3) {
         this.readPropertiesFileIgnoreInvalidCharacters(var1);
      }
   }

   private void readPropetiesFileCommon(String var1) throws IOException {
      Stream var2 = Files.lines(Paths.get(var1));

      try {
         var2.forEach(var1x -> this.storeLine(var1x));
      } catch (Throwable var6) {
         if (var2 != null) {
            try {
               var2.close();
            } catch (Throwable var5) {
               var6.addSuppressed(var5);
            }
         }

         throw var6;
      }

      if (var2 != null) {
         var2.close();
      }
   }

   private void readPropertiesFileIgnoreInvalidCharacters(String var1) throws IOException {
      Charset var2 = Charset.forName("UTF-8");
      CharsetDecoder var3 = var2.newDecoder();
      var3.onMalformedInput(CodingErrorAction.IGNORE);
      var3.onUnmappableCharacter(CodingErrorAction.IGNORE);
      BufferedReader var4 = new BufferedReader(new InputStreamReader(Files.newInputStream(Paths.get(var1)), var3));

      try {
         for (String var7 : var4.lines().collect(Collectors.toList())) {
            this.storeLine(var7);
         }
      } catch (Throwable var9) {
         try {
            var4.close();
         } catch (Throwable var8) {
            var9.addSuppressed(var8);
         }

         throw var9;
      }

      var4.close();
   }

   private void storeLine(String var1) {
      int var2 = var1.indexOf("_");
      if (var2 != -1) {
         String var3 = var1.substring(0, var2);
         String var4 = var1.substring(var3.length() + 1);
         String[] var5 = var4.split("=");
         if (var5.length == 2) {
            Mt4SymbolProperties var6 = this.values.get(var3);
            if (var6 == null) {
               var6 = new Mt4SymbolProperties();
               this.values.put(var3, var6);
            }

            var6.put(var5[0].toLowerCase(), var5[1]);
         }
      }
   }

   public JSONArray toJSON() {
      JSONArray var1 = new JSONArray();
      LinkedList var2 = new LinkedList<>(this.values.keySet());
      Collections.sort(var2);

      for (String var4 : var2) {
         Mt4SymbolProperties var5 = this.values.get(var4);
         JSONObject var6 = new JSONObject();
         var6.put("symbol", var4);
         var6.put("properties", var5.toJSON());
         var1.put(var6);
      }

      return var1;
   }

   public String findBySymbol(String var1) throws Exception {
      if (!this.values.containsKey(var1)) {
         throw new Exception(String.format("Symbol '%s' properties not found in '%s'", var1, PropertiesPath));
      } else {
         return this.values.get(var1).toString();
      }
   }
}
