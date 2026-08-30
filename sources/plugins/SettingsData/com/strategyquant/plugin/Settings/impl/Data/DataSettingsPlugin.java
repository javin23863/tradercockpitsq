package com.strategyquant.plugin.Settings.impl.Data;

import com.strategyquant.datalib.DataInfo;
import com.strategyquant.datalib.TimeframeManager;
import com.strategyquant.datalib.consts.Precisions;
import com.strategyquant.datalib.data.DataManager;
import com.strategyquant.lib.L;
import com.strategyquant.lib.SQTime;
import com.strategyquant.lib.XMLUtil;
import com.strategyquant.pluginlib.annotations.Category;
import com.strategyquant.pluginlib.annotations.License;
import com.strategyquant.pluginlib.annotations.Name;
import com.strategyquant.pluginlib.annotations.ShortDesc;
import com.strategyquant.tradinglib.engine.ChartSetups;
import com.strategyquant.tradinglib.project.ProjectConfigHelper;
import com.strategyquant.tradinglib.servlet.IServletPlugin;
import com.strategyquant.tradinglib.simulator.Engines;
import com.strategyquant.tradinglib.strategy.OutOfSample;
import com.strategyquant.tradinglib.task.settings.ISettingTabPlugin;
import com.strategyquant.tradinglib.task.settings.TaskSettingsData;
import com.strategyquant.tradinglib.taskImpl.ISQTask;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import net.xeoh.plugins.base.annotations.PluginImplementation;
import net.xeoh.plugins.base.annotations.meta.Author;
import org.eclipse.jetty.server.Handler;
import org.jdom2.Element;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Author(name = "Tomas Brynda")
@Name(name = "Data servlet plugin")
@Category(name = "Settings")
@License(text = "")
@ShortDesc(text = "Short description")
@PluginImplementation
public class DataSettingsPlugin implements ISettingTabPlugin, IServletPlugin {
   public static final Logger Log = LoggerFactory.getLogger(DataSettingsPlugin.class);

   public Handler getHandler() {
      return null;
   }

   public String getProduct() {
      return "SQUANTAlgoWizardAlgoWizardStandaloneBACKTESTNODE";
   }

   public int getPreferredPosition() {
      return 10;
   }

   public void initPlugin() throws Exception {
   }

   private void fixSettings(Element var1, ISQTask var2) {
      Element var3 = XMLUtil.tryAddElement(var1, this.getSettingName());
      Element var4 = XMLUtil.tryAddElement(var3, "Setups");
      String var5 = var2.getType();
      int var6 = 0;
      if (var5.equals("Build")) {
         try {
            Element var7 = XMLUtil.getChildElem(var1, "WhatToBuild");
            Element var8 = XMLUtil.getChildElem(var7, "RulesComplexity");
            var6 = var8.getChildren("Chart").size();
         } catch (Exception var16) {
         }
      }

      List var17 = var4.getChildren("Setup");

      for (int var19 = 0; var19 < var17.size(); var19++) {
         Element var9 = (Element)var17.get(var19);
         int var10 = var9.getChildren("Chart").size();
         if (var6 == 0) {
            var6 = var10;
         } else {
            double var11 = 3.0;

            try {
               var11 = Double.parseDouble(var9.getChild("Chart").getAttributeValue("spread"));
            } catch (Exception var15) {
               Log.error("Exception in spread", var15);
            }

            for (int var13 = var10; var13 < var6; var13++) {
               Element var14 = new Element("Chart");
               var14.setAttribute("symbol", "Same as main chart");
               var14.setAttribute("timeframe", "H1");
               var14.setAttribute("spread", String.valueOf(var11));
               var9.addContent(var14);
            }
         }
      }

      XMLUtil.tryAddElement(var3, "OutOfSample");
      var17 = var4.getChildren("Setup");

      for (int var20 = 0; var20 < var17.size(); var20++) {
         Element var21 = (Element)var17.get(var20);
         List var22 = var21.getChildren("Chart");

         for (int var23 = 0; var23 < var22.size(); var23++) {
            Element var12 = (Element)var22.get(var23);
            this.fixChart(var12, var21);
         }
      }
   }

