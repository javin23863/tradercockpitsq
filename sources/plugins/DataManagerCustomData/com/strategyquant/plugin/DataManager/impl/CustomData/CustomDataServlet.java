package com.strategyquant.plugin.DataManager.impl.CustomData;

import com.strategyquant.datalib.customData.AvailableCustomDataFormats;
import com.strategyquant.datalib.customData.CustomData;
import com.strategyquant.datalib.customData.CustomDataBinReader;
import com.strategyquant.datalib.customData.CustomDataInfo;
import com.strategyquant.datalib.customData.CustomDataManager;
import com.strategyquant.datalib.data.imports.CsvFileReader;
import com.strategyquant.datalib.data.imports.CustomDataFormat;
import com.strategyquant.datalib.data.imports.DataColumns;
import com.strategyquant.datalib.data.imports.DataImportEngine;
import com.strategyquant.datalib.data.io.ImportDataInfo;
import com.strategyquant.datalib.data.io.columns.CustomValue;
import com.strategyquant.datalib.data.io.columns.DateCol;
import com.strategyquant.datalib.data.io.columns.DefaultCol;
import com.strategyquant.datalib.data.io.columns.TimeCol;
import com.strategyquant.datalib.data.io.columns.UnusedCol;
import com.strategyquant.datalib.indicators.SCustomIndicator;
import com.strategyquant.datalib.indicators.SCustomIndicatorFileParser;
import com.strategyquant.gridlib.client.SQGrid;
import com.strategyquant.lib.L;
import com.strategyquant.lib.SQTime;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.XMLUtil;
import com.strategyquant.lib.app.MainApp;
import com.strategyquant.lib.time.DateFormats;
import com.strategyquant.plugin.DataManager.impl.CustomData.job.CustomDataImporterJob;
import com.strategyquant.qdm.QDM;
import com.strategyquant.tradinglib.project.console.CLILogger;
import com.strategyquant.tradinglib.project.websocket.DataManagerConfirm;
import com.strategyquant.tradinglib.project.websocket.DataManagerCustomDataProgress;
import com.strategyquant.tradinglib.project.websocket.DataToSend;
import com.strategyquant.tradinglib.project.websocket.MultiProgressListener;
import com.strategyquant.tradinglib.project.websocket.SQWebSocketManager;
import com.strategyquant.tradinglib.project.websocket.WSDataObjects;
import com.strategyquant.webguilib.servlet.HttpJSONServlet;
import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import org.jdom2.Element;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CustomDataServlet extends HttpJSONServlet {
   private static final long serialVersionUID = 1L;
   private static final Logger Log = LoggerFactory.getLogger(CustomDataServlet.class);

   protected String execute(String var1, Map<String, String[]> var2, String var3) throws Exception {
      switch (var1) {
         case "list":
            return this.onList(var2);
         case "add":
            return this.onAdd(var2);
         case "edit":
            return this.onEdit(var2);
         case "clear":
            return this.onClear(var2);
         case "remove":
            return this.onRemove(var2);
         case "import":
            return this.onImportData(var2);
         case "importAction":
            return this.onImportAction(var2);
         case "importGetInfo":
            return this.onImportGetInfo();
         case "importSaveNewDataFormat":
            return this.onImportSaveNewDataFormat(var2);
         case "importDeleteDataFormat":
            return this.onImportDeleteDataFormat(var2);
         case "importUpdateDataFormat":
            return this.onImportUpdateDataFormat(var2);
         case "importGetOverview":
            return this.onImportGetOverview(var2);
         case "importcli":
            return this.onImportDataCli(var2);
         case "recognizeFromFile":
            return this.onRecognizeFromFile(var2);
         case "reviewData":
            return this.onReviewData(var2);
         case "load":
            return this.onLoad(var2);
         case "save":
            return this.onSave(var2);
         default:
            throw new Exception(L.t("Unknown command '%s'.", new Object[]{var1}));
      }
   }

   private String onRecognizeFromFile(Map<String, String[]> var1) throws Exception {
      JSONObject var2 = new JSONObject();
      String var3 = this.tryGetParam(var1, "filePath")[0];

      SCustomIndicator var4;
      try {
         File var5 = new File(var3);
         if (!var5.exists()) {
            throw new Exception(L.t("File doesn't exist.", new Object[0]));
         }

         var4 = SCustomIndicatorFileParser.parse(var5);
         CustomDataManager.add(var4);
         this.sendDataUpdate(var4.shortName, "add");
      } catch (Exception var6) {
         throw new Exception(L.t("Failed to recognize custom indicator from file. Reason: %s", new Object[]{var6.getMessage()}));
      }

      var2.put("success", L.t("Indicator '%s' added.", new Object[]{var4.shortName}));
      return var2.toString();
   }

   private String onReviewData(Map<String, String[]> var1) throws Exception {
      JSONObject var2 = new JSONObject();

      int var3;
      try {
         var3 = Integer.parseInt(((String[])var1.get("posStart"))[0]);
      } catch (Exception var17) {
         var3 = 0;
      }

      int var4;
      try {
         var4 = Integer.parseInt(((String[])var1.get("count"))[0]);
      } catch (Exception var16) {
         var4 = 100;
      }

      String var5 = ((String[])var1.get("name"))[0].toString();
      CustomDataInfo var6 = CustomDataManager.getDataInfo(var5);
      if (var6 == null) {
         throw new Exception(L.t("Indicator with name '%s' doesn't exist.", new Object[]{var5}));
      }

      JSONArray var7 = new JSONArray();
      CustomDataBinReader var8 = new CustomDataBinReader(var6);
      var8.open();
      long var9 = 0L;
      long var11 = 0L;

      while (var8.hasNextData()) {
         var8.loadData();
         if (var9 < var3) {
            var9++;
         } else {
            if (var11 >= var4) {
               break;
            }

            CustomData var13 = var8.loadedData;
            JSONArray var14 = new JSONArray();
            var14.put(SQTime.toFullDateMinuteString(var13.time));

            for (int var15 = 0; var15 < var13.values.length; var15++) {
               var14.put(var13.values[var15]);
            }

            JSONObject var18 = new JSONObject();
            var18.put("id", "r" + (var3 + var9));
            var18.put("data", var14);
            var7.put(var18);
            var11++;
         }
      }

      var8.close();
      var2.put("total_count", var6.rows);
      var2.put("rows", var7);
      var2.put("pos", var3);
      var2.put("success", L.t("Data listed.", new Object[0]));
      return var2.toString();
   }

   private String onList(Map<String, String[]> var1) {
      JSONObject var2 = new JSONObject();

      try {
         DataToSend var3 = WSDataObjects.getCustomData("", "list");
         if (MainApp.runInConsoleWithoutActiveWebserver()) {
            JSONArray var4 = var3.getDataObject().getJSONArray("data");

            for (int var5 = 0; var5 < var4.length(); var5++) {
               JSONObject var6 = var4.getJSONObject(var5);
               CLILogger.log(String.format("External indicator %d: %s => %s", var5, var6.getString("name"), var6.toString()));
            }
         } else {
            SQWebSocketManager.addToDataQueue(var3, new String[]{"SQUANT", "QDM"});
         }
      } catch (Exception var7) {
         return apiErrorJSON(L.t("Cannot get list of indicators.", new Object[0]), var7);
      }

      var2.put("success", L.t("Indicators listed.", new Object[0]));
      return var2.toString();
   }

   private String onAdd(Map<String, String[]> var1) {
      JSONObject var2 = new JSONObject();

      try {
         String var3 = this.tryGetParam(var1, "name")[0];
         String var4 = this.tryGetParam(var1, "values")[0];
         int var5 = Integer.parseInt(this.tryGetParam(var1, "type")[0]);
         if (var3.trim().isEmpty()) {
            throw new Exception(L.t("Name cannot be empty", new Object[0]));
         }

         CustomDataInfo var6 = CustomDataManager.getDataInfo(var3);
         if (var6 != null) {
            throw new Exception(L.t("Custom data with name '%s' already exists.", new Object[]{var3}));
         }

         CustomDataManager.add(var3, var4, var5);
         this.sendDataUpdate(var3, "add");
      } catch (Exception var7) {
         return apiErrorJSON(L.t("Cannot add indicator.", new Object[0]), var7);
      }

      var2.put("success", L.t("Indicator added.", new Object[0]));
      return var2.toString();
   }

   private String onEdit(Map<String, String[]> var1) throws Exception {
      JSONObject var2 = new JSONObject();
      String var3 = this.tryGetParam(var1, "oldName")[0];
      String var4 = this.tryGetParam(var1, "name")[0];
      String var5 = this.tryGetParam(var1, "values")[0];
      int var6 = Integer.parseInt(this.tryGetParam(var1, "type")[0]);
      if (var4.trim().isEmpty()) {
         throw new Exception(L.t("Name cannot be empty", new Object[0]));
      }

      if (!var3.equals(var4)) {
         throw new Exception(L.t("Custom indicator cannot be renamed", new Object[0]));
      }

      CustomDataInfo var7 = CustomDataManager.getDataInfo(var3);
      if (var7 == null) {
         throw new Exception(L.t("Indicator with name '%s' doesn't exist.", new Object[]{var3}));
      }

      CustomDataManager.update(var3, var4, var5, var6);
      this.sendDataUpdate(var4, "update");
      var2.put("success", L.t("Indicator updated.", new Object[0]));
      return var2.toString();
   }

   private String onClear(Map<String, String[]> var1) {
      JSONObject var2 = new JSONObject();

      try {
         String[] var3 = this.tryGetParam(var1, "data")[0].split(",");

         for (int var4 = 0; var4 < var3.length; var4++) {
            CustomDataManager.clear(var3[var4]);
         }

         this.sendDataUpdate(this.tryGetParam(var1, "data")[0], "clear");
      } catch (Exception var5) {
         return apiErrorJSON(L.t("Cannot clear indicator data.", new Object[0]), var5);
      }

      var2.put("success", L.t("Indicator data cleared.", new Object[0]));
      return var2.toString();
   }

   private String onRemove(Map<String, String[]> var1) {
      JSONObject var2 = new JSONObject();

      try {
         String[] var3 = this.tryGetParam(var1, "data")[0].split(",");

         for (int var4 = 0; var4 < var3.length; var4++) {
            CustomDataManager.delete(var3[var4]);
         }

         this.sendDataUpdate(this.tryGetParam(var1, "data")[0], "remove");
      } catch (Exception var5) {
         return apiErrorJSON(L.t("Cannot remove indicator.", new Object[0]), var5);
      }

      var2.put("success", L.t("Indicator removed.", new Object[0]));
      return var2.toString();
   }

   private String onImportGetInfo() {
      JSONObject var1 = new JSONObject();
      JSONObject var2 = new JSONObject();
      JSONArray var3 = new JSONArray();
      JSONObject var4 = new JSONObject();
      var4.put("skipColumns", "0");
      var4.put("skipRows", "0");
      var4.put("dateFormat", DateFormats.getInstance().getAvailableDateFormats().get(0));
      var4.put("separator", ",");
      var4.put("name", "Custom");
      var3.put(var4);

      for (CustomDataFormat var6 : AvailableCustomDataFormats.getInstance().getAvailableFileFormats()) {
         JSONObject var7 = new JSONObject();
         var7.put("columnTypes", this.listColumnTypes(var6.getColumns()));
         var7.put("skipColumns", var6.getSkipColumns());
         var7.put("skipRows", var6.getSkipRows());
         var7.put("dateFormat", var6.getDateFormat());
         var7.put("separator", var6.getSeparator());
         var7.put("name", var6.getName());
         var7.put("predefined", var6.isPredefined());
         var3.put(var7);
      }

      var2.put("fileFormats", var3);
      JSONArray var12 = new JSONArray();
      Calendar var13 = Calendar.getInstance();
      var13.set(2016, 6, 21, 8, 15, 22);
      var13.set(14, 250);
      long var14 = var13.getTimeInMillis();

      for (int var9 = 0; var9 < DateFormats.getInstance().getAvailableDateFormats().size(); var9++) {
         JSONObject var10 = new JSONObject();
         String var11 = (String)DateFormats.getInstance().getAvailableDateFormats().get(var9);
         var10.put("pattern", var11);
         var10.put(
            "example", !var11.equals("Long (millis from epoch)") && !var11.equals("Long (seconds from epoch)") ? SQUtils.formatDate(var14, var11) : var14
         );
         var12.put(var10);
      }

      var2.put("dateFormats", var12);
      var1.put("importInfo", var2);
      var1.put("success", "ok");
      return var1.toString();
   }

   private String onImportGetOverview(Map<String, String[]> var1) {
      JSONObject var2 = new JSONObject();

      try {
         String var3 = this.tryGetParam(var1, "filePath")[0];
         String var4 = this.tryGetParam(var1, "customFormat")[0];
         String var5 = this.tryGetParam(var1, "separator")[0];
         int var6 = Integer.parseInt(this.tryGetParam(var1, "skipRows")[0]);
         int var7 = Integer.parseInt(this.tryGetParam(var1, "skipColumns")[0]);
         boolean var8 = var4.equals("true");
         boolean var9 = false;
         String var10 = null;

         try {
            var10 = this.tryGetParam(var1, "fileFormat")[0];
         } catch (Exception var27) {
         }

         CustomDataFormat var11 = null;
         ImportDataInfo var12 = new ImportDataInfo();
         var12.filePath = var3;
         var12.rowCount = 10;
         if (var8) {
            var12.separator = var5;
            var12.skipRows = var6;
            var12.skipCols = var7;
         } else {
            try {
               if (var10 != null) {
                  var11 = AvailableCustomDataFormats.getInstance().findFileFormatByName(var10);
               }

               if (var11 == null) {
                  ArrayList var13 = AvailableCustomDataFormats.getInstance().getAvailableFileFormats();
                  var11 = DataImportEngine.getFileFormat(var3, var13, (CustomDataFormat)var13.get(0));
               }

               JSONObject var29 = new JSONObject();
               var29.put("skipColumns", var11.getSkipColumns());
               var29.put("skipRows", var11.getSkipRows());
               var29.put("dateFormat", var11.getDateFormat());
               var29.put("separator", var11.getSeparator());
               var29.put("name", var11.getName());
               var29.put("columnTypes", this.listColumnTypes(var11.getColumns()));
               var2.put("format", var29);
               var12.name = var11.getName();
               var12.skipRows = var11.getSkipRows();
               var12.skipCols = var11.getSkipColumns();
               var12.separator = var11.getSeparator();
            } catch (Exception var26) {
               Log.error("Cannot recognize file format. ", var26);
               var12.skipRows = 0;
               var12.skipCols = 0;
               var9 = true;
            }
         }

         CsvFileReader var30 = new CsvFileReader(var12);
         String[][] var14 = var30.read(var9);
         String var15 = null;
         JSONArray var16 = new JSONArray();
         if (var14 != null && var14.length > 0) {
            for (String[] var20 : var14) {
               JSONArray var21 = new JSONArray();

               for (String var25 : var20) {
                  if (var15 == null) {
                     var15 = var25;
                  }

                  var21.put(var25);
               }

               var16.put(var11 == null ? var21 : this.fillMissingValues(var11, var21));
            }
         }

         if (var9) {
            JSONObject var31 = new JSONObject();
            var31.put("custom", true);
            var31.put("separator", var30.getSeparator());
            var31.put("skipRows", var12.skipRows);
            String var32 = DateFormats.getInstance().recognize(var15);
            if (var32 != null) {
               var31.put("dateFormat", var32);
            }

            var2.put("format", var31);
         }

         var2.put("overviewData", var16);
      } catch (Exception var28) {
         return apiErrorJSON(L.t("Cannot get file overview.", new Object[0]), var28);
      }

      var2.put("success", L.t("File overview returned.", new Object[0]));
      return var2.toString();
   }

   private JSONArray fillMissingValues(CustomDataFormat var1, JSONArray var2) {
      int var3 = 1;

      for (int var5 : var1.getColumns().keySet()) {
         if (var2.length() < var3) {
            var2.put("");
         }

         var3++;
      }

      return var2;
   }

   private String onImportData(Map<String, String[]> var1) {
      JSONObject var2 = new JSONObject();

      try {
         String var3 = this.tryGetParam(var1, "data")[0];
         ImportDataInfo var4 = new ImportDataInfo();
         var4.name = this.tryGetParam(var1, "fileFormat")[0];
         var4.filePath = this.tryGetParam(var1, "filePath")[0];
         var4.dateFormat = this.tryGetParam(var1, "dateFormat")[0];
         var4.separator = this.tryGetParam(var1, "separator")[0];
         var4.skipRows = Integer.parseInt(this.tryGetParam(var1, "skipRows")[0]);
         var4.skipCols = Integer.parseInt(this.tryGetParam(var1, "skipColumns")[0]);
         var4.errorHandling = Integer.parseInt(this.tryGetParam(var1, "errorHandling")[0]);
         CustomDataFormat var5 = AvailableCustomDataFormats.getInstance().findFileFormatByName(var4.name);
         if (var5 != null) {
            var4.timeFormat = var5.getTimeFormat();
         }

         ArrayList var6 = new ArrayList();
         String[] var7 = this.tryGetParam(var1, "columnTypes")[0].split(",");

         for (String var11 : var7) {
            var6.add(DataColumns.getInstance().findColTypeByName(var11));
         }

         var4.columnTypes = var6;
         QDM.getInstance().activity.increase(5);
         MultiProgressListener var13 = DataManagerCustomDataProgress.get().createListener(var3, "customdata/importAction");
         String var14 = "CustomDataImport_" + var3;
         CustomDataImporterJob var15 = new CustomDataImporterJob(var14, var13, CustomDataManager.getDataInfo(var3), var4);
         SQGrid.getGridClient().executeOnGrid(var14, var15);
      } catch (Exception var12) {
         return apiErrorJSON(L.t("Cannot import data.", new Object[0]), var12);
      }

      var2.put("success", L.t("Importing indicator data.", new Object[0]));
      return var2.toString();
   }

   private String onImportAction(Map<String, String[]> var1) throws Exception {
      this.checkParamExists(var1, new String[]{"data", "action"});
      String var2 = this.tryGetParam(var1, "data")[0];
      String var3 = this.tryGetParam(var1, "action")[0];
      JSONObject var4 = new JSONObject();
      String var5 = "CustomDataImport_" + var2;
      switch (var3) {
         case "stop":
            SQGrid.getGridClient().stop(var5);
            break;
         case "pause":
            SQGrid.getGridClient().pause(var5);
            break;
         case "continue":
            SQGrid.getGridClient().restart(var5);
      }

      var4.put("success", "ok");
      return var4.toString();
   }

   private String onImportSaveNewDataFormat(Map<String, String[]> var1) {
      JSONObject var2 = new JSONObject();

      try {
         String var3 = this.tryGetParam(var1, "name")[0];
         CustomDataFormat var4 = this.getFileFormat(var1);
         if (AvailableCustomDataFormats.getInstance().formatExists(var3)) {
            throw new Exception(L.t("Data format with name '%s' already exists. Please choose another name.", new Object[]{var3}));
         }

         AvailableCustomDataFormats.getInstance().addDataFormat(var4);
      } catch (Exception var5) {
         return apiErrorJSON(L.t("Cannot save indicator data format.", new Object[0]), var5);
      }

      var2.put("success", L.t("Data format saved.", new Object[0]));
      return var2.toString();
   }

   private String onImportDeleteDataFormat(Map<String, String[]> var1) {
      JSONObject var2 = new JSONObject();

      try {
         String var3 = this.tryGetParam(var1, "name")[0];
         AvailableCustomDataFormats.getInstance().deleteDataFormat(var3);
      } catch (Exception var4) {
         return apiErrorJSON(L.t("Cannot delete indicator data format.", new Object[0]), var4);
      }

      var2.put("success", L.t("Data format deleted.", new Object[0]));
      return var2.toString();
   }

   private String onImportUpdateDataFormat(Map<String, String[]> var1) {
      JSONObject var2 = new JSONObject();

      try {
         AvailableCustomDataFormats.getInstance().updateDataFormat(this.getFileFormat(var1));
      } catch (Exception var4) {
         return apiErrorJSON(L.t("Cannot update indicator data format.", new Object[0]), var4);
      }

      var2.put("success", L.t("Data format updated.", new Object[0]));
      return var2.toString();
   }

   private CustomDataFormat getFileFormat(Map<String, String[]> var1) throws Exception {
      CustomDataFormat var2 = new CustomDataFormat();
      var2.setName(this.tryGetParam(var1, "name")[0]);
      var2.setSeparator(this.tryGetParam(var1, "separator")[0]);
      var2.setSkipColumns(Integer.parseInt(this.tryGetParam(var1, "skipColumns")[0]));
      var2.setSkipRows(Integer.parseInt(this.tryGetParam(var1, "skipRows")[0]));
      var2.setDateFormat(this.tryGetParam(var1, "dateFormat")[0]);
      var2.setPredefined(false);
      HashMap var3 = new HashMap();
      String[] var4 = this.tryGetParam(var1, "columnTypes")[0].split(",");

      for (int var5 = 0; var5 < var4.length; var5++) {
         DefaultCol var6 = DataColumns.getInstance().findColTypeByName(var4[var5]);
         if (var6 == null) {
            if (var4[var5].startsWith("Choose")) {
               throw new Exception(L.t("Column no. %d not set.", new Object[]{var5 + 1}));
            }

            throw new Exception(L.t("Unknown column '%s'.", new Object[]{var4[var5]}));
         }

         var3.put(var5, var6);
      }

      var2.setColumns(var3);
      return var2;
   }

   private JSONArray listColumnTypes(HashMap<Integer, DefaultCol> var1) {
      JSONArray var2 = new JSONArray();

      for (int var4 : var1.keySet()) {
         DefaultCol var5 = (DefaultCol)var1.get(var4);
         var2.put(var5.getName());
      }

      return var2;
   }

   private void sendDataUpdate(String var1, String var2) {
      try {
         SQWebSocketManager.addToDataQueue(WSDataObjects.getCustomData(var1, var2), new String[]{"SQUANT", "QDM", "AlgoWizard"});
      } catch (Exception var4) {
         Log.error("Cannot send websocket data update. ", var4);
      }
   }

   private String onLoad(Map<String, String[]> var1) {
      try {
         String var2 = this.tryGetParam(var1, "filePath")[0];
         File var3 = new File(var2);
         JSONObject var4 = new JSONObject();
         ArrayList var5 = new ArrayList();
         Element var6 = XMLUtil.fileToXmlElement(var3);
         if (!var6.getName().equals("ExternalIndicators")) {
            throw new Exception(L.t("No Indicators found to import.", new Object[0]));
         }

         for (Element var8 : var6.getChildren("IndicatorDataInfo")) {
            CustomDataInfo var9 = new CustomDataInfo();
            var9.setFromXML(var8);
            if (!CustomDataManager.checkDataExists(var9.name)) {
               CustomDataManager.add(var9);
               var5.add(var9.name);
            } else {
               DataManagerConfirm.get()
                  .awaitConfirmation(
                     L.t("Overwrite confirm", new Object[0]),
                     L.t("Indicator '%s' already exists, do you want to overwrite it with the imported one?", new Object[]{var9.name})
                  );
               String var10 = DataManagerConfirm.get().getConfirmedAction();
               if (!var10.equals("skip")) {
                  if (var10.equals("cancel")) {
                     break;
                  }

                  if (var10.equals("overwrite")) {
                     CustomDataManager.clear(var9.name);
                     CustomDataManager.update(var9);
                     var5.add(var9.name);
                  }
               }
            }
         }

         this.sendDataUpdate(String.join(",", var5), "add");
         var4.put("success", L.t("Indicators (%d) loaded.", new Object[]{var5.size()}));
         return var4.toString();
      } catch (Exception var11) {
         return apiErrorJSON(L.t("Cannot load indicators.", new Object[0]), var11);
      }
   }

   private String onSave(Map<String, String[]> var1) {
      try {
         String var2 = this.tryGetParam(var1, "filePath")[0];
         File var3 = new File(var2);
         JSONObject var4 = new JSONObject();
         Element var5 = new Element("ExternalIndicators");
         String[] var6 = this.tryGetParam(var1, "data")[0].split(",");

         for (String var10 : var6) {
            CustomDataInfo var11 = CustomDataManager.getDataInfo(var10);
            var5.addContent(var11.getXML());
         }

         XMLUtil.xmlToFile(var5, var3);
         var4.put("success", L.t("Indicators saved.", new Object[0]));
         return var4.toString();
      } catch (Exception var12) {
         return apiErrorJSON(L.t("Cannot save indicators.", new Object[0]), var12);
      }
   }

   private String onImportDataCli(Map<String, String[]> var1) {
      JSONObject var2 = new JSONObject();

      try {
         String var3 = this.tryGetParam(var1, "name")[0];
         CustomDataInfo var4 = CustomDataManager.getDataInfo(var3);
         if (var4 == null) {
            throw new Exception(L.t("External indicator with name '%s' doesn't exist.", new Object[]{var3}));
         }

         ImportDataInfo var5 = new ImportDataInfo();
         var5.name = "";
         var5.filePath = this.tryGetParam(var1, "file")[0];
         var5.dateFormat = "dd/MM/yyyy";
         var5.timeFormat = "HH:mm:ss";
         var5.separator = ",";
         var5.skipRows = 0;
         var5.skipCols = 0;
         var5.errorHandling = 0;
         ArrayList var6 = new ArrayList();
         var6.add(new DateCol());
         var6.add(new TimeCol());
         var6.add(new UnusedCol());
         var6.add(new UnusedCol());
         var6.add(new UnusedCol());
         var6.add(new UnusedCol());
         var6.add(new UnusedCol());

         for (int var7 = 0; var7 < var4.values; var7++) {
            var6.add(new CustomValue(var7));
         }

         var5.columnTypes = var6;
         MultiProgressListener var11 = DataManagerCustomDataProgress.get().createListener(var3, "customdata/importAction");
         String var8 = "CustomDataImport_" + var3;
         CustomDataImporterJob var9 = new CustomDataImporterJob(var8, var11, CustomDataManager.getDataInfo(var3), var5);
         SQGrid.getGridClient().executeOnGrid(var8, var9);
      } catch (Exception var10) {
         return apiErrorJSON(L.t("Cannot import data.", new Object[0]), var10);
      }

      var2.put("success", L.t("Importing external indicator data.", new Object[0]));
      return var2.toString();
   }
}
