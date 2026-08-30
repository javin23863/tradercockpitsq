package com.strategyquant.datalib.instrument;

import com.strategyquant.datalib.data.DataException;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.constants.SQPaths;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AliasManager {
   private static final Logger Log = LoggerFactory.getLogger("AliasManager");
   private static final String filePath = SQPaths.settingsDirPath + "/aliases.txt";
   private static final String lineDelimiter = "\n";
   private static final String valueDelimiter = "#";
   private static AliasManager instance;
   private ArrayList<InstrumentAlias> aliases;

   private AliasManager() {
      this.load();
      instance = this;
   }

   private static synchronized AliasManager get() {
      return instance != null ? instance : new AliasManager();
   }

   public static ArrayList<InstrumentAlias> getAliases() {
      return get().aliases;
   }

   public static InstrumentAlias getAlias(String var0) {
      for (int var1 = 0; var1 < get().aliases.size(); var1++) {
         InstrumentAlias var2 = get().aliases.get(var1);
         if (var2.alias.equals(var0)) {
            return var2;
         }
      }

      return null;
   }

   public static boolean checkAliasExists(String var0) {
      return getAlias(var0) != null;
   }

   public static String getAliasInstrument(String var0) {
      InstrumentAlias var1 = getAlias(var0);
      return var1 != null ? var1.instrument : null;
   }

   public static ArrayList<InstrumentAlias> listInstrumentAliases(String var0) {
      ArrayList var1 = new ArrayList();

      for (int var2 = 0; var2 < get().aliases.size(); var2++) {
         InstrumentAlias var3 = get().aliases.get(var2);
         if (var3.instrument.equals(var0)) {
            var1.add(var3);
         }
      }

      return var1;
   }

   public static void removeInstrumentAliases(String var0) {
      for (int var1 = get().aliases.size() - 1; var1 >= 0; var1--) {
         InstrumentAlias var2 = get().aliases.get(var1);
         if (var2.instrument.equals(var0)) {
            get().aliases.remove(var1);
         }
      }

      save();
   }

   public static void addAlias(String var0, String var1, String var2) throws DataException {
      if (checkAliasExists(var0)) {
         throw new DataException(3, "DB: Alias '" + var0 + "' already exists.");
      }

      get().aliases.add(new InstrumentAlias(var0, var1, var2));
      save();
   }

   public static void updateAlias(String var0, String var1, String var2) throws DataException {
      if (!checkAliasExists(var1)) {
         throw new DataException(3, "Alias '" + var1 + "' doesn't exist.");
      }

      InstrumentAlias var3 = getAlias(var1);
      var3.instrument = var0;
      var3.description = var2;
      save();
   }

   public static void removeAlias(String var0) throws DataException {
      if (!checkAliasExists(var0)) {
         throw new DataException(3, "Alias '" + var0 + "' doesn't exist.");
      }

      InstrumentAlias var1 = getAlias(var0);
      get().aliases.remove(var1);
      save();
   }

   private void load() {
      this.aliases = new ArrayList<>();
      File var1 = new File(filePath);
      if (!var1.exists()) {
         tryCreateFile(var1);
      } else {
         try {
            String[] var2 = SQUtils.fileToString(var1).split("\n");

            for (int var3 = 0; var3 < var2.length; var3++) {
               String var4 = var2[var3];

               try {
                  String[] var5 = var4.split("#");
                  if (var5.length >= 2) {
                     String var6 = var5.length == 3 ? var5[2] : "";
                     String var7 = var5[0].trim();
                     String var8 = var5[1].trim();
                     if (InstrumentManager.checkInstrumentExists(var8, true)) {
                        this.aliases.add(new InstrumentAlias(var7, var8, var6));
                     }
                  }
               } catch (Exception var9) {
               }
            }
         } catch (Exception var10) {
            Log.error("Error while loading aliases. ", var10);
         }
      }
   }

   private static void save() {
      File var0 = new File(filePath);
      if (!var0.exists()) {
         tryCreateFile(var0);
      }

      PrintWriter var1 = null;
      String var2 = "";

      try {
         var1 = new PrintWriter(new File(filePath));

         for (int var3 = 0; var3 < get().aliases.size(); var3++) {
            InstrumentAlias var4 = get().aliases.get(var3);
            var2 = var4.alias + "#" + var4.instrument + "#" + var4.description + "\n";
            var1.print(var2);
         }

         var1.flush();
         var1.close();
      } catch (Exception var5) {
         if (var1 != null) {
            var1.close();
         }

         Log.error("Cannot save aliases settings.", var5);
      }
   }

   private static void tryCreateFile(File var0) {
      File var1 = var0.getParentFile();
      if (var1 != null) {
         var1.mkdirs();

         try {
            var0.createNewFile();
         } catch (IOException var3) {
            Log.error("Cannot create settings file '" + filePath + "'", var3);
         }
      }
   }
}