   private void fixChart(Element var1, Element var2) {
      String var3 = var1.getAttributeValue("symbol");
      if (!var3.equals("Same as main chart")) {
         try {
            DataInfo var4 = DataManager.getDataInfo("History", var3);
            if (var4 == null) {
               ArrayList var5 = DataManager.list();
               if (var5.size() == 0) {
                  Log.error("There are no data!");
                  return;
               }

               var4 = (DataInfo)var5.get(0);
               var1.setAttribute("symbol", var4.symbol);
               String var6 = var1.getAttributeValue("timeframe");
               String var7 = "";
               long var8 = TimeframeManager.getMillis(var6);
               long var10 = TimeframeManager.getMillis(var4.timeframe);
               long var12 = TimeframeManager.getMillis("H1");
               if (var8 >= var10) {
                  var7 = var6;
               } else if (var12 >= var10) {
                  var7 = "H1";
               } else {
                  var7 = var4.timeframe;
               }

               var1.setAttribute("timeframe", var7);
            }

            long var23 = XMLUtil.parseDate(var2.getAttributeValue("dateFrom"));
            long var25 = XMLUtil.parseDate(var2.getAttributeValue("dateTo"));
            if (var23 == 0L || var23 < SQTime.correctDayStart(var4.dateFrom)) {
               var23 = var4.dateFrom;
               var2.setAttribute("dateFrom", SQTime.toUIDateString(var23));
            }

            if (var25 == 0L || var25 > SQTime.correctDayEnd(var4.dateTo) || var23 >= var25) {
               var25 = var4.dateTo;
               var2.setAttribute("dateTo", SQTime.toUIDateString(var25));
            }

            Element var9 = var2.getParentElement();
            if (var9 != null && var9.getName().equals("Setups")) {
               Element var26 = var9.getParentElement();
               if (var26 != null && var26.getName().equals("Data")) {
                  Element var11 = var26.getChild("OutOfSample");
                  if (var11 != null) {
                     List var27 = var11.getChildren("Range");
                     if (var27 != null && var27.size() > 0) {
                        for (int var13 = 0; var13 < var27.size(); var13++) {
                           Element var14 = (Element)var27.get(var13);
                           long var15 = XMLUtil.parseDate(var14.getAttributeValue("dateFrom"));
                           long var17 = XMLUtil.parseDate(var14.getAttributeValue("dateTo"));
                           boolean var19 = false;
                           if (var15 == 0L || var15 < var23) {
                              var15 = (long)(var23 + 0.7 * (var25 - var23));
                              var19 = true;
                           }

                           if (var17 == 0L || var17 > var25) {
                              var17 = var25;
                              var19 = true;
                           }

                           if (var15 > var17) {
                              long var20 = var15;
                              var15 = var17;
                              var17 = var20;
                              var19 = true;
                           }

                           if (var19) {
                              var15 = SQTime.setTime(var15, 0, 0, 0, 0);
                              var17 = SQTime.setTime(var17, 0, 0, 0, 0);
                              var14.setAttribute("dateFrom", SQTime.toUIDateString(var15));
                              var14.setAttribute("dateTo", SQTime.toUIDateString(var17));
                           }
                        }
                     }
                  }
               }
            }
         } catch (Exception var22) {
            Log.error("Error verifying date", var22);
            var22.printStackTrace();
         }
      }
   }

