package com.strategyquant.tradinglib.talib.reflection;

import com.strategyquant.lib.L;
import com.strategyquant.tradinglib.talib.TALibIndicators;
import com.tictactec.ta.lib.MAType;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class TALibXmlGenApp {
   private static final String OUT_PREFIX = "out";
   private static final String OUT_REAL_PREFIX = "outReal";
   private String[] BLACK_LIST = new String[]{
      "ADD",
      "SIN",
      "COS",
      "TAN",
      "SQRT",
      "LOG10",
      "EXP",
      "MIN",
      "MAX",
      "AD",
      "SUM",
      "LN",
      "ASIN",
      "ACOS",
      "ATAN",
      "CEIL",
      "FLOOR",
      "SINH",
      "COSH",
      "TANH",
      "SUB",
      "MULT",
      "DIV"
   };
   private static final Map<String, double[]> minMaxMap = new HashMap<>();
   private StringBuilder sb;
   private StringBuilder problems;
   private Set<String> blackListSet;
   private Map<String, String> namesMap = new HashMap<>();
   private Map<String, String> typesMap = new HashMap<>();
   private Map<String, String> returnTypeMap = new HashMap<>();
   private Map<String, String> categoryTypeMap = new HashMap<>();

   public TALibXmlGenApp() throws IOException {
      this.blackListSet = new HashSet<>();
      this.initNiceNameMap();
      this.initMinMax();
   }

   private void initMinMax() {
      minMaxMap.put("BBANDS", new double[]{0.0, 10.0});
      minMaxMap.put("STDDEV", new double[]{0.0, 10.0});
      minMaxMap.put("VAR", new double[]{0.0, 10.0});
      minMaxMap.put("SAR", new double[]{0.0, 1.0});
      minMaxMap.put("SAREXT", new double[]{0.0, 1.0});
      minMaxMap.put("CDLABANDONEDBABY", new double[]{0.0, 10.0});
      minMaxMap.put("CDLDARKCLOUDCOVER", new double[]{0.0, 10.0});
      minMaxMap.put("CDLEVENINGDOJISTAR", new double[]{0.0, 10.0});
      minMaxMap.put("CDLEVENINGSTAR", new double[]{0.0, 10.0});
      minMaxMap.put("CDLMATHOLD", new double[]{0.0, 10.0});
      minMaxMap.put("CDLMORNINGDOJISTAR", new double[]{0.0, 10.0});
      minMaxMap.put("CDLMORNINGSTAR", new double[]{0.0, 10.0});
   }

   private void initNiceNameMap() throws IOException {
      InputStream var1 = TALibXmlGenApp.class.getResourceAsStream("talibdesc.txt");

      try {
         for (String var4 : new BufferedReader(new InputStreamReader(var1, StandardCharsets.UTF_8)).lines().collect(Collectors.toList())) {
            if (!var4.isBlank()) {
               String[] var5 = var4.split("=");
               if (var5.length == 2) {
                  String var6 = var5[0];
                  String var7 = var5[1];
                  String[] var8 = var7.split(";");
                  this.namesMap.put(var6, var8[0]);
                  this.typesMap.put(var6, var8[1]);
                  this.returnTypeMap.put(var6, var8[2]);
                  this.categoryTypeMap.put(var6, var8[3]);
               } else {
                  System.out.println("Ignoring line:" + var4);
               }
            }
         }
      } catch (Throwable var10) {
         if (var1 != null) {
            try {
               var1.close();
            } catch (Throwable var9) {
               var10.addSuppressed(var9);
            }
         }

         throw var10;
      }

      if (var1 != null) {
         var1.close();
      }
   }

   public void generate() {
      TALibFunction[] var1 = TALibFunctions.getTALibFunctions();
      Arrays.sort(var1, new Comparator<TALibFunction>() {
         public int compare(TALibFunction var1, TALibFunction var2) {
            return var1.getName().compareTo(var2.getName());
         }
      });
      this.sb = new StringBuilder();
      this.problems = new StringBuilder();
      this.sb.append("<Talib>\n");
      this.printType("indy", "Indicators", var1);
      this.printType("candle", "Candles", var1);
      this.printType("func", "Functions", var1);
      this.printType("price", "Prices", var1);
      this.sb.append("</Talib>\n");
      System.out.println(this.sb.toString());
      System.out.println(this.problems.toString());
   }

   private String getCategoryType(String var1) {
      String var2 = "indicator";
      if (var1.equals("candle")) {
         var2 = "simpleRules";
      } else if (var1.equals("func")) {
         var2 = "other";
      }

      return var2;
   }

   private void printType(String var1, String var2, TALibFunction[] var3) {
      this.sb.append("\t<" + var2 + ">\n");

      for (TALibFunction var7 : var3) {
         this.print(var7, var1);
      }

      this.sb.append("\t</" + var2 + ">\n");
   }

   private String getType(TALibFunction var1) {
      return this.typesMap.get("talib_" + var1.getName());
   }

   private void print(TALibFunction var1, String var2) {
      if (!this.shouldSkip(var1)) {
         if (var2.equals(this.getType(var1))) {
            String var3 = this.getCategoryType(var1);
            this.begin(var1, var2);
            this.params(var1, var3);
            this.end();
         }
      }
   }

   private void params(TALibFunction var1, String var2) {
      this.sb.append("\t\t\t<paramCategory name=\"Default\" expanded=\"true\">\n");
      this.addStdParams(var1);
      this.addOptParams(var1);
      this.addOutParams(var1);
      this.addShift(var2);
      this.sb.append("\t\t\t</paramCategory>\n");
   }

   private void addOutParams(TALibFunction var1) {
      TALibFunction.Param[] var2 = var1.getOutputs();
      if (var2 != null && var2.length >= 2) {
         StringBuilder var3 = new StringBuilder();
         int var4 = 0;

         for (TALibFunction.Param var8 : var2) {
            if (var3.length() != 0) {
               var3.append(",");
            }

            String var9 = var8.getName();
            if (var9.startsWith("outReal")) {
               var9 = var9.substring("outReal".length());
            } else if (var9.startsWith("out")) {
               var9 = var9.substring("out".length());
            }

            var3.append(var9 + "=" + var4);
            var4++;
         }

         String var10 = "0";
         String var11 = var3.toString();
         this.sb.append("\t\t\t\t");
         this.sb.append("<Param key=\"#Line#\" type=\"int\" name=\"Line\" controlType=\"combo\" values=\"" + var11 + "\" defaultValue=\"" + var10 + "\" />\n");
      }
   }

   private void addStdParams(TALibFunction var1) {
      this.sb.append("\t\t\t\t");
      this.sb.append("<Param key=\"#Chart#\" name=\"Chart\" type=\"data\" controlType=\"dataVar\" defaultValue=\"0\" />\n");

      for (TALibFunction.Param var5 : var1.getInputs()) {
         String var6 = var5.getName();
         if (var6.equals("inReal")) {
            this.sb.append("\t\t\t\t");
            this.sb
               .append(
                  "<Param key=\"#ComputedFrom#\" name=\"Computed From\" type=\"int\" controlType=\"combo\" values=\"Close=0,Open=1,High=2,Low=3,Median=4,Typical=5,Weighted=6,Volume=7\" defaultValue=\"0\" />\n"
               );
         } else if (var6.equals("inReal0")) {
            this.sb.append("\t\t\t\t");
            this.sb
               .append(
                  "<Param key=\"#ComputedFrom#\" name=\"Computed From 0\" type=\"int\" controlType=\"combo\" values=\"Close=0,Open=1,High=2,Low=3,Median=4,Typical=5,Weighted=6,Volume=7\" defaultValue=\"0\" />\n"
               );
         } else if (var6.equals("inReal1")) {
            this.sb.append("\t\t\t\t");
            this.sb
               .append(
                  "<Param key=\"#ComputedFrom1#\" name=\"Computed From 1\" type=\"int\" controlType=\"combo\" values=\"Close=0,Open=1,High=2,Low=3,Median=4,Typical=5,Weighted=6,Volume=7\" defaultValue=\"0\" />\n"
               );
         }
      }
   }

   private void addShift(String var1) {
      this.sb.append("\t\t\t\t");
      if ("simpleRules".equals(var1)) {
         this.sb
            .append(
               "<Param key=\"#Shift#\" name=\"Shift\" type=\"int\" defaultValue=\"1\" controlType=\"jspinnerVar\" minValue=\"0\" maxValue=\"100\" genMinValue=\"-1000001\" genMaxValue=\"-1000002\" paramType=\"shift\" step=\"1\" builderStep=\"1\" />\n"
            );
      } else {
         this.sb
            .append(
               "<Param key=\"#Shift#\" name=\"Shift\" type=\"int\" defaultValue=\"1\" controlType=\"jspinnerVar\" minValue=\"0\" maxValue=\"1000\" genMinValue=\"-1000001\" genMaxValue=\"-1000002\" paramType=\"shift\" step=\"1\" builderStep=\"1\" />\n"
            );
      }
   }

   private void addOptParams(TALibFunction var1) {
      for (TALibFunction.Param var5 : var1.getOptInputs()) {
         String var6 = var5.getName();
         if (!var6.startsWith("optIn")) {
            throw new RuntimeException(L.t("invalid opt param %s", new Object[]{var6}));
         }

         var6 = var6.substring(5);
         String var7 = this.evalType(var5);
         String var8 = this.evalAdditional(var1, var5);
         this.sb.append("\t\t\t\t");
         this.sb.append("<Param key=\"#" + var6 + "#\" name=\"" + var5.getDisplayName() + "\" type=\"" + var7 + "\" " + var8 + " />\n");
      }
   }

   private Object getDefault(TALibFunction var1, TALibFunction.Param var2) {
      TALibFunction.IntegerParams var3 = var1.getIntParameter(var2.getName());
      TALibFunction.DoubleParams var4 = var1.getDoubleParameter(var2.getName());
      if (var3 != null) {
         return var3.getDef();
      } else {
         return var4 != null ? var4.getDef() : 0;
      }
   }

   private Object getMinValue(TALibFunction var1, TALibFunction.Param var2) {
      TALibFunction.IntegerParams var3 = var1.getIntParameter(var2.getName());
      TALibFunction.DoubleParams var4 = var1.getDoubleParameter(var2.getName());
      if (var3 != null) {
         return var3.getMin();
      } else if (var4 != null) {
         return this.normalizeMinMaxValue(var1, var4.getMin(), true);
      } else {
         throw new RuntimeException("Cannot evaluate min value for: " + var1.getName());
      }
   }

   private Object getMaxValue(TALibFunction var1, TALibFunction.Param var2) {
      TALibFunction.IntegerParams var3 = var1.getIntParameter(var2.getName());
      TALibFunction.DoubleParams var4 = var1.getDoubleParameter(var2.getName());
      if (var3 != null) {
         return var3.getMax();
      } else if (var4 != null) {
         return this.normalizeMinMaxValue(var1, var4.getMax(), false);
      } else {
         throw new RuntimeException("Cannot evaluate max value for: " + var1.getName());
      }
   }

   private double normalizeMinMaxValue(TALibFunction var1, double var2, boolean var4) {
      String var5 = String.valueOf(var2);
      if (var5.contains("E")) {
         double[] var6 = minMaxMap.get(var1.getName());
         if (var6 == null) {
            throw new RuntimeException("Unknown min/max for: " + var1.getName());
         } else {
            return var4 ? var6[0] : var6[1];
         }
      } else {
         return var2;
      }
   }

   private Object getStep(TALibFunction var1, TALibFunction.Param var2) {
      TALibFunction.IntegerParams var3 = var1.getIntParameter(var2.getName());
      TALibFunction.DoubleParams var4 = var1.getDoubleParameter(var2.getName());
      if (var3 != null) {
         return var3.getStep();
      } else {
         return var4 != null ? var4.getStep() : 1;
      }
   }

   private Object getSuggestedStart(TALibFunction var1, TALibFunction.Param var2) {
      TALibFunction.IntegerParams var3 = var1.getIntParameter(var2.getName());
      TALibFunction.DoubleParams var4 = var1.getDoubleParameter(var2.getName());
      if (var3 != null) {
         return var3.getSuggStart();
      } else {
         return var4 != null ? var4.getSuggStart() : null;
      }
   }

   private Object getSuggestedEnd(TALibFunction var1, TALibFunction.Param var2) {
      TALibFunction.IntegerParams var3 = var1.getIntParameter(var2.getName());
      TALibFunction.DoubleParams var4 = var1.getDoubleParameter(var2.getName());
      if (var3 != null) {
         return var3.getSuggEnd();
      } else {
         return var4 != null ? var4.getSuggEnd() : null;
      }
   }

   private String evalAdditional(TALibFunction var1, TALibFunction.Param var2) {
      String var3 = var2.getType();
      String var4 = "jspinnerVar";
      String var5 = " defaultValue=\""
         + this.getDefault(var1, var2)
         + "\" step=\""
         + this.getStep(var1, var2)
         + "\" builderStep=\""
         + this.getStep(var1, var2)
         + "\" minValue=\""
         + this.getMinValue(var1, var2)
         + "\" maxValue=\""
         + this.getMaxValue(var1, var2)
         + "\" builderMinValue=\""
         + this.getSuggestedStart(var1, var2)
         + "\" builderMaxValue=\""
         + this.getSuggestedEnd(var1, var2)
         + "\" builderStepValue=\""
         + this.getStep(var1, var2)
         + "\" ";
      this.checkMinMax(var1, var2);
      if (var3.equals("TA_OptInput_IntegerList")) {
         var4 = "combo";
         int var6 = 0;
         String var7 = "";

         for (MAType var11 : MAType.values()) {
            var7 = var7 + var11.name() + "=" + var6 + ",";
            var6++;
         }

         var5 = " values=\"" + var7.substring(0, var7.length() - 1) + "\" defaultValue=\"" + this.getDefault(var1, var2) + "\" ";
      } else if (TALibIndicators.isPeriod(var2.getName())) {
         var5 = var5 + " paramType=\"period\" genMinValue=\"-1000003\" genMaxValue=\"-1000004\"";
      }

      return "controlType=\"" + var4 + "\"" + var5;
   }

   private void checkMinMax(TALibFunction var1, TALibFunction.Param var2) {
      Double var3 = Double.valueOf(this.getMinValue(var1, var2).toString());
      Double var4 = Double.valueOf(this.getMaxValue(var1, var2).toString());
      if (TALibIndicators.isPeriod(var2.getName()) && (var4 < 2.0 || var3 > 2.0)) {
         this.problems.append(var1.getName() + " - " + var2.getName() + ": min=" + var3 + ", max:" + var4 + "\n");
      }
   }

   private String evalType(TALibFunction.Param var1) {
      switch (var1.getType()) {
         case "TA_OptInput_IntegerRange":
         case "TA_OptInput_IntegerList":
            return "int";
         case "TA_OptInput_RealRange":
            return "double";
         default:
            throw new RuntimeException(L.t("Invalid type %s.", new Object[]{var1.getType()}));
      }
   }

   private boolean shouldSkip(TALibFunction var1) {
      return this.blackListSet.contains(var1.getName());
   }

   private void end() {
      this.sb.append("\t\t</Item>\n");
   }

   private void begin(TALibFunction var1, String var2) {
      String var3 = this.getDisplay(var1);
      String var4 = this.getCategoryType(var1);
      String var5 = "";
      if (var2.equals("candle")) {
         var3 = this.getCandleDisplay(var1);
      } else if (var2.equals("func")) {
         var5 = " mI=\"Functions\"";
      } else if (var2.equals("price")) {
         var5 = " mI=\"Price\"";
      }

      if (var1.getName().equals("MAVP")) {
         var5 = var5 + " ignoreInBuilder=\"true\"";
      }

      this.sb
         .append(
            "\t\t<Item key=\"talib_"
               + var1.getName()
               + "\" name=\""
               + this.getUserFriendlyName(var1)
               + "\" display=\""
               + var3
               + "\" returnType=\""
               + this.getReturnType(var1, var2, var4)
               + "\" categoryType=\""
               + var4
               + "\""
               + var5
               + ">\n"
         );
   }

   private String getCategoryType(TALibFunction var1) {
      return this.categoryTypeMap.get("talib_" + var1.getName());
   }

   private String getUserFriendlyName(TALibFunction var1) {
      String var2 = var1.getName();
      String var3 = this.namesMap.get("talib_" + var2);
      return var3 != null ? var3 : var2;
   }

   private String getReturnType(TALibFunction var1, String var2, String var3) {
      return "simpleRules".equals(var3) ? "boolean" : this.returnTypeMap.get("talib_" + var1.getName());
   }

   private String getDisplay(TALibFunction var1) {
      StringBuilder var2 = new StringBuilder();
      var2.append(var1.getName() + "(@Chart@");

      for (int var3 = 0; var3 < var1.getOptInputs().length; var3++) {
         TALibFunction.Param var4 = var1.getOptInputs()[var3];
         String var5 = var4.getName().substring(5);
         if (var3 != 0) {
            var2.append(",");
         }

         var2.append("#" + var5 + "#");
      }

      var2.append(")[#Shift#]");
      return var2.toString();
   }

   private String getCandleDisplay(TALibFunction var1) {
      StringBuilder var2 = new StringBuilder();
      var2.append(this.getUserFriendlyName(var1));
      var2.append(" pattern #Shift# bar(s) ago");
      return var2.toString();
   }

   public static void main(String[] var0) throws IOException {
      new TALibXmlGenApp().generate();
   }
}
