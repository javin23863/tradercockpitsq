package com.strategyquant.plugin.DataSource.impl.TD;

import com.strategyquant.datalib.DataInfo;
import com.strategyquant.datalib.SymbolData;
import com.strategyquant.datalib.data.DataManager;
import com.strategyquant.datalib.data.DukasDataManager;
import com.strategyquant.lib.L;
import com.strategyquant.tradinglib.dukascopy.ImportInfo;
import com.strategyquant.tradinglib.project.websocket.DataManagerDataProgress;
import com.strategyquant.tradinglib.project.websocket.DataManagerProgressListener;
import com.strategyquant.tradinglib.project.websocket.SQWebSocketManager;
import com.strategyquant.tradinglib.project.websocket.WSDataObjects;
import com.strategyquant.webguilib.servlet.HttpJSONServlet;
import java.io.File;
import java.util.Map;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DataSourceTDServlet extends HttpJSONServlet {
   DateTimeFormatter formaterDate = DateTimeFormat.forPattern("yyyyMMdd");
   private static final Logger Log = LoggerFactory.getLogger(DataSourceTDServlet.class);

   protected String execute(String var1, Map<String, String[]> var2, String var3) throws Exception {
      switch (var1) {
         case "importData":
            return this.onImportData(var2);
         case "importAction":
            return this.onImportAction(var2);
         case "loadAvailableSymbols":
            return this.onLoadAvailableSymbols(var2);
         default:
            return apiErrorJSON(L.t("Execution failed. Incorrect url arguments '%s'.", new Object[]{var1}), null);
      }
   }

   private String onLoadAvailableSymbols(Map<String, String[]> var1) throws Exception {
      String var2 = this.tryGetParam(var1, "path")[0];
      File var3 = new File(var2 + "//tickdata");
      if (var3.exists()) {
         var2 = var3.getAbsolutePath();
      }

      JSONObject var4 = new JSONObject();
      JSONArray var5 = new JSONArray();

      try {
         File var6 = new File(var2);
         File[] var7 = var6.listFiles();
         if (var7 != null) {
            for (int var8 = 0; var8 < var7.length; var8++) {
               var3 = var7[var8];
               if (var3.isDirectory()) {
                  var5.put(var3.getName());
               }
            }
         }
      } catch (Exception var9) {
         return apiErrorJSON(L.t("Cannot load available symbols.", new Object[0]), var9);
      }

      var4.put("symbols", var5);
      var4.put("success", L.t("Symbols loaded.", new Object[0]));
      return var4.toString();
   }

   private String onImportData(Map<String, String[]> var1) throws Exception {
      JSONObject var2 = new JSONObject();

      try {
         String var3 = this.tryGetParam(var1, "symbols")[0];
         String var4 = var1.containsKey("postfix") ? this.tryGetParam(var1, "postfix")[0] : "";
         String var5 = this.tryGetParam(var1, "path")[0];
         File var6 = new File(var5 + "//tickdata");
         if (var6.exists()) {
            var5 = var6.getAbsolutePath();
         }

         String[] var7 = var3.split(",");

         for (int var8 = 0; var8 < var7.length; var8++) {
            String var9 = var7[var8];
            DataInfo var10 = DataManager.getDataInfo("History", var9 + var4);
            if (var10 != null) {
               throw new Exception(L.t("Symbol with name '%s' already exists. Please enter a postfix to generate unique symbol names.", new Object[]{var9}));
            }
         }

         for (int var15 = 0; var15 < var7.length; var15++) {
            String var16 = var7[var15];
            DukasDataManager.get().addData(var16, var4, -1, null);
            ImportInfo var17 = new ImportInfo();
            var17.origSymbol = var16;
            var17.symbol = var16 + var4;
            var17.overwrite = true;
            DataInfo var11 = DataManager.getDataInfo("History", var17.symbol);
            var17.instrumentInfo = var11.symbolInfo;
            var17.rows = var11.rows;
            var17.oldDateFrom = var11.dateFrom;
            var17.oldDateTo = var11.dateTo;
            SymbolData var12 = DukasDataManager.get().getAvailableDataInfo(var17.origSymbol);
            var17.priceConstant = Math.pow(10.0, -var12.decimals);
            DataManagerProgressListener var13 = (DataManagerProgressListener)DataManagerDataProgress.get()
               .createListener(var16 + var4, "dataSourceTD/importAction");
            TDDataManager.get().importTDData(var17, var5, var13);
         }

         SQWebSocketManager.addToDataQueue(WSDataObjects.getInstruments(), new String[]{"SQUANT", "QDM", "AlgoWizard"});
         SQWebSocketManager.addToDataQueue(WSDataObjects.getData(var3, "add"), new String[]{"SQUANT", "QDM", "AlgoWizard"});
      } catch (Exception var14) {
         return apiErrorJSON(L.t("Cannot import data.", new Object[0]), var14);
      }

      var2.put("success", L.t("Data import started", new Object[0]));
      return var2.toString();
   }

   private String onImportAction(Map<String, String[]> var1) throws Exception {
      this.checkParamExists(var1, new String[]{"symbol", "action"});
      String var2 = this.tryGetParam(var1, "symbol")[0];
      String var3 = this.tryGetParam(var1, "action")[0];
      JSONObject var4 = new JSONObject();
      switch (var3) {
         case "stop":
            TDDataManager.get().stopTDImport(var2);
            break;
         case "pause":
            TDDataManager.get().pauseTDImport(var2);
            break;
         case "continue":
            TDDataManager.get().restartTDImport(var2);
      }

      var4.put("success", "ok");
      return var4.toString();
   }
}