   public void readSettings(String var1, ISQTask var2, Element var3, TaskSettingsData var4) {
      this.fixSettings(var3, var2);
      Element var5 = var3.getChild("Data");
      if (var5 == null) {
         var4.addError(this.getSettingName(), null, "Cannot find Data settings.");
      } else {
         Element var6 = var5.getChild("Setups");
         if (var6 == null) {
            var4.addError(this.getSettingName(), null, "Cannot find Data.Setups settings.");
         } else {
            Element var7 = var6.getChild("Setup");
            if (var7 == null) {
               var4.addError(this.getSettingName(), null, "You have to set up at least one backtest!");
            } else {
               new ChartSetups();
               this.checkSetup(var4, var7, 1);

               try {
                  var4.addParam("Slippage", Double.parseDouble(var7.getAttributeValue("slippage")));
               } catch (Exception var16) {
                  var4.addError(this.getSettingName(), null, "Cannot get main chart slippage value");
               }

               try {
                  var4.addParam("MinDistance", Double.parseDouble(var7.getAttributeValue("minDist")));
               } catch (Exception var15) {
                  var4.addError(this.getSettingName(), null, "Cannot get main chart min distance value");
               }

               try {
                  var4.addParam("Commission", ProjectConfigHelper.getCommissionsMethod(var7));
               } catch (Exception var14) {
                  var4.addError(this.getSettingName(), null, "Cannot get main chart commissions config");
               }

               try {
                  var4.addParam("Swap", ProjectConfigHelper.getSwapMethod(var7));
               } catch (Exception var13) {
                  var4.addError(this.getSettingName(), null, "Cannot get main chart swap config");
               }

               try {
                  ChartSetups var8 = ProjectConfigHelper.getChartSetups(var5);
                  var4.addParam("ChartSetups", var8);
               } catch (Exception var12) {
                  Log.error("Exception", var12);
                  var4.addError(this.getSettingName(), null, "Cannot initialize Main chart setup - " + var12.getMessage());
               }

               try {
                  Element var9 = XMLUtil.getChildElem(var5, "OutOfSample");
                  OutOfSample var10 = ProjectConfigHelper.getOOS(var9);
                  var4.addParam("OutOfSample", var10);
               } catch (Exception var11) {
                  var4.addError(this.getSettingName(), null, "Cannot initialize OOS periods - " + var11.getMessage());
               }
            }
         }
      }
   }

   private void checkSetup(TaskSettingsData var1, Element var2, int var3) {
      if (!this.checkCommissionSettings(var2)) {
         var1.addError(this.getSettingName(var3), null, String.format("Commissions method in %s is not set!", getBacktestLabel(var3)));
      }

      if (!XMLUtil.attributeValid(var2, "slippage")) {
         var1.addError(this.getSettingName(var3), null, String.format("Slippage in %s is undefined!", getBacktestLabel(var3)));
      }

      if (!XMLUtil.attributeValid(var2, "minDist")) {
         var1.addError(this.getSettingName(var3), null, String.format("Min Distance in %s is undefined!", getBacktestLabel(var3)));
      }

      if (Engines.getEngine(var2.getAttributeValue("engine")) == -1) {
         var1.addError(this.getSettingName(var3), null, String.format("Trading engine in %s is undefined!", getBacktestLabel(var3)));
      }

      long var4 = this.checkDate(var1, var2.getAttributeValue("dateFrom"), "Date From", var3);
      long var6 = this.checkDate(var1, var2.getAttributeValue("dateTo"), "Date To", var3);
      if (var4 > 0L && var6 > 0L && var4 > var6) {
         var1.addError(this.getSettingName(var3), null, String.format(L.t("Date from must be before Date to.", new Object[0]), getBacktestLabel(var3)));
      }

      Element var8 = (Element)var2.getChildren().get(0);
      String var9 = var8.getAttributeValue("timeframe");
      String var10 = var8.getAttributeValue("symbol");
      String var11 = var2.getAttributeValue("testPrecision");

      try {
         int var12 = Precisions.getPrecision(Integer.parseInt(var11));
         if (var12 == 5 && TimeframeManager.getMillis(var9) < TimeframeManager.getMillis("M1")) {
            var1.addError(
               this.getSettingName(var3),
               null,
               String.format("Incorrect timeframe used in %s! Timeframe M1 or higher must be selected for On Bar Open precision.", getBacktestLabel(var3))
            );
         }
      } catch (Exception var17) {
         var1.addError(this.getSettingName(var3), null, String.format("Precision in %s is undefined!", getBacktestLabel(var3)));
      }

      int var18 = 0;
      IntOpenHashSet var13 = new IntOpenHashSet();

      for (Element var15 : var2.getChildren("Chart")) {
         int var16 = this.checkChart(var1, var15, var3, ++var18, var10, var9, var2);
         if (var13.contains(var16)) {
            var1.addError(this.getSettingName(var3), null, String.format("You cannot have duplicate subcharts!", getBacktestLabel(var3)));
         } else {
            var13.add(var16);
         }
      }
   }

   private boolean checkCommissionSettings(Element var1) {
      try {
         Element var2 = var1.getChild("Commissions");
         List var3 = var2.getChildren("Method");
         if (var3.size() == 1) {
            return true;
         }

         for (Element var5 : var2.getChildren("Method")) {
            if (XMLUtil.elementIs(var5, "use")) {
               return true;
            }
         }

         return false;
      } catch (Exception var6) {
         return XMLUtil.attributeValid(var1, "commissions");
      }
   }

