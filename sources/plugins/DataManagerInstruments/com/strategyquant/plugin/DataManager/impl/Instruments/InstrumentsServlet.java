package com.strategyquant.plugin.DataManager.impl.Instruments;

import com.strategyquant.datalib.InstrumentInfo;
import com.strategyquant.datalib.broker.BrokerDto;
import com.strategyquant.datalib.broker.BrokerManager;
import com.strategyquant.datalib.consts.DataTypes;
import com.strategyquant.datalib.data.DataException;
import com.strategyquant.datalib.instrument.InstrumentManager;
import com.strategyquant.lib.L;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.XMLUtil;
import com.strategyquant.lib.app.MainApp;
import com.strategyquant.tradinglib.CommissionsMethod;
import com.strategyquant.tradinglib.SwapMethod;
import com.strategyquant.tradinglib.SwapTypes;
import com.strategyquant.tradinglib.commissions.CommissionsMethodsList;
import com.strategyquant.tradinglib.project.ProjectResources;
import com.strategyquant.tradinglib.project.console.CLILogger;
import com.strategyquant.tradinglib.project.websocket.DataManagerAddProgressSender;
import com.strategyquant.tradinglib.project.websocket.DataManagerConfirm;
import com.strategyquant.tradinglib.project.websocket.SQWebSocketManager;
import com.strategyquant.tradinglib.project.websocket.WSDataObjects;
import com.strategyquant.webguilib.servlet.HttpJSONServlet;
import java.io.File;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Map;
import java.util.TreeSet;
import org.jdom2.Element;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InstrumentsServlet extends HttpJSONServlet {
   private static final Logger Log = LoggerFactory.getLogger(InstrumentsServlet.class);

   protected String execute(String var1, Map<String, String[]> var2, String var3) throws Exception {
      switch (var1) {
         case "list":
            return this.onList(var2);
         case "listInstruments":
            return this.onListInstruments();
         case "addInstrument":
            return this.onAddInstrument(var2);
         case "editInstrument":
            return this.onEditInstrument(var2);
         case "massEditInstrument":
            return this.onMassEditInstrument(var2);
         case "removeInstrument":
            return this.onRemoveInstrument(var2);
         case "listAliases":
            return this.onListAliases();
         case "addAlias":
            return this.onAddAlias(var2);
         case "editAlias":
            return this.onEditAlias(var2);
         case "removeAlias":
            return this.onRemoveAlias(var2);
         case "load":
            return this.onLoad(var2);
         case "save":
            return this.onSave(var2);
         case "clone":
            return this.onClone(var2);
         default:
            throw new Exception("Unknown command '" + var1 + "'.");
      }
   }

   private String onClone(Map<String, String[]> var1) {
      try {
         String var2 = this.tryGetParam(var1, "name")[0];
         int var3 = Integer.valueOf(this.tryGetParam(var1, "broker")[0]);
         String var4 = this.tryGetParam(var1, "clone")[0];
         var4 = BrokerManager.getInstance().getBrokerDependantName(var4, var3);
         if (InstrumentManager.checkInstrumentExists(var4)) {
            throw new Exception(L.t("Instrument with name '%s' already exists.", new Object[]{var2}));
         }

         InstrumentInfo var5 = InstrumentManager.getInstrumentInfo(var2);
         if (var5 == null) {
            throw new Exception(L.t("Instrument with name '%s' doesn't exist.", new Object[]{var2}));
         }

         InstrumentInfo var6 = var5.clone();
         var6.broker = var3;
         var6.instrument = var4;
         InstrumentManager.addInstrument(var6);
         SQWebSocketManager.addToDataQueue(WSDataObjects.getInstruments(), new String[]{"SQUANT", "QDM", "AlgoWizard"});
         JSONObject var7 = new JSONObject();
         var7.put("success", L.t("Instrument cloned.", new Object[0]));
         return var7.toString();
      } catch (Exception var8) {
         return apiErrorJSON(L.t("Cannot clone session.", new Object[0]), var8);
      }
   }

   private String onList(Map<String, String[]> var1) throws Exception {
      JSONObject var2 = new JSONObject();
      if (MainApp.runInConsoleWithoutActiveWebserver()) {
         PrintWriter var3 = null;

         try {
            var3 = new PrintWriter(this.tryGetParam(var1, "csv")[0]);
         } catch (Exception var16) {
         }

         ArrayList var4 = InstrumentManager.list();
         DecimalFormat var5 = new DecimalFormat("0", DecimalFormatSymbols.getInstance(Locale.ENGLISH));
         var5.setMaximumFractionDigits(10);
         String var6 = "Instrument,Description,Point value,Pip/Tick size,Pip/Tick step,Default spread,Default slippage,Data type,Order size multiplier,Order size step";
         if (var3 == null) {
            CLILogger.log(var6);
         } else {
            var3.println(var6);
         }

         for (int var7 = 0; var7 < var4.size(); var7++) {
            InstrumentInfo var8 = (InstrumentInfo)var4.get(var7);
            String var9 = DataTypes.toString(var8.dataType);
            String var10 = var8.instrument
               + ","
               + var8.description
               + ","
               + var8.pointValue
               + ","
               + var5.format(var8.tickSize)
               + ","
               + var5.format(var8.tickStep)
               + ","
               + var8.defaultSpread
               + ","
               + var8.defaultSlippage
               + ","
               + var9
               + ","
               + var8.orderSizeMultiplier
               + ","
               + var8.orderSizeStep;
            if (var3 == null) {
               CLILogger.log(var10);
            } else {
               var3.println(var10);
            }
         }

         if (var3 != null) {
            var3.flush();
            var3.close();
         }
      } else {
         this.checkParamExists(var1, new String[]{"type", "search"});
         String var17 = ((String[])var1.get("search"))[0];

         int var18;
         try {
            var18 = Integer.parseInt(((String[])var1.get("type"))[0]);
         } catch (Exception var15) {
            var18 = -1;
         }

         Integer var19 = null;

         try {
            var19 = Integer.parseInt(((String[])var1.get("broker"))[0]);
         } catch (Exception var14) {
         }

         ArrayList var20 = InstrumentManager.list();
         ArrayList var21 = new ArrayList();

         for (int var22 = 0; var22 < var20.size(); var22++) {
            InstrumentInfo var24 = (InstrumentInfo)var20.get(var22);
            if ((var18 == -1 || var24.dataType == var18)
               && this.checkFilter(var24, var17)
               && (!var24.instrument.startsWith("[") || !var24.instrument.endsWith("]"))
               && (var19 == null || var24.broker == var19)) {
               var21.add(var24);
            }
         }

         JSONArray var23 = new JSONArray();

         for (int var25 = 0; var25 < var21.size(); var25++) {
            InstrumentInfo var26 = (InstrumentInfo)var21.get(var25);
            BrokerDto var11 = BrokerManager.getInstance().getBroker(var26.broker);
            JSONObject var12 = new JSONObject();
            var12.put("id", "r" + var25);
            JSONArray var13 = new JSONArray();
            var13.put(var26.instrument);
            var13.put(var26.description);
            var13.put(var11 == null ? "SQ default" : var11.getName());
            var13.put(var26.pointValue);
            var13.put(var26.tickSize);
            var13.put(var26.tickStep);
            var13.put(var26.defaultSpread);
            var13.put(var26.defaultSlippage);
            if (!MainApp.checkProduct("QDM")) {
               var13.put(this.printCommission(var26.commissions));
            }

            if (!MainApp.checkProduct("QDM")) {
               var13.put(this.printSwap(var26.swap));
            }

            var13.put(DataTypes.toString(var26.dataType));
            var13.put(var26.orderSizeMultiplier);
            var13.put(var26.orderSizeStep);
            var12.put("data", var13);
            var23.put(var12);
         }

         var2.put("rows", var23);
      }

      var2.put("success", L.t("Instruments listed.", new Object[0]));
      return var2.toString();
   }

   private String printCommission(String var1) {
      String var2 = "";

      try {
         Element var3 = XMLUtil.stringToElement(var1);
         String var4 = var3.getAttributeValue("type");
         Element var5 = new Element("Commissions");
         var5.addContent(var3);
         CommissionsMethod var6 = (CommissionsMethod)CommissionsMethodsList.get().findClassByName(var4);
         var6.setFromXML(var5);
         var2 = var6.printFormatedName();
      } catch (Exception var7) {
         var2 = "?";
      }

      return var2;
   }

   private String printSwap(String var1) {
      String var2 = "No swap";

      try {
         if (var1 != null && !var1.isEmpty()) {
            Element var3 = XMLUtil.stringToElement(var1);
            SwapMethod var4 = new SwapMethod();
            var4.setFromXML(var3);
            var2 = var4.isUsed() ? "Long: " + var4.getLongSwap() + ", Short: " + var4.getShortSwap() + " " + SwapTypes.toString(var4.getType()) : "Not used";
         }
      } catch (Exception var5) {
         var2 = "?";
      }

      return var2;
   }

   private boolean checkFilter(InstrumentInfo var1, String var2) throws DataException {
      var2 = var2.trim().toLowerCase();
      return var2.length() == 0 ? true : var1.instrument.toLowerCase().contains(var2) || var1.description.toLowerCase().contains(var2);
   }

   private String onListInstruments() {
      JSONObject var1 = new JSONObject();

      try {
         SQWebSocketManager.addToDataQueue(WSDataObjects.getInstruments(), new String[]{"SQUANT", "QDM"});
      } catch (Exception var3) {
         return apiErrorJSON(L.t("Cannot get list of instruments.", new Object[0]), var3);
      }

      var1.put("success", L.t("Instruments listed.", new Object[0]));
      return var1.toString();
   }

   private String onListAliases() {
      JSONObject var1 = new JSONObject();

      try {
         SQWebSocketManager.addToDataQueue(WSDataObjects.getAliases(), new String[]{"SQUANT", "QDM"});
      } catch (Exception var3) {
         return apiErrorJSON(L.t("Cannot get list of data.", new Object[0]), var3);
      }

      var1.put("success", L.t("Data listed.", new Object[0]));
      return var1.toString();
   }

   private String onAddInstrument(Map<String, String[]> var1) {
      JSONObject var2 = new JSONObject();

      try {
         int var3 = Integer.parseInt(this.tryGetParam(var1, "broker")[0]);
         double var4 = Double.parseDouble(this.tryGetParam(var1, "minDistance")[0]);
         String var6 = this.tryGetParam(var1, "instrument")[0];
         String var7 = this.tryGetParam(var1, "description")[0];
         double var8 = Double.parseDouble(this.tryGetParam(var1, "pointValue")[0]);
         double var10 = Double.parseDouble(this.tryGetParam(var1, "tickSize")[0]);
         double var12 = Double.parseDouble(this.tryGetParam(var1, "tickStep")[0]);
         double var14 = Double.parseDouble(this.tryGetParam(var1, "defaultSpread")[0]);
         double var16 = Double.parseDouble(this.tryGetParam(var1, "defaultSlippage")[0]);
         String var18 = this.tryGetParam(var1, "commissions")[0];
         byte var19 = Byte.parseByte(this.tryGetParam(var1, "dataType")[0]);
         double var20 = Double.parseDouble(this.getParam(var1, "orderSizeMultiplier", "1"));
         double var22 = Double.parseDouble(this.getParam(var1, "orderSizeStep", "1"));
         String var24 = null;
         String var25 = null;
         String var26 = null;
         String var27 = null;
         var7 = var7.equals("not set") ? "" : var7;
         if (var6.trim().isEmpty()) {
            throw new Exception(L.t("Intrument cannot be empty.", new Object[0]));
         }

         try {
            var24 = this.tryGetParam(var1, "exchange")[0];
            if (InstrumentManager.trySaveExchange(var24)) {
               SQWebSocketManager.addToDataQueue(WSDataObjects.getExchanges(), new String[]{"SQUANT", "QDM"});
            }
         } catch (Exception var32) {
         }

         try {
            var25 = this.tryGetParam(var1, "country")[0];
            if (InstrumentManager.trySaveCountry(var25)) {
               SQWebSocketManager.addToDataQueue(WSDataObjects.getCountries(), new String[]{"SQUANT", "QDM"});
            }
         } catch (Exception var31) {
         }

         try {
            var26 = this.tryGetParam(var1, "sector")[0];
            if (InstrumentManager.trySaveSector(var26)) {
               SQWebSocketManager.addToDataQueue(WSDataObjects.getSectors(), new String[]{"SQUANT", "QDM"});
            }
         } catch (Exception var30) {
         }

         try {
            var27 = this.tryGetParam(var1, "swap")[0];
         } catch (Exception var29) {
         }

         var6 = BrokerManager.getInstance().getBrokerDependantName(var6, var3);
         InstrumentManager.addInstrument(var6, var3, var7, var8, var10, var12, var14, var16, var4, var18, var19, var24, var25, var26, var27, var20, var22);
         SQWebSocketManager.addToDataQueue(WSDataObjects.getInstruments(), new String[]{"SQUANT", "QDM", "AlgoWizard"});
      } catch (Exception var33) {
         return apiErrorJSON(L.t("Cannot add instrument.", new Object[0]), var33);
      }

      var2.put("success", L.t("Instrument added.", new Object[0]));
      return var2.toString();
   }

   private String onEditInstrument(Map<String, String[]> var1) {
      JSONObject var2 = new JSONObject();

      try {
         String var3 = this.tryGetParam(var1, "instrument")[0];
         String var4 = null;
         String var5 = null;
         String var6 = null;
         String var7 = null;

         try {
            var4 = this.tryGetParam(var1, "exchange")[0];
            if (InstrumentManager.trySaveExchange(var4)) {
               SQWebSocketManager.addToDataQueue(WSDataObjects.getExchanges(), new String[]{"SQUANT", "QDM"});
            }
         } catch (Exception var43) {
         }

         try {
            var5 = this.tryGetParam(var1, "country")[0];
            if (InstrumentManager.trySaveCountry(var5)) {
               SQWebSocketManager.addToDataQueue(WSDataObjects.getCountries(), new String[]{"SQUANT", "QDM"});
            }
         } catch (Exception var42) {
         }

         try {
            var6 = this.tryGetParam(var1, "sector")[0];
            if (InstrumentManager.trySaveSector(var6)) {
               SQWebSocketManager.addToDataQueue(WSDataObjects.getSectors(), new String[]{"SQUANT", "QDM"});
            }
         } catch (Exception var41) {
         }

         try {
            var7 = this.tryGetParam(var1, "swap")[0];
         } catch (Exception var40) {
         }

         InstrumentInfo var8 = InstrumentManager.getInstrumentInfo(var3);
         String var9 = var8.description;
         double var10 = var8.pointValue;
         double var12 = var8.tickSize;
         double var14 = var8.tickStep;
         double var16 = var8.defaultSpread;
         double var18 = var8.defaultSlippage;
         double var20 = var8.orderSizeMultiplier;
         double var22 = var8.orderSizeStep;
         double var24 = var8.minDistance;
         String var26 = var8.commissions;
         byte var27 = (byte)var8.dataType;

         try {
            var9 = this.tryGetParam(var1, "description")[0];
            var9 = var9.equals("not set") ? "" : var9;
         } catch (Exception var39) {
         }

         try {
            var10 = Double.parseDouble(this.tryGetParam(var1, "pointValue")[0]);
         } catch (Exception var38) {
         }

         try {
            var12 = Double.parseDouble(this.tryGetParam(var1, "tickSize")[0]);
         } catch (Exception var37) {
         }

         try {
            var24 = Double.parseDouble(this.tryGetParam(var1, "minDistance")[0]);
         } catch (Exception var36) {
         }

         try {
            var14 = Double.parseDouble(this.tryGetParam(var1, "tickStep")[0]);
         } catch (Exception var35) {
         }

         try {
            var16 = Double.parseDouble(this.tryGetParam(var1, "defaultSpread")[0]);
         } catch (Exception var34) {
         }

         try {
            var18 = Double.parseDouble(this.tryGetParam(var1, "defaultSlippage")[0]);
         } catch (Exception var33) {
         }

         try {
            var26 = this.tryGetParam(var1, "commissions")[0];
         } catch (Exception var32) {
         }

         try {
            var27 = Byte.parseByte(this.tryGetParam(var1, "dataType")[0]);
         } catch (Exception var31) {
         }

         try {
            var20 = Double.parseDouble(this.tryGetParam(var1, "orderSizeMultiplier")[0]);
         } catch (Exception var30) {
         }

         try {
            var22 = Double.parseDouble(this.tryGetParam(var1, "orderSizeStep")[0]);
         } catch (Exception var29) {
         }

         InstrumentManager.updateInstrument(var3, var9, var10, var12, var14, var16, var18, var24, var26, var27, var4, var5, var6, var7, var20, var22);
         SQWebSocketManager.addToDataQueue(WSDataObjects.getData("", "list"), new String[]{"SQUANT", "QDM", "AlgoWizard"});
         SQWebSocketManager.addToDataQueue(WSDataObjects.getInstruments(), new String[]{"SQUANT", "QDM", "AlgoWizard"});
      } catch (Exception var44) {
         return apiErrorJSON(L.t("Cannot edit instrument.", new Object[0]), var44);
      }

      var2.put("success", L.t("Instrument updated.", new Object[0]));
      return var2.toString();
   }

   private String onMassEditInstrument(Map<String, String[]> var1) {
      JSONObject var2 = new JSONObject();

      try {
         String[] var3 = this.tryGetParam(var1, "instruments[]");

         for (String var7 : var3) {
            InstrumentInfo var8 = InstrumentManager.getInstrumentInfo(var7);

            try {
               if (this.getBoolean(var1, "changeSwap")) {
                  var8.swap = this.tryGetParam(var1, "swap")[0];
               }
            } catch (Exception var20) {
            }

            try {
               if (this.getBoolean(var1, "changePointValue")) {
                  var8.pointValue = Double.parseDouble(this.tryGetParam(var1, "pointValue")[0]);
               }
            } catch (Exception var19) {
            }

            try {
               if (this.getBoolean(var1, "changeTickSize")) {
                  var8.tickSize = Double.parseDouble(this.tryGetParam(var1, "tickSize")[0]);
               }
            } catch (Exception var18) {
            }

            try {
               if (this.getBoolean(var1, "changeMinDistance")) {
                  var8.minDistance = Double.parseDouble(this.tryGetParam(var1, "minDistance")[0]);
               }
            } catch (Exception var17) {
            }

            try {
               if (this.getBoolean(var1, "changeTickStep")) {
                  var8.tickStep = Double.parseDouble(this.tryGetParam(var1, "tickStep")[0]);
               }
            } catch (Exception var16) {
            }

            try {
               if (this.getBoolean(var1, "changeDefaultSpread")) {
                  var8.defaultSpread = Double.parseDouble(this.tryGetParam(var1, "defaultSpread")[0]);
               }
            } catch (Exception var15) {
            }

            try {
               if (this.getBoolean(var1, "changeDefaultSlippage")) {
                  var8.defaultSlippage = Double.parseDouble(this.tryGetParam(var1, "defaultSlippage")[0]);
               }
            } catch (Exception var14) {
            }

            try {
               if (this.getBoolean(var1, "changeCommisionModel")) {
                  var8.commissions = this.tryGetParam(var1, "commissions")[0];
               }
            } catch (Exception var13) {
            }

            try {
               if (this.getBoolean(var1, "changeDataType")) {
                  var8.dataType = Byte.parseByte(this.tryGetParam(var1, "dataType")[0]);
               }
            } catch (Exception var12) {
            }

            try {
               if (this.getBoolean(var1, "changeSizeMultiplier")) {
                  var8.orderSizeMultiplier = Double.parseDouble(this.tryGetParam(var1, "orderSizeMultiplier")[0]);
               }
            } catch (Exception var11) {
            }

            try {
               if (this.getBoolean(var1, "changeOrderSizeStep")) {
                  var8.orderSizeStep = Double.parseDouble(this.tryGetParam(var1, "orderSizeStep")[0]);
               }
            } catch (Exception var10) {
            }

            InstrumentManager.updateInstrument(var8);
         }

         SQWebSocketManager.addToDataQueue(WSDataObjects.getData("", "list"), new String[]{"SQUANT", "QDM", "AlgoWizard"});
         SQWebSocketManager.addToDataQueue(WSDataObjects.getInstruments(), new String[]{"SQUANT", "QDM", "AlgoWizard"});
      } catch (Exception var21) {
         return apiErrorJSON(L.t("Cannot edit instrument.", new Object[0]), var21);
      }

      var2.put("success", L.t("Instrument updated.", new Object[0]));
      return var2.toString();
   }

   private boolean getBoolean(Map<String, String[]> var1, String var2) {
      try {
         String var3 = this.tryGetParamValue(var1, var2);
         return Boolean.valueOf(var3);
      } catch (Exception var4) {
         return false;
      }
   }

   private String onRemoveInstrument(Map<String, String[]> var1) {
      JSONObject var2 = new JSONObject();

      try {
         String[] var3 = this.tryGetParam(var1, "instruments")[0].split("\\,");
         if (var3.length > 10) {
            this.removeInstrumentsAsync(var3);
            var2.put("success", L.t("Instruments removing.", new Object[0]));
            return var2.toString();
         }

         for (String var7 : var3) {
            InstrumentManager.removeInstrument(var7);
         }

         SQWebSocketManager.addToDataQueue(WSDataObjects.getInstruments(), new String[]{"SQUANT", "QDM", "AlgoWizard"});
         var2.put("success", L.t("Instruments removed.", new Object[0]));
         return var2.toString();
      } catch (Exception var8) {
         return apiErrorJSON(L.t("Cannot remove instruments.", new Object[0]), var8);
      }
   }

   private void removeInstrumentsAsync(final String[] var1) {
      final int var2 = var1.length;
      (new Thread() {
         @Override
         public void run() {
            JSONObject var1x = new JSONObject();
            LinkedList var2x = new LinkedList();
            int var3 = 0;

            for (String var7 : var1) {
               try {
                  InstrumentManager.removeInstrument(var7);
                  Integer var8 = SQUtils.round(100.0 * var3 / var2);
                  var1x.put("percent", var8);
                  var1x.put("info", L.t("Removed instrument '%s'", new Object[]{var7}));
                  if (var8 == 100) {
                     var1x.put("message", "Instruments removed");
                  }

                  DataManagerAddProgressSender.getInstance().sendData(var1x);
               } catch (Exception var14) {
                  var2x.add(var7);
               } finally {
                  var3++;
               }
            }

            try {
               SQWebSocketManager.addToDataQueue(WSDataObjects.getInstruments(), new String[]{"SQUANT", "QDM", "AlgoWizard"});
            } catch (Exception var13) {
               InstrumentsServlet.Log.error("", var13);
            }

            var1x.put("percent", 100);
            var1x.put("info", L.t("Completed", new Object[0]));
            var1x.put("message", "Instruments removed");
            if (var2x.size() > 0) {
               var1x.put("error", L.t("Could'd be deleted: %s", new Object[]{String.join(",", var2x)}));
            }

            DataManagerAddProgressSender.getInstance().sendData(var1x);
         }
      }).start();
   }

   private String onAddAlias(Map<String, String[]> var1) {
      JSONObject var2 = new JSONObject();

      try {
         String var3 = this.tryGetParam(var1, "instrument")[0];
         String var4 = this.tryGetParam(var1, "alias")[0];
         InstrumentManager.addAlias(var4, var3, "");
         SQWebSocketManager.addToDataQueue(WSDataObjects.getInstruments(), new String[]{"SQUANT", "QDM", "AlgoWizard"});
      } catch (Exception var5) {
         return apiErrorJSON(L.t("Cannot add alias.", new Object[0]), var5);
      }

      var2.put("success", L.t("Alias added.", new Object[0]));
      return var2.toString();
   }

   private String onEditAlias(Map<String, String[]> var1) {
      JSONObject var2 = new JSONObject();

      try {
         String var3 = this.tryGetParam(var1, "instrument")[0];
         String var4 = this.tryGetParam(var1, "alias")[0];
         String var5 = this.tryGetParam(var1, "description")[0];
         SQWebSocketManager.addToDataQueue(WSDataObjects.getInstruments(), new String[]{"SQUANT", "QDM", "AlgoWizard"});
      } catch (Exception var6) {
         return apiErrorJSON(L.t("Cannot edit alias.", new Object[0]), var6);
      }

      var2.put("success", L.t("Alias updated.", new Object[0]));
      return var2.toString();
   }

   private String onRemoveAlias(Map<String, String[]> var1) {
      JSONObject var2 = new JSONObject();

      try {
         String var3 = this.tryGetParam(var1, "alias")[0];
         SQWebSocketManager.addToDataQueue(WSDataObjects.getInstruments(), new String[]{"SQUANT", "QDM", "AlgoWizard"});
      } catch (Exception var4) {
         return apiErrorJSON(L.t("Cannot remove alias.", new Object[0]), var4);
      }

      var2.put("success", L.t("Alias removed.", new Object[0]));
      return var2.toString();
   }

   private String onLoad(Map<String, String[]> var1) {
      try {
         String var2 = this.tryGetParam(var1, "filePath")[0];
         File var3 = new File(var2);
         JSONObject var4 = new JSONObject();
         ArrayList var5 = new ArrayList();
         Element var6 = XMLUtil.fileToXmlElement(var3);
         if (!var6.getName().equals("Instruments")) {
            throw new Exception(L.t("No Instruments found to import.", new Object[0]));
         }

         HashMap var7 = new HashMap();

         for (Element var9 : var6.getChildren("Broker")) {
            BrokerDto var10 = ProjectResources.loadBrokerElement(var9);
            Integer var11 = var10.getId();
            BrokerDto var12 = BrokerManager.getInstance().getBroker(var10.getName());
            if (var12 != null) {
               var7.put(var11, var12.getId());
            } else {
               var10.setId(null);
               BrokerManager.getInstance().saveBroker(var10);
               var7.put(var11, var10.getId());
            }
         }

         boolean var15 = false;

         for (Element var17 : var6.getChildren("InstrumentInfo")) {
            InstrumentInfo var18 = new InstrumentInfo();
            var18.setFromXML(var17);
            Integer var19 = (Integer)var7.get(var18.broker);
            var18.broker = var19 == null ? -1 : var19;
            if (!InstrumentManager.checkInstrumentExists(var18.instrument)) {
               ProjectResources.checkInstrumentExists(var18);
               var5.add(var18.instrument);
            } else {
               String var13 = "";
               if (!var15) {
                  DataManagerConfirm.get()
                     .awaitConfirmation(
                        L.t("Overwrite confirm", new Object[0]),
                        L.t("Instrument '%s' already exists, do you want to overwrite it with the imported one?", new Object[]{var18.instrument})
                     );
                  var13 = DataManagerConfirm.get().getConfirmedAction();
               }

               if (!var13.equals("skip")) {
                  if (var13.equals("cancel")) {
                     break;
                  }

                  if (var13.equals("overwrite") || var13.equals("overwrite_all") || var15) {
                     if (var13.equals("overwrite_all")) {
                        var15 = true;
                     }

                     InstrumentManager.updateInstrument(var18);
                     var5.add(var18.instrument);
                  }
               }
            }
         }

         SQWebSocketManager.addToDataQueue(WSDataObjects.getInstruments(), new String[]{"SQUANT", "QDM", "AlgoWizard"});
         var4.put("success", L.t("Instruments (%d) loaded.", new Object[]{var5.size()}));
         return var4.toString();
      } catch (Exception var14) {
         return apiErrorJSON(L.t("Cannot load instruments.", new Object[0]), var14);
      }
   }

   private String onSave(Map<String, String[]> var1) {
      try {
         String var2 = this.tryGetParam(var1, "filePath")[0];
         File var3 = new File(var2);
         JSONObject var4 = new JSONObject();
         Element var5 = new Element("Instruments");
         String[] var6 = this.tryGetParam(var1, "instruments")[0].split("\\,");
         TreeSet var7 = new TreeSet();

         for (String var11 : var6) {
            InstrumentInfo var12 = InstrumentManager.getInstrumentInfo(var11);
            var5.addContent(var12.getXML());
            var7.add(var12.broker);
         }

         for (Integer var15 : var7) {
            if (var15 != -1) {
               BrokerDto var16 = BrokerManager.getInstance().getBroker(var15);
               var5.addContent(ProjectResources.createBrokerElement(var16));
            }
         }

         XMLUtil.xmlToFile(var5, var3);
         var4.put("success", L.t("Instruments saved.", new Object[0]));
         return var4.toString();
      } catch (Exception var13) {
         return apiErrorJSON(L.t("Cannot save instruments.", new Object[0]), var13);
      }
   }
}
