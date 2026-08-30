package com.strategyquant.tradinglib.blocks.random;

import com.strategyquant.datalib.customData.CustomDataInfo;
import com.strategyquant.lib.IRandomGenerator;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.tradinglib.blocks.annotations.Value;
import com.strategyquant.tradinglib.generator.BlockSettingsConverter;
import com.strategyquant.tradinglib.generator.GenerateException;
import java.util.ArrayList;
import org.jdom2.Attribute;
import org.jdom2.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReplacementParameter {
   public static final Logger Log = LoggerFactory.getLogger("ReplacementParameter");
   public static final byte Fixed = 1;
   public static final byte Random = 2;
   public static final byte Formula = 3;
   public static final double ATR_BASED_EXIT = -1.0;
   public static final double FIXED_PIPS_EXIT = 1.0;
   public BlockParameter blockParam;
   public byte generation;
   private BlockDefinition blockDef;
   private ReplacementConfig replacementConfig;
   private ReplacementsConfig replacementsConfig;
   private double formulaProbability;
   public String formulaDependentOn;
   private ArrayList<FormulaValue> formulaValues;
   private FormulaValue formulaNoneValue = null;
   private int formulaCummulativeWeight;
   private BlocksConfig blocksConfig;
   private int MinSLInPips;
   private int MaxSLInPips;
   private double MinSLInPercent;
   private double MaxSLInPercent;
   private double MinSLATRMultiple;
   private double MaxSLATRMultiple;
   private int MinSLATRPeriod;
   private int MaxSLATRPeriod;
   private int MinPTInPips;
   private int MaxPTInPips;
   private double MinPTInPercent;
   private double MaxPTInPercent;
   private double MinPTATRMultiple;
   private double MaxPTATRMultiple;
   private int MinPTATRPeriod;
   private int MaxPTATRPeriod;
   private boolean LimitSLPTRRR;
   private double LimitSLPTRRRFromCoef;
   private double LimitSLPTRRRToCoef;

   public ReplacementParameter(byte var1, ReplacementsConfig var2, BlockParameter var3, BlocksConfig var4, BlockDefinition var5, ReplacementConfig var6) {
      this.blockParam = var3.clone();
      this.replacementConfig = var6;
      this.replacementsConfig = var2;
      this.blocksConfig = var4;
      this.blockDef = var5;
      this.generation = 2;
      this.MinSLInPips = var6.buildSettings.getInt("MinSLInPips");
      this.MaxSLInPips = var6.buildSettings.getInt("MaxSLInPips");
      if (this.MaxSLInPips < this.MinSLInPips) {
         Log.error(String.format("=== ERROR - MAX is smaller than MIN, min: %d, max: %d", this.MinSLInPips, this.MaxSLInPips));
      }

      this.MinPTInPercent = var6.buildSettings.getDouble("MinPTInPercent");
      this.MaxPTInPercent = var6.buildSettings.getDouble("MaxPTInPercent");
      this.MinSLInPercent = var6.buildSettings.getDouble("MinSLInPercent");
      this.MaxSLInPercent = var6.buildSettings.getDouble("MaxSLInPercent");
      this.MinSLATRMultiple = var6.buildSettings.getDouble("MinSLATRMultiple");
      this.MaxSLATRMultiple = var6.buildSettings.getDouble("MaxSLATRMultiple");
      this.MinSLATRPeriod = var6.buildSettings.getInt("MinSLATRPeriod");
      this.MaxSLATRPeriod = var6.buildSettings.getInt("MaxSLATRPeriod");
      this.MinPTInPips = var6.buildSettings.getInt("MinPTInPips");
      this.MaxPTInPips = var6.buildSettings.getInt("MaxPTInPips");
      this.MinPTATRMultiple = var6.buildSettings.getDouble("MinPTATRMultiple");
      this.MaxPTATRMultiple = var6.buildSettings.getDouble("MaxPTATRMultiple");
      this.MinPTATRPeriod = var6.buildSettings.getInt("MinPTATRPeriod");
      this.MaxPTATRPeriod = var6.buildSettings.getInt("MaxPTATRPeriod");
      this.LimitSLPTRRR = var6.buildSettings.getBoolean("LimitSLPTRRR");
      if (this.LimitSLPTRRR) {
         double var7 = var6.buildSettings.getInt("LimitSLPTRRRFrom");
         double var9 = var6.buildSettings.getInt("LimitSLPTRRRTo");
         this.LimitSLPTRRRFromCoef = 1.0 / (var7 / 100.0);
         this.LimitSLPTRRRToCoef = 1.0 / (var9 / 100.0);
      }
   }

   ReplacementParameter() {
   }

   private double fixValue(double var1, ReplacementChartConfig var3, boolean var4) {
      int var5 = (int)var1;
      switch (var5) {
         case -1000020:
            return this.MaxSLInPercent;
         case -1000019:
            return this.MinSLInPercent;
         case -1000018:
            return this.MaxPTInPercent;
         case -1000017:
            return this.MinPTInPercent;
         case -1000016:
            return this.MaxPTATRPeriod;
         case -1000015:
            return this.MinPTATRPeriod;
         case -1000014:
            return this.MaxPTATRMultiple;
         case -1000013:
            return this.MinPTATRMultiple;
         case -1000012:
            return this.MaxPTInPips;
         case -1000011:
            return this.MinPTInPips;
         case -1000010:
            return this.MaxSLATRPeriod;
         case -1000009:
            return this.MinSLATRPeriod;
         case -1000008:
            return this.MaxSLATRMultiple;
         case -1000007:
            return this.MinSLATRMultiple;
         case -1000006:
            return this.MaxSLInPips;
         case -1000005:
            return this.MinSLInPips;
         case -1000004:
            return var3 == null ? var1 : var3.maxPeriod;
         case -1000003:
            return var3 == null ? var1 : var3.minPeriod;
         case -1000002:
            return var3 == null ? var1 : var3.maxShift;
         case -1000001:
            return var3 == null ? var1 : var3.minShift;
         default:
            return var1;
      }
   }

   private boolean isConstantValue(int var1) {
      switch (var1) {
         case -1000016:
         case -1000015:
         case -1000014:
         case -1000013:
         case -1000012:
         case -1000011:
         case -1000010:
         case -1000009:
         case -1000008:
         case -1000007:
         case -1000006:
         case -1000005:
         case -1000004:
         case -1000003:
         case -1000002:
         case -1000001:
            return true;
         default:
            return false;
      }
   }

   public void initFromGeneratedParam(String var1, Element var2, Element var3) throws GenerateException {
      String var4 = var2.getAttributeValue("generation");
      if (var4 == null) {
         String var12 = var2.getParentElement().getParentElement().getAttributeValue("key");
         if (var12 == null) {
            var12 = "Unknown";
         }

         throw new GenerateException(
            String.format("Bad config - missing 'generation' attribute for parameter with key %s->%s!", var12, var2.getAttributeValue("key"))
         );
      } else {
         switch (var4) {
            case "fixed":
               this.generation = 1;
               break;
            case "random":
               this.generation = 2;
               break;
            case "formula":
               this.generation = 3;
         }

         boolean var11 = false;

         for (Attribute var7 : var2.getAttributes()) {
            String var8 = var7.getName();
            String var9 = var7.getValue();
            if (!var9.equals("undefined") && !var9.equals("null") && !var8.equals("key") && !var8.equals("name") && !var9.equals("")) {
               this.blockParam.setParamAttribute(var8, var9);
               var11 = true;
            }
         }

         if (this.generation == 1) {
            String var14 = var2.getAttributeValue("type");
            if (var14 != null) {
               String var15 = var2.getText();
               if (var15 != null && !var15.equals("")) {
                  try {
                     if (var14.equals("int")) {
                        int var16 = Integer.parseInt(var15);
                        this.blockParam.iDefaultValue = var16;
                     } else if (var14.equals("double")) {
                        double var17 = Double.parseDouble(var15);
                        this.blockParam.dDefaultValue = var17;
                     }
                  } catch (NumberFormatException var10) {
                  }
               }
            }
         }

         if (var11) {
            this.blockParam.initializeRanges(this.generation == 2, this.blockDef.key);
            this.blockParam.recognizeDecimals();
         }

         if (this.generation == 3 && var3 != null) {
            this.initFormulas(var3);
         }
      }
   }

   private void initFormulas(Element var1) throws GenerateException {
      for (Element var3 : var1.getChildren()) {
         if (var3.getAttributeValue("key").equals(this.blockParam.key)) {
            this.formulaDependentOn = var3.getAttributeValue("dependentOn");
            this.formulaValues = new ArrayList<>();
            String var4 = var3.getAttributeValue("probability");
            if (var4 == null) {
               this.formulaProbability = 0.5;
            } else {
               this.formulaProbability = Integer.parseInt(var4) / 100.0;
            }

            this.formulaCummulativeWeight = 0;

            for (Element var6 : var3.getChildren()) {
               if (var6.getName().contains("Value")) {
                  String var7 = var6.getAttributeValue("use");
                  if (var7 == null || !var7.equals("false")) {
                     FormulaValue var8 = new FormulaValue(this.replacementsConfig, var6, this.blocksConfig, this.replacementConfig);
                     if (var8.isNoneValue) {
                        this.formulaNoneValue = var8;
                     } else {
                        this.formulaCummulativeWeight = this.formulaCummulativeWeight + var8.weight;
                        this.formulaValues.add(var8);
                     }
                  }
               }
            }
            break;
         }
      }
   }

   public void setDefaultValue(String var1) {
      this.blockParam.setDefaultValue(var1);
   }

   public void addIntParam(Element var1, IRandomGenerator var2, ReplacementChartConfig var3, int var4, ItemRandomValueRanges var5) throws GenerateException {
      Element var7 = new Element("Param");
      var7.setAttribute("key", this.blockParam.key);
      var7.setAttribute("controlType", this.blockParam.controlType);
      var7.setAttribute("type", this.blockParam.type);
      if (this.blockParam.exitMethod != null) {
         var7.setAttribute("exitMethod", this.blockParam.exitMethod);
      }

      if (this.blockParam.exitMethodType != null) {
         var7.setAttribute("exitMethodType", this.blockParam.exitMethodType);
      }

      try {
         if (this.generation == 1) {
            if (this.blockParam.couldBeVariable) {
               if (this.replacementConfig.buildSettings.containsKey(this.blockParam.defaultValue)) {
                  var7.setAttribute("variable", "true");
                  var7.setAttribute("variableType", "INT");
                  if (var4 != -1) {
                     var7.addContent(Integer.toString(var4));
                  } else {
                     var7.addContent(this.replacementConfig.buildSettings.getString(this.blockParam.defaultValue));
                  }
               }
            } else {
               int var6;
               if (var4 != -1) {
                  var6 = var4;
               } else {
                  var6 = this.blockParam.iDefaultValue;
               }

               var7.addContent(Integer.toString(var6));
            }
         } else {
            int var10;
            if (var4 != -1) {
               var10 = var4;
            } else {
               var10 = this.getIntParam(var2, var3, var5);
            }

            var7.addContent(Integer.toString(var10));
         }

         var1.addContent(var7);
      } catch (IllegalArgumentException var9) {
         if (var9.getMessage().contains("n must be positive")) {
            Log.error("Exception generating parameter for {}.{}", var1.getAttributeValue("key"), this.blockParam.key);
         }

         throw var9;
      }
   }

   public int getIntParam(IRandomGenerator var1, ReplacementChartConfig var2, ItemRandomValueRanges var3) throws GenerateException {
      boolean var5 = false;
      if (this.blockParam.key.contains("ExitAfterBars")) {
         String var6 = this.blockParam.getParamAttribute("probability");
         if (var6 != null && !var6.equals("100") && var1.nextInt(2) == 0) {
            var5 = true;
         }
      }

      int var4;
      if (var5) {
         var4 = 0;
      } else if (this.blockParam.controlType.equals("combo")) {
         String var13 = this.blockParam.values;
         String[] var7 = var13.split(",");
         int var8 = var1.nextInt(var7.length);
         String var9 = var7[var8];
         String[] var10 = var9.split("=");
         var4 = Integer.parseInt(var10[1]);
      } else {
         var4 = Integer.MAX_VALUE;
         if (var3 != null) {
            var4 = (int)this.replacementConfig.generateValueFromRandomValue(this.blockParam.key, var3, var1, false);
         } else if (this.blockParam.rvConfig != null) {
            var4 = (int)this.blockParam.rvConfig.generateRandomValue(var1, false);
         }

         if (var4 == Integer.MAX_VALUE) {
            if (!this.blockParam.controlType.equals("jspinnerVar") && !this.blockParam.controlType.equals("jspinner")) {
               if (!this.blockParam.controlType.equals("comboVar")) {
                  throw new GenerateException("Unsupported control type '" + this.blockParam.controlType + "' when generating INT random parameter!");
               }

               var4 = 0;
            } else if (!this.blockParam.key.equals("#TimeFrom#") && !this.blockParam.key.equals("#TimeTo#")) {
               int var15 = (int)this.blockParam.minValue;
               int var17 = (int)this.blockParam.maxValue;
               int var18 = var15;
               int var19 = var17;
               if (var2 != null) {
                  var18 = (int)this.fixValue(var15, var2, true);
                  var19 = (int)this.fixValue(var17, var2, false);
               }

               int var20 = (int)this.blockParam.step;
               if (var20 <= 0) {
                  var20 = 1;
               }

               if (var19 < var18) {
                  int var11 = var18;
                  var18 = var19;
                  var19 = var11;
               }

               if (var19 == var18) {
                  var4 = var19;
               } else if (var20 == 0) {
                  var4 = var18;
               } else {
                  int var21 = (var19 - var18) / var20 + 1;
                  if (var21 <= 0) {
                  }

                  int var12 = var1.nextInt(var21);
                  var4 = var18 + var12 * var20;
               }
            } else {
               int var14 = Value.generateHH(var1, this.blockParam);
               int var16 = Value.generateMM(var1, this.blockParam);
               var4 = var14 * 100 + var16;
            }
         }
      }

      return var4;
   }

   public void addDoubleParam(Element var1, IRandomGenerator var2, ItemRandomValueRanges var3) throws GenerateException {
      this._addDoubleParam(var1, var2, -1.0, -1.0, var3);
   }

   private void _addDoubleParam(Element var1, IRandomGenerator var2, double var3, double var5, ItemRandomValueRanges var7) throws GenerateException {
      Element var8 = new Element("Param");
      var8.setAttribute("key", this.blockParam.key);
      var8.setAttribute("controlType", this.blockParam.controlType);
      var8.setAttribute("type", this.blockParam.type);
      if (this.blockParam.exitMethod != null) {
         var8.setAttribute("exitMethod", this.blockParam.exitMethod);
      }

      if (this.blockParam.exitMethodType != null) {
         var8.setAttribute("exitMethodType", this.blockParam.exitMethodType);
      }

      String var9 = this.getDoubleParam(var2, var3, var5, var7);
      var8.addContent(var9);
      var1.addContent(var8);
   }

   public String getDoubleParam(IRandomGenerator var1, double var2, double var4, ItemRandomValueRanges var6) {
      String var7;
      if (this.generation == 1) {
         double var8 = this.blockParam.dDefaultValue;
         int var10 = this.blockParam.decimals;
         var10 = this.replacementConfig.getDecimals(var8, var10);
         var7 = SQUtils.d2String(var8, var10);
      } else if (this.blockParam.controlType.equals("combo")) {
         String var19 = this.blockParam.values;
         String[] var9 = var19.split(",");
         int var22 = var1.nextInt(var9.length);
         String var11 = var9[var22];
         String[] var12 = var11.split("=");
         var7 = var12[1];
      } else {
         double var20 = Double.MAX_VALUE;
         int var23 = this.blockParam.decimals;
         if (var6 != null) {
            var20 = this.replacementConfig.generateValueFromRandomValue(this.blockParam.key, var6, var1, true);
            var23 = this.replacementConfig.getDecimals(this.blockParam.key, var6, var23);
         } else if (this.blockParam.rvConfig != null) {
            var20 = this.blockParam.rvConfig.generateRandomValue(var1, true);
            var23 = this.blockParam.rvConfig.getDecimals(var23);
         }

         if (var20 == Double.MAX_VALUE) {
            if (!this.blockParam.controlType.equals("jspinnerVar") && !this.blockParam.controlType.equals("jspinner")) {
               var20 = this.blockParam.dDefaultValue;
            } else {
               double var24 = this.fixValue(this.blockParam.minValue, null, true);
               double var13 = this.fixValue(this.blockParam.maxValue, null, false);
               double var15 = this.blockParam.step;
               if (var2 != -1.0 && var2 > var24) {
                  if (var2 < var13) {
                     var24 = var2;
                  } else {
                     var24 = var13;
                  }
               }

               if (var4 != -1.0 && var4 < var13) {
                  if (var4 > var24) {
                     var13 = var4;
                  } else {
                     var13 = var24;
                  }
               }

               if (var24 == var13) {
                  var20 = var13;
               } else if (var15 == 0.0) {
                  var20 = var24;
               } else {
                  if (var24 < 0.0 && var13 < 0.0 && var15 > 0.0) {
                     var15 *= -1.0;
                  }

                  int var17 = (int)((var13 - var24) / var15) + 1;
                  if (var17 < 0) {
                     if (var24 < 0.0) {
                        var24 = -10000.0;
                     } else {
                        var24 = 0.0;
                     }

                     if (var13 > 0.0) {
                        var13 = 10000.0;
                     } else {
                        var13 = 0.0;
                     }

                     if (var15 < 1.0) {
                        var15 = 1.0;
                     }

                     var17 = (int)((var13 - var24) / var15) + 1;
                  }

                  int var18 = var1.nextInt(var17);
                  var20 = var24 + var18 * var15;
               }
            }
         }

         var7 = SQUtils.d2String(var20, var23);
      }

      return var7;
   }

   public void addBooleanParam(Element var1, IRandomGenerator var2) throws GenerateException {
      boolean var3 = false;
      if (this.generation == 1) {
         var3 = this.blockParam.bDefaultValue;
      } else {
         if (!this.blockParam.controlType.equals("booleanVar")) {
            throw new GenerateException("Unsupported control type '" + this.blockParam.controlType + "' when generating BOOLEAN random parameter!");
         }

         if (var2.nextInt(2) == 1) {
            var3 = true;
         }
      }

      Element var4 = new Element("Param");
      var4.setAttribute("key", this.blockParam.key);
      var4.setAttribute("controlType", this.blockParam.controlType);
      var4.setAttribute("type", this.blockParam.type);
      var4.addContent(Boolean.toString(var3));
      var1.addContent(var4);
   }

   public void addStringParam(Element var1, IRandomGenerator var2) throws GenerateException {
      String var3 = "";
      if (this.generation == 1) {
         var3 = this.blockParam.defaultValue;
      } else if (!this.blockParam.controlType.equals("symbolVar") && !this.blockParam.controlType.equals("editbox")) {
         throw new GenerateException("Unsupported control type '" + this.blockParam.controlType + "' when generating STRING random parameter!");
      }

      Element var4 = new Element("Param");
      var4.setAttribute("key", this.blockParam.key);
      var4.setAttribute("controlType", this.blockParam.controlType);
      var4.setAttribute("type", this.blockParam.type);
      var4.addContent(var3);
      var1.addContent(var4);
   }

   public void addDataParam(Element var1, IRandomGenerator var2, ReplacementChartConfig var3) {
      Element var4 = new Element("Param");
      var4.setAttribute("key", this.blockParam.key);
      var4.setAttribute("controlType", this.blockParam.controlType);
      var4.setAttribute("type", this.blockParam.type);
      var4.addContent(Integer.toString(var3.chartIndex));
      var1.addContent(var4);
   }

   public void addFormulaParam(
      Element var1, IRandomGenerator var2, ArrayList<ReplacementParameter> var3, ReplacementChartConfig var4, int var5, double[] var6, int var7
   ) throws GenerateException {
      Element var8 = new Element("Param");
      var8.setAttribute("key", this.blockParam.key);
      var8.setAttribute("controlType", this.blockParam.controlType);
      var8.setAttribute("type", this.blockParam.type);
      if (this.blockParam.exitMethod != null) {
         var8.setAttribute("exitMethod", this.blockParam.exitMethod);
      }

      if (this.blockParam.exitMethodType != null) {
         var8.setAttribute("exitMethodType", this.blockParam.exitMethodType);
      }

      Element var9;
      if (this.formulaValues != null && this.formulaValues.size() != 0) {
         var9 = this.generateFormula(var1, var2, var3, var4, var5, var6, var7);
      } else {
         if (this.formulaNoneValue == null) {
            throw new GenerateException("No formula values specified for parameter '" + this.blockParam.key + "'");
         }

         var9 = new Element("Formula");
         var9.setAttribute("key", this.formulaNoneValue.key);
      }

      var8.addContent(var9);
      var1.addContent(var8);
   }

   private Element generateFormula(
      Element var1, IRandomGenerator var2, ArrayList<ReplacementParameter> var3, ReplacementChartConfig var4, int var5, double[] var6, int var7
   ) throws GenerateException {
      Element var8 = new Element("Formula");
      boolean var9 = false;
      if (this.formulaDependentOn != null) {
         Element var10 = this.findDependentParam(var1, this.formulaDependentOn);
         if (var10 == null) {
            throw new GenerateException("Dependent formula '" + this.formulaDependentOn + "' not found in for formula '" + this.blockParam.key + "'");
         }

         if (!this.dependentParamIsNone(var10, var3)) {
            var9 = true;
         }
      } else if (this.formulaProbability < 1.0) {
         var9 = var2.probability(this.formulaProbability);
      } else {
         var9 = true;
      }

      if (!var9) {
         if (this.formulaNoneValue == null) {
            throw new GenerateException("There is no valueNone specified for formula '" + this.blockParam.key + "'");
         }

         var8.setAttribute("key", this.formulaNoneValue.key);
         return var8;
      } else {
         FormulaValue var12 = this.generateFVRandomly(var2, var6);
         String var11 = var12.key;
         var8.setAttribute("key", var11);
         this.generateFormulaParameters(var12, var8, var2, var4, var5, var6, var7);
         return var8;
      }
   }

   private void generateFormulaParameters(
      FormulaValue var1, Element var2, IRandomGenerator var3, ReplacementChartConfig var4, int var5, double[] var6, int var7
   ) throws GenerateException {
      if (var1.parameters != null && var1.parameters.size() != 0) {
         ReplacementParameters var8 = var1.parameters.chooseRandomly(var3, var4, null);

         for (ReplacementParameter var10 : var8.parameters) {
            if (var10.generation != 3) {
               switch (var10.blockParam.type) {
                  case "value":
                     if (var10.blockParam.valueType != null && var10.blockParam.valueType.equals("price")) {
                        this.addFormulaPriceParam(var10, var2, var3, var5, 75, var7);
                        break;
                     }

                     this.addFormulaPriceParam(var10, var2, var3, var5, 50, var7);
                     break;
                  case "int":
                     this.addIntParamSpecial(var2, var3, var10, var4, var1, var6);
                     break;
                  case "double":
                     this.addDoubleParamSpecial(var2, var3, var10, var1, var6);
                     break;
                  case "string":
                     var10.addStringParam(var2, var3);
                     break;
                  case "boolean":
                     var10.addBooleanParam(var2, var3);
                     break;
                  case "data":
                     var10.addDataParam(var2, var3, var4);
                     break;
                  default:
                     Log.error("Adding parameter of unknown type: " + var10.blockParam.type);
               }
            }
         }
      }
   }

   public void addIntParamSpecial(Element var1, IRandomGenerator var2, ReplacementParameter var3, ReplacementChartConfig var4, FormulaValue var5, double[] var6) throws GenerateException {
      if (var6 != null && var5.exitBasedType == var6[0]) {
         if (this.LimitSLPTRRR && this.blockParam.key.contains("StopLoss")) {
            var3.addIntParam(var1, var2, var4, (int)var6[2], null);
         } else {
            var3.addIntParam(var1, var2, var4, -1, null);
         }
      } else {
         var3.addIntParam(var1, var2, var4, -1, null);
      }
   }

   private void addDoubleParamSpecial(Element var1, IRandomGenerator var2, ReplacementParameter var3, FormulaValue var4, double[] var5) throws GenerateException {
      if (var5 != null && var4.exitBasedType == var5[0]) {
         double var6 = -1.0;
         double var8 = -1.0;
         if (this.LimitSLPTRRR && this.blockParam.key.contains("StopLoss")) {
            var6 = SQUtils.round2(var5[1] * this.LimitSLPTRRRToCoef);
            var8 = SQUtils.round2(var5[1] * this.LimitSLPTRRRFromCoef);
         } else if (this.blockParam.key.contains("MoveSL2BE") || this.blockParam.key.contains("TrailingStop")) {
            var6 = -1.0;
            var8 = 0.75 * var5[1];
         }

         var3._addDoubleParam(var1, var2, var6, var8, null);
      } else {
         var3.addDoubleParam(var1, var2, null);
      }
   }

   private void addFormulaValueParam(ReplacementParameter var1, Element var2, IRandomGenerator var3) throws GenerateException {
      throw new GenerateException("Not implemented yet - addFormulaValueParam for " + var1.blockDef.key + "." + var1.blockParam.key);
   }

   private void addFormulaPriceParam(ReplacementParameter var1, Element var2, IRandomGenerator var3, int var4, int var5, int var6) throws GenerateException {
      Element var7 = new Element("Block");
      var7.setAttribute("key", "#Value#");
      var7.setAttribute("name", "Value");
      var7.setAttribute("type", "value");
      var7.setAttribute("controlType", "value");
      var2.addContent(var7);
      boolean var9 = false;
      Element var10 = this.replacementConfig.generateSpecialBlock(var3, 2, 0, var4, var6, null);
      String var11 = var10.getAttributeValue("key");
      boolean var8;
      if (!var11.startsWith("CBlock") && this.replacementConfig.checkPriceRangeBlockExists()) {
         var8 = var3.nextInt(100) < var5;
         if (var8) {
            var9 = var3.nextInt(3) == 0;
         }
      } else {
         var8 = false;
      }

      if (var8) {
         Element var12 = new Element("Item");
         var12.setAttribute("key", var9 ? "Minus" : "Plus");
         var12.setAttribute("returnType", "pricenumber");
         var12.setAttribute("categoryType", "other");
         var7.addContent(var12);
         Element var13 = new Element("Block");
         var13.setAttribute("key", "#Left#");
         var12.addContent(var13);
         Element var14 = var10;
         var13.addContent(var14);
         Element var15 = new Element("Block");
         var15.setAttribute("key", "#Right#");
         var12.addContent(var15);
         Element var16 = new Element("Item");
         var16.setAttribute("key", "Multiplication");
         var16.setAttribute("returnType", "pricenumber");
         var16.setAttribute("categoryType", "other");
         var15.addContent(var16);
         Element var17 = new Element("Block");
         var17.setAttribute("key", "#Left#");
         var16.addContent(var17);
         Element var18 = new Element("Item");
         var18.setAttribute("key", "Number");
         var18.setAttribute("returnType", "number");
         var18.setAttribute("categoryType", "other");
         var17.addContent(var18);
         Element var19 = new Element("Param");
         var19.setAttribute("key", "#Number#");
         var19.addContent(this.generateAtrNumber(var3));
         var18.addContent(var19);
         Element var20 = new Element("Block");
         var20.setAttribute("key", "#Right#");
         var16.addContent(var20);
         Element var21 = this.replacementConfig.generateSpecialBlock(var3, 7, 0, var4, var6, null);
         var20.addContent(var21);
      } else {
         var7.addContent(var10);
      }
   }

   private String generateAtrNumber(IRandomGenerator var1) {
      double var2 = 0.1;
      double var4 = 3.0;
      double var6 = 0.1;
      int var8 = (int)((var4 - var2) / var6) + 1;
      int var9 = var1.nextInt(var8);
      double var10 = var2 + var9 * var6;
      return SQUtils.d2(var10);
   }

   private boolean dependentParamIsNone(Element var1, ArrayList<ReplacementParameter> var2) throws GenerateException {
      String var3 = var1.getAttributeValue("key");

      for (ReplacementParameter var5 : var2) {
         if (var5.blockParam.key.equals(var3)) {
            String var6 = var1.getChild("Formula").getAttributeValue("key");
            if (var5.formulaNoneValue == null) {
               throw new GenerateException("Dependent formula '" + var3 + "' doesn't have ValueNone set!");
            }

            if (var5.formulaNoneValue.key.equals(var6)) {
               return true;
            }
         }
      }

      return false;
   }

   private Element findDependentParam(Element var1, String var2) {
      for (Element var4 : var1.getChildren()) {
         if (var4.getAttributeValue("key").equals(var2)) {
            return var4;
         }
      }

      return null;
   }

   private FormulaValue generateFVRandomly(IRandomGenerator var1, double[] var2) throws GenerateException {
      if (this.formulaValues.size() == 1) {
         return this.formulaValues.get(0);
      }

      if (var2 != null && this.LimitSLPTRRR) {
         for (int var3 = 0; var3 < this.formulaValues.size(); var3++) {
            FormulaValue var4 = this.formulaValues.get(var3);
            if (var2[0] == var4.exitBasedType) {
               return var4;
            }
         }
      }

      int var7 = var1.nextInt(this.formulaCummulativeWeight);
      int var8 = 0;

      for (int var5 = 0; var5 < this.formulaValues.size(); var5++) {
         FormulaValue var6 = this.formulaValues.get(var5);
         var8 += var6.weight;
         if (var7 < var8) {
            return var6;
         }
      }

      throw new GenerateException("No Formula Value found for '" + this.blockParam.key + "', this shouldn't happen!");
   }

   public boolean formulaHasValues() {
      return this.formulaValues != null && this.formulaValues.size() != 0;
   }

   public static ReplacementParameter createCDataIndyShiftParam(BlockDefinition var0, ReplacementsConfig var1, ReplacementConfig var2) {
      ReplacementParameter var3 = new ReplacementParameter();
      var3.blockDef = var0;
      var3.generation = 2;
      var3.replacementConfig = var2;
      var3.replacementsConfig = var1;
      var3.blockParam = new BlockParameter(BlockSettingsConverter.createCDataShiftParamElement());
      return var3;
   }

   public static ReplacementParameter createCDataIndyValueParam(
      int var0, BlockDefinition var1, ReplacementsConfig var2, ReplacementConfig var3, CustomDataInfo var4
   ) throws GenerateException {
      ReplacementParameter var5 = new ReplacementParameter();
      var5.blockDef = var1;
      var5.generation = 2;
      var5.replacementConfig = var3;
      var5.replacementsConfig = var2;
      var5.blockParam = new BlockParameter(BlockSettingsConverter.createCDataValueParamElement(var0, var4));
      return var5;
   }

   public boolean verifyParam(String var1, Element var2) throws GenerateException {
      String var3 = var2.getAttributeValue("type");
      if (var3.equals("double")) {
         return this.verifyDoubleParam(var1, var2);
      } else {
         return var3.equals("int") ? this.verifyIntParam(var1, var2) : true;
      }
   }

   private boolean verifyIntParam(String var1, Element var2) throws GenerateException {
      if (var1.equals("#MagicNumber#")) {
         return true;
      }

      if (var1.contains(".")) {
         return true;
      }

      String var3 = var2.getText();
      if (var3 != null && !var3.isEmpty()) {
         int var4;
         try {
            var4 = Integer.parseInt(var3);
         } catch (Exception var11) {
            Log.info("Cannot convert parameter '" + var1 + "' to int, value = '" + var3 + "'!");
            return true;
         }

         if (this.blockParam.key.contains("ExitAfterBars")) {
            return true;
         }

         if (this.generation == 1) {
            if (var4 == this.blockParam.iDefaultValue) {
               return true;
            }

            Log.info("Ver false 1 - {} - val: {} != {}", new Object[]{var1, var4, this.blockParam.iDefaultValue});
            return false;
         } else {
            if (this.blockParam.controlType.equals("combo")) {
               String var12 = this.blockParam.values;
               String[] var13 = var12.split(",");
               return this.isInSubparts(var4, var13);
            }

            if (this.blockParam.controlType.equals("jspinnerVar")) {
               if (!this.blockParam.key.equals("#TimeFrom#") && !this.blockParam.key.equals("#TimeTo#")) {
                  int var5 = (int)this.blockParam.minValue;
                  int var6 = (int)this.blockParam.maxValue;
                  int var7 = var5;
                  int var8 = var6;
                  if (this.isConstantValue(var7)) {
                     return true;
                  }

                  int var9 = (int)this.blockParam.step;
                  if (var8 < var7) {
                     int var10 = var7;
                     var7 = var8;
                     var8 = var10;
                  }

                  if (var8 == var7) {
                     if (var4 == var8) {
                        return true;
                     }

                     Log.info("Ver false 2 - {} - val: {}, {} <> {}", new Object[]{var1, var4, var7, var8});
                     return false;
                  } else if (var9 == 0) {
                     if (var4 == var7) {
                        return true;
                     }

                     Log.info("Ver false 3 - {} - val: {}, {} <> {}", new Object[]{var1, var4, var7, var8});
                     return false;
                  } else {
                     if (var4 >= var7 && var4 <= var8) {
                        return true;
                     }

                     Log.info("Ver false 4 - {} - val: {}, {} <> {}", new Object[]{var1, var4, var7, var8});
                     return false;
                  }
               } else {
                  return true;
               }
            } else if (this.blockParam.controlType.equals("comboVar")) {
               return true;
            } else {
               throw new GenerateException("Unsupported control type '" + this.blockParam.controlType + "' when generating INT random parameter!");
            }
         }
      } else {
         return true;
      }
   }

   private boolean isInSubparts(int var1, String[] var2) {
      if (var2 == null) {
         return true;
      }

      for (int var3 = 0; var3 < var2.length; var3++) {
         String[] var4 = var2[var3].split("=");
         int var5 = Integer.parseInt(var4[1]);
         if (var1 == var5) {
            return true;
         }
      }

      return false;
   }

   private boolean verifyDoubleParam(String var1, Element var2) {
      if (var1.contains(".")) {
         return true;
      }

      String var3 = var2.getText();
      if (var3 != null && !var3.isEmpty()) {
         double var4;
         try {
            var4 = Double.parseDouble(var3);
         } catch (Exception var12) {
            Log.info("Cannot convert parameter '" + var1 + "' to int, value = '" + var3 + "'!");
            return true;
         }

         if (this.generation == 1) {
            return var4 == this.blockParam.iDefaultValue;
         }

         if (this.blockParam.controlType.equals("combo")) {
            String var13 = this.blockParam.values;
            String[] var7 = var13.split(",");
            return this.isInSubparts((int)var4, var7);
         }

         if (this.blockParam.controlType.equals("jspinnerVar") || this.blockParam.controlType.equals("jspinner")) {
            double var6 = this.fixValue(this.blockParam.minValue, null, true);
            double var8 = this.fixValue(this.blockParam.maxValue, null, false);
            double var10 = this.blockParam.step;
            if (var6 == var8) {
               return var4 == var8;
            } else {
               return var10 == 0.0 ? var4 == var6 : var4 >= var6 && var4 <= var8;
            }
         } else {
            return var4 == this.blockParam.dDefaultValue;
         }
      } else {
         return true;
      }
   }
}