   private int checkChart(TaskSettingsData var1, Element var2, int var3, int var4, String var5, String var6, Element var7) {
      String var8 = var4 > 1 ? "Subchart " + (var4 - 1) : "Main Chart";
      if (!XMLUtil.attributeValid(var2, "symbol")) {
         var1.addError(this.getSettingName(var3), null, String.format("Symbol in %s in %s is undefined!", var8, getBacktestLabel(var3)));
      }

      if (!XMLUtil.attributeValid(var2, "timeframe")) {
         var1.addError(this.getSettingName(var3), null, String.format("Timeframe in %s in %s is undefined!", var8, getBacktestLabel(var3)));
      }

      if (!XMLUtil.attributeValid(var2, "spread")) {
         var1.addError(this.getSettingName(var3), null, String.format("Spread in %s in %s is undefined!", var8, getBacktestLabel(var3)));
      }

      String var9 = var2.getAttributeValue("symbol");
      String var10 = var2.getAttributeValue("timeframe");
      if (var4 > 1 && (var9.equals(var5) || var9.equals("Same as main chart")) && var10.equals(var6)) {
      }

      return var9.hashCode() + var10.hashCode();
   }

   private long checkDate(TaskSettingsData var1, String var2, String var3, int var4) {
      if (var2 != null && !var2.equals("") && !var2.equals("undefined")) {
         try {
            return SQTime.parseDateToMilis(var2);
         } catch (ParseException var6) {
            var1.addError(this.getSettingName(var4), null, String.format("%s in %s has incorrect value'%s'!", var3, getBacktestLabel(var4), var2));
         }
      } else {
         var1.addError(this.getSettingName(var4), null, String.format("%s in %s is undefined!", var3, getBacktestLabel(var4)));
      }

      return -1L;
   }

   private String getSettingName(int var1) {
      return this.getSettingName();
   }

   private static String getBacktestLabel(int var0) {
      return var0 == 1 ? "Main backtest" : String.format("Additional Backtest %d", var0 - 1);
   }

   public String getSettingName() {
      return "Data";
   }

