package com.strategyquant.tradinglib.talib.reflection;

import com.strategyquant.lib.L;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class TALibSwitchGenApp {
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
   private StringBuilder sb;
   private Set<String> blackListSet = new HashSet<>();
   private List<String> hlcIndicators;

   public void generate() {
      TALibFunction[] var1 = TALibFunctions.getTALibFunctions();
      this.sb = new StringBuilder();
      this.hlcIndicators = new LinkedList<>();

      for (TALibFunction var5 : var1) {
         this.print(var5);
      }

      System.out.println(this.sb.toString());
      System.out.println("\n\n\"" + this.hlcIndicators.stream().collect(Collectors.joining("\",\"")) + "\"");
   }

   private void print(TALibFunction var1) {
      if (!this.shouldSkip(var1)) {
         this.caseBegin(var1);
         this.coreFce(var1);
         this.caseEnd();
      }
   }

   private void coreFce(TALibFunction var1) {
      this.prepareOutputs(var1);
      this.sb.append("\t\t");
      this.sb.append("retCode = core." + var1.getTalibMethod().getName() + "(");
      this.sb.append("startIdx, endIdx, ");
      boolean var2 = this.addIns(var1);
      if (var2) {
         this.hlcIndicators.add(var1.getName());
      }

      this.addOptParams(var1);
      this.sb.append("outBegIdx, outNbElement");
      this.addOutputs(var1);
      this.sb.append(");\n");
   }

   private void addOutputs(TALibFunction var1) {
      int var2 = 0;
      int var3 = 0;

      for (TALibFunction.Param var7 : var1.getOutputs()) {
         if (var7.getType().equals("TA_Output_Real")) {
            this.sb.append(", outDlb[" + var3 + "]");
            var3++;
         } else if (var7.getType().equals("TA_Output_Integer")) {
            this.sb.append(", outInt[" + var2 + "]");
            var2++;
         }
      }
   }

   private void prepareOutputs(TALibFunction var1) {
      int var2 = 0;
      int var3 = 0;
      boolean[] var4 = new boolean[var1.getOutputs().length];
      int var5 = 0;

      for (TALibFunction.Param var9 : var1.getOutputs()) {
         if (var9.getType().equals("TA_Output_Real")) {
            var3++;
            var4[var5++] = true;
         } else if (var9.getType().equals("TA_Output_Integer")) {
            var2++;
            var4[var5++] = false;
         }
      }

      if (var3 != 0) {
         this.sb.append("\t\toutDlb = new double[" + var3 + "][o.length];\n");
      }

      if (var2 != 0) {
         this.sb.append("\t\toutInt = new int[" + var2 + "][o.length];\n");
      }

      if (var5 > 0) {
         this.sb.append("\t\tresultFlags = new boolean[]{");

         for (int var10 = 0; var10 < var5; var10++) {
            this.sb.append(var4[var10]);
            if (var10 < var5 - 1) {
               this.sb.append(", ");
            }
         }

         this.sb.append("};\n");
      }
   }

   private void addOptParams(TALibFunction var1) {
      for (TALibFunction.Param var5 : var1.getOptInputs()) {
         String var6 = var5.getName();
         if (!var6.startsWith("optIn")) {
            throw new RuntimeException(L.t("invalid opt param %", new Object[]{var6}));
         }

         var6 = var6.substring(5);
         boolean var7 = this.isMin(var6);
         boolean var8 = this.isMax(var6);
         String var9;
         if (var5.getType().equals("TA_OptInput_IntegerList")) {
            var9 = "params.getInt(\"" + var6 + "\")";
         } else {
            var9 = "params.get" + this.getParamTyp(var5.getType()) + "(\"" + var6 + "\")";
         }

         if (var7) {
            var9 = "getMin(" + var9 + "," + this.getOpposite(false, var9) + ")";
         }

         if (var8) {
            var9 = "getMax(" + var9 + "," + this.getOpposite(true, var9) + ")";
         }

         if (this.isMAType(var6)) {
            var9 = "getMAType(" + var9 + ")";
         }

         this.sb.append(var9 + ",");
      }
   }

   private String getOpposite(boolean var1, String var2) {
      return var1 ? var2.replace("Max", "Min").replace("Slow", "Fast") : var2.replace("Min", "Max").replace("Fast", "Slow");
   }

   private boolean isMAType(String var1) {
      return var1.contains("MAType");
   }

   private boolean isMin(String var1) {
      return var1.equals("MinPeriod") || var1.equals("FastPeriod");
   }

   private boolean isMax(String var1) {
      return var1.equals("MaxPeriod") || var1.equals("SlowPeriod");
   }

   private String getParamTyp(String var1) {
      switch (var1) {
         case "TA_OptInput_IntegerRange":
            return "Int";
         case "TA_OptInput_RealRange":
            return "Double";
         default:
            return null;
      }
   }

   private boolean addIns(TALibFunction var1) {
      boolean var2 = false;

      for (TALibFunction.Param var6 : var1.getInputs()) {
         String var7 = var6.getName();
         if (var7.startsWith("inPrice")) {
            for (int var8 = 7; var8 < var7.length(); var8++) {
               char var9 = var7.charAt(var8);
               switch (var9) {
                  case 'C':
                     this.sb.append("c, ");
                     var2 = true;
                     break;
                  case 'H':
                     this.sb.append("h, ");
                     var2 = true;
                     break;
                  case 'L':
                     this.sb.append("l, ");
                     var2 = true;
                     break;
                  case 'O':
                     this.sb.append("o, ");
                     break;
                  case 'V':
                     this.sb.append("v, ");
                     break;
                  default:
                     throw new RuntimeException("invalid price arg: " + var9);
               }
            }
         } else if (var7.equals("inReal")) {
            this.sb.append("in0, ");
         } else if (var7.equals("inReal0")) {
            this.sb.append("in0, ");
         } else if (var7.equals("inReal1")) {
            this.sb.append("in1, ");
         } else if (var7.equals("inPeriods")) {
            this.sb.append("p, ");
         }
      }

      return var2;
   }

   private boolean shouldSkip(TALibFunction var1) {
      return this.blackListSet.contains(var1.getName());
   }

   private void caseEnd() {
      this.sb.append("\t\tbreak;\n");
   }

   private void caseBegin(TALibFunction var1) {
      this.sb.append("\tcase \"" + var1.getName() + "\":\n");
   }

   public static void main(String[] var0) {
      new TALibSwitchGenApp().generate();
   }
}
