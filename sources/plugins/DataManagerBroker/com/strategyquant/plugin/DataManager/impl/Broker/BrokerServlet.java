package com.strategyquant.plugin.DataManager.impl.Broker;

import com.strategyquant.datalib.DataInfo;
import com.strategyquant.datalib.InstrumentInfo;
import com.strategyquant.datalib.broker.BrokerDto;
import com.strategyquant.datalib.broker.BrokerManager;
import com.strategyquant.datalib.broker.BrokerStockDto;
import com.strategyquant.datalib.data.DataManager;
import com.strategyquant.datalib.data.DownloadSemaphore;
import com.strategyquant.datalib.instrument.InstrumentManager;
import com.strategyquant.datalib.session.Session;
import com.strategyquant.datalib.session.SessionException;
import com.strategyquant.datalib.session.SessionManager;
import com.strategyquant.lib.L;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.XMLUtil;
import com.strategyquant.lib.app.MainApp;
import com.strategyquant.pluginlib.program.Program;
import com.strategyquant.pluginlib.program.ProgramDoesntExistException;
import com.strategyquant.tradinglib.project.websocket.DataManagerAddProgressSender;
import com.strategyquant.tradinglib.project.websocket.DataManagerConfirm;
import com.strategyquant.tradinglib.project.websocket.SQWebSocketManager;
import com.strategyquant.tradinglib.project.websocket.WSDataObjects;
import com.strategyquant.tradinglib.project.websocket.WebSocketNotification;
import com.strategyquant.webguilib.servlet.HttpJSONServlet;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.jdom2.Element;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BrokerServlet extends HttpJSONServlet {
   private static final long serialVersionUID = 1L;
   private static final Logger Log = LoggerFactory.getLogger(BrokerServlet.class);

   protected String execute(String var1, Map<String, String[]> var2, String var3) throws Exception {
      switch (var1) {
         case "importGetOverview":
            return this.onImportGetOverview(var2);
         case "importSessionInstrument":
            return this.onImportSessionInstrument(var2);
         case "list":
            return this.onListBroker(var2);
         case "save":
            return this.onSaveBroker(var2);
         case "delete":
            return this.onDeleteBroker(var2);
         case "listStocks":
            return this.onListStocks(var2);
         case "editStocks":
            return this.onEditStocks(var2);
         case "import":
            return this.onImportStocks(var2);
         case "export":
            return this.onExportStocks(var2);
         case "updateData":
            return this.onUpdateData(var2);
         case "loadXml":
            return this.onLoad(var2);
         case "saveXml":
            return this.onSave(var2);
         default:
            throw new Exception("Unknown command '" + var1 + "'.");
      }
   }

   private String onImportGetOverview(Map<String, String[]> var1) {
      try {
         String var2 = this.tryGetParam(var1, "filePath")[0];
         String var3 = this.tryGetParam(var1, "type")[0];
         BrokerXmlImporter var4 = new BrokerXmlImporter();
         JSONObject var5 = new JSONObject();
         if (var3.equals("session")) {
            List var6 = var4.importSessions(var2, null);
            JSONArray var7 = new JSONArray();

            for (Session var9 : var6) {
               JSONObject var10 = new JSONObject();
               var10.put("name", var9.getSessionName());
               var7.put(var10);
            }

            var5.put("data", var7);
         } else {
            List var12 = var4.importInstruments(var2, null);
            JSONArray var13 = new JSONArray();

            for (InstrumentInfo var15 : var12) {
               JSONObject var16 = new JSONObject();
               var16.put("name", var15.instrument);
               var16.put("description", var15.description);
               var13.put(var16);
            }

            var5.put("data", var13);
         }

         var5.put("success", L.t("Import overview", new Object[0]));
         return var5.toString();
      } catch (Exception var11) {
         return apiErrorJSON(L.t("Cannot load XML content.", new Object[0]), var11);
      }
   }

   private String onImportSessionInstrument(Map<String, String[]> var1) {
      try {
         String var2 = this.tryGetParam(var1, "filePath")[0];
         String var3 = this.tryGetParam(var1, "type")[0];
         String var4 = var1.containsKey("postfix") ? this.tryGetParam(var1, "postfix")[0] : "";
         int var5 = Integer.valueOf(this.tryGetParam(var1, "broker")[0]);
         List var6 = Arrays.asList(this.tryGetParam(var1, "selected[]"));
         if (var6.isEmpty()) {
            throw new IllegalArgumentException("No records were selected.");
         }

         BrokerDto var7 = BrokerManager.getInstance().getBroker(var5);
         if (var7 == null) {
            throw new IllegalArgumentException("Broker was not found");
         }

         BrokerXmlImporter var8 = new BrokerXmlImporter();
         JSONObject var9 = new JSONObject();
         if (var3.equals("session")) {
            List var10 = var8.importSessions(var2, var6);
            if (var10.size() > 10) {
               this.importSessionAsync(var10, var5, var4, var7);
            } else {
               for (Session var12 : var10) {
                  this.performImportSession(var12, var5, var4, var7);
               }

               SQWebSocketManager.addToDataQueue(WSDataObjects.getSessions(), new String[]{"SQUANT", "QDM", "AlgoWizard"});
            }
         } else {
            List var14 = var8.importInstruments(var2, var6);
            if (var14.size() > 10) {
               this.importInstrumentsAsync(var14, var5, var4, var7);
            } else {
               for (InstrumentInfo var16 : var14) {
                  this.performImportInstrument(var16, var5, var4, var7);
               }

               SQWebSocketManager.addToDataQueue(WSDataObjects.getInstruments(), new String[]{"SQUANT", "QDM", "AlgoWizard"});
            }
         }

         var9.put("success", L.t("File imported", new Object[0]));
         return var9.toString();
      } catch (Exception var13) {
         return apiErrorJSON(L.t("Cannot import XML file.", new Object[0]), var13);
      }
   }

   private void importSessionAsync(final List<Session> var1, final int var2, final String var3, final BrokerDto var4) {
      final int var5 = var1.size();
      (new Thread() {
         @Override
         public void run() {
            JSONObject var1x = new JSONObject();
            LinkedList var2x = new LinkedList();
            int var3x = 0;

            for (Session var5x : var1) {
               try {
                  BrokerServlet.this.performImportSession(var5x, var2, var3, var4);
                  Integer var6 = SQUtils.round(100.0 * var3x / var5);
                  var1x.put("percent", var6);
                  var1x.put("info", L.t("Imported session '%s'", new Object[]{var5x.getSessionName()}));
                  if (var6 == 100) {
                     var1x.put("message", "Sessions imported");
                  }

                  DataManagerAddProgressSender.getInstance().sendData(var1x);
               } catch (Exception var12) {
                  var2x.add(var5x.getSessionName());
               } finally {
                  var3x++;
               }
            }

            try {
               SQWebSocketManager.addToDataQueue(WSDataObjects.getSessions(), new String[]{"SQUANT", "QDM", "AlgoWizard"});
            } catch (Exception var11) {
               BrokerServlet.Log.error("", var11);
            }

            var1x.put("percent", 100);
            var1x.put("info", L.t("Completed", new Object[0]));
            var1x.put("message", "Sessions imported");
            if (var2x.size() > 0) {
               var1x.put("error", L.t("Could'd be imported: %s", new Object[]{String.join(",", var2x)}));
            }

            DataManagerAddProgressSender.getInstance().sendData(var1x);
         }
      }).start();
   }

   private void performImportSession(Session var1, int var2, String var3, BrokerDto var4) throws SessionException {
      var1.setBroker(var2);
      String var5 = var1.getSessionName() + var3;
      Session var6 = new Session(var5, var1.getElements());
      var6.setBroker(var2);
      if (SessionManager.checkSessionExists(var5)) {
         SessionManager.removeSession(var5);
      }

      SessionManager.addSession(var6);
   }

   private void performImportInstrument(InstrumentInfo var1, int var2, String var3, BrokerDto var4) throws Exception {
      var1.broker = var2;
      var1.instrument = var1.instrument + var3;
      if (InstrumentManager.checkInstrumentExists(var1.instrument)) {
         InstrumentManager.removeInstrument(var1.instrument);
      }

      InstrumentManager.addInstrument(var1);
   }

   private void importInstrumentsAsync(final List<InstrumentInfo> var1, final int var2, final String var3, final BrokerDto var4) {
      final int var5 = var1.size();
      (new Thread() {
         @Override
         public void run() {
            JSONObject var1x = new JSONObject();
            LinkedList var2x = new LinkedList();
            int var3x = 0;

            for (InstrumentInfo var5x : var1) {
               try {
                  BrokerServlet.this.performImportInstrument(var5x, var2, var3, var4);
                  Integer var6 = SQUtils.round(100.0 * var3x / var5);
                  var1x.put("percent", var6);
                  var1x.put("info", L.t("Imported instrument '%s'", new Object[]{var5x.instrument}));
                  if (var6 == 100) {
                     var1x.put("message", "Instruments imported");
                  }

                  DataManagerAddProgressSender.getInstance().sendData(var1x);
               } catch (Exception var12) {
                  var2x.add(var5x.instrument);
               } finally {
                  var3x++;
               }
            }

            try {
               SQWebSocketManager.addToDataQueue(WSDataObjects.getInstruments(), new String[]{"SQUANT", "QDM", "AlgoWizard"});
            } catch (Exception var11) {
               BrokerServlet.Log.error("", var11);
            }

            var1x.put("percent", 100);
            var1x.put("info", L.t("Completed", new Object[0]));
            var1x.put("message", "Instruments imported");
            if (var2x.size() > 0) {
               var1x.put("error", L.t("Could'd be imported: %s", new Object[]{String.join(",", var2x)}));
            }

            DataManagerAddProgressSender.getInstance().sendData(var1x);
         }
      }).start();
   }

   private String onLoad(Map<String, String[]> var1) {
      try {
         String var2 = this.tryGetParam(var1, "filePath")[0];
         File var3 = new File(var2);
         JSONObject var4 = new JSONObject();
         Element var5 = XMLUtil.fileToXmlElement(var3);
         if (!var5.getName().equals("Brokers")) {
            throw new Exception(L.t("No Brokers found to import.", new Object[0]));
         }

         LinkedList var6 = new LinkedList();

         for (Element var8 : var5.getChildren("Broker")) {
            BrokerDto var9 = new BrokerDto();
            String var10 = var8.getAttributeValue("name");
            if (var10.startsWith("[[")) {
               var10 = var10.substring(1, var10.length() - 1);
            }

            var9.setName(var10);
            var9.setDesc(var8.getChildText("Desc"));
            String var11 = var8.getAttributeValue("stockPickerUse");
            var9.setStockPickerUse(var11 == null ? false : Boolean.valueOf(var11));
            String var12 = var8.getAttributeValue("mtUse");
            var9.setMtUse(var12 == null ? false : Boolean.valueOf(var12));
            if (var9.isMtUse()) {
               var9.setMtTimezone(var8.getAttributeValue("mtTimezone"));
               var9.setPostfix(var8.getAttributeValue("mtPostfix"));
            }

            ArrayList var13 = new ArrayList();
            Element var14 = var8.getChild("Stocks");
            if (var14 != null) {
               for (Element var17 : var14.getChildren("Stock")) {
                  BrokerStockDto var18 = new BrokerStockDto();
                  var18.setTicker(var17.getAttributeValue("name"));
                  var13.add(var18);
               }

               if (var13.size() > 0) {
                  var9.setStockPickerUse(true);
               }
            }

            BrokerDto var20 = BrokerManager.getInstance().getBroker(var10);
            if (var20 != null && !MainApp.runInConsole()) {
               DataManagerConfirm.get()
                  .awaitConfirmation(
                     L.t("Overwrite confirm", new Object[0]),
                     L.t("Broker '%s' already exists, do you want to overwrite it with the imported one?", new Object[]{var10})
                  );
               String var21 = DataManagerConfirm.get().getConfirmedAction();
               if (!var21.equals("skip")) {
                  if (var21.equals("cancel")) {
                     break;
                  }

                  if (var21.equals("overwrite") || var21.equals("overwrite_all")) {
                     var9.setId(var20.getId());
                     BrokerManager.getInstance().saveBroker(var9);
                     BrokerManager.getInstance().saveStocks(var9.getId(), var13);
                     var6.add(var9.getName());
                  }
               }
            } else {
               if (var20 != null) {
                  var9.setId(var20.getId());
               }

               BrokerManager.getInstance().saveBroker(var9);
               BrokerManager.getInstance().saveStocks(var9.getId(), var13);
               var6.add(var9.getName());
            }
         }

         BrokerManager.getInstance().refresh();
         SQWebSocketManager.addToDataQueue(WSDataObjects.getBrokers(), new String[]{"SQUANT", "QDM", "AlgoWizard"});
         var4.put("success", L.t("Brokers (%d) loaded.", new Object[]{var6.size()}));
         return var4.toString();
      } catch (Exception var19) {
         return apiErrorJSON(L.t("Cannot load brokers.", new Object[0]), var19);
      }
   }

   private String onSave(Map<String, String[]> var1) {
      try {
         String var2 = this.tryGetParam(var1, "filePath")[0];
         File var3 = new File(var2);
         JSONObject var4 = new JSONObject();
         Element var5 = new Element("Brokers");
         String[] var6 = this.tryGetParam(var1, "ids[]");

         for (String var10 : var6) {
            BrokerDto var11 = BrokerManager.getInstance().getBroker(Integer.valueOf(var10));
            List var12 = BrokerManager.getInstance().getStocks(var11.getId());
            var5.addContent(this.toXML(var11, var12));
         }

         XMLUtil.xmlToFile(var5, var3);
         var4.put("success", L.t("Brokers saved.", new Object[0]));
         return var4.toString();
      } catch (Exception var13) {
         return apiErrorJSON(L.t("Cannot save brokers.", new Object[0]), var13);
      }
   }

   private Element toXML(BrokerDto var1, List<String> var2) {
      Element var3 = new Element("Broker");
      var3.setAttribute("name", var1.getName());
      var3.setAttribute("stockPickerUse", String.valueOf(var1.isStockPickerUse()));
      var3.setAttribute("mtUse", String.valueOf(var1.isMtUse()));
      if (var1.isMtUse()) {
         var3.setAttribute("mtTimezone", var1.getMtTimezone());
         var3.setAttribute("mtPostfix", var1.getPostfix());
      }

      Element var4 = new Element("Desc");
      var4.setText(var1.getDesc());
      var3.addContent(var4);
      Element var5 = new Element("Stocks");
      var3.addContent(var5);

      for (String var7 : var2) {
         Element var8 = new Element("Stock");
         var8.setAttribute("name", var7);
         var5.addContent(var8);
      }

      return var3;
   }

   private String onUpdateData(Map<String, String[]> var1) throws Exception {
      if (!DownloadSemaphore.getInstance().startDownloadData(5)) {
         return apiErrorJSON(L.t("Please wait couple of seconds and try again.", new Object[0]), null);
      }

      try {
         String[] var2 = this.tryGetParamValue(var1, "brokerId").split(",");
         final LinkedList var3 = new LinkedList();

         for (String var7 : var2) {
            Integer var8 = Integer.valueOf(var7);
            BrokerDto var9 = BrokerManager.getInstance().getBroker(var8);
            if (var9 == null) {
               DownloadSemaphore.getInstance().downloadFinished();
               return apiErrorJSON(L.t("Broker doesn't exist", new Object[0]), null);
            }

            for (String var12 : BrokerManager.getInstance().getStocks(var8)) {
               if (!var3.contains(var12)) {
                  var3.add(var12);
               }
            }
         }

         if (var3.isEmpty()) {
            DownloadSemaphore.getInstance().downloadFinished();
            return this.getSuccessResult(L.t("Nothing to update. Broker is without any stocks.", new Object[0]));
         }

         if (MainApp.runInConsoleWithoutActiveWebserver()) {
            this._onUpdateData(var3);
         } else {
            new Thread(new Runnable() {
               @Override
               public void run() {
                  try {
                     BrokerServlet.this._onUpdateData(var3);
                  } catch (Exception var2x) {
                     BrokerServlet.Log.error("Error while updating data", var2x);
                  }
               }
            }).start();
         }
      } catch (Exception var13) {
         DownloadSemaphore.getInstance().downloadFinished();
         throw var13;
      }

      return this.getSuccessResult(L.t("Starting updating data. Please wait...", new Object[0]));
   }

   private void _onUpdateData(List<String> var1) throws ProgramDoesntExistException, Exception {
      try {
         HashMap var2 = new HashMap();
         ArrayList var3 = new ArrayList();
         ArrayList var4 = new ArrayList();
         WebSocketNotification.sendSuccess(L.t("Loading stock informations.", new Object[0]), new String[]{"QDM"});

         for (int var5 = 0; var5 < var1.size(); var5++) {
            String var6 = (String)var1.get(var5);
            DataInfo var7 = DataManager.getDataInfo("History", var6);
            boolean var8 = var7 != null && (var7.source == 6 || var7.source == 5);
            if (!var8) {
               var4.add(var6);
            }

            var3.add(var6);
         }

         WebSocketNotification.sendSuccess(
            L.t("Loading stock informations finished. Creating symbols for group...", new Object[0]), new String[]{"QDM", "SQUANT"}
         );
         var2.put("symbols", new String[]{String.join(",", var4)});
         var2.put("postfix", new String[]{""});
         var2.put("barType", new String[]{"1"});
         Collections.shuffle(var3);
         var2.put("updateSymbols", new String[]{String.join(",", var3)});
         Program.get("DataSourceSQEquityData").call("add", new Object[]{var2});
      } catch (Exception var9) {
         DownloadSemaphore.getInstance().downloadFinished();
         throw var9;
      }
   }

   private String onExportStocks(Map<String, String[]> var1) throws Exception {
      String var2 = this.tryGetParam(var1, "filePath")[0];
      Integer var3 = Integer.valueOf(this.tryGetParam(var1, "brokerId")[0]);

      try {
         BrokerDto var4 = BrokerManager.getInstance().getBroker(var3);
         if (var4 == null) {
            return apiErrorJSON(L.t("Broker doesn't exist.", new Object[0]), null);
         }

         List var5 = BrokerManager.getInstance().getStocks(var3);
         this.writeStocksToCsv(var2, var5);
         return this.getSuccessResult(L.t("Stocks were exported.", new Object[0]));
      } catch (Exception var6) {
         return apiErrorJSON(L.t("Cannot export stocks.", new Object[0]), var6);
      }
   }

   private void writeStocksToCsv(String var1, List<String> var2) throws IOException {
      StringBuilder var3 = new StringBuilder();

      for (String var5 : var2) {
         var3.append(var5);
         var3.append("\n");
      }

      Files.writeString(new File(var1).toPath(), var3.toString(), StandardOpenOption.CREATE);
   }

   private String onImportStocks(Map<String, String[]> var1) throws Exception {
      String var2 = this.tryGetParam(var1, "filePath")[0];
      Integer var3 = Integer.valueOf(this.tryGetParam(var1, "brokerId")[0]);
      String var4 = this.getParam(var1, "prefix", "");

      try {
         BrokerDto var5 = BrokerManager.getInstance().getBroker(var3);
         if (var5 == null) {
            return apiErrorJSON(L.t("Broker doesn't exist.", new Object[0]), null);
         }

         if (var5.isSystem()) {
            return apiErrorJSON(L.t("This broker profile can't be modified.", new Object[0]), null);
         }

         if (!var5.isStockPickerUse()) {
            return apiErrorJSON(L.t("This broker profile can't hold any stocks.", new Object[0]), null);
         }

         String var6 = Files.readString(new File(var2).toPath());
         BrokerManager.getInstance().importFromCsv(var6, var3, var4);
         SQWebSocketManager.addToDataQueue(WSDataObjects.getBrokers(), new String[]{"SQUANT", "QDM", "AlgoWizard"});
         return this.getSuccessResult(L.t("Stocks were imported", new Object[0]));
      } catch (Exception var7) {
         return apiErrorJSON(L.t("Cannot import stocks from file '%s'.", new Object[]{var2}), var7);
      }
   }

   private String onDeleteBroker(Map<String, String[]> var1) throws Exception {
      String[] var2 = this.tryGetParam(var1, "brokers")[0].split("\\,");

      for (String var6 : var2) {
         int var7 = Integer.valueOf(var6);
         BrokerDto var8 = BrokerManager.getInstance().deleteBroker(var7, false);
         SQWebSocketManager.addToDataQueue(WSDataObjects.getData(var8.getName(), "remove"), new String[]{"SQUANT", "QDM", "AlgoWizard"});
      }

      SQWebSocketManager.addToDataQueue(WSDataObjects.getBrokers(), new String[]{"SQUANT", "QDM", "AlgoWizard"});
      return this.getSuccessResult(L.t("Broker deleted", new Object[0]));
   }

   private String getSuccessResult(String var1) {
      JSONObject var2 = new JSONObject();
      var2.put("success", var1);
      return var2.toString();
   }

   private String onSaveBroker(Map<String, String[]> var1) throws Exception {
      JSONObject var2 = new JSONObject();

      try {
         String var3 = this.tryGetParam(var1, "name")[0];
         String var4 = this.tryGetParam(var1, "desc")[0];
         String var5 = this.tryGetParam(var1, "id")[0];
         String var6 = this.tryGetParam(var1, "postfix")[0];
         String var7 = this.tryGetParam(var1, "mtTimezone")[0];
         boolean var8 = Boolean.valueOf(this.tryGetParam(var1, "mtUse")[0]);
         boolean var9 = Boolean.valueOf(this.tryGetParam(var1, "stockPickerUse")[0]);
         Integer var10 = null;

         try {
            var10 = Integer.valueOf(var5);
         } catch (Exception var12) {
         }

         BrokerDto var11 = new BrokerDto();
         var11.setId(var10);
         var11.setName(var3);
         var11.setDesc(var4);
         var11.setMtTimezone(var7);
         var11.setPostfix(var6);
         var11.setStockPickerUse(var9);
         var11.setMtUse(var8);
         BrokerManager.getInstance().saveBroker(var11);
         SQWebSocketManager.addToDataQueue(WSDataObjects.getBrokers(), new String[]{"SQUANT", "QDM", "AlgoWizard"});
         var2.put("success", L.t("broker saved.", new Object[0]));
         return var2.toString();
      } catch (Exception var13) {
         return apiErrorJSON(L.t("Cannot save broker.", new Object[0]), var13);
      }
   }

   private String onListBroker(Map<String, String[]> var1) throws Exception {
      JSONObject var2 = new JSONObject();
      JSONArray var3 = new JSONArray();
      BrokerManager.getInstance().refresh();
      List var4 = BrokerManager.getInstance().getAllBrokers();

      for (int var5 = 0; var5 < var4.size(); var5++) {
         BrokerDto var6 = (BrokerDto)var4.get(var5);
         JSONObject var7 = new JSONObject();
         var7.put("id", var6.getId());
         var7.put("system", var6.isSystem());
         var7.put("name", var6.getName());
         var7.put("desc", var6.getDesc());
         var7.put("postfix", var6.getPostfix());
         var7.put("mtUse", var6.isMtUse());
         var7.put("mtTimezone", var6.getMtTimezone());
         var7.put("stockPickerUse", var6.isStockPickerUse());
         var7.put("customizedStocks", var6.getCustomizedStocks());
         var7.put("customizedInstruments", var6.getCustomizedInstruments());
         var7.put("customizedSessions", var6.getCustomizedSessions());
         JSONObject var8 = new JSONObject();
         var8.put("userdata", var7);
         JSONArray var9 = new JSONArray();
         var9.put(var6.getName());
         var9.put(var6.getDesc());
         var9.put(var6.getPostfix());
         var9.put(var6.getMtTimezone());
         if (!MainApp.checkProduct("QDM")) {
            var9.put(var6.getCustomizedStocks());
         }

         var9.put(var6.getCustomizedInstruments());
         if (!MainApp.checkProduct("QDM")) {
            var9.put(var6.getCustomizedSessions());
         }

         var8.put("data", var9);
         var3.put(var8);
      }

      var2.put("rows", var3);
      var2.put("success", L.t("Brokers listed.", new Object[0]));
      return var2.toString();
   }

   private String onEditStocks(Map<String, String[]> var1) throws Exception {
      JSONObject var2 = new JSONObject();
      Integer var3 = Integer.valueOf(this.tryGetParam(var1, "id")[0]);
      String var4 = "";

      try {
         var4 = this.tryGetParam(var1, "stocks")[0];
      } catch (Exception var8) {
      }

      List var5 = BrokerManager.getInstance().updateStocksFromStr(var4, var3, "");
      SQWebSocketManager.addToDataQueue(WSDataObjects.getBrokers(), new String[]{"SQUANT", "QDM", "AlgoWizard"});

      for (BrokerStockDto var7 : var5) {
         SQWebSocketManager.addToDataQueue(WSDataObjects.getData(var7.getTicker(), "update"), new String[]{"SQUANT", "QDM", "AlgoWizard"});
      }

      var2.put("success", L.t("Stocks saved.", new Object[0]));
      return var2.toString();
   }

   private String onListStocks(Map<String, String[]> var1) throws Exception {
      JSONObject var2 = new JSONObject();
      Integer var3 = Integer.valueOf(this.tryGetParam(var1, "brokerId")[0]);
      BrokerDto var4 = BrokerManager.getInstance().getBroker(var3);
      if (var4 == null) {
         return apiErrorJSON(L.t("Broker doesn't exist.", new Object[0]), null);
      }

      List var5 = BrokerManager.getInstance().getStocks(var3);
      StringBuilder var6 = new StringBuilder();

      for (int var7 = 0; var7 < var5.size(); var7++) {
         String var8 = (String)var5.get(var7);
         var6.append(var8);
         var6.append("\n");
      }

      var2.put("stocks", var6);
      var2.put("success", L.t("Stocks listed.", new Object[0]));
      return var2.toString();
   }
}
