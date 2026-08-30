package com.strategyquant.plugin.DataManager.impl.Basket;

import com.strategyquant.datalib.DataInfo;
import com.strategyquant.datalib.basket.BasketDto;
import com.strategyquant.datalib.basket.BasketOfStocksManager;
import com.strategyquant.datalib.basket.StockDto;
import com.strategyquant.datalib.data.DataManager;
import com.strategyquant.datalib.data.DownloadSemaphore;
import com.strategyquant.datalib.data.StockGroupUpdateErrorManager;
import com.strategyquant.lib.L;
import com.strategyquant.lib.SQTime;
import com.strategyquant.lib.XMLUtil;
import com.strategyquant.lib.app.MainApp;
import com.strategyquant.pluginlib.program.Program;
import com.strategyquant.pluginlib.program.ProgramDoesntExistException;
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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.jdom2.Element;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BasketServlet extends HttpJSONServlet {
   private static final long serialVersionUID = 1L;
   private static final Logger Log = LoggerFactory.getLogger(BasketServlet.class);

   protected String execute(String var1, Map<String, String[]> var2, String var3) throws Exception {
      switch (var1) {
         case "list":
            return this.onListBasket(var2);
         case "save":
            return this.onSaveBasket(var2);
         case "delete":
            return this.onDeleteBasket(var2);
         case "import":
            return this.onImportStocks(var2);
         case "listStocks":
            return this.onListStocks(var2);
         case "editStocks":
            return this.onEditStocks(var2);
         case "updateData":
            return this.onUpdateData(var2);
         case "export":
            return this.onExportStocks(var2);
         case "loadXml":
            return this.onLoad(var2);
         case "saveXml":
            return this.onSave(var2);
         default:
            throw new Exception("Unknown command '" + var1 + "'.");
      }
   }

   private String onLoad(Map<String, String[]> var1) {
      try {
         String var2 = this.tryGetParam(var1, "filePath")[0];
         File var3 = new File(var2);
         JSONObject var4 = new JSONObject();
         Element var5 = XMLUtil.fileToXmlElement(var3);
         if (!var5.getName().equals("Groups")) {
            throw new Exception(L.t("No Groups found to import.", new Object[0]));
         }

         LinkedList var6 = new LinkedList();

         for (Element var8 : var5.getChildren("Group")) {
            BasketDto var9 = new BasketDto();
            String var10 = var8.getAttributeValue("name");
            if (var10.startsWith("[[")) {
               var10 = var10.substring(1, var10.length() - 1);
            }

            var9.setName(var10);
            var9.setDesc(var8.getChildText("Desc"));
            ArrayList var11 = new ArrayList();
            Element var12 = var8.getChild("Stocks");

            for (Element var15 : var12.getChildren("Stock")) {
               StockDto var16 = new StockDto();
               var16.setTicker(var15.getAttributeValue("name"));

               try {
                  var16.setDateFrom(Long.valueOf(var15.getAttributeValue("from")));
               } catch (Exception var19) {
               }

               try {
                  var16.setDateTo(Long.valueOf(var15.getAttributeValue("to")));
               } catch (Exception var18) {
               }

               var11.add(var16);
            }

            var9.setCount(var11.size());
            BasketDto var21 = BasketOfStocksManager.getInstance().getBasket(var10);
            if (var21 != null && !MainApp.runInConsole()) {
               DataManagerConfirm.get()
                  .awaitConfirmation(
                     L.t("Overwrite confirm", new Object[0]),
                     L.t("Group '%s' already exists, do you want to overwrite it with the imported one?", new Object[]{var10})
                  );
               String var22 = DataManagerConfirm.get().getConfirmedAction();
               if (!var22.equals("skip")) {
                  if (var22.equals("cancel")) {
                     break;
                  }

                  if (var22.equals("overwrite") || var22.equals("overwrite_all")) {
                     var9.setId(var21.getId());
                     BasketOfStocksManager.getInstance().saveGroup(var9);
                     BasketOfStocksManager.getInstance().saveStocks(var9.getId(), var11);
                     var6.add(var9.getName());
                  }
               }
            } else {
               if (var21 != null) {
                  var9.setId(var21.getId());
               }

               BasketOfStocksManager.getInstance().saveGroup(var9);
               BasketOfStocksManager.getInstance().saveStocks(var9.getId(), var11);
               var6.add(var9.getName());
            }
         }

         SQWebSocketManager.addToDataQueue(WSDataObjects.getBaskets(), new String[]{"SQUANT", "QDM", "AlgoWizard"});
         var4.put("success", L.t("Stockgroups (%d) loaded.", new Object[]{var6.size()}));
         return var4.toString();
      } catch (Exception var20) {
         return apiErrorJSON(L.t("Cannot load stockgroups.", new Object[0]), var20);
      }
   }

   private String onSave(Map<String, String[]> var1) {
      try {
         String var2 = this.tryGetParam(var1, "filePath")[0];
         File var3 = new File(var2);
         JSONObject var4 = new JSONObject();
         Element var5 = new Element("Groups");
         String[] var6 = this.tryGetParam(var1, "ids[]");

         for (String var10 : var6) {
            BasketDto var11 = BasketOfStocksManager.getInstance().getBasket(Integer.valueOf(var10));
            List var12 = BasketOfStocksManager.getInstance().getStocks(var11.getId());
            var5.addContent(this.toXML(var11, var12));
         }

         XMLUtil.xmlToFile(var5, var3);
         var4.put("success", L.t("Groups saved.", new Object[0]));
         return var4.toString();
      } catch (Exception var13) {
         return apiErrorJSON(L.t("Cannot save groups.", new Object[0]), var13);
      }
   }

   private Element toXML(BasketDto var1, List<StockDto> var2) {
      Element var3 = new Element("Group");
      var3.setAttribute("name", var1.getName());
      Element var4 = new Element("Desc");
      var4.setText(var1.getDesc());
      var3.addContent(var4);
      Element var5 = new Element("Stocks");
      var3.addContent(var5);

      for (StockDto var7 : var2) {
         Element var8 = new Element("Stock");
         var8.setAttribute("name", var7.getTicker());
         var8.setAttribute("from", var7.getDateFrom() == null ? "" : var7.getDateFrom().toString());
         var8.setAttribute("to", var7.getDateTo() == null ? "" : var7.getDateTo().toString());
         var5.addContent(var8);
      }

      return var3;
   }

   private String onEditGroup(Map<String, String[]> var1) {
      return null;
   }

   private String onExportStocks(Map<String, String[]> var1) throws Exception {
      String var2 = this.tryGetParam(var1, "filePath")[0];
      Integer var3 = Integer.valueOf(this.tryGetParam(var1, "basketId")[0]);

      try {
         BasketDto var4 = BasketOfStocksManager.getInstance().getBasket(var3);
         if (var4 == null) {
            return apiErrorJSON(L.t("Group doesn't exist.", new Object[0]), null);
         }

         List var5 = BasketOfStocksManager.getInstance().getStocks(var3);
         this.writeStocksToCsv(var2, var5);
         return this.getSuccessResult("Stocks were exported");
      } catch (Exception var6) {
         return apiErrorJSON(L.t("Cannot import stocks.", new Object[0]), var6);
      }
   }

   private void writeStocksToCsv(String var1, List<StockDto> var2) throws IOException {
      StringBuilder var3 = new StringBuilder();

      for (StockDto var5 : var2) {
         var3.append(var5.getTicker() + ";" + SQTime.formatDate(var5.getDateFrom()));
         if (var5.getDateTo() != null) {
            var3.append(";" + SQTime.formatDate(var5.getDateTo()));
         }

         var3.append("\n");
      }

      Files.writeString(new File(var1).toPath(), var3.toString(), StandardOpenOption.CREATE);
   }

   private String onImportStocks(Map<String, String[]> var1) throws Exception {
      String var2 = this.tryGetParam(var1, "filePath")[0];
      Integer var3 = Integer.valueOf(this.tryGetParam(var1, "basketId")[0]);
      String var4 = this.getParam(var1, "prefix", "");

      try {
         BasketDto var5 = BasketOfStocksManager.getInstance().getBasket(var3);
         if (var5 == null) {
            return apiErrorJSON(L.t("Group doesn't exist.", new Object[0]), null);
         }

         if (var5.isSystem()) {
            return apiErrorJSON(L.t("This group can't be modified.", new Object[0]), null);
         }

         String var6 = Files.readString(new File(var2).toPath());
         BasketOfStocksManager.getInstance().importFromCsv(var6, var3, null, var4);
         SQWebSocketManager.addToDataQueue(WSDataObjects.getBaskets(), new String[]{"SQUANT", "QDM", "AlgoWizard"});
         return this.getSuccessResult(L.t("Stocks were imported", new Object[0]));
      } catch (Exception var7) {
         return apiErrorJSON(L.t("Cannot import stocks from file '%s'.", new Object[]{var2}), var7);
      }
   }

   private String onDeleteBasket(Map<String, String[]> var1) throws Exception {
      String[] var2 = this.tryGetParam(var1, "baskets")[0].split("\\,");

      for (String var6 : var2) {
         int var7 = Integer.valueOf(var6);
         BasketDto var8 = BasketOfStocksManager.getInstance().deleteGroup(var7);
         SQWebSocketManager.addToDataQueue(WSDataObjects.getData(var8.getName(), "remove"), new String[]{"SQUANT", "QDM", "AlgoWizard"});
      }

      SQWebSocketManager.addToDataQueue(WSDataObjects.getBaskets(), new String[]{"SQUANT", "QDM", "AlgoWizard"});
      return this.getSuccessResult(L.t("Group deleted", new Object[0]));
   }

   private String getSuccessResult(String var1) {
      JSONObject var2 = new JSONObject();
      var2.put("success", var1);
      return var2.toString();
   }

   private String onSaveBasket(Map<String, String[]> var1) throws Exception {
      JSONObject var2 = new JSONObject();

      try {
         String var3 = this.tryGetParam(var1, "name")[0];
         String var4 = this.tryGetParam(var1, "desc")[0];
         String var5 = this.tryGetParam(var1, "id")[0];
         Integer var6 = null;

         try {
            var6 = Integer.valueOf(var5);
         } catch (Exception var8) {
         }

         BasketDto var7 = new BasketDto();
         var7.setId(var6);
         var7.setName(var3);
         var7.setDesc(var4);
         BasketOfStocksManager.getInstance().saveGroup(var7);
         SQWebSocketManager.addToDataQueue(WSDataObjects.getBaskets(), new String[]{"SQUANT", "QDM", "AlgoWizard"});
         if (var6 == null) {
            SQWebSocketManager.addToDataQueue(WSDataObjects.getData("", "add"), new String[]{"SQUANT", "QDM", "AlgoWizard"});
            SQWebSocketManager.addToDataQueue(WSDataObjects.getInstruments(), new String[]{"SQUANT", "QDM", "AlgoWizard"});
         }

         var2.put("success", L.t("Group added.", new Object[0]));
         return var2.toString();
      } catch (Exception var9) {
         return apiErrorJSON(L.t("Cannot save group.", new Object[0]), var9);
      }
   }

   private String onListBasket(Map<String, String[]> var1) throws Exception {
      JSONObject var2 = new JSONObject();
      JSONArray var3 = new JSONArray();
      HashMap var4 = new HashMap();
      DataManager.get();
      ArrayList var5 = DataManager.list();
      HashSet var6 = new HashSet();

      for (DataInfo var8 : var5) {
         var6.add(var8.symbol);
         var4.put(var8.symbol, var8);
      }

      List var15 = BasketOfStocksManager.getInstance().getGroups();

      for (int var16 = 0; var16 < var15.size(); var16++) {
         BasketDto var9 = (BasketDto)var15.get(var16);
         JSONObject var10 = new JSONObject();
         var10.put("id", var9.getId());
         var10.put("system", var9.isSystem());
         JSONObject var11 = new JSONObject();
         var11.put("userdata", var10);
         JSONArray var12 = new JSONArray();
         var12.put(var9.getName());
         var12.put(var9.getCount() + " / " + var9.getTotal());
         var12.put(var9.getDesc());
         var12.put(BasketOfStocksManager.getNumberOfSymbolsInGroup(var6, var9));
         int var13 = BasketOfStocksManager.getDownloaded(var4, var9.getId());
         var12.put(var13 + " / " + var9.getTotal());
         var12.put(
            var9.getTotal() > 0 && BasketOfStocksManager.isReadyForUse(var13, var9.getTotal())
               ? "Yes"
               : "No, <a onmousedown=\"sqGridSendEvent(event, 'updateSpecificGroup')\">Update data</a> <i title=\"Stock group is not ready for use, not all its symbols are downloaded\" class=\"fa fa-question-circle-o\" aria-hidden=\"true\"></i>"
         );
         DataInfo var14 = DataManager.getDataInfo("History", var9.getName());
         if (var14 == null) {
            var12.put("-");
            var12.put("-");
         } else {
            var12.put(var14.dateFrom == 0L ? "" : SQTime.toDateString(var14.dateFrom));
            var12.put(var14.dateTo == 0L ? "" : SQTime.toDateString(var14.dateTo));
         }

         var11.put("data", var12);
         var3.put(var11);
      }

      var2.put("rows", var3);
      var2.put("success", L.t("Groups listed", new Object[0]));
      return var2.toString();
   }

   private String onEditStocks(Map<String, String[]> var1) throws Exception {
      JSONObject var2 = new JSONObject();
      Integer var3 = Integer.valueOf(this.tryGetParam(var1, "id")[0]);
      String var4 = this.tryGetParam(var1, "stocks")[0];
      List var5 = BasketOfStocksManager.getInstance().updateStocksFromStr(var4, var3, "");
      SQWebSocketManager.addToDataQueue(WSDataObjects.getBaskets(), new String[]{"SQUANT", "QDM", "AlgoWizard"});
      var2.put("success", L.t("Stocks saved", new Object[0]));
      return var2.toString();
   }

   private String onListStocks(Map<String, String[]> var1) throws Exception {
      JSONObject var2 = new JSONObject();
      Integer var3 = Integer.valueOf(this.tryGetParam(var1, "basketId")[0]);
      BasketDto var4 = BasketOfStocksManager.getInstance().getBasket(var3);
      if (var4 == null) {
         return apiErrorJSON(L.t("Group doesn't exist.", new Object[0]), null);
      }

      List var5 = BasketOfStocksManager.getInstance().getStocks(var3);
      StringBuilder var6 = new StringBuilder();

      for (int var7 = 0; var7 < var5.size(); var7++) {
         StockDto var8 = (StockDto)var5.get(var7);
         var6.append(var8.getTicker());
         if (var8.getDateFrom() != null && var8.getDateFrom() != -1L) {
            var6.append(";");
            var6.append(SQTime.toDateString(var8.getDateFrom()));
         }

         if (var8.getDateTo() != null && var8.getDateTo() != -1L) {
            var6.append(";");
            var6.append(SQTime.toDateString(var8.getDateTo()));
         }

         var6.append("\n");
      }

      var2.put("stocks", var6);
      var2.put("success", L.t("Stocks listed.", new Object[0]));
      return var2.toString();
   }

   private String onUpdateData(Map<String, String[]> var1) throws Exception {
      if (!DownloadSemaphore.getInstance().startDownloadData(5)) {
         return apiErrorJSON(L.t("Please wait couple of seconds and try again.", new Object[0]), null);
      }

      try {
         StockGroupUpdateErrorManager.getInstance().start();
         String[] var2 = this.tryGetParamValue(var1, "basketId").split(",");
         final LinkedList var3 = new LinkedList();
         HashSet var4 = new HashSet();

         for (String var8 : var2) {
            Integer var9 = Integer.valueOf(var8);
            BasketDto var10 = BasketOfStocksManager.getInstance().getBasket(var9);
            if (var10 == null) {
               DownloadSemaphore.getInstance().downloadFinished();
               return apiErrorJSON(L.t("Group doesn't exist.", new Object[0]), null);
            }

            if (var10.getName().equalsIgnoreCase("[[S&P 100 limited]]")) {
               DownloadSemaphore.getInstance().downloadFinished();
               return apiErrorJSON(L.t("Group S&P 100 limited cannot be updated, it is only in clean installation.", new Object[0]), null);
            }

            for (StockDto var13 : BasketOfStocksManager.getInstance().getStocks(var9)) {
               if (!var4.contains(var13.getTicker())) {
                  var3.add(var13);
                  var4.add(var13.getTicker());
               }
            }
         }

         if (var3.isEmpty()) {
            DownloadSemaphore.getInstance().downloadFinished();
            return this.getSuccessResult(L.t("Nothing to update. Group is without any stocks.", new Object[0]));
         }

         if (MainApp.runInConsoleWithoutActiveWebserver()) {
            this._onUpdateData(var3);
         } else {
            new Thread(new Runnable() {
               @Override
               public void run() {
                  try {
                     BasketServlet.this._onUpdateData(var3);
                  } catch (Exception var2x) {
                     BasketServlet.Log.error("Error while updating data", var2x);
                  }
               }
            }).start();
         }
      } catch (Exception var14) {
         DownloadSemaphore.getInstance().downloadFinished();
         throw var14;
      }

      return this.getSuccessResult(L.t("Starting updating data. Please wait...", new Object[0]));
   }

   private void _onUpdateData(List<StockDto> var1) throws ProgramDoesntExistException, Exception {
      try {
         HashMap var2 = new HashMap();
         ArrayList var3 = new ArrayList();
         ArrayList var4 = new ArrayList();
         WebSocketNotification.sendSuccess(L.t("Loading stock informations. ", new Object[0]), new String[]{"QDM"});
         Log.info("Checking stocks in groups");

         for (int var5 = 0; var5 < var1.size(); var5++) {
            StockDto var6 = (StockDto)var1.get(var5);
            String var7 = var6.getTicker();
            DataInfo var8 = DataManager.getDataInfo("History", var7);
            boolean var9 = var8 != null && (var8.source == 6 || var8.source == 5);
            if (!var9) {
               var4.add(var7);
            }

            var3.add(var7);
         }

         WebSocketNotification.sendSuccess(
            L.t("Loading stock informations finished. Creating symbols for group...", new Object[0]), new String[]{"QDM", "SQUANT"}
         );
         var2.put("symbols", new String[]{String.join(",", var4)});
         var2.put("postfix", new String[]{""});
         var2.put("barType", new String[]{"1"});
         if (var4.size() > 0) {
            Log.info("Adding {} stocks are necessary.", String.join(",", var4));
         } else {
            Log.info("No adding stocks are necessary.");
         }

         Collections.shuffle(var3);
         var2.put("updateSymbols", new String[]{String.join(",", var3)});
         Log.info("Running update process");
         Program.get("DataSourceSQEquityData").call("add", new Object[]{var2});
      } catch (Exception var10) {
         DownloadSemaphore.getInstance().downloadFinished();
         throw var10;
      }
   }
}
