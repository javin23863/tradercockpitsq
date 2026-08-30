package com.strategyquant.tradinglib.talib.reflection;

import com.tictactec.ta.lib.meta.annotation.InputParameterType;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class TALibFunction {
   private String name;
   private int inputCount;
   private int optInputCount;
   private int outputCount;
   private Method talibMethod;
   private ArrayList<TALibFunction.Param> inputs = new ArrayList<>();
   private ArrayList<TALibFunction.Param> optInputs = new ArrayList<>();
   private ArrayList<TALibFunction.Param> outputs = new ArrayList<>();
   private Map<String, TALibFunction.IntegerParams> intParams = new HashMap<>();
   private Map<String, TALibFunction.DoubleParams> dblParams = new HashMap<>();

   public TALibFunction(Method var1, String var2, int var3, int var4, int var5) {
      this.talibMethod = var1;
      this.name = var2;
      this.inputCount = var3;
      this.optInputCount = var4;
      this.outputCount = var5;
   }

   public Method getTalibMethod() {
      return this.talibMethod;
   }

   public void addInput(TALibFunction.Param var1) {
      this.inputs.add(var1);
   }

   public void addOptInput(TALibFunction.Param var1) {
      this.optInputs.add(var1);
   }

   public void addOutput(TALibFunction.Param var1) {
      this.outputs.add(var1);
   }

   public TALibFunction.Param[] getInputs() {
      return this.inputs.toArray(new TALibFunction.Param[this.inputs.size()]);
   }

   public TALibFunction.Param[] getOptInputs() {
      return this.optInputs.toArray(new TALibFunction.Param[this.optInputs.size()]);
   }

   public TALibFunction.Param[] getOutputs() {
      return this.outputs.toArray(new TALibFunction.Param[this.outputs.size()]);
   }

   public int getInputParameterCount() {
      int var1 = 0;
      if (this.outputs.size() > 1) {
         var1++;
      }

      for (TALibFunction.Param var3 : this.inputs) {
         if (!var3.getType().equals(InputParameterType.TA_Input_Price.toString())) {
            var1++;
         }
      }

      return this.optInputCount + var1;
   }

   public String getName() {
      return this.name;
   }

   @Override
   public String toString() {
      String var1 = this.name + "(" + this.inputCount + "," + this.optInputCount + "," + this.outputCount + ") \n";

      for (TALibFunction.Param var3 : this.inputs) {
         var1 = var1 + "\t" + var3.getName() + "=" + var3.getType() + "\n";
      }

      for (TALibFunction.Param var6 : this.optInputs) {
         var1 = var1 + "\t" + var6.getName() + "=" + var6.getType() + "\n";
      }

      for (TALibFunction.Param var7 : this.outputs) {
         var1 = var1 + "\t" + var7.getName() + "=" + var7.getType() + "\n";
      }

      return var1;
   }

   public TALibFunction.IntegerParams getIntParameter(String var1) {
      return this.intParams.get(var1);
   }

   public void addIntegerRange(String var1, TALibFunction.IntegerParams var2) {
      this.intParams.put(var1, var2);
   }

   public TALibFunction.DoubleParams getDoubleParameter(String var1) {
      return this.dblParams.get(var1);
   }

   public void addDoubleRange(String var1, TALibFunction.DoubleParams var2) {
      this.dblParams.put(var1, var2);
   }

   public static class DoubleParams {
      private double min;
      private double max;
      private double def;
      private double step;
      private double suggStart;
      private double suggEnd;

      public DoubleParams(double var1, double var3, double var5, double var7, double var9, double var11) {
         this.min = var1;
         this.max = var3;
         this.def = var5;
         this.suggStart = var7;
         this.suggEnd = var9;
         this.step = var11;
      }

      public double getSuggStart() {
         return this.suggStart;
      }

      public void setSuggStart(double var1) {
         this.suggStart = var1;
      }

      public double getSuggEnd() {
         return this.suggEnd;
      }

      public void setSuggEnd(double var1) {
         this.suggEnd = var1;
      }

      public double getMin() {
         return this.min;
      }

      public void setMin(double var1) {
         this.min = var1;
      }

      public double getMax() {
         return this.max;
      }

      public void setMax(double var1) {
         this.max = var1;
      }

      public double getDef() {
         return this.def;
      }

      public void setDef(double var1) {
         this.def = var1;
      }

      public double getStep() {
         return this.step;
      }

      public void setStep(double var1) {
         this.step = var1;
      }
   }

   public static class IntegerParams {
      private int min;
      private int max;
      private int def;
      private int step;
      private int suggStart;
      private int suggEnd;

      public IntegerParams(int var1, int var2, int var3, int var4, int var5, int var6) {
         this.min = var1;
         this.max = var2;
         this.def = var3;
         this.suggStart = var4;
         this.suggEnd = var5;
         this.step = var6;
      }

      public int getSuggStart() {
         return this.suggStart;
      }

      public void setSuggStart(int var1) {
         this.suggStart = var1;
      }

      public int getSuggEnd() {
         return this.suggEnd;
      }

      public void setSuggEnd(int var1) {
         this.suggEnd = var1;
      }

      public int getMin() {
         return this.min;
      }

      public void setMin(int var1) {
         this.min = var1;
      }

      public int getMax() {
         return this.max;
      }

      public void setMax(int var1) {
         this.max = var1;
      }

      public int getDef() {
         return this.def;
      }

      public void setDef(int var1) {
         this.def = var1;
      }

      public int getStep() {
         return this.step;
      }

      public void setStep(int var1) {
         this.step = var1;
      }
   }

   public static class Param {
      private String name;
      private String type;
      private String displayName;
      private int flags;

      public Param(String var1, String var2, int var3) {
         this(var1, var2, var1, var3);
      }

      public Param(String var1, String var2, String var3, int var4) {
         this.name = var1;
         this.type = var2;
         this.flags = var4;
         this.setDisplayName(var3);
      }

      public int getFlags() {
         return this.flags;
      }

      public String getName() {
         return this.name;
      }

      public void setName(String var1) {
         this.name = var1;
      }

      public String getType() {
         return this.type;
      }

      public void setType(String var1) {
         this.type = var1;
      }

      public boolean isSame(TALibFunction.Param var1) {
         return this.name.equals(var1.getName()) && this.type.equals(var1.getType());
      }

      public String getDisplayName() {
         return this.displayName;
      }

      public void setDisplayName(String var1) {
         this.displayName = var1;
      }
   }
}
