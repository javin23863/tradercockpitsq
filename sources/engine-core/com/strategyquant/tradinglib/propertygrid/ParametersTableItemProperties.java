package com.strategyquant.tradinglib.propertygrid;

import com.strategyquant.lib.time.SQTimeOld;
import com.strategyquant.tradinglib.Activator;
import com.strategyquant.tradinglib.Editor;
import com.strategyquant.tradinglib.Editors;
import com.strategyquant.tradinglib.ForEngine;
import com.strategyquant.tradinglib.Help;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.debug.Debugger;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ParametersTableItemProperties extends Debugger implements Serializable {
   public static final Logger Log = LoggerFactory.getLogger("ParametersTableItemProperties");
   public static final SimpleDateFormat df = new SimpleDateFormat(SQTimeOld.shortDateFormat);
   public static final int TYPE_INT = 1;
   public static final int TYPE_DOUBLE = 2;
   public static final int TYPE_BOOLEAN = 3;
   public static final int TYPE_INT_LIST = 4;
   public static final int TYPE_STR_LIST = 5;
   public static final int TYPE_TIME = 10;
   public static final int TYPE_DATE = 20;
   public static final int TYPE_STRING = 30;
   protected ArrayList<String> propertiesOrder = new ArrayList<>();
   private ArrayList<IPGParameter> params = null;

   public ParametersTableItemProperties() {
      this.setDefaultValues();
   }

   public void setParams(ArrayList<IPGParameter> var1) {
      this.params = var1;
   }

   public ArrayList<IPGParameter> getParams() {
      if (this.params == null) {
         this.params = new ArrayList<>();

         for (Field var4 : this.getClass().getFields()) {
            String var5 = var4.getName();
            Parameter var6 = var4.getAnnotation(Parameter.class);
            if (var6 != null) {
               Activator var7 = var4.getAnnotation(Activator.class);
               String var8 = var7 != null ? var7.param() : null;
               ForEngine var9 = var4.getAnnotation(ForEngine.class);
               String var10 = var9 != null ? var9.value() : null;
               switch (var4.getType().getSimpleName()) {
                  case "int":
                     Editor var21 = var4.getAnnotation(Editor.class);
                     if (var21 != null) {
                        if (var21.type() == 110) {
                           PGTimeParameter var22 = new PGTimeParameter();
                           var22.setKey(var5);
                           var22.setName(var6.name());
                           var22.setCategory(var6.category());
                           var22.setEngine(var10);
                           var22.setActivator(var8);
                           var22.setDescription(this.getHelpValue(var4));

                           try {
                              var22.setValue((int)Double.parseDouble(var6.defaultValue()));
                           } catch (Exception var20) {
                              var22.setValue(3600);
                           }

                           this.params.add(var22);
                        } else if (var21.type() == 40) {
                           PGComboParameter var23 = new PGComboParameter();
                           var23.setKey(var5);
                           var23.setName(var6.name());
                           var23.setCategory(var6.category());
                           var23.setEngine(var10);
                           var23.setActivator(var8);
                           var23.setDescription(this.getHelpValue(var4));
                           var23.setValue(var6.defaultValue());
                           var23.setOptions(this.getOptions(var21));
                           this.params.add(var23);
                        } else {
                           Log.error("Unsupported type of Editor - " + var21.type());
                        }
                     } else {
                        PGIntParameter var24 = new PGIntParameter();
                        var24.setKey(var5);
                        var24.setName(var6.name());
                        var24.setCategory(var6.category());
                        var24.setEngine(var10);
                        var24.setActivator(var8);
                        var24.setDescription(this.getHelpValue(var4));
                        var24.setMax((int)var6.maxValue());
                        var24.setMin((int)var6.minValue());
                        var24.setStep((int)var6.step());

                        try {
                           var24.setValue((int)Double.parseDouble(var6.defaultValue()));
                        } catch (Exception var19) {
                           var24.setValue(var24.getMin());
                        }

                        this.params.add(var24);
                     }
                     break;
                  case "double":
                     PGDoubleParameter var14 = new PGDoubleParameter();
                     var14.setKey(var5);
                     var14.setName(var6.name());
                     var14.setCategory(var6.category());
                     var14.setEngine(var10);
                     var14.setActivator(var8);
                     var14.setDescription(this.getHelpValue(var4));
                     var14.setMax(var6.maxValue());
                     var14.setMin(var6.minValue());
                     var14.setStep(var6.step());
                     var14.setDecimals(var6.decimals());

                     try {
                        var14.setValue(Double.parseDouble(var6.defaultValue()));
                     } catch (Exception var18) {
                        var14.setValue(var14.getMin());
                     }

                     this.params.add(var14);
                     break;
                  case "String":
                     Editor var13 = var4.getAnnotation(Editor.class);
                     if (var13 != null) {
                        if (var13.type() == 40) {
                           PGComboParameter var25 = new PGComboParameter();
                           var25.setKey(var5);
                           var25.setName(var6.name());
                           var25.setCategory(var6.category());
                           var25.setEngine(var10);
                           var25.setActivator(var8);
                           var25.setDescription(this.getHelpValue(var4));
                           var25.setValue(var6.defaultValue());
                           var25.setOptions(this.getOptions(var13));
                           this.params.add(var25);
                        } else {
                           Log.error("Unsupported type of Editor - " + var13.type());
                        }
                     } else {
                        PGStringParameter var26 = new PGStringParameter();
                        var26.setKey(var5);
                        var26.setName(var6.name());
                        var26.setCategory(var6.category());
                        var26.setEngine(var10);
                        var26.setActivator(var8);
                        var26.setDescription(this.getHelpValue(var4));
                        var26.setValue(var6.defaultValue());
                        this.params.add(var26);
                     }
                     break;
                  case "boolean":
                     PGBooleanParameter var15 = new PGBooleanParameter();
                     var15.setKey(var5);
                     var15.setName(var6.name());
                     var15.setCategory(var6.category());
                     var15.setEngine(var10);
                     var15.setActivator(var8);
                     var15.setDescription(this.getHelpValue(var4));

                     try {
                        var15.setValue(Boolean.parseBoolean(var6.defaultValue()));
                     } catch (Exception var17) {
                        var15.setValue(false);
                     }

                     this.params.add(var15);
               }
            }
         }
      }

      return this.params;
   }

   public void setDefaultValues() {
      for (Field var4 : this.getClass().getFields()) {
         try {
            Parameter var5 = var4.getAnnotation(Parameter.class);
            if (var5 != null && !var5.defaultValue().equals("Null")) {
               switch (var4.getType().getSimpleName()) {
                  case "int":
                     var4.setInt(this, Integer.parseInt(var5.defaultValue()));
                     break;
                  case "double":
                     var4.setDouble(this, Double.parseDouble(var5.defaultValue()));
                     break;
                  case "String":
                     var4.set(this, var5.defaultValue());
                     break;
                  case "boolean":
                     var4.setBoolean(this, Boolean.parseBoolean(var5.defaultValue()));
               }
            }
         } catch (Exception var8) {
            Log.error("Error while loading default values of snippet " + this.getClass().getSimpleName() + " / " + var4.getName(), var8);
         }
      }
   }

   private String getHelpValue(Field var1) {
      Help var2 = var1.getAnnotation(Help.class);
      return var2 != null ? var2.value() : "Null";
   }

   protected void applyParameters(Object[] var1) throws Exception {
      int var2 = 0;

      for (Field var6 : this.getClass().getFields()) {
         for (Annotation var10 : var6.getAnnotations()) {
            if (var10 instanceof Parameter) {
               IPGParameter var11 = this.getPGParameter(var6.getName());
               var11.setValue(var1[var2] + "");
               var6.set(this, var1[var2]);
               var2++;
            }
         }
      }
   }

   protected void applyParameters(ArrayList<IPGParameter> var1) throws Exception {
      for (Field var5 : this.getClass().getFields()) {
         for (Annotation var9 : var5.getAnnotations()) {
            if (var9 instanceof Parameter) {
               boolean var10 = false;
               String var11 = var5.getName();

               for (IPGParameter var13 : var1) {
                  if (var13.getKey().equals(var11)) {
                     switch (var13.getType()) {
                        case 1:
                           var5.set(this, ((PGIntParameter)var13).getValue());
                           break;
                        case 2:
                           var5.set(this, ((PGDoubleParameter)var13).getValue());
                           break;
                        case 3:
                           var5.set(this, ((PGBooleanParameter)var13).getValue());
                           break;
                        case 4:
                           var5.set(this, ((PGComboParameter)var13).getValue());
                           break;
                        case 5:
                           var5.set(this, ((PGComboParameter)var13).getValue());
                           break;
                        case 10:
                           var5.set(this, ((PGTimeParameter)var13).getValue());
                           break;
                        case 30:
                           var5.set(this, ((PGStringParameter)var13).getValue());
                           break;
                        default:
                           throw new Exception(String.format("Cannot recognize type %d for parameter %s", var13.getType(), var11));
                     }

                     var10 = true;
                     break;
                  }
               }

               if (!var10) {
                  throw new Exception("Value for parameter '" + var11 + "' not found when creating new instance");
               }
            }
         }
      }
   }

   public Object getPGParameterValue(String var1) throws Exception {
      IPGParameter var2 = this.getPGParameter(var1);
      switch (var2.getType()) {
         case 1:
            return ((PGIntParameter)var2).getValue();
         case 2:
            return ((PGDoubleParameter)var2).getValue();
         case 3:
            return ((PGBooleanParameter)var2).getValue();
         case 4:
            return ((PGComboParameter)var2).getValue();
         case 5:
            return ((PGComboParameter)var2).getValue();
         case 6:
         case 7:
         case 8:
         case 9:
         case 11:
         case 12:
         case 13:
         case 14:
         case 15:
         case 16:
         case 17:
         case 18:
         case 19:
         case 21:
         case 22:
         case 23:
         case 24:
         case 25:
         case 26:
         case 27:
         case 28:
         case 29:
         default:
            throw new Exception(String.format("Cannot recognize type %d for parameter %s", var2.getType(), var1));
         case 10:
            return ((PGTimeParameter)var2).getValue();
         case 20:
            throw new Exception("Not implemented yet!");
         case 30:
            return ((PGStringParameter)var2).getValue();
      }
   }

   public synchronized List<String> getParametersList() {
      if (this.params == null) {
         this.getParams();
      }

      ArrayList var1 = new ArrayList();

      for (IPGParameter var3 : this.params) {
         var1.add(var3.getKey());
      }

      return var1;
   }

   public void setParameterValue(String var1, String var2) throws Exception {
      try {
         Field var3 = this.getClass().getField(var1);
         Editor var4 = var3.getAnnotation(Editor.class);
         if (var4 != null && var4.type() == 110) {
            var2 = Editors.convertTimeToHHMM(var2);
         }

         this.setFieldValue(var3, var2);
         IPGParameter var5 = this.getPGParameter(var1);
         var5.setValue(var2);
      } catch (NoSuchFieldException var6) {
         if (Log.isDebugEnabled()) {
            Log.debug("Couldn't set parameter value - NoSuchField: {}.{}={}", new Object[]{this.getClass().getSimpleName(), var1, var2});
         }
      }
   }

   private void setFieldValue(Field var1, String var2) throws Exception {
      Class var3 = var1.getType();
      if (var2.trim().equalsIgnoreCase("nan")) {
         throw new Exception(String.format("'%s' parameter value cannot be NaN", var1.getName()));
      }

      if (var3.isAssignableFrom(Integer.class) || var3.isAssignableFrom(int.class)) {
         var1.set(this, Integer.parseInt(var2));
      } else if (var3.isAssignableFrom(Long.class) || var3.isAssignableFrom(long.class)) {
         var1.set(this, Long.parseLong(var2));
      } else if (var3.isAssignableFrom(Double.class) || var3.isAssignableFrom(double.class)) {
         var1.set(this, Double.parseDouble(var2));
      } else if (var3.isAssignableFrom(Float.class) || var3.isAssignableFrom(float.class)) {
         var1.set(this, Float.parseFloat(var2));
      } else if (!var3.isAssignableFrom(Boolean.class) && !var3.isAssignableFrom(boolean.class)) {
         var1.set(this, var2);
      } else {
         var1.set(this, Boolean.parseBoolean(var2));
      }
   }

   public Object getParameterValue(String var1) throws Exception {
      Field var2 = this.getClass().getField(var1);
      return var2.get(this);
   }

   private IPGParameter getPGParameter(String var1) throws ParameterDoesntExistException {
      for (IPGParameter var3 : this.getParams()) {
         if (var3.getKey().equals(var1)) {
            return var3;
         }
      }

      throw new ParameterDoesntExistException(var1);
   }

   public TreeMap<String, String> getCustomOptions() {
      return null;
   }

   private TreeMap<String, String> getOptions(Editor var1) {
      TreeMap var2 = this.getCustomOptions();
      return var2 == null ? this.getOptions(var1.values()) : var2;
   }

   private TreeMap<String, String> getOptions(String var1) {
      TreeMap var2 = new TreeMap();
      String[] var3 = var1.split(",");

      for (String var7 : var3) {
         String[] var8 = var7.split("=");
         if (var8.length == 2) {
            String var9 = var8[0].trim();
            String var10 = var8[1].trim();
            var2.put(var9, var10);
         } else {
            Log.debug(String.format("ComboBox items - Invalid item definition '%s'.", var7));
         }
      }

      return var2;
   }
}
