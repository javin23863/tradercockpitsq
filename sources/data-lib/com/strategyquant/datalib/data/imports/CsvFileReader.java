package com.strategyquant.datalib.data.imports;

import com.strategyquant.datalib.data.io.ImportDataInfo;
import com.strategyquant.lib.L;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.regex.PatternSyntaxException;

public class CsvFileReader {
   private ImportDataInfo importInfo;

   public CsvFileReader(ImportDataInfo var1) {
      this.importInfo = var1;
   }

   public int getDigitsCount(String var1) {
      int var2 = 0;

      for (char var6 : var1.toCharArray()) {
         if (Character.isDigit(var6)) {
            var2++;
         }
      }

      return var2;
   }

   public int getAlphabeticsCount(String var1) {
      int var2 = 0;

      for (char var6 : var1.toCharArray()) {
         if (!Character.isDigit(var6)) {
            var2++;
         }
      }

      return var2;
   }

   public String[][] read(Boolean var1) throws Exception {
      this.importInfo.resetLastAskBid();
      BufferedReader var2 = null;
      String var4 = null;
      String[][] var5 = null;
      String[] var6 = null;
      boolean var7 = true;
      int var8 = 0;
      int var9 = 0;
      String[] var3 = new String[this.importInfo.rowCount];

      try {
         File var10 = new File(this.importInfo.filePath);
         var2 = new BufferedReader(new FileReader(var10));
      } catch (FileNotFoundException var21) {
         throw new Exception("File not found: " + this.importInfo.filePath);
      }

      try {
         while ((var4 = var2.readLine()) != null) {
            var4 = var4.replaceAll("( +)", " ").trim();
            if (var1 && this.getDigitsCount(var4) < this.getAlphabeticsCount(var4) && var9 == 0) {
               this.importInfo.skipRows++;
            }

            if (var8 < this.importInfo.skipRows) {
               var8++;
            } else {
               if (var9 >= this.importInfo.rowCount) {
                  break;
               }

               var3[var9] = var4;
               var9++;
            }
         }

         if (var1) {
            this.importInfo.separator = findSeparator(var3, this.importInfo.skipRows);
         }

         var9 = 0;

         for (int var28 = 0; var28 < var3.length; var28++) {
            var4 = var3[var28];
            if (var4 == null) {
               break;
            }

            var6 = var4.split(this.importInfo.separator);
            if (this.importInfo.isMT5TickImport()) {
               var6 = this.importInfo.correctMT5TickData(var6);
               if (var6 == null) {
                  continue;
               }
            }

            if (var6.length <= this.importInfo.skipCols) {
               break;
            }

            if (var7) {
               if (var6.length - this.importInfo.skipCols <= 0) {
                  throw new Exception();
               }

               int var11 = 0;

               for (int var12 = 0; var12 < var3.length && var3[var12] != null; var12++) {
                  var11++;
               }

               var5 = new String[var11][var6.length - this.importInfo.skipCols];
               var7 = false;
            }

            for (int var29 = this.importInfo.skipCols; var29 < var6.length; var29++) {
               var5[var9][var29 - this.importInfo.skipCols] = var6[var29];
            }

            var9++;
         }
      } catch (PatternSyntaxException var23) {
         throw var23;
      } catch (Exception var24) {
         if (var4 != null && var4.length() > 100) {
            var4 = var4.substring(0, 100) + "...";
         }

         throw new Exception(L.t("Error parsing on line %d. Incorrect file format?\nLine contents: %s", new Object[]{this.importInfo.skipRows + var9, var4}));
      } finally {
         try {
            if (var2 != null) {
               var2.close();
            }
         } catch (IOException var22) {
            throw new Exception(L.t("Cannot close file :%s", new Object[]{this.importInfo.filePath}));
         }
      }

      return var5;
   }

   public static String findSeparator(String[] var0, int var1) throws Exception {
      String[] var2 = Separators.listValues();
      String var3 = null;
      String var4 = null;
      int var5 = 0;
      int var6 = 0;

      try {
         for (var6 = 0; var6 < var0.length; var6++) {
            var3 = var0[var6];
            if (var3 == null) {
               break;
            }

            for (int var7 = 0; var7 < var2.length; var7++) {
               if (var3.split(var2[var7]).length > var5) {
                  var5 = var3.split(var2[var7]).length;
                  var4 = var2[var7];
               }
            }
         }

         if (var4 == null) {
            var4 = var2[0];
         }

         return var4;
      } catch (PatternSyntaxException var8) {
         if (var3 != null && var3.length() > 100) {
            var3 = var3.substring(0, 100) + "...";
         }

         throw new Exception(L.t("Error parsing on line %d. Incorrect file format?\nLine contents: %s", new Object[]{var1 + var6, var3}));
      }
   }

   public String getSeparator() {
      return this.importInfo.separator;
   }

   public String getDateFormat() {
      return this.importInfo.dateFormat;
   }
}