   public void getStrategyConfigSettings(Element var1, JSONArray var2) throws Exception {
      if (var1 != null) {
         Element var3 = var1.getChild("Data");
         if (var3 != null) {
            Element var4 = var3.getChild("Setups");
            if (var4 != null) {
               Element var5 = var4.getChild("Setup");
               if (var5 != null) {
                  Element var6 = var5.getChild("Chart");
                  if (var6 != null) {
                     String var7 = var5.getAttributeValue("engine");
                     String var8 = var6.getAttributeValue("symbol");
                     String var9 = var6.getAttributeValue("timeframe");
                     String var10 = var6.getAttributeValue("spread");
                     String var11 = var5.getAttributeValue("dateFrom");
                     String var12 = var5.getAttributeValue("dateTo");
                     String var13 = var5.getAttributeValue("testPrecision");
                     String var14 = var5.getAttributeValue("slippage");
                     String var15 = var5.getAttributeValue("minDist");

                     try {
                        var11 = SQTime.toUIDateString(SQTime.parseDateToMilis(var11));
                     } catch (Exception var36) {
                     }

                     try {
                        var12 = SQTime.toUIDateString(SQTime.parseDateToMilis(var12));
                     } catch (Exception var35) {
                     }

                     String var16 = null;
                     String var17 = null;

                     try {
                        var16 = ProjectConfigHelper.getCommissionsMethod(var5).printFormatedName();
                     } catch (Exception var34) {
                     }

                     try {
                        var17 = ProjectConfigHelper.getSwapMethod(var5).printFormatedName();
                     } catch (Exception var33) {
                     }

                     var2.put(new JSONObject().put("key", "Engine").put("value", var7 != null ? var7 : "N/A"));
                     var2.put(new JSONObject().put("key", "Symbol").put("value", var8 != null ? var8 : "N/A"));
                     var2.put(new JSONObject().put("key", "Timeframe").put("value", var9 != null ? var9 : "N/A"));
                     var2.put(new JSONObject().put("key", "Start date").put("value", var11 != null ? var11 : "N/A"));
                     var2.put(new JSONObject().put("key", "End date").put("value", var12 != null ? var12 : "N/A"));
                     var2.put(new JSONObject().put("key", "Test precision").put("value", var13 != null ? Precisions.toString(Integer.parseInt(var13)) : "N/A"));
                     var2.put(new JSONObject().put("key", "Spread").put("value", var10 != null ? var10 : "N/A"));
                     var2.put(new JSONObject().put("key", "Slippage ").put("value", var14 != null ? var14 : "N/A"));
                     var2.put(new JSONObject().put("key", "Min distance ").put("value", var15 != null ? var15 : "N/A"));
                     var2.put(new JSONObject().put("key", "Commission").put("value", var16 != null ? var16 : "N/A"));
                     var2.put(new JSONObject().put("key", "Swap").put("value", var17 != null ? var17 : "N/A"));
                     JSONArray var18 = new JSONArray();
                     ArrayList var19 = new ArrayList();
                     var19.add(var8);
                     String var20 = "None";
                     List var21 = var5.getChildren("Chart");
                     if (var21.size() > 1) {
                        var20 = "";

                        for (int var22 = 1; var22 < var21.size(); var22++) {
                           String var23 = ((Element)var21.get(var22)).getAttributeValue("symbol");
                           var20 = var20 + String.format("Subcharts (%d): ", var22);
                           var20 = var20 + String.format("%s, %s", var23, ((Element)var21.get(var22)).getAttributeValue("timeframe"));
                           var20 = var20 + "\n";
                           if (var23 != null && !var23.equals("Same as main chart")) {
                              var19.add(var23);
                           }
                        }
                     }

                     var2.put(new JSONObject().put("key", "Subcharts").put("value", var20));

                     for (int var40 = 0; var40 < var19.size(); var40++) {
                        var18.put(new JSONObject().put("key", "Instrument " + (String)var19.get(var40)).put("value", "N/A"));
                     }

                     try {
                        for (int var41 = 0; var41 < var18.length(); var41++) {
                           JSONObject var45 = var18.getJSONObject(var41);
                           String var24 = var45.getString("key").replace("Instrument ", "");
                           DataInfo var25 = DataManager.getDataInfo("History", var24);
                           if (var25 != null) {
                              var45.put("value", this.getInstrumentInfo(var25.barTimeType, var25.timezone));
                           }
                        }
                     } catch (Exception var37) {
                     }

                     var2.put(new JSONObject().put("key", "Instruments").put("value", var18));
                     String var42 = "None";
                     Element var46 = var3.getChild("OutOfSample");
                     if (var46 != null) {
                        List var47 = var46.getChildren("Range");
                        if (var47.size() > 0) {
                           var42 = "";
                           int[] var48 = new int[]{0, 0, 0};

                           for (int var26 = 0; var26 < var47.size(); var26++) {
                              Element var27 = (Element)var47.get(var26);
                              long var28 = XMLUtil.parseDate(var27.getAttributeValue("dateFrom"));
                              long var30 = XMLUtil.parseDate(var27.getAttributeValue("dateTo"));
                              String var32 = XMLUtil.getStringAttr(var27, "type", "oos");
                              if (var32.equals("isv")) {
                                 var48[0]++;
                                 var42 = var42 + String.format("ISV%d: ", var48[0]);
                              } else if (var32.equals("oos")) {
                                 var48[1]++;
                                 var42 = var42 + String.format("OOS%d: ", var48[1]);
                              } else {
                                 if (!var32.equals("notrade")) {
                                    continue;
                                 }

                                 var48[2]++;
                                 var42 = var42 + String.format("NoTrade%d: ", var48[2]);
                              }

                              var42 = var42 + String.format("%s - %s", SQTime.toString(var28, "yyyy.MM.dd"), SQTime.toString(var30, "yyyy.MM.dd"));
                              var42 = var42 + "\n";
                           }
                        }
                     }

                     var2.put(new JSONObject().put("key", "Data ranges").put("value", var42));
                  }
               }
            }
         }
      }
   }

   private String getInstrumentInfo(int var1, String var2) {
      String var3 = var1 == 2 ? "End of bar" : "Start of bar";
      return var3 + " | " + var2;
   }

   public String getName() {
      return L.tsq("Data");
   }

   public JSONObject getInitializationData() throws Exception {
      return null;
   }
}
