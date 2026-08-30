package com.strategyquant.wizard.desktop.indicators;

import com.strategyquant.lib.L;
import java.io.File;
import java.nio.file.Files;

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
         String var3 = new String(Files.readAllBytes(var0.toPath()));
         String var4 = var3.replaceAll("\\s", "");
         if (var4.contains("#propertyindicator_chart_window")) {
            var1.returnType = "price";
         } else {
            if (!var4.contains("#propertyindicator_separate_window")) {
               throw new Exception(L.t("Unknown return type!", new Object[0]));
            }

            var1.returnType = "number";
         }

         Integer var5 = 1;
         int var6 = 0;

         while (true) {
            while (true) {
               var6 = var3.indexOf("extern", ++var6);
               if (var6 == -1) {
                  var6 = var4.indexOf("indicator_buffers");
                  if (var6 == -1) {
                     throw new Exception(L.t("Indicator doesn't contain any output data, it cannot be used as EA.", new Object[0]));
                  }

                  var6 += 17;
                  String var22 = "";

                  while (true) {
                     if (Character.isDigit(var4.charAt(var6))) {
                        var22 = var22 + var4.charAt(var6);
                     } else if (!Character.isDigit(var4.charAt(var6)) && !var22.equals("")) {
                        Integer var26 = Integer.parseInt(var22);
                        if (var26 == 1) {
                           var1.outputList.add(var1.fileName);
                           return var1;
                        }

                        var6 = 0;
                        var5 = 1;

                        while (true) {
                           var6 = var3.indexOf("SetIndexBuffer", ++var6);
                           if (var6 == -1) {
                              break;
                           }

                           int var27 = var3.indexOf(")", var6);
                           String[] var28 = var3.substring(var6, var27).split("[,]+");
                           if (var28.length != 2) {
                              throw new Exception(L.t("Unknown output definition!", new Object[0]));
                           }

                           var1.outputList.add(var28[1].trim());
                           if (var26 == var5) {
                              break;
                           }

                           var5 = var5 + 1;
                        }

                        return var1;
                     }

                     var6++;
                  }
               }

               int var7 = var6 - 1024;
               if (var7 < 0) {
                  var7 = 0;
               }

               String var8 = var3.substring(var7, var6);

               try {
                  if (var8.substring(var8.lastIndexOf("\n")).contains("//")) {
                     continue;
                  }
               } catch (Exception var14) {
               }
               break;
            }

            String var23 = var3.substring(var6 + 6);
            var23 = var23.replaceAll("\\s", "");
            var23 = var23.substring(0, 6);
            if (var23.contains("ENUM")
               || var23.contains("int")
               || var23.contains("double")
               || var23.contains("color")
               || var23.contains("string")
               || var23.contains("bool")) {
               int var9 = var3.indexOf(";", var6);
               SParametersParser var10 = new SParametersParser(var3.substring(var6, var9).trim());
               var10.parse();

               for (SParametersParser.InputParameter var12 : var10.list) {
                  var1.parameterList.add(new SParameter(var12.name.trim(), var10.type, var12.value, var5));
                  var5 = var5 + 1;
               }
            }
         }
      } catch (Exception var15) {
         throw var15;
      }
   }
}
