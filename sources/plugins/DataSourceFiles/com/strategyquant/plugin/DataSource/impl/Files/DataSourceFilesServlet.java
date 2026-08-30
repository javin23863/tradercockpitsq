package com.strategyquant.plugin.DataSource.impl.Files;

import com.strategyquant.datalib.DataInfo;
import com.strategyquant.datalib.InstrumentInfo;
import com.strategyquant.datalib.data.DataManager;
import com.strategyquant.datalib.data.imports.AvailableDataFormats;
import com.strategyquant.datalib.data.imports.CsvFileReader;
import com.strategyquant.datalib.data.imports.CustomDataFormat;
import com.strategyquant.datalib.data.imports.DataColumns;
import com.strategyquant.datalib.data.imports.DataImportEngine;
import com.strategyquant.datalib.data.io.ImportDataInfo;
import com.strategyquant.datalib.data.io.MassImportDataInfo;
import com.strategyquant.datalib.data.io.MassImportDataInfo.OverwriteStrategy;
import com.strategyquant.datalib.data.io.columns.DefaultCol;
import com.strategyquant.datalib.instrument.InstrumentManager;
import com.strategyquant.gridlib.client.SQGrid;
import com.strategyquant.lib.L;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.app.MainApp;
import com.strategyquant.lib.time.DateFormats;
import com.strategyquant.plugin.DataSource.impl.Files.job.DataAppImporterJob;
import com.strategyquant.plugin.DataSource.impl.Files.job.DataImporterJob;
import com.strategyquant.plugin.DataSource.impl.Files.job.DataMassImporterMasterJob;
import com.strategyquant.qdm.QDM;
import com.strategyquant.tradinglib.project.websocket.DataManagerDataProgress;
import com.strategyquant.tradinglib.project.websocket.MultiProgressListener;
import com.strategyquant.tradinglib.project.websocket.SQWebSocketManager;
import com.strategyquant.tradinglib.project.websocket.WSDataObjects;
import com.strategyquant.webguilib.servlet.HttpJSONServlet;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DataSourceFilesServlet extends HttpJSONServlet {
   private static final Logger Log = LoggerFactory.getLogger(DataSourceFilesServlet.class);
   private boolean canceled = false;

   protected String execute(String var1, Map<String, String[]> var2, String var3) throws Exception {
      switch (var1) {
         case "add":
            return this.onAdd(var2);
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
         case "appImport":
            return this.onAppImport(var2);
         case "cancelAppImport":
            return this.onCancelAppImportAction(var2);
         case "massImport":
            return this.onMassImport(var2);
         case "cancelMassImport":
            return this.onCancelMassImportAction(var2);
         default:
            throw new Exception("Unknown command '" + var1 + "'.");
      }
   }

   private String onAppImport(Map<String, String[]> var1) throws Exception {
      JSONObject var2 = new JSONObject();
      String var3 = null;
      String var4 = null;

      try {
         var3 = this.tryGetParam(var1, "license")[0];
         var4 = this.tryGetParam(var1, "path")[0];
      } catch (Exception var8) {
         return apiErrorJSON(L.t("Cannot import data.", new Object[0]), var8);
      }

      String var5 = "DataAppImportJob";
      DataAppImporterJob var6 = new DataAppImporterJob(var5, var4, var3);
      String var7 = var6.validate();
      if (var7 != null) {
         return apiErrorJSON(L.t(var7, new Object[0]), null);
      }

      SQGrid.getGridClient().executeOnGrid(var5, var6);
      var2.put("success", L.t("Importing data.", new Object[0]));
      return var2.toString();
   }

   private String onCancelAppImportAction(Map<String, String[]> var1) throws Exception {
      JSONObject var2 = new JSONObject();
      String var3 = "DataAppImportJob";
      SQGrid.getGridClient().stop(var3);
      var2.put("success", "ok");
      return var2.toString();
   }

   private String onMassImport(Map<String, String[]> var1) throws Exception {
      JSONObject var2 = new JSONObject();
      this.canceled = false;
      MassImportDataInfo var3 = new MassImportDataInfo();
      var3.setDateFormat(this.tryGetParam(var1, "dateFormat")[0]);
      var3.setPath(this.tryGetParam(var1, "path")[0]);
      var3.setConnection(this.tryGetParam(var1, "connection")[0]);
      var3.setBarType(Integer.parseInt(this.tryGetParam(var1, "barType")[0]));
      var3.setOverwriteStrategy(OverwriteStrategy.valueOf(this.tryGetParam(var1, "exists")[0]));
      var3.setInstrument(this.tryGetParam(var1, "instrument")[0]);
      var3.setPostfix(this.tryGetParam(var1, "postfix")[0]);
      var3.setCreateStockGroup(Boolean.valueOf(this.tryGetParam(var1, "createStockGroup")[0]));
      String var4 = this.tryGetParam(var1, "timezone")[0];
      var3.setTimezone(var4);
      MainApp.settings().set("timezone", var4);
      String var5 = this.tryGetParam(var1, "timeframe")[0];
      var3.setTimeframe(var5);
      String var6 = "DataMassImportJob";
      DataMassImporterMasterJob var7 = new DataMassImporterMasterJob(var6, var3);
      if (!var7.hasFilesToImport()) {
         return apiErrorJSON(L.t("There is nothing to import.", new Object[0]), null);
      }

      SQGrid.getGridClient().executeOnGrid(var6, var7);
      var2.put("success", L.t("Import symbols.", new Object[0]));
      return var2.toString();
   }

   private String onCancelMassImportAction(Map<String, String[]> var1) throws Exception {
      JSONObject var2 = new JSONObject();
      String var3 = "DataMassImportJob";
      SQGrid.getGridClient().stop(var3);
      var2.put("success", "ok");
      return var2.toString();
   }

   private String onAdd(Map<String, String[]> var1) {
      JSONObject var2 = new JSONObject();

      try {
         String var3 = this.tryGetParam(var1, "connection")[0];
         String var4 = this.tryGetParam(var1, "symbol")[0];
         String var5 = this.tryGetParam(var1, "instrument")[0];
         int var6 = Integer.parseInt(this.tryGetParam(var1, "barType")[0]);
         DataInfo var7 = DataManager.getDataInfo("History", var4);
         if (var7 != null) {
            throw new Exception(L.t("Symbol with name '%s' already exists.", new Object[]{var4}));
         }

         int var8 = -1;
         if (InstrumentManager.checkInstrumentExists(var5)) {
            InstrumentInfo var9 = InstrumentManager.getInstrumentInfo(var5);
            var8 = var9.broker;
         }

         DataManager.addData(var3, var4, var5, var6, 1, -1, var8);
         this.sendDataUpdate(var4, "add");
      } catch (Exception var10) {
         return apiErrorJSON(L.t("Cannot add data.", new Object[0]), var10);
      }

      var2.put("success", L.t("Data added.", new Object[0]));
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

      for (CustomDataFormat var6 : AvailableDataFormats.getInstance().getAvailableFileFormats()) {
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
      var1.put("success", L.t("Timezones listed.", new Object[0]));
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
                  var11 = AvailableDataFormats.getInstance().findFileFormatByName(var10);
               }

               if (var11 == null) {
                  ArrayList var13 = AvailableDataFormats.getInstance().getAvailableFileFormats();
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
         String var3 = this.tryGetParam(var1, "connectionName")[0];
         String var4 = this.tryGetParam(var1, "symbol")[0];
         String var5 = this.tryGetParam(var1, "timezone")[0];
         MainApp.settings().set("timezone", var5);
         ImportDataInfo var6 = new ImportDataInfo();
         var6.name = this.tryGetParam(var1, "fileFormat")[0];
         var6.filePath = this.tryGetParam(var1, "filePath")[0];
         var6.dateFormat = this.tryGetParam(var1, "dateFormat")[0];
         var6.separator = this.tryGetParam(var1, "separator")[0];
         var6.skipRows = Integer.parseInt(this.tryGetParam(var1, "skipRows")[0]);
         var6.skipCols = Integer.parseInt(this.tryGetParam(var1, "skipColumns")[0]);
         var6.errorHandling = Integer.parseInt(this.tryGetParam(var1, "errorHandling")[0]);
         var6.timezone = var5;
         String var7 = this.tryGetParam(var1, "timeframe")[0];
         var6.timeframe = var7.equals("auto") ? null : var7;
         CustomDataFormat var8 = AvailableDataFormats.getInstance().findFileFormatByName(var6.name);
         if (var8 != null) {
            var6.timeFormat = var8.getTimeFormat();
         }

         ArrayList var9 = new ArrayList();
         String[] var10 = this.tryGetParam(var1, "columnTypes")[0].split(",");

         for (String var14 : var10) {
            var9.add(DataColumns.getInstance().findColTypeByName(var14));
         }

         var6.columnTypes = var9;
         QDM.getInstance().activity.increase(5);
         MultiProgressListener var16 = DataManagerDataProgress.get().createListener(var4, "dataSourceFiles/importAction");
         String var17 = "DataImportFromFile_" + var4;
         DataImporterJob var18 = new DataImporterJob(var4, var17, var16, DataManager.getDataInfo(var3, var4), var6);
         SQGrid.getGridClient().executeOnGrid(var17, var18);
         if (this.getParam(var1, "waitForFinish", "false").equals("true")) {
            SQGrid.getGridClient().waitForFinish(this, null, null, var17);
         }
      } catch (Exception var15) {
         return apiErrorJSON(L.t("Cannot import data.", new Object[0]), var15);
      }

      var2.put("success", L.t("Importing data.", new Object[0]));
      return var2.toString();
   }

   private String onImportAction(Map<String, String[]> var1) throws Exception {
      this.checkParamExists(var1, new String[]{"symbol", "action"});
      String var2 = this.tryGetParam(var1, "symbol")[0];
      String var3 = this.tryGetParam(var1, "action")[0];
      JSONObject var4 = new JSONObject();
      String var5 = "DataImportFromFile_" + var2;
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
         if (AvailableDataFormats.getInstance().formatExists(var3)) {
            throw new Exception(L.t("Data format with name '%s' already exists. Please choose another name.", new Object[]{var3}));
         }

         AvailableDataFormats.getInstance().addDataFormat(var4);
      } catch (Exception var5) {
         return apiErrorJSON(L.t("Cannot save custom data format.", new Object[0]), var5);
      }

      var2.put("success", L.t("Data format saved", new Object[0]));
      return var2.toString();
   }

   private String onImportDeleteDataFormat(Map<String, String[]> var1) {
      JSONObject var2 = new JSONObject();

      try {
         String var3 = this.tryGetParam(var1, "name")[0];
         AvailableDataFormats.getInstance().deleteDataFormat(var3);
      } catch (Exception var4) {
         return apiErrorJSON(L.t("Cannot delete custom data format.", new Object[0]), var4);
      }

      var2.put("success", L.t("Data format deleted", new Object[0]));
      return var2.toString();
   }

   private String onImportUpdateDataFormat(Map<String, String[]> var1) {
      JSONObject var2 = new JSONObject();

      try {
         AvailableDataFormats.getInstance().updateDataFormat(this.getFileFormat(var1));
      } catch (Exception var4) {
         return apiErrorJSON(L.t("Cannot update custom data format.", new Object[0]), var4);
      }

      var2.put("success", L.t("Data format updated", new Object[0]));
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
         SQWebSocketManager.addToDataQueue(WSDataObjects.getData(var1, var2), new String[]{"SQUANT", "QDM", "AlgoWizard"});
      } catch (Exception var4) {
         Log.error("Cannot send websocket data update. ", var4);
      }
   }
}
