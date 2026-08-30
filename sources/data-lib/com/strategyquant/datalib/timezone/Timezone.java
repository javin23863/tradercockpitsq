package com.strategyquant.datalib.timezone;

import com.strategyquant.lib.L;
import java.util.TimeZone;

public class Timezone {
   public static final String DEFAULT = "Etc/UCT";
   private String name;
   private String id;
   public boolean dst = false;

   public Timezone(String var1, String var2) {
      this.name = var1;
      this.id = var2;
      if (var2.equals("EETUS")) {
         var2 = "America/New_York";
      }

      TimeZone var3 = TimeZone.getTimeZone(var2);
      this.dst = var3.useDaylightTime();
   }

   @Override
   public String toString() {
      return this.name + ", DST: " + (this.dst ? "Yes" : "No");
   }

   public String getId() {
      return this.id;
   }

   public String getName() {
      return this.toString();
   }

   public static String print(String var0, int var1, boolean var2) {
      if (var1 != 5 && var1 != 6 || var0 != null && !var0.isBlank()) {
         if (var0 == null) {
            return null;
         }

         String var3 = "";
         String[] var4 = var0.split("\\|");
         String var5 = var4[0];
         Timezone var6 = Timezones.findByKey(var5);
         if (var6 == null) {
            return var5;
         }

         var3 = var6.getName();
         if (var2) {
            var3 = var3.substring(0, var3.lastIndexOf(")") + 1);
            var3 = var3.replaceAll("\\(", "");
            var3 = var3.replaceAll("\\)", "");
         }

         if (var4.length > 1) {
            for (int var7 = 1; var7 < var4.length; var7++) {
               int var8 = Integer.parseInt(var4[var7]);
               var3 = var3 + " " + (var8 > 0 ? "+" + var4[var7] : var4[var7]);
            }
         }

         return var3;
      } else {
         return L.t("Exchange", new Object[0]);
      }
   }

   public String printShortName() {
      return this.name.substring(0, this.name.indexOf(")") + 1);
   }

   public static String shiftHours(String var0, int var1) {
      return var1 == 0 ? var0 : var0 + "|" + var1;
   }

   public static String parseId(String var0) {
      return var0.split("\\|")[0];
   }
}
