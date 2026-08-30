package com.strategyquant.datalib.indicators;

import java.util.ArrayList;

public class SParametersParser {
   private String input = null;
   public String type = null;
   public ArrayList<SParametersParser.InputParameter> list = new ArrayList<>();
   private static final int STATE_READ_NAME = 1;
   private static final int STATE_WAIT_READ_NEW_VALUE = 2;
   private static final int STATE_READ_VALUE = 3;
   private static final int STATE_READ_STRING_VALUE = 4;

   public SParametersParser(String var1) {
      this.input = var1;
   }

   public void parse() throws Exception {
      String[] var1 = this.input.split("[\\s]+");
      if (var1.length < 3) {
         throw new Exception("Unknown input(param) definition!");
      }

      this.type = var1[1].trim();
      String var2 = this.input.substring(this.input.indexOf(var1[1]) + var1[1].length()).trim();
      if (this.type.equalsIgnoreCase("color")) {
         var2 = var2.replaceAll("C'[^C']*'", "Yellow");
      }

      this.parseValues(var2);
   }

   private void parseValues(String var1) {
      char var3 = '_';
      byte var4 = 1;
      String var5 = "";
      String var6 = null;

      for (int var7 = 0; var7 < var1.length(); var7++) {
         char var2 = var1.charAt(var7);
         switch (var4) {
            case 1:
               if (var5 != null || var2 != ',') {
                  if (var2 == '=') {
                     var4 = 2;
                  } else if (var2 == ',') {
                     this.list.add(new SParametersParser.InputParameter(var5, null));
                     var5 = "";
                  } else {
                     if (var5 == null) {
                        var5 = "";
                     }

                     var5 = var5 + var2;
                  }
               }
               break;
            case 2:
               if (!Character.isWhitespace(var2)) {
                  if (var2 == '"') {
                     var4 = 4;
                  } else {
                     var6 = "" + var2;
                     var4 = 3;
                  }
               }
               break;
            case 3:
               if (var2 == ',') {
                  this.list.add(new SParametersParser.InputParameter(var5, var6));
                  var5 = null;
                  var6 = null;
                  var4 = 1;
               } else {
                  if (var6 == null) {
                     var6 = "";
                  }

                  var6 = var6 + var2;
               }
               break;
            case 4:
               if (var2 == '"' && var3 != '\\') {
                  this.list.add(new SParametersParser.InputParameter(var5, var6));
                  var5 = null;
                  var6 = null;
                  var4 = 1;
               } else {
                  if (var6 == null) {
                     var6 = "";
                  }

                  var6 = var6 + var2;
               }

               var3 = var2;
         }
      }

      if (var5 != null) {
         this.list.add(new SParametersParser.InputParameter(var5, var6));
      }
   }

   public class InputParameter {
      public String name = null;
      public String value = null;

      public InputParameter(String nullx, String nullxx) {
         this.name = nullx;
         this.value = nullxx;
      }
   }
}
