package com.strategyquant.tradinglib.optimization;

import com.strategyquant.lib.L;
import com.strategyquant.lib.SQTime;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.ValuesMap;
import com.strategyquant.lib.XMLUtil;
import com.strategyquant.tradinglib.StrategyBase;
import com.strategyquant.tradinglib.TradingOption;
import com.strategyquant.tradinglib.Variable;
import com.strategyquant.tradinglib.options.TradingOptions;
import com.strategyquant.tradinglib.project.ProjectConfigHelper;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import org.jdom2.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ParametersSettings {
   public static final Logger Log = LoggerFactory.getLogger("ParametersSettings");
   public static final ValuesMap AllParamTypes = new ValuesMap();
   private static final String OPTMETHOD_FALBACK = "<OptimizationMethod settings=\"automatic\" symmetricVariables=\"false\" symmetryDisabled=\"false\" method=\"brute-force\" maxSteps=\"20\" />";
   private static final String WHATTOPARAM_FALLBACK = "<WhatToParametrize type=\"0\">\n      <Recommended>true</Recommended>\n      <Periods>false</Periods>\n      <Shifts>false</Shifts>\n      <Constants>false</Constants>\n      <OtherParams>false</OtherParams>\n      <EntryParams>false</EntryParams>\n      <EntryLogic>false</EntryLogic>\n      <ExitParamsUsed>false</ExitParamsUsed>\n      <ExitParamsUnused>false</ExitParamsUnused>\n      <BooleanParams>false</BooleanParams>\n      <TradingOptions>false</TradingOptions>\n    </WhatToParametrize>";
   private static final String AUTOSETTINGS_FALLBACK = "<AutomaticSettings distributionUp=\"20\" distributionDown=\"20\" maxSteps=\"20\" ignoreMagicNumbers=\"undefined\" ignoreBoolean=\"undefined\" ignoreDouble=\"undefined\" distribution=\"20\" />";
   public OptimizationParams params = new OptimizationParams();
   public boolean manualMode;
   public boolean symmetry;
   public int method;
   public int distributionUp;
   public int distributionDown;
   public int maxSteps;
   public long combinationsCount;
   public ValuesMap paramTypes = new ValuesMap();

   public ParametersSettings() {
      AllParamTypes.set("ParamTypePeriod", true);
      AllParamTypes.set("ParamTypeShift", false);
      AllParamTypes.set("ParamTypeConstant", false);
      AllParamTypes.set("ParamTypeOtherParam", false);
      AllParamTypes.set("ParamTypeEntryLevel", true);
      AllParamTypes.set("ParamTypeEntryLogic", false);
      AllParamTypes.set("ParamTypeExitUsed", true);
      AllParamTypes.set("ParamTypeExitUnused", false);
      AllParamTypes.set("ParamTypeBoolean", false);
      AllParamTypes.set("ParamTypeTradingOptions", false);
   }

   public void setFromXML(Element var1, Element var2) throws Exception {
      try {
         Element var3 = XMLUtil.getChildElem(var1, "Optimization");
         Element var4 = XMLUtil.getChildElem(
            var3,
            "OptimizationMethod",
            "<OptimizationMethod settings=\"automatic\" symmetricVariables=\"false\" symmetryDisabled=\"false\" method=\"brute-force\" maxSteps=\"20\" />"
         );
         Element var5 = XMLUtil.getChildElem(
            var3,
            "WhatToParametrize",
            "<WhatToParametrize type=\"0\">\n      <Recommended>true</Recommended>\n      <Periods>false</Periods>\n      <Shifts>false</Shifts>\n      <Constants>false</Constants>\n      <OtherParams>false</OtherParams>\n      <EntryParams>false</EntryParams>\n      <EntryLogic>false</EntryLogic>\n      <ExitParamsUsed>false</ExitParamsUsed>\n      <ExitParamsUnused>false</ExitParamsUnused>\n      <BooleanParams>false</BooleanParams>\n      <TradingOptions>false</TradingOptions>\n    </WhatToParametrize>"
         );
         int var6 = XMLUtil.getIntAttr(var3, "type", 1);
         boolean var7 = false;

         try {
            var7 = XMLUtil.getNodeValue(var5, "Recommended").equals("true");
         } catch (Exception var27) {
         }

         if (var7) {
            this.paramTypes.set("ParamTypeRecommended", true);
            this.symmetry = true;
         } else {
            this.paramTypes.set("ParamTypeRecommended", false);
            this.paramTypes.set("ParamTypePeriod", XMLUtil.getNodeValue(var5, "Periods").equals("true"));
            this.paramTypes.set("ParamTypeShift", XMLUtil.getNodeValue(var5, "Shifts").equals("true"));
            this.paramTypes.set("ParamTypeConstant", XMLUtil.getNodeValue(var5, "Constants").equals("true"));
            this.paramTypes.set("ParamTypeOtherParam", XMLUtil.getNodeValue(var5, "OtherParams").equals("true"));
            this.paramTypes.set("ParamTypeExitUsed", XMLUtil.getNodeValue(var5, "ExitParamsUsed").equals("true"));
            this.paramTypes.set("ParamTypeExitUnused", XMLUtil.getNodeValue(var5, "ExitParamsUnused").equals("true"));
            this.paramTypes.set("ParamTypeBoolean", XMLUtil.getNodeValue(var5, "BooleanParams").equals("true"));
            this.paramTypes.set("ParamTypeEntryLevel", XMLUtil.getNodeValue(var5, "EntryParams", "false").equals("true"));
            this.paramTypes.set("ParamTypeEntryLogic", XMLUtil.getNodeValue(var5, "EntryLogic", "false").equals("true"));
            this.symmetry = XMLUtil.elementIs(var4, "symmetricVariables");
         }

         this.paramTypes.set("ParamTypeTradingOptions", XMLUtil.getNodeValue(var5, "TradingOptions", "false").equals("true"));
         this.manualMode = var4.getAttributeValue("settings").equals("manual");
         this.method = this.getMethod(var4.getAttributeValue("method"));
         this.params.clear();
         if (this.manualMode) {
            for (Element var9 : XMLUtil.getNestedElements(var3, "Param")) {
               boolean var10 = XMLUtil.tryGetBoolAttr(var9, "use");
               String var11 = var9.getAttributeValue("name");
               String var12 = var9.getAttributeValue("type");
               String var13 = var9.getAttributeValue("value");
               byte var14 = Parameter.getType(var12);
               double var15 = 0.0;
               double var17 = 0.0;
               double var19 = 0.0;
               double var21 = 0.0;
               boolean var23 = XMLUtil.elementIs(var9, "calculateValues");
               if (var23) {
                  this.distributionUp = XMLUtil.getIntAttr(var9, "distributionUp", 20);
                  this.distributionDown = XMLUtil.getIntAttr(var9, "distributionDown", 20);
                  this.maxSteps = XMLUtil.getIntAttr(var9, "maxSteps", 10);
                  Parameter var24 = createParameter(
                     new Variable(var11, var11, var12, var13, false, false), this.distributionUp, this.distributionDown, this.maxSteps
                  );
                  this.params.addParam(var24);
                  var9.setAttribute("start", String.valueOf(var24.start));
                  var9.setAttribute("stop", String.valueOf(var24.stop));
                  var9.setAttribute("step", String.valueOf(var24.step));
               } else {
                  if (var14 != 4 && var14 != 1 && var14 != 5) {
                     var21 = var9.getAttributeValue("value").equals("true") ? 1.0 : 0.0;
                  } else {
                     var15 = Double.parseDouble(var9.getAttributeValue("start"));
                     var17 = Double.parseDouble(var9.getAttributeValue("stop"));
                     var19 = Double.parseDouble(var9.getAttributeValue("step"));
                     var21 = Double.parseDouble(var13);
                  }

                  this.params.addParam(var11, var14, var10, var15, var17, var19, var21);
               }
            }
         } else {
            Element var29 = XMLUtil.getChildElem(
               var3,
               "AutomaticSettings",
               "<AutomaticSettings distributionUp=\"20\" distributionDown=\"20\" maxSteps=\"20\" ignoreMagicNumbers=\"undefined\" ignoreBoolean=\"undefined\" ignoreDouble=\"undefined\" distribution=\"20\" />"
            );
            this.distributionUp = XMLUtil.getIntAttr(var29, "distributionUp", 20);
            this.distributionDown = XMLUtil.getIntAttr(var29, "distributionDown", 20);
            this.maxSteps = XMLUtil.tryGetIntAttr(var29, "maxSteps");
            StrategyBase var31 = StrategyBase.createXmlStrategy(var2);
            var31.transformToVariables(this.symmetry, this.paramTypes);
            ArrayList var33 = XMLUtil.getNestedElements(var2, "signal");
            TradingOptions var34 = null;

            try {
               var34 = ProjectConfigHelper.getOptions(XMLUtil.getChildElem(var1, "Options").getChild("BuildTradingOptions"));
            } catch (Exception var26) {
               Log.error("Cannot load trading options from settings", var26);
            }

            for (Variable var36 : var31.variables()) {
               if (!var36.getName().startsWith("Magic") && !this.isSignalVariable(var36.getId(), var33)) {
                  if (var34 != null && var36.getParamType() == "ParamTypeTradingOptions") {
                     String[] var37 = var36.getId().split("\\.");
                     if (var37.length == 2) {
                        String var38 = var37[0];
                        String var16 = var37[1];

                        for (int var39 = 0; var39 < var34.size(); var39++) {
                           TradingOption var18 = var34.get(var39);
                           if (var18.getClass().getSimpleName().equals(var38)) {
                              Object var40 = var18.getParameterValue(var16);
                              var36.setFromString(var40.toString());
                           }
                        }
                     }
                  }

                  this.params.addParam(createParameter(var36, this.distributionUp, this.distributionDown, this.maxSteps));
               }
            }

            if (Log.isDebugEnabled()) {
               Log.debug("Variables count: {}", var31.variables().size());
            }
         }

         this.combinationsCount = 1L;

         for (Parameter var32 : this.params) {
            try {
               if (var6 == 4) {
                  this.combinationsCount = Math.addExact(this.combinationsCount, var32.getCombinationsCount());
               } else {
                  this.combinationsCount = Math.multiplyExact(this.combinationsCount, var32.getCombinationsCount());
               }
            } catch (ArithmeticException var25) {
               this.combinationsCount = -1L;
            }

            if (this.combinationsCount < 0L) {
               break;
            }
         }

         Log.debug("Total combinations: {}", this.combinationsCount);
      } catch (Exception var28) {
         throw new Exception("Cannot load Parameters settings. " + var28.getMessage(), var28);
      }
   }

   private boolean isSignalVariable(String var1, ArrayList<Element> var2) {
      if (var2 != null && var2.size() != 0) {
         for (int var3 = 0; var3 < var2.size(); var3++) {
            Element var4 = (Element)var2.get(var3);
            String var5 = var4.getAttributeValue("variable");
            if (var5 != null && var5.equals(var1)) {
               return true;
            }
         }

         return false;
      } else {
         return false;
      }
   }

   public static void checkParamValid(double var0, double var2, double var4) throws Exception {
      if (var4 == 0.0 && (var0 != 0.0 || var2 != 0.0) && var0 != var2) {
         throw new Exception(L.t("Zero step used", new Object[0]));
      }

      if ((var2 - var0) / var4 <= 0.0) {
         throw new Exception(L.t("Invalid start/stop/step values", new Object[0]));
      }
   }

   public static void checkTimeParamValid(int var0, int var1, int var2) throws Exception {
      checkParamValid(var0, var1, var2);
      checkTimeValue(var0);
      checkTimeValue(var1);
      checkTimeValue(var2);
   }

   private static void checkTimeValue(int var0) throws Exception {
      if (var0 < 0 || var0 > 86340) {
         throw new Exception(L.t("Time must be in range 00:00 - 23:59", new Object[0]));
      }
   }

   private int getMethod(String var1) {
      return var1 != null && var1.equals("brute-force") ? 1 : 0;
   }

   public static Parameter createParameter(Variable var0, int var1, int var2, int var3) {
      String var4 = var0.getName();
      byte var5 = var0.getInternalType();
      if (var0.getParamType() == null) {
         var0.setParamType("ParamTypeOtherParam");
      }

      double var6 = 0.0;
      double var8 = 0.0;
      double var10 = 0.0;
      double var12 = 0.0;
      String var14 = var0.getId();
      if (var14.contains("MinimumSL") || var14.contains("MaximumSL") || var14.contains("MinimumPT") || var14.contains("MaximumPT")) {
         var12 = var0.getValueAsDouble();
         var6 = 0.0;
         var8 = 500.0;
         var10 = findStep((int)var6, (int)var8, var3);
      } else if (var5 == 4) {
         double var15 = var0.getValueAsDouble();
         double var17 = var15 / 100.0 * var1;
         double var19 = var15 / 100.0 * var2;
         byte var21 = 2;
         var12 = var0.getValueAsDouble();
         var6 = SQUtils.round(var15 - var19, var21);
         var8 = SQUtils.round(var15 + var17, var21);
         var10 = findStep(var6, var8, var21, var3);
      } else if (var5 == 1) {
         int var28 = (int)var0.getMin();
         int var16 = (int)var0.getMax();
         if (var28 != Integer.MIN_VALUE && var16 != Integer.MIN_VALUE) {
            var6 = var28;
            var8 = var16;
         } else {
            double var31 = var0.getValueAsDouble();
            double var33 = var31 / 100.0 * var1;
            double var34 = var31 / 100.0 * var2;
            var12 = var0.getValueAsDouble();
            var6 = (int)(var31 - var34);
            var8 = (int)(var31 + var33);
            if (var31 <= 3.0 & var8 <= 4.0) {
               var8 = 6.0;
            }
         }

         if (var0.getParamType().equals("ParamTypePeriod")) {
            var6 = var6 > 2.0 ? var6 : 2.0;
         }

         var10 = findStep((int)var6, (int)var8, var3);
      } else if (var5 == 5) {
         var12 = var0.getValueAsInt();
         double var29 = SQTime.HHMMToMinutes((int)var12);
         int var32 = (int)(var29 / 100.0 * var1);
         int var18 = (int)(var29 / 100.0 * var2);
         var6 = var29 - var18;
         var6 = var6 < 0.0 ? 0.0 : var6;
         var8 = var29 + var32;
         var8 = var8 > 1439.0 ? 1439.0 : var8;
         var10 = findStep((int)var6, (int)var8, var3);
         var6 = SQTime.minutesToHHMM((int)var6);
         var8 = SQTime.minutesToHHMM((int)var8);
         var10 = SQTime.minutesToHHMM((int)var10);
      } else if (var5 == 2) {
         boolean var30 = var0.getValueAsBoolean();
         var12 = var30 ? 1.0 : 0.0;
         var6 = var12;
      }

      return new Parameter(var4, var5, true, var6, var8, var10, var12);
   }

   private static int findStep(int var0, int var1, int var2) {
      double var3 = ((double)var1 - var0) / (var2 - 1);
      if (Math.abs(var3 - (int)var3) < 0.001) {
         return (int)var3;
      }

      if (var1 - var0 > 0) {
         var3 = var3 < 1.0 ? 1.0 : (int)var3;
      } else {
         var3 = var3 > -1.0 ? -1.0 : (int)var3;
      }

      for (int var5 = getStepsCount(var0, var1, var3); var5 > var2 - 1; var5 = getStepsCount(var0, var1, var3)) {
         var3 += var3 > 0.0 ? 1.0 : -1.0;
      }

      return (int)var3;
   }

   private static double findStep(double var0, double var2, int var4, int var5) {
      double var6 = (var2 - var0) / (var5 - 1);
      double var8 = SQUtils.round(var6, var4);
      double var10 = 1.0 / Math.pow(10.0, var4);
      if (var8 == var6) {
         return var8;
      }

      if (var8 > var6) {
         var6 = var6 > 0.0 ? var8 - var10 : var8;
      } else {
         var6 = var6 > 0.0 ? var8 : var8 + var10;
      }

      for (int var12 = getStepsCount(var0, var2, var6); var12 > var5 - 1; var12 = getStepsCount(var0, var2, var6)) {
         var6 += var6 >= 0.0 ? var10 : -var10;
      }

      return SQUtils.round(var6, var4);
   }

   private static int getStepsCount(double var0, double var2, double var4) {
      return (int)((var2 - var0) / var4);
   }

   public static HashMap<String, ParametersSettings> loadParamConfig(File var0) throws Exception {
      HashMap var1 = new HashMap();

      try {
         BufferedReader var2 = new BufferedReader(new FileReader(var0));

         try {
            int var4 = 0;

            String var3;
            while ((var3 = var2.readLine()) != null) {
               var4++;
               if (var3.length() >= 2) {
                  String[] var5 = var3.split(";");
                  if (var5.length == 0) {
                     throw new Exception("Bad format at line #" + var4 + ": " + var3);
                  }

                  ParametersSettings var6 = new ParametersSettings();
                  var6.setFromXMLOptimConfig(var5, var3, var4);
                  var1.put(var5[0], var6);
               }
            }
         } catch (Throwable var8) {
            try {
               var2.close();
            } catch (Throwable var7) {
               var8.addSuppressed(var7);
            }

            throw var8;
         }

         var2.close();
         return var1;
      } catch (Exception var9) {
         throw var9;
      }
   }

   public void setFromXMLOptimConfig(String[] var1, String var2, int var3) throws Exception {
      try {
         this.manualMode = true;
         this.method = this.getMethod("brute-force");
         this.params.clear();

         for (int var4 = 1; var4 < var1.length; var4++) {
            String[] var5 = var1[var4].split(":");
            if (var5.length != 6) {
               throw new Exception("Cannot parse line #" + var3 + ": " + var2);
            }

            String var6 = var5[0];
            byte var7 = Parameter.getType(var5[1]);
            double var8 = Double.parseDouble(var5[2]);
            double var10 = Double.parseDouble(var5[3]);
            double var12 = Double.parseDouble(var5[4]);
            double var14 = Double.parseDouble(var5[5]);
            this.params.addParam(var6, var7, true, var10, var12, var14, var8);
         }

         this.combinationsCount = 1L;

         for (Parameter var19 : this.params) {
            try {
               this.combinationsCount = Math.multiplyExact(this.combinationsCount, var19.getCombinationsCount());
            } catch (ArithmeticException var16) {
               this.combinationsCount = -1L;
            }

            if (this.combinationsCount < 0L) {
               break;
            }
         }

         Log.debug("Total combinations: {}", this.combinationsCount);
      } catch (Exception var17) {
         throw new Exception("Cannot load Parameters settings. " + var17.getMessage(), var17);
      }
   }
}
