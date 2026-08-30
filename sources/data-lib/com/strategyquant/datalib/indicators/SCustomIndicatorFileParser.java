package com.strategyquant.datalib.indicators;

import com.strategyquant.lib.SQUtils;
import java.io.File;

public class SCustomIndicatorFileParser {
   public static SCustomIndicator parse(File var0) throws Exception {
      try {
         SCustomIndicator var1 = new SCustomIndicator();
         int var2 = var0.getName().indexOf(".mq4");
         if (var2 == -1) {
            var2 = var0.getName().indexOf(".xml");
         }

         var1.fileName = var0.getName().substring(0, var2);
         var1.shortName = var1.fileName.replaceAll("\\s", "_");
         var1.longName = var1.fileName;
         String[] var3 = new String[]{"UTF-8", "UTF-16"};
         String var4 = null;
         String var5 = null;

         for (int var6 = 0; var6 < var3.length; var6++) {
            var5 = SQUtils.fileToString(var0, var3[var6]);
            var4 = var5.replaceAll("\\s", "");
            if (var4.contains("property")) {
               break;
            }
         }

         if (var4.contains("#propertyindicator_chart_window")) {
            var1.returnType = 1;
         } else {
            if (!var4.contains("#propertyindicator_separate_window")) {
               throw new Exception("Unknown return type!");
            }

            var1.returnType = 2;
         }

         Integer var17 = 1;
         int var7 = 0;

         while (true) {
            while (true) {
               var7 = var5.indexOf("extern", ++var7);
               if (var7 == -1) {
                  var7 = var4.indexOf("indicator_buffers");
                  if (var7 == -1) {
                     throw new Exception("Indicator doesn't contain any output data, it cannot be used as EA.");
                  }

                  var7 += 17;
                  String var24 = "";

                  while (true) {
                     if (Character.isDigit(var4.charAt(var7))) {
                        var24 = var24 + var4.charAt(var7);
                     } else if (!Character.isDigit(var4.charAt(var7)) && !var24.equals("")) {
                        Integer var28 = Integer.parseInt(var24);
                        if (var28 == 1) {
                           var1.outputList.add(var1.fileName);
                           return var1;
                        }

                        var7 = 0;
                        var17 = 1;

                        while (true) {
                           var7 = var5.indexOf("SetIndexBuffer", ++var7);
                           if (var7 == -1) {
                              break;
                           }

                           int var29 = var5.indexOf(")", var7);
                           String var30 = var5.substring(var7, var29);
                           String[] var31 = var30.split("[,]+");
                           if (var31.length < 2) {
                              throw new Exception(String.format("Invalid output definition: '%s'", var30));
                           }

                           var1.outputList.add(var31[1].trim());
                           if (var28 == var17) {
                              break;
                           }

                           var17 = var17 + 1;
                        }

                        return var1;
                     }

                     var7++;
                  }
               }

               int var8 = var7 - 1024;
               if (var8 < 0) {
                  var8 = 0;
               }

               String var9 = var5.substring(var8, var7);

               try {
                  if (var9.substring(var9.lastIndexOf("\n")).contains("//")) {
                     continue;
                  }
               } catch (Exception var15) {
               }
               break;
            }

            String var25 = var5.substring(var7 + 6);
            var25 = var25.replaceAll("\\s", "");
            var25 = var25.substring(0, 6);
            if (var25.contains("ENUM")
               || var25.contains("int")
               || var25.contains("double")
               || var25.contains("color")
               || var25.contains("string")
               || var25.contains("bool")) {
               int var10 = var5.indexOf(";", var7);
               SParametersParser var11 = new SParametersParser(var5.substring(var7, var10).trim());
               var11.parse();

               for (SParametersParser.InputParameter var13 : var11.list) {
                  var1.parameterList.add(new SParameter(var13.name.trim(), var11.type, var13.value, var17));
                  var17 = var17 + 1;
               }
            }
         }
      } catch (Exception var16) {
         throw var16;
      }
   }
}
