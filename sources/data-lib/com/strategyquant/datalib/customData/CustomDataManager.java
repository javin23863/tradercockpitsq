package com.strategyquant.datalib.customData;

import com.strategyquant.datalib.customData.ct.CTCustomIndicators;
import com.strategyquant.datalib.indicators.SCustomIndicator;
import com.strategyquant.datalib.indicators.SParameter;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.XMLUtil;
import com.strategyquant.lib.constants.SQPaths;
import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import org.jdom2.Element;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CustomDataManager extends CustomDataDb {
   public static final Logger Log = LoggerFactory.getLogger(CustomDataManager.class);
   private static CustomDataManager instance = null;
   private HashMap<String, CustomDataInfo> dataInfoCache = new HashMap<>();

   public static void init(String var0) throws Exception {
      if (instance == null) {
         instance = new CustomDataManager(var0);
         instance._list();
         if (!configFileExists()) {
            instance._updateConfigFileLock();
         }
      }
   }

   private CustomDataManager(String var1) {
      super(var1);
   }

   static CustomDataManager get() {
      return instance;
   }

   @Override
   public void initDatabase() {
      try {
         Class.forName("org.sqlite.JDBC");
         if (!this.tableExists("DATA")) {
            String var1 = "CREATE TABLE DATA (ID INTEGER PRIMARY KEY AUTOINCREMENT, NAME\t\t\tVARCHAR(50)\tNOT NULL, VALUESOBJECT\tVARCHAR(1024), DATATYPE \t\tINT, TIMEFRAME \t\tVARCHAR(50), FILENAME \t\tVARCHAR(100), DATEFROM     \tLONG, DATETO \t\tLONG, ROWS \t\t\tINT DEFAULT 0)";
            this.sqlCommand(var1);
            Log.info("Custom Data table created successfully");
         }
      } catch (Exception var2) {
         Log.error("DB error: Cannot create Custom Data table.", var2);
      }
   }

   public static Collection<CustomDataInfo> list() throws Exception {
      return get()._list();
   }

   public static void add(CustomDataInfo var0) throws Exception {
      add(var0.name, var0.valuesObject.toString(), var0.dataType);
   }

   public static void add(String var0, String var1, int var2) throws Exception {
      get()._add(var0, var1, var2);
   }

   public static void update(CustomDataInfo var0) throws Exception {
      update(var0.name, var0.name, var0.valuesObject.toString(), var0.dataType);
   }

   public static void update(String var0, String var1, String var2, int var3) throws Exception {
      get()._update(var0, var1, var2, var3);
   }

   public static void updateData(String var0, long var1, long var3, int var5, String var6) throws Exception {
      get()._updateData(var0, var1, var3, var5, var6);
   }

   public static boolean checkDataExists(String var0) {
      return get()._checkDataExists(var0);
   }

   public static CustomDataInfo getDataInfo(String var0) {
      return get()._getDataInfo(var0);
   }

   public static void delete(String var0) {
      get()._delete(var0);
   }

   private void _delete(String var1) {
      try {
         String var2 = "DELETE FROM DATA WHERE NAME='" + var1 + "'";
         this.sqlCommand(var2);
         removeDataFile(var1);
         synchronized (this.dataInfoCache) {
            this.dataInfoCache.remove(var1);
            this._updateConfigFile();
         }
      } catch (Exception var6) {
         Log.error("Custom Data DB error:", var6);
      }
   }

   public static void clear(String var0) throws Exception {
      removeDataFile(var0);
      updateData(var0, 0L, 0L, 0, null);
   }

   public static void removeDataFile(String var0) {
      String var1 = getDataFileName(var0);
      File var2 = new File(var1);
      if (var2.exists() && !var2.delete()) {
         Log.error("Unable to delete file " + var1 + ".");
      }
   }

   private synchronized Collection<CustomDataInfo> _list() throws Exception {
      if (!this.dataInfoCache.isEmpty()) {
         return this.dataInfoCache.values();
      }

      ArrayList var1 = new ArrayList();
      Connection var2 = null;
      Statement var3 = null;
      ResultSet var4 = null;
      var2 = this.getConnection();
      var3 = var2.createStatement();
      var4 = var3.executeQuery("SELECT * FROM DATA");

      while (var4.next()) {
         try {
            CustomDataInfo var5 = new CustomDataInfo();
            var5.id = var4.getInt("ID");
            var5.name = var4.getString("NAME");
            var5.setValues(var4.getString("VALUESOBJECT"));
            var5.dataType = var4.getInt("DATATYPE");
            var5.timeframe = var4.getString("TIMEFRAME");
            var5.filename = var4.getString("FILENAME");
            var5.dateFrom = var4.getLong("DATEFROM");
            var5.dateTo = var4.getLong("DATETO");
            var5.rows = var4.getInt("ROWS");
            var1.add(var5);
            this.dataInfoCache.put(var5.name, var5);
         } catch (Exception var6) {
            Log.error("Error while loading External indicator. Exc.", var6);
         }
      }

      this.close(var4);
      this.close(var3);
      this.close(var2);
      return var1;
   }

   public void _add(String var1, String var2, int var3) throws Exception {
      Connection var4 = null;

      try {
         var4 = this.getConnection();
         if (this._checkDataExists(var4, var1)) {
            throw new Exception(String.format("Indicator with name '%s' already exists.", var1));
         }

         try {
            new JSONArray(var2);
         } catch (Exception var16) {
            throw new Exception("Invalid values object.", var16);
         }

         PreparedStatement var5 = var4.prepareStatement("INSERT INTO DATA (NAME, VALUESOBJECT, DATATYPE) VALUES (?,?,?)", 1);
         var5.setString(1, var1);
         var5.setString(2, var2);
         var5.setInt(3, var3);
         int var6 = this.sqlInsertReturnAutoId(var4, var5);
         CustomDataInfo var7 = new CustomDataInfo();
         var7.id = var6;
         var7.name = var1;
         var7.setValues(var2);
         var7.dataType = var3;
         synchronized (this.dataInfoCache) {
            this.dataInfoCache.put(var1, var7);
            this._updateConfigFile();
         }
      } catch (Exception var17) {
         Log.error("DB Exception", var17);
         throw var17;
      } finally {
         this.close(var4);
      }
   }

   public static void updateConfigFile() {
      get()._updateConfigFileLock();
   }

   private void _updateConfigFileLock() {
      synchronized (this.dataInfoCache) {
         this._updateConfigFile();
      }
   }

   private void _updateConfigFile() {
      try {
         Element var1 = new Element("CustomDataIndys");

         for (CustomDataInfo var3 : this.dataInfoCache.values()) {
            if (var3.values > 0) {
               Element var4 = new Element("Item");
               var4.setAttribute("key", "CDataIndy_" + var3.name);

               try {
                  var4.setAttribute("returnType", CustomDataTypes.translateDataType((byte)var3.dataType));
                  if (var3.dataType == 10) {
                     var4.setAttribute("categoryType", "simpleRules");
                  } else {
                     var4.setAttribute("categoryType", "indicator");
                  }
               } catch (Exception var7) {
                  Log.error("Error setting custom indocator return type! DataType=" + var3.dataType, var7);
               }

               Element var5 = new Element("Param");
               var5.setAttribute("key", "#Shift#");
               var5.setAttribute("controlType", "jspinnerVar");
               var5.setAttribute("type", "int");
               var5.setText("0");
               var4.addContent(var5);
               var5 = new Element("Param");
               var5.setAttribute("key", "#Value#");
               var5.setAttribute("controlType", "combo");
               var5.setAttribute("type", "int");
               String var6 = createCDataValues(var3.values, var3);
               var5.setAttribute("values", var6);
               var5.setText("0");
               var4.addContent(var5);
               var1.addContent(var4);
            }
         }

         String var9 = XMLUtil.elementToString(var1);
         SQUtils.stringToFile(SQPaths.algoWizardCustomDataIndysPath, var9);
      } catch (Exception var8) {
         Log.error("Cannot save custom data indicators config!");
      }
   }

   public static boolean configFileExists() {
      File var0 = new File(SQPaths.algoWizardCustomDataIndysPath);
      return var0.exists();
   }

   public static String createCDataValues(int var0, CustomDataInfo var1) throws Exception {
      try {
         StringBuilder var2 = new StringBuilder();

         for (int var3 = 0; var3 < var0; var3++) {
            if (var3 > 0) {
               var2.append(",");
            }

            var2.append(var1.getValue(var3, "name"));
            var2.append("=");
            var2.append(var3);
         }

         return var2.toString();
      } catch (Exception var4) {
         throw new Exception("Cannot init Custom Data Indicators", var4);
      }
   }

   public void _update(String var1, String var2, String var3, int var4) throws Exception {
      Connection var5 = null;

      try {
         CustomDataInfo var6 = this.getRecordFromCache(var1);

         try {
            new JSONArray(var3);
         } catch (Exception var16) {
            throw new Exception("Invalid values object.", var16);
         }

         var5 = this.getConnection();
         PreparedStatement var7 = var5.prepareStatement("UPDATE DATA SET NAME = ?, VALUESOBJECT = ?, DATATYPE = ? WHERE NAME = ?");
         var7.setString(1, var2);
         var7.setString(2, var3);
         var7.setInt(3, var4);
         var7.setString(4, var1);
         this.sqlCommand(var5, var7);
         var6.name = var2;
         var6.setValues(var3);
         var6.dataType = var4;
         synchronized (this.dataInfoCache) {
            this.dataInfoCache.remove(var1);
            this.dataInfoCache.put(var2, var6);
            this._updateConfigFile();
         }
      } catch (Exception var17) {
         Log.error("Custom Data DB error:", var17);
         throw var17;
      } finally {
         this.close(var5);
      }
   }

   public void _updateData(String var1, long var2, long var4, int var6, String var7) throws Exception {
      try {
         CustomDataInfo var8 = this.getRecordFromCache(var1);
         String var9 = "UPDATE DATA SET DATEFROM='" + var2 + "',DATETO='" + var4 + "',ROWS=" + var6 + ",TIMEFRAME='" + var7 + "' WHERE NAME ='" + var1 + "'";
         if (var7 == null) {
            var9 = "UPDATE DATA SET DATEFROM='" + var2 + "',DATETO='" + var4 + "',ROWS=" + var6 + ",TIMEFRAME=NULL WHERE NAME ='" + var1 + "'";
         }

         this.sqlCommand(var9);
         var8.dateFrom = var2;
         var8.dateTo = var4;
         var8.rows = var6;
         var8.timeframe = var7;
      } catch (Exception var10) {
         Log.error("Custom Data DB error:", var10);
         throw var10;
      }
   }

   private boolean _checkDataExists(String var1) {
      synchronized (this.dataInfoCache) {
         String var3 = var1;
         if (this.dataInfoCache.containsKey(var3)) {
            return true;
         }

         Connection var4 = null;

         boolean var6;
         try {
            var4 = this.getConnection();
            return this._checkDataExists(var4, var1);
         } catch (Exception var12) {
            Log.error("DB Exception", var12);
            var6 = false;
         } finally {
            this.close(var4);
         }

         return var6;
      }
   }

   private boolean _checkDataExists(Connection var1, String var2) {
      String var3 = "SELECT * FROM DATA WHERE NAME='" + var2 + "'";
      return this.sqlCheckRecordExists(var1, var3);
   }

   private synchronized CustomDataInfo _getDataInfo(String var1) {
      synchronized (this.dataInfoCache) {
         if (this.dataInfoCache.containsKey(var1)) {
            return this.dataInfoCache.get(var1);
         }

         Connection var3 = null;
         Statement var4 = null;
         ResultSet var5 = null;

         CustomDataInfo var6;
         try {
            var3 = this.getConnection();
            if (this._checkDataExists(var3, var1)) {
               var6 = new CustomDataInfo();
               String var18 = "SELECT * FROM DATA WHERE NAME='" + var1 + "'";
               var4 = var3.createStatement();
               var5 = var4.executeQuery(var18);
               if (!var5.next()) {
                  return null;
               }

               var6.id = var5.getInt("ID");
               var6.name = var5.getString("NAME");
               var6.setValues(var5.getString("VALUESOBJECT"));
               var6.dataType = var5.getInt("DATATYPE");
               var6.timeframe = var5.getString("TIMEFRAME");
               var6.filename = var5.getString("FILENAME");
               var6.dateFrom = var5.getLong("DATEFROM");
               var6.dateTo = var5.getLong("DATETO");
               var6.rows = var5.getInt("ROWS");
               this.dataInfoCache.put(var1, var6);
               return var6;
            }

            var6 = null;
         } catch (Exception var14) {
            Log.error("DB Exception", var14);
            return null;
         } finally {
            this.close(var5);
            this.close(var4);
            this.close(var3);
         }

         return var6;
      }
   }

   private CustomDataInfo getRecordFromCache(String var1) {
      return this.dataInfoCache.get(var1);
   }

   public static String getDataFileName(String var0) {
      return get()._getDataFileName(var0);
   }

   private String _getDataFileName(String var1) {
      String var2 = var1 + ".dat";
      return this._getDirectory() + "/" + fixFilename(var2);
   }

   public static String fixFilename(String var0) {
      return var0.replaceAll("[^a-zA-Z0-9.-@#()]", "_");
   }

   public static void addCDataIndySourceCodes(Element var0) throws Exception {
      CTCustomIndicators var1 = null;
      Element var2 = var0.getChild("Strategy").getChild("CustomIndicators");
      if (var2 != null && !var2.getChildren().isEmpty()) {
         var1 = new CTCustomIndicators(var2);
      }

      get()._addCDataIndySourceCodes(var0, var1);
   }

   private void _addCDataIndySourceCodes(Element var1, CTCustomIndicators var2) throws Exception {
      if (var1.getName().equals("Item")) {
         String var3 = var1.getAttributeValue("key");
         if (var3 != null && var3.startsWith("CDataIndy")) {
            String var4 = getCDataIndyId(var3);
            if (var4 != null) {
               if (var2 == null) {
                  this._addSourceCodesToItem(var1, var4);
               } else {
                  var2.addSourceCodesToItem(var1, var4);
               }
            }
         }
      }

      List var6 = var1.getChildren();
      if (var6 != null && var6.size() > 0) {
         for (int var7 = 0; var7 < var6.size(); var7++) {
            Element var5 = (Element)var6.get(var7);
            if (!var5.getName().equals("CustomIndicators")) {
               this._addCDataIndySourceCodes(var5, var2);
            }
         }
      }
   }

   public static String getCDataIndyId(String var0) {
      return var0.length() <= 10 ? null : var0.substring(10);
   }

   private void _addSourceCodesToItem(Element var1, String var2) {
      var1.setAttribute("cdataIndyName", var2);
      CustomDataInfo var3 = getDataInfo(var2);
      if (var3 == null) {
         Log.debug("Cannot load data info for CustomIndicator {}", var2);
         var1.setAttribute("mt4", "");
         var1.setAttribute("mt5", "");
         var1.setAttribute("el", "");
         var1.setAttribute("jf", "");
      } else {
         int var4 = 0;
         Element var6 = XMLUtil.findFirstWithKey(var1, "Param", "#Value#");
         if (var6 != null) {
            String var7 = var6.getText();
            if (var7 != null && !var7.equals("")) {
               var4 = Integer.parseInt(var7);
            }
         }

         try {
            String var5 = var3.getValue(var4, "mt4");
            if (var5 == null) {
               var5 = "";
            }

            var1.setAttribute("mt4", var5);
            var5 = var3.getValue(var4, "mt5");
            if (var5 == null) {
               var5 = "";
            }

            var1.setAttribute("mt5", var5);
            var5 = var3.getValue(var4, "el");
            if (var5 == null) {
               var5 = "";
            }

            var1.setAttribute("el", var5);
         } catch (Exception var8) {
            Log.error("Cannot load Src value", var8);
         }
      }
   }

   public static void add(SCustomIndicator var0) throws Exception {
      String var1 = var0.shortName;
      JSONArray var2 = new JSONArray();

      for (int var3 = 0; var3 < 3; var3++) {
         String var4 = null;
         String var5 = null;
         String var6 = null;
         String var7 = null;

         try {
            var4 = var0.outputList.get(var3);
            String var8 = "";

            for (int var9 = 0; var9 < var0.parameterList.size(); var9++) {
               SParameter var10 = var0.parameterList.get(var9);
               if (!var10.type.equals("string") && !var10.type.equals("color")) {
                  var8 = var8 + var10.value;
               } else {
                  var8 = var8 + "\\\"" + var10.value + "\\\"";
               }

               if (var9 < var0.parameterList.size() - 1) {
                  var8 = var8 + ",";
               }
            }

            var6 = String.format("iCustom(NULL, 0, \"%s\",%s %d, #Shift#)", var1, var8.isBlank() ? "" : var8 + ",", var3);
            var7 = String.format("iCustom(NULL, 0, \"%s\",%s)", var1, var8, var3);
            var5 = String.format("%s.%s[#Shift#]", var1, var4.equals(var1) ? "Main" : var4);
         } catch (Exception var11) {
         }

         JSONObject var12 = new JSONObject();
         var12.put("name", var4 == null ? JSONObject.NULL : var4);
         var12.put("el", var5 == null ? JSONObject.NULL : var5);
         var12.put("mt4", var6 == null ? JSONObject.NULL : var6);
         var12.put("mt5", var7 == null ? JSONObject.NULL : var7);
         var2.put(var12);
      }

      add(var1, var2.toString(), var0.returnType);
   }

   public static void checkData() {
      try {
         for (CustomDataInfo var2 : get()._list()) {
            if (var2.rows > 0) {
               File var3 = new File(getDataFileName(var2.name));
               if (!var3.exists() || var3.length() == 0L) {
                  clear(var2.name);
               }
            }
         }
      } catch (Exception var4) {
         Log.error("Error while checking Custom data.", var4);
      }
   }
}
