package com.strategyquant.datalib.instrument;

import com.strategyquant.datalib.InstrumentInfo;
import com.strategyquant.datalib.data.DataDb;
import com.strategyquant.datalib.data.DataException;
import com.strategyquant.datalib.data.DataManager;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.constants.SQPaths;
import com.strategyquant.lib.utils.StringComparator;
import java.io.File;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class InstrumentManager extends DataDb {
   private static InstrumentManager instance = null;
   private static final String exchangesFilePath = SQPaths.settingsDirPath + "/exchanges.txt";
   private static final String countriesFilePath = SQPaths.settingsDirPath + "/countries.txt";
   private static final String sectorsFilePath = SQPaths.settingsDirPath + "/sectors.txt";
   private static final String delimiter = "\r\n";
   public static final String defaultCommissionsMethod = "<Method type=\"None\" use=\"true\"><Params/></Method>";
   private ArrayList<String> exchanges = new ArrayList<>();
   private ArrayList<String> countries = new ArrayList<>();
   private ArrayList<String> sectors = new ArrayList<>();
   private InstrumentManager.InstrumentCache instrumentCache = new InstrumentManager.InstrumentCache();

   public static void init(String var0) throws Exception {
      if (instance != null) {
         throw new Exception("InstrumentManager.init() called more than once!");
      }

      new InstrumentManager(var0);
   }

   private InstrumentManager(String var1) {
      super(var1);
      instance = this;
      File var2 = new File(exchangesFilePath);
      File var3 = new File(countriesFilePath);
      File var4 = new File(sectorsFilePath);
      if (var2.exists()) {
         try {
            SQUtils.fileToArrayList(this.exchanges, exchangesFilePath, "\r\n");
         } catch (Exception var8) {
            Log.error("Cannot load Exchanges file. ", var8);
         }
      } else {
         Log.warn("Exchanges file doesn't exist.");
      }

      if (var3.exists()) {
         try {
            SQUtils.fileToArrayList(this.countries, countriesFilePath, "\r\n");
         } catch (Exception var7) {
            Log.error("Cannot load Countries file. ", var7);
         }
      } else {
         Log.warn("Countries file doesn't exist.");
      }

      if (var4.exists()) {
         try {
            SQUtils.fileToArrayList(this.sectors, sectorsFilePath, "\r\n");
         } catch (Exception var6) {
            Log.error("Cannot load Sectors file. ", var6);
         }
      } else {
         Log.warn("Sectors file doesn't exist.");
      }
   }

   private static InstrumentManager get() {
      return instance;
   }

   @Override
   public void initDatabase() {
      try {
         Class.forName("org.sqlite.JDBC");
         if (!this.tableExists("INSTRUMENTS")) {
            String var1 = "CREATE TABLE INSTRUMENTS (INSTRUMENT \tVARCHAR(40) \tPRIMARY KEY NOT NULL, DESCRIPTION\tVARCHAR(255),  POINTVALUE \tDOUBLE \t\t\tNOT NULL, TICKSIZE \t\tDOUBLE \t\t\tNOT NULL, TICKSTEP \t\tDOUBLE \t\t\tNOT NULL, DEFAULTSPREAD \tDOUBLE \t\t\tDEFAULT 0, COMMISSIONS \tTEXT, DATATYPE\t\tINT\t\t\t\tNOT NULL, EXCHANGE\t\tVARCHAR(50), COUNTRY\t\tVARCHAR(50), SECTOR\t\t\tVARCHAR(100), SWAP\t\t\tTEXT, ORDERSIZEMULTIPLIER DOUBLE\t\tDEFAULT 1, ORDERSIZESTEP DOUBLE\t\tDEFAULT 0)";
            this.sqlCommand(var1);
            Log.info("Instruments table created successfully");
         }

         if (!this.columnExists("INSTRUMENTS", "DEFAULTSLIPPAGE")) {
            String var3 = "ALTER TABLE INSTRUMENTS ADD DEFAULTSLIPPAGE DOUBLE DEFAULT 0";
            this.sqlCommand(var3);
         }

         if (!this.columnExists("INSTRUMENTS", "SWAP")) {
            String var4 = "ALTER TABLE INSTRUMENTS ADD SWAP TEXT DEFAULT NULL";
            this.sqlCommand(var4);
         }

         if (!this.columnExists("INSTRUMENTS", "ORDERSIZEMULTIPLIER")) {
            String var5 = "ALTER TABLE INSTRUMENTS ADD ORDERSIZEMULTIPLIER DOUBLE DEFAULT 1";
            this.sqlCommand(var5);
         }

         if (!this.columnExists("INSTRUMENTS", "ORDERSIZESTEP")) {
            String var6 = "ALTER TABLE INSTRUMENTS ADD ORDERSIZESTEP DOUBLE DEFAULT 0";
            this.sqlCommand(var6);
         }

         if (!this.columnExists("INSTRUMENTS", "BROKER_ID")) {
            String var7 = "ALTER TABLE INSTRUMENTS ADD BROKER_ID INT DEFAULT -1";
            this.sqlCommand(var7);
         }

         if (!this.columnExists("INSTRUMENTS", "MIN_DISTANCE")) {
            String var8 = "ALTER TABLE INSTRUMENTS ADD MIN_DISTANCE DOUBLE DEFAULT 0.0";
            this.sqlCommand(var8);
         }
      } catch (Exception var2) {
         Log.error("DB error: Cannot create Instruments table.", var2);
      }
   }

   public static InstrumentInfo getInstrumentInfo(String var0) throws DataException {
      return get()._getInstrumentInfo(var0);
   }

   public static boolean checkInstrumentExists(String var0) {
      return get()._checkInstrumentExists(var0, false);
   }

   public static boolean checkInstrumentExists(String var0, boolean var1) {
      return get()._checkInstrumentExists(var0, var1);
   }

   public static boolean checkAliasExists(String var0) {
      return AliasManager.checkAliasExists(var0);
   }

   public static void addInstrumentInBatch(List<InstrumentInfo> var0) throws Exception {
      get()._addInstrumentInBatch(var0);
   }

   public static void addInstrument(String var0, String var1, double var2, double var4, double var6, double var8, double var10, String var12, byte var13) throws Exception {
      get()._addInstrument(var0, -1, var1, var2, var4, var6, var8, var10, 0.0, var12, var13, null, null, null, null, 1.0, 0.0);
   }

   public static void addInstrument(
      String var0,
      String var1,
      double var2,
      double var4,
      double var6,
      double var8,
      double var10,
      String var12,
      byte var13,
      String var14,
      String var15,
      String var16,
      String var17,
      double var18,
      double var20
   ) throws Exception {
      get()._addInstrument(var0, -1, var1, var2, var4, var6, var8, var10, 0.0, var12, var13, var14, var15, var16, var17, var18, var20);
   }

   public static void addInstrument(
      String var0,
      int var1,
      String var2,
      double var3,
      double var5,
      double var7,
      double var9,
      double var11,
      double var13,
      String var15,
      byte var16,
      String var17,
      String var18,
      String var19,
      String var20,
      double var21,
      double var23
   ) throws Exception {
      get()._addInstrument(var0, var1, var2, var3, var5, var7, var9, var11, var13, var15, var16, var17, var18, var19, var20, var21, var23);
   }

   public static void addInstrument(InstrumentInfo var0) throws Exception {
      get()
         ._addInstrument(
            var0.instrument,
            var0.broker,
            var0.description,
            var0.pointValue,
            var0.tickSize,
            var0.tickStep,
            var0.defaultSpread,
            var0.defaultSlippage,
            var0.minDistance,
            var0.commissions,
            (byte)var0.dataType,
            var0.exchange,
            var0.country,
            var0.sector,
            var0.swap,
            var0.orderSizeMultiplier,
            var0.orderSizeStep
         );
   }

   public static void updateInstrument(InstrumentInfo var0) throws Exception {
      get()
         ._updateInstrument(
            var0.instrument,
            var0.description,
            var0.pointValue,
            var0.tickSize,
            var0.tickStep,
            var0.defaultSpread,
            var0.defaultSlippage,
            var0.minDistance,
            var0.commissions,
            (byte)var0.dataType,
            var0.exchange,
            var0.country,
            var0.sector,
            var0.swap,
            var0.orderSizeMultiplier,
            var0.orderSizeStep
         );
   }

   public static void updateInstrument(
      String var0,
      String var1,
      double var2,
      double var4,
      double var6,
      double var8,
      double var10,
      double var12,
      String var14,
      byte var15,
      String var16,
      String var17,
      String var18,
      String var19,
      double var20,
      double var22
   ) throws Exception {
      get()._updateInstrument(var0, var1, var2, var4, var6, var8, var10, var12, var14, var15, var16, var17, var18, var19, var20, var22);
   }

   public static void removeInstrument(String var0) throws Exception {
      get()._removeInstrument(var0);
   }

   public static void addAlias(String var0, String var1, String var2) throws DataException {
      get()._addAlias(var0, var1, var2);
   }

   public static void updateAlias(String var0, String var1, String var2) throws Exception {
      get()._updateAlias(var0, var1, var2);
   }

   public static void updateDataType(String var0, byte var1) throws Exception {
      get()._updateDataType(var0, var1);
   }

   private void _updateDataType(String var1, byte var2) throws Exception {
      if (this._checkInstrumentExists(var1, false)) {
         try {
            String var3 = "UPDATE INSTRUMENTS SET DATATYPE='" + var2 + "'";
            var3 = var3 + " WHERE INSTRUMENT='" + var1 + "'";
            this.sqlCommand(var3);
            InstrumentInfo var4 = this.instrumentCache.get(var1);
            var4.dataType = var2;
            this.instrumentCache.store(var4);
         } catch (Exception var5) {
            Log.error("DB Exception", var5);
            throw new DataException(3, var5.getMessage());
         }
      } else {
         throw new Exception("Instrument '" + var1 + "' doesn't exist.");
      }
   }

   public static void removeAlias(String var0) throws Exception {
      AliasManager.removeAlias(var0);
   }

   public static ArrayList<InstrumentInfo> list() throws DataException {
      return get()._list();
   }

   public static ArrayList<InstrumentAlias> listAliases() throws DataException {
      return AliasManager.getAliases();
   }

   public static ArrayList<InstrumentAlias> listInstrumentAliases(String var0) throws DataException {
      return AliasManager.listInstrumentAliases(var0);
   }

   public static void removeInstrumentAliases(String var0) throws DataException {
      AliasManager.removeInstrumentAliases(var0);
   }

   public static boolean trySaveExchange(String var0) throws DataException {
      return get()._trySaveExchange(var0);
   }

   public static boolean trySaveCountry(String var0) throws DataException {
      return get()._trySaveCountry(var0);
   }

   public static boolean trySaveSector(String var0) throws DataException {
      return get()._trySaveSector(var0);
   }

   public static ArrayList<String> getExchanges() {
      ArrayList var0 = get().exchanges;
      var0.sort(new StringComparator());
      return var0;
   }

   public static ArrayList<String> getCountries() {
      ArrayList var0 = get().countries;
      var0.sort(new StringComparator());
      return var0;
   }

   public static ArrayList<String> getSectors() {
      ArrayList var0 = get().sectors;
      var0.sort(new StringComparator());
      return var0;
   }

   private ArrayList<InstrumentInfo> _list() throws DataException {
      return this.instrumentCache.list();
   }

   private void _addInstrumentInBatch(List<InstrumentInfo> var1) throws Exception {
      Connection var2 = null;

      try {
         var2 = this.getConnection();

         for (InstrumentInfo var4 : var1) {
            String var5 = "INSERT INTO INSTRUMENTS (INSTRUMENT, DESCRIPTION, POINTVALUE, TICKSIZE, TICKSTEP, DEFAULTSPREAD, DEFAULTSLIPPAGE, COMMISSIONS, DATATYPE, ORDERSIZEMULTIPLIER, ORDERSIZESTEP";
            if (var4.exchange != null) {
               var5 = var5 + ", EXCHANGE";
            }

            if (var4.country != null) {
               var5 = var5 + ", COUNTRY";
            }

            if (var4.sector != null) {
               var5 = var5 + ", SECTOR";
            }

            if (var4.swap != null) {
               var5 = var5 + ", SWAP";
            }

            var5 = var5
               + ") VALUES ('"
               + var4.instrument
               + "','"
               + var4.description
               + "',"
               + var4.pointValue
               + ","
               + var4.tickSize
               + ","
               + var4.tickStep
               + ","
               + var4.defaultSpread
               + ","
               + var4.defaultSlippage
               + ",'"
               + var4.commissions
               + "',"
               + var4.dataType
               + ","
               + var4.orderSizeMultiplier
               + ","
               + var4.orderSizeStep;
            if (var4.exchange != null) {
               var5 = var5 + ", '" + var4.exchange + "'";
            }

            if (var4.country != null) {
               var5 = var5 + ", '" + var4.country + "'";
            }

            if (var4.sector != null) {
               var5 = var5 + ", '" + var4.sector + "'";
            }

            if (var4.swap != null) {
               var5 = var5 + ", '" + var4.swap + "'";
            }

            var5 = var5 + ")";
            this.sqlCommand(var5);
            this.instrumentCache.store(var4);
         }
      } catch (Exception var9) {
         Log.error("DB Exception", var9);
         throw var9;
      } finally {
         this.close(var2);
      }
   }

   private void _addInstrument(
      String var1,
      int var2,
      String var3,
      double var4,
      double var6,
      double var8,
      double var10,
      double var12,
      double var14,
      String var16,
      byte var17,
      String var18,
      String var19,
      String var20,
      String var21,
      double var22,
      double var24
   ) throws Exception {
      if (!this._checkInstrumentExists(var1, false)) {
         try {
            String var26 = "INSERT INTO INSTRUMENTS (INSTRUMENT, BROKER_ID, DESCRIPTION, POINTVALUE, TICKSIZE, TICKSTEP, DEFAULTSPREAD, DEFAULTSLIPPAGE, MIN_DISTANCE, COMMISSIONS, DATATYPE, ORDERSIZEMULTIPLIER, ORDERSIZESTEP";
            if (var18 != null) {
               var26 = var26 + ", EXCHANGE";
            }

            if (var19 != null) {
               var26 = var26 + ", COUNTRY";
            }

            if (var20 != null) {
               var26 = var26 + ", SECTOR";
            }

            if (var21 != null) {
               var26 = var26 + ", SWAP";
            }

            var26 = var26
               + ") VALUES ('"
               + var1
               + "',"
               + var2
               + ",'"
               + var3
               + "',"
               + var4
               + ","
               + var6
               + ","
               + var8
               + ","
               + var10
               + ","
               + var12
               + ","
               + var14
               + ",'"
               + var16
               + "',"
               + var17
               + ","
               + var22
               + ","
               + var24;
            if (var18 != null) {
               var26 = var26 + ", '" + var18 + "'";
            }

            if (var19 != null) {
               var26 = var26 + ", '" + var19 + "'";
            }

            if (var20 != null) {
               var26 = var26 + ", '" + var20 + "'";
            }

            if (var21 != null) {
               var26 = var26 + ", '" + var21 + "'";
            }

            var26 = var26 + ")";
            this.sqlCommand(var26);
            this.instrumentCache.store(var1, var2, var3, var4, var6, var8, var10, var12, var14, var16, var17, var18, var19, var20, var21, var22, var24);
         } catch (Exception var27) {
            Log.error("DB Exception", var27);
            throw new Exception("Adding instrument '" + var1 + "' failed.");
         }
      } else {
         throw new Exception("Instrument '" + var1 + "' already exists.");
      }
   }

   private void _updateInstrument(
      String var1,
      String var2,
      double var3,
      double var5,
      double var7,
      double var9,
      double var11,
      double var13,
      String var15,
      byte var16,
      String var17,
      String var18,
      String var19,
      String var20,
      double var21,
      double var23
   ) throws Exception {
      if (this._checkInstrumentExists(var1, false)) {
         try {
            String var25 = "UPDATE INSTRUMENTS SET DESCRIPTION='"
               + var2
               + "', POINTVALUE="
               + var3
               + ", TICKSIZE="
               + var5
               + ", TICKSTEP="
               + var7
               + ", DEFAULTSPREAD="
               + var9
               + ", DEFAULTSLIPPAGE="
               + var11
               + ", MIN_DISTANCE="
               + var13
               + ", COMMISSIONS='"
               + var15
               + "', DATATYPE="
               + var16
               + ",ORDERSIZEMULTIPLIER="
               + var21
               + ",ORDERSIZESTEP="
               + var23;
            if (var17 != null) {
               var25 = var25 + ", EXCHANGE='" + var17 + "'";
            }

            if (var18 != null) {
               var25 = var25 + ", COUNTRY='" + var18 + "'";
            }

            if (var19 != null) {
               var25 = var25 + ", SECTOR='" + var19 + "'";
            }

            if (var20 != null) {
               var25 = var25 + ", SWAP='" + var20 + "'";
            }

            var25 = var25 + " WHERE INSTRUMENT='" + var1 + "'";
            this.sqlCommand(var25);
            this.instrumentCache.store(var1, null, var2, var3, var5, var7, var9, var11, var13, var15, var16, var17, var18, var19, var20, var21, var23);
            DataManager.get().removeRecordFromCacheForInstrument(var1);
         } catch (Exception var26) {
            Log.error("DB Exception", var26);
            throw new DataException(3, var26.getMessage());
         }
      } else {
         throw new Exception("Instrument '" + var1 + "' doesn't exist.");
      }
   }

   private void _removeInstrument(String var1) throws Exception {
      if (!this._checkInstrumentExists(var1, false)) {
         throw new Exception("Instrument '" + var1 + "' doesn't exist.");
      }

      try {
         ArrayList var2 = DataManager.getSymbolsForInstrument(var1);
         if (var2 != null && !var2.isEmpty()) {
            String var7 = "";

            for (String var5 : var2) {
               var7 = var7 + var5 + ",";
            }

            var7 = var7.substring(0, var7.length() - 1);
            throw new Exception("Cannot delete instrument '" + var1 + "' as it is currently used by symbols: " + var7);
         } else {
            String var3 = "DELETE FROM INSTRUMENTS WHERE INSTRUMENT='" + var1 + "'";
            this.sqlCommand(var3);
            this.instrumentCache.remove(var1);
            AliasManager.removeInstrumentAliases(var1);
         }
      } catch (Exception var6) {
         Log.error("DB Exception", var6);
         throw new DataException(3, var6.getMessage());
      }
   }

   public InstrumentInfo _getInstrumentInfo(String var1) throws DataException {
      InstrumentInfo var2 = this.instrumentCache.get(var1);
      if (var2 != null) {
         return var2;
      }

      Connection var3 = null;
      Statement var4 = null;
      ResultSet var5 = null;

      try {
         var3 = this.getConnection();
         String var6;
         if (this._checkInstrumentExists(var3, var1)) {
            var6 = var1;
         } else {
            if (!AliasManager.checkAliasExists(var1)) {
               throw new DataException(3, "DB: Instrument or alias '" + var1 + "' not found.");
            }

            var6 = AliasManager.getAliasInstrument(var1);
         }

         InstrumentInfo var7 = new InstrumentInfo();
         var4 = var3.createStatement();
         var5 = var4.executeQuery("SELECT * FROM INSTRUMENTS WHERE INSTRUMENT='" + var6 + "'");
         if (var5.next()) {
            var7.instrument = var6.intern();
            var7.alias = var1.intern();
            var7.description = var5.getString("DESCRIPTION").intern();
            var7.broker = var5.getInt("BROKER_ID");
            var7.pointValue = var5.getDouble("POINTVALUE");
            var7.tickSize = var5.getDouble("TICKSIZE");
            var7.tickStep = var5.getDouble("TICKSTEP");
            var7.defaultSpread = var5.getDouble("DEFAULTSPREAD");
            var7.defaultSlippage = var5.getDouble("DEFAULTSLIPPAGE");
            var7.commissions = var5.getString("COMMISSIONS");
            var7.swap = var5.getString("SWAP");
            var7.dataType = var5.getInt("DATATYPE");
            var7.exchange = var5.getString("EXCHANGE");
            var7.country = var5.getString("COUNTRY");
            var7.sector = var5.getString("SECTOR");
            var7.decimals = SQUtils.getDecimalPlaces(var7.tickStep);
            var7.orderSizeMultiplier = var5.getDouble("ORDERSIZEMULTIPLIER");
            var7.orderSizeStep = var5.getDouble("ORDERSIZESTEP");
            var7.minDistance = var5.getDouble("MIN_DISTANCE");
            this.instrumentCache.store(var7);
            return var7;
         }
      } catch (Exception var12) {
         Log.error("DB Exception", var12);
         throw new DataException(3, var12.getMessage());
      } finally {
         this.close(var5);
         this.close(var4);
         this.close(var3);
      }

      throw new DataException(3, "DB: Instrument or alias '" + var1 + "' not found (2).");
   }

   private boolean _checkInstrumentExists(String var1, boolean var2) {
      Connection var3 = null;

      try {
         if (this.instrumentCache.contains(var1)) {
            return true;
         }

         var3 = this.getConnection();
         boolean var4 = this._checkInstrumentExists(var3, var1);
         return !var2 && !var4 ? AliasManager.checkAliasExists(var1) : var4;
      } catch (Exception var9) {
         Log.error("DB Exception", var9);
         return false;
      } finally {
         this.close(var3);
      }
   }

   private boolean _checkInstrumentExists(Connection var1, String var2) {
      return this.sqlCheckRecordExists(var1, "SELECT * FROM INSTRUMENTS WHERE INSTRUMENT='" + var2 + "'");
   }

   private void _addAlias(String var1, String var2, String var3) throws DataException {
      Connection var4 = null;
      Statement var5 = null;
      Object var6 = null;

      try {
         var4 = this.getConnection();
         var5 = var4.createStatement();
         if (!this._checkInstrumentExists(var4, var2)) {
            throw new DataException(3, "DB: Instrument '" + var2 + "' doesn't exist.");
         }

         if (this._checkInstrumentExists(var4, var1)) {
            throw new DataException(3, "DB: Alias must not have the same name as an existing instrument.");
         }

         AliasManager.addAlias(var1, var2, var3);
      } catch (Exception var11) {
         Log.error("DB Exception", var11);
         throw new DataException(3, var11.getMessage());
      } finally {
         this.close((ResultSet)var6);
         this.close(var5);
         this.close(var4);
      }
   }

   private void _updateAlias(String var1, String var2, String var3) throws DataException {
      Connection var4 = null;
      Statement var5 = null;
      Object var6 = null;

      try {
         var4 = this.getConnection();
         var5 = var4.createStatement();
         if (!this._checkInstrumentExists(var4, var1)) {
            throw new DataException(3, "DB: Instrument '" + var1 + "' doesn't exist.");
         }

         if (this._checkInstrumentExists(var4, var2)) {
            throw new DataException(3, "DB: Alias must not have the same name as an existing instrument.");
         }

         AliasManager.updateAlias(var2, var1, var3);
      } catch (Exception var11) {
         Log.error("DB Exception", var11);
         throw new DataException(3, var11.getMessage());
      } finally {
         this.close((ResultSet)var6);
         this.close(var5);
         this.close(var4);
      }
   }

   private boolean _trySaveExchange(String var1) {
      if (!this.exchanges.contains(var1) && !var1.isEmpty()) {
         this.exchanges.add(var1);

         try {
            SQUtils.arrayListToFile(this.exchanges, exchangesFilePath, "\r\n");
         } catch (Exception var3) {
            Log.error("Saving exchanges failed. ", var3);
         }

         return true;
      } else {
         return false;
      }
   }

   private boolean _trySaveCountry(String var1) {
      if (!this.countries.contains(var1) && !var1.isEmpty()) {
         this.countries.add(var1);

         try {
            SQUtils.arrayListToFile(this.countries, countriesFilePath, "\r\n");
         } catch (Exception var3) {
            Log.error("Saving countries failed. ", var3);
         }

         return true;
      } else {
         return false;
      }
   }

   private boolean _trySaveSector(String var1) {
      if (!this.sectors.contains(var1) && !var1.isEmpty()) {
         this.sectors.add(var1);

         try {
            SQUtils.arrayListToFile(this.sectors, sectorsFilePath, "\r\n");
         } catch (Exception var3) {
            Log.error("Saving sectors failed. ", var3);
         }

         return true;
      } else {
         return false;
      }
   }

   public static InstrumentInfo findClosestInstrument(String var0) throws DataException {
      return get()._findClosestInstrument(var0);
   }

   private InstrumentInfo _findClosestInstrument(String var1) throws DataException {
      ArrayList var2 = this._list();
      InstrumentInfo var3 = null;
      int var4 = Integer.MAX_VALUE;

      for (int var5 = 0; var5 < var2.size(); var5++) {
         InstrumentInfo var6 = (InstrumentInfo)var2.get(var5);
         int var7 = SQUtils.levenshteinDistance(var1, var6.instrument);
         if (var7 < var4) {
            var3 = var6;
            var4 = var7;
         }
      }

      return var3;
   }

   private class InstrumentCache {
      private HashMap<String, InstrumentInfo> instrumentInfoCache = new HashMap<>();
      private boolean cacheReady = false;

      private InstrumentCache() {
      }

      private synchronized void ensureCache() throws DataException {
         if (!this.cacheReady) {
            Connection var1 = null;
            Statement var2 = null;
            ResultSet var3 = null;
            ArrayList var4 = new ArrayList();

            try {
               var1 = InstrumentManager.this.getConnection();
               var2 = var1.createStatement();
               var3 = var2.executeQuery("SELECT * FROM INSTRUMENTS");

               while (var3.next()) {
                  InstrumentInfo var5 = new InstrumentInfo();
                  var5.instrument = var3.getString("INSTRUMENT").intern();
                  var5.description = var3.getString("DESCRIPTION").intern();
                  var5.pointValue = var3.getDouble("POINTVALUE");
                  var5.tickSize = var3.getDouble("TICKSIZE");
                  var5.tickStep = var3.getDouble("TICKSTEP");
                  var5.defaultSpread = var3.getDouble("DEFAULTSPREAD");
                  var5.defaultSlippage = var3.getDouble("DEFAULTSLIPPAGE");
                  var5.commissions = var3.getString("COMMISSIONS");
                  var5.dataType = var3.getInt("DATATYPE");
                  var5.exchange = var3.getString("EXCHANGE");
                  var5.country = var3.getString("COUNTRY");
                  var5.sector = var3.getString("SECTOR");
                  var5.decimals = SQUtils.getDecimalPlaces(var5.tickStep);
                  var5.swap = var3.getString("SWAP");
                  var5.broker = var3.getInt("BROKER_ID");
                  var5.minDistance = var3.getDouble("MIN_DISTANCE");
                  var5.orderSizeMultiplier = var3.getDouble("ORDERSIZEMULTIPLIER");
                  var5.orderSizeStep = var3.getDouble("ORDERSIZESTEP");
                  var4.add(var5);
                  this.instrumentInfoCache.put(var5.instrument, var5);
               }
            } catch (Exception var9) {
               DataDb.Log.error("DB Exception", var9);
               throw new DataException(3, var9.getMessage());
            } finally {
               InstrumentManager.this.close(var3);
               InstrumentManager.this.close(var2);
               InstrumentManager.this.close(var1);
            }

            this.cacheReady = true;
         }
      }

      public synchronized InstrumentInfo get(String var1) throws DataException {
         this.ensureCache();
         return this.instrumentInfoCache.get(var1);
      }

      public boolean contains(String var1) throws DataException {
         this.ensureCache();
         return this.instrumentInfoCache.containsKey(var1);
      }

      public synchronized InstrumentInfo remove(String var1) throws DataException {
         this.ensureCache();
         return this.instrumentInfoCache.remove(var1);
      }

      public synchronized void store(
         String var1,
         Integer var2,
         String var3,
         double var4,
         double var6,
         double var8,
         double var10,
         double var12,
         double var14,
         String var16,
         byte var17,
         String var18,
         String var19,
         String var20,
         String var21,
         double var22,
         double var24
      ) throws DataException {
         InstrumentInfo var26 = this.get(var1);
         if (var26 == null) {
            var26 = new InstrumentInfo();
            var26.instrument = var1;
         }

         if (var2 != null) {
            var26.broker = var2;
         }

         var26.description = var3;
         var26.pointValue = var4;
         var26.tickSize = var6;
         var26.tickStep = var8;
         var26.defaultSpread = var10;
         var26.defaultSlippage = var12;
         var26.minDistance = var14;
         var26.commissions = var16;
         var26.dataType = var17;
         var26.orderSizeMultiplier = var22;
         var26.orderSizeStep = var24;
         var26.decimals = SQUtils.getDecimalPlaces(var26.tickStep);
         if (var18 != null) {
            var26.exchange = var18;
         }

         if (var19 != null) {
            var26.country = var19;
         }

         if (var20 != null) {
            var26.sector = var20;
         }

         if (var21 != null) {
            var26.swap = var21;
         }

         this.store(var26);
      }

      public synchronized InstrumentInfo store(InstrumentInfo var1) throws DataException {
         this.ensureCache();
         var1.decimals = SQUtils.getDecimalPlaces(var1.tickStep);
         return this.instrumentInfoCache.put(var1.instrument, var1);
      }

      public synchronized ArrayList<InstrumentInfo> list() throws DataException {
         this.ensureCache();
         return new ArrayList<>(
            this.instrumentInfoCache.values().stream().sorted((var0, var1) -> var0.instrument.compareTo(var1.instrument)).collect(Collectors.toList())
         );
      }
   }
}
