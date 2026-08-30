package com.strategyquant.datalib.timezone;

import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.snippets.compile.SQStructure;
import java.io.File;
import java.util.ArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Timezones {
   public static final Logger Log = LoggerFactory.getLogger(Timezones.class);
   private static final String path = SQStructure.INTERNAL_DIR_PATH + "/web/SQMANAGER/timezones.csv";
   private static Timezones instance;
   private ArrayList<Timezone> timezones = new ArrayList<>();

   private Timezones() {
      this.loadTimezones();
   }

   private static Timezones get() {
      if (instance == null) {
         instance = new Timezones();
      }

      return instance;
   }

   public static ArrayList<Timezone> getTimezones() {
      return get().timezones;
   }

   private void loadTimezones() {
      try {
         String var1 = SQUtils.fileToString(new File(path));
         String[] var2 = var1.split("\n");

         for (String var6 : var2) {
            String[] var7 = var6.split(";");
            Timezone var8 = new Timezone(var7[0].trim(), var7[1].trim());
            this.timezones.add(var8);
         }
      } catch (Exception var9) {
         Log.error("Cannot load timezones", var9);
      }
   }

   public static Timezone findByKey(String var0) {
      for (Timezone var2 : getTimezones()) {
         if (var2.getId().equals(var0)) {
            return var2;
         }
      }

      return null;
   }
}
