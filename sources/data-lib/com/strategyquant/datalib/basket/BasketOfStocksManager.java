package com.strategyquant.datalib.basket;

import com.strategyquant.datalib.DataInfo;
import com.strategyquant.datalib.data.DataDb;
import com.strategyquant.datalib.data.DataManager;
import com.strategyquant.lib.L;
import com.strategyquant.lib.SQTime;
import com.strategyquant.lib.app.MainApp;
import com.strategyquant.lib.constants.SQPaths;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BasketOfStocksManager extends DataDb {
   private static final Logger Log = LoggerFactory.getLogger(BasketOfStocksManager.class);
   private static final String GROUP_OF_STOCKS_VERSION = "group_of_stocks.version";
   private static final String version = "v2";
   public static final String LimitedGroupName = "[[S&P 100 limited]]";
   private static BasketOfStocksManager instance = null;
   private static final String insertBasketSQL = "INSERT INTO STOCK_GROUP (NAME, DESC, SYSTEM)values(?,?, ?)";
   private static final String updateBasketSQL = "UPDATE STOCK_GROUP set NAME=?, DESC=? where ID=?";
   private static final String insertStockSql = "INSERT INTO STOCK (BASKET_ID, TICKER, DATE_FROM, DATE_TO) values(?,?,?,?)";
   private Map<String, StockDto> quickStockMap = new HashMap<>();
   private Map<String, Set<Integer>> groupsForStocks = new HashMap<>();
   private Map<Integer, List<StockDto>> stocksOfGroup;
   private LinkedHashMap<Integer, BasketDto> groupsMap;
   private Map<Integer, List<StockDto>> customStocksOfGroup = new HashMap<>();
   private LinkedHashMap<Integer, BasketDto> customGroupsMap = new LinkedHashMap<>();
   private String url;
   private String version_url;
   private boolean devel = false;

   public static void init(String var0) throws Exception {
      if (instance != null) {
         throw new Exception("BasketOfStocksManager.init() called more than once!");
      }

      instance = new BasketOfStocksManager(var0);
   }

   private void checkDevelMode() {
      File var1 = new File(MainApp.getDataPath() + "/usenewdata.txt");
      if (var1.exists()) {
         Log.info("Using development mode for downloading data. So it means that test data will be downloaded instead of real one!!!!!!!!!!");
         this.devel = true;
      }
   }

   public void synhronizeAsync(final Runnable var1) {
      boolean var2 = MainApp.runInConsole() && !MainApp.CLIWebServeActive;
      if (!var2) {
         new Thread(new Runnable() {
            @Override
            public void run() {
               try {
                  BasketOfStocksManager.instance.sync(var1);
               } catch (Throwable var2x) {
                  BasketOfStocksManager.Log.error("Error while sync stock group definition", var2x);
               }
            }
         }).start();
      }
   }

   private BasketOfStocksManager(String var1) {
      super(var1);
      String var2 = BasketBrokerDev.getInstance().isDev() ? "_dev" : "";
      this.url = "http://autoupdate.strategyquant.com/stockgroups_v2" + var2 + "/stocks.zip";
      this.version_url = "http://autoupdate.strategyquant.com/stockgroups_v2" + var2 + "/stocks.version";
      this.checkDevelMode();
      if (this.devel) {
         this.url = "http://autoupdate.strategyquant.com/stockgroups_dev/stocks.zip";
         this.version_url = "http://autoupdate.strategyquant.com/stockgroups_dev/stocks.version";
      }
   }

   public static BasketOfStocksManager getInstance() {
      return instance;
   }

   public void sync(Runnable var1) throws Exception {
      Log.info("Syncing stockgroup starting");
      long var2 = System.currentTimeMillis();
      String var4 = this.getNewAvailableVersion();
      if (var4 != null) {
         byte[] var5 = this.download(this.url);
         Map var6 = this.unzip(var5);
         Map var7 = this.getSystemGroupsByName();

         for (String var9 : var6.keySet()) {
            try {
               Log.debug("Syncing group: {}", var9);
               String var10 = (String)var6.get(var9);
               BasketDto var11 = (BasketDto)var7.get(var9);
               Integer var12 = null;
               if (var11 != null) {
                  var12 = var11.getId();
                  var7.remove(var9);
               }

               this.importFromCsv(var10, var12, var9);
            } catch (Exception var13) {
               Log.error("Syncing group: " + var9 + " failed", var13);
            }
         }

         for (BasketDto var16 : var7.values()) {
            this.deleteGroup(var16.getId());
         }

         this.saveLatestVersion(var4);
         this.performLoadGroups();
         if (var1 != null) {
            var1.run();
         }
      }

      this.checkDataAliases();
      if (var4 == null) {
         this.updateGroupsOfSymbols();
      }

      long var14 = System.currentTimeMillis() - var2;
      Log.info("Syncing stockgroup finished in {}ms", var14);
   }

   private void saveLatestVersion(String var1) throws IOException {
      Log.debug("Updating data version file");
      File var2 = null;
      if (this.devel) {
         var2 = new File(SQPaths.dataDirPath, "group_of_stocks_dev.version");
      } else {
         var2 = new File(SQPaths.dataDirPath, "group_of_stocks.version");
      }

      Files.write(var2.toPath(), var1.getBytes(), StandardOpenOption.CREATE);
   }

   private Map<String, String> unzip(byte[] var1) throws IOException {
      HashMap var2 = new HashMap();
      byte[] var3 = new byte[4096];
      ZipInputStream var4 = new ZipInputStream(new ByteArrayInputStream(var1));

      try {
         for (ZipEntry var5 = var4.getNextEntry(); var5 != null; var5 = var4.getNextEntry()) {
            String var6 = var5.getName();
            if (var6.endsWith(".csv")) {
               ByteArrayOutputStream var7 = new ByteArrayOutputStream();

               int var8;
               while ((var8 = var4.read(var3)) > 0) {
                  var7.write(var3, 0, var8);
               }

               var7.close();
               byte[] var9 = var7.toByteArray();
               String var10 = var6.substring(0, var6.length() - 4);
               var2.put(var10, new String(var9));
            }
         }

         return var2;
      } finally {
         var4.closeEntry();
         var4.close();
      }
   }

   private String getCurrentDownloadedVersion() throws IOException {
      File var1 = null;
      if (this.devel) {
         var1 = new File(SQPaths.dataDirPath, "group_of_stocks_dev.version");
      } else {
         var1 = new File(SQPaths.dataDirPath, "group_of_stocks.version");
      }

      return !var1.exists() ? null : new String(Files.readAllBytes(var1.toPath()));
   }

   private String getNewAvailableVersion() throws IOException {
      String var1 = this.getCurrentDownloadedVersion();
      Log.debug("Current version: {}", var1);
      byte[] var2 = this.download(this.version_url + "?time=" + new Date().getTime());
      if (var2 == null) {
         return null;
      }

      String var3 = new String(var2);
      Log.debug("Available version: {}", var3);
      return var1 != null && var1.compareTo(var3) >= 0 ? null : var3;
   }

   private byte[] download(String var1) throws IOException {
      CloseableHttpClient var2 = HttpClientBuilder.create().build();

      Object var4;
      label55: {
         Object var12;
         label56: {
            byte[] var7;
            try {
               try {
                  CloseableHttpResponse var3 = var2.execute(new HttpGet(var1));
                  int var11 = var3.getStatusLine().getStatusCode();
                  if (var11 != 200) {
                     Log.error("Http result: {} for url: {}", var11, var1);
                     var12 = null;
                     break label56;
                  }

                  InputStream var5 = var3.getEntity().getContent();
                  byte[] var6 = IOUtils.toByteArray(var5);
                  var7 = var6;
               } catch (Exception var9) {
                  Log.error("Error while downloading url: " + var1, var9);
                  var4 = null;
                  break label55;
               }
            } catch (Throwable var10) {
               if (var2 != null) {
                  try {
                     var2.close();
                  } catch (Throwable var8) {
                     var10.addSuppressed(var8);
                  }
               }

               throw var10;
            }

            if (var2 != null) {
               var2.close();
            }

            return var7;
         }

         if (var2 != null) {
            var2.close();
         }

         return (byte[])var12;
      }

      if (var2 != null) {
         var2.close();
      }

      return (byte[])var4;
   }

   public List<StockDto> readStocksFromCsv(String var1, String var2) throws Exception {
      String[] var3 = var1.split("\n");
      ArrayList var4 = new ArrayList(var3.length);

      for (String var8 : var3) {
         var8 = var8.trim();
         if (!var8.isEmpty()) {
            String[] var9 = var8.split(";");
            StockDto var10 = new StockDto();
            var10.setTicker(var2 + var9[0]);
            if (var9.length > 1 && !var9[1].isBlank()) {
               var10.setDateFrom(this.parseDate(var9[1]));
            } else {
               var10.setDateFrom(-1L);
            }

            if (var9.length > 2 && !var9[2].isBlank()) {
               var10.setDateTo(this.parseDate(var9[2]));
            } else {
               var10.setDateTo(-1L);
            }

            var4.add(var10);
         }
      }

      if (var4.isEmpty()) {
         throw new Exception("No stocks recognized.");
      } else {
         return var4;
      }
   }

   private long parseDate(String var1) {
      try {
         var1 = var1.trim();
         return SQTime.parseToMilis(var1, "dd.MM.yyyy");
      } catch (Exception var5) {
         try {
            return SQTime.parseToMilis(var1, "yyyy.MM.dd");
         } catch (Exception var4) {
            Log.error("Error while parsing date: " + var1);
            return -1L;
         }
      }
   }

   public void importFromCsv(String var1, Integer var2, String var3) throws Exception {
      this.importFromCsv(var1, var2, var3, "");
   }

   public void importFromCsv(String var1, Integer var2, String var3, String var4) throws Exception {
      List var6 = this.readStocksFromCsv(var1, var4);
      if (var2 == null) {
         BasketDto var5 = this.getSystemBasket(var3);
         if (var5 == null) {
            var5 = new BasketDto();
            var5.setSystem(true);
            var5.setName(var3);
            this.saveGroup(var5);
         }

         var2 = var5.getId();
      }

      BasketDto var7 = this.getBasket(var2);
      if (var7 == null) {
         throw new Exception("Stockgroup was not found.");
      }

      DataManager.createGroupAlias(var7.getName(), var2);
      this.saveStocks(var2, var6);
      this.updateCount(var7);
   }

   public List<StockDto> updateStocksFromStr(String var1, Integer var2, String var3) throws Exception {
      List var4 = this.readStocksFromCsv(var1, var3);
      BasketDto var5 = this.getBasket(var2);
      if (var5 == null) {
         throw new Exception("Stockgroup was not found.");
      }

      this.saveStocks(var2, var4);
      this.updateCount(var5);
      return var4;
   }

   private void updateCount(BasketDto var1) {
      List var2 = this.stocksOfGroup.get(var1.getId());
      long var3 = var2 == null ? 0L : var2.stream().filter(var0 -> var0.isActive()).count();
      var1.setCount((int)var3);
      var1.setTotal(var2.size());
   }

   @Override
   public void initDatabase() {
      try {
         if (!this.tableExists("STOCK_GROUP")) {
            String var1 = "CREATE TABLE STOCK_GROUP (ID INTEGER PRIMARY KEY AUTOINCREMENT, NAME VARCHAR(50) NOT NULL,SYSTEM BOOLEAN NOT NULL,DESC VARCHAR(250))";
            this.sqlCommand(var1);
            Log.debug("Stockgroup table created successfully");
         }

         if (!this.tableExists("STOCK")) {
            String var3 = "CREATE TABLE STOCK (ID INTEGER PRIMARY KEY AUTOINCREMENT, TICKER VARCHAR(50) NOT NULL,BASKET_ID INTEGER NOT NULL,DATE_FROM LONG NOT NULL,DATE_TO LONG,FOREIGN KEY(BASKET_ID) REFERENCES STOCK_GROUP(ID))";
            this.sqlCommand(var3);
            Log.debug("Stock table created successfully");
         }
      } catch (Exception var2) {
         Log.error("DB error: Cannot create baskets table.", var2);
      }
   }

   public synchronized void saveGroup(BasketDto var1) throws Exception {
      boolean var2 = var1.getId() == null;
      if (var1.getName() == null) {
         throw new IllegalArgumentException("Group's name must be set");
      }

      String var3 = var1.getName().trim();
      if (!var3.startsWith("[")) {
         var3 = "[" + var3;
      }

      if (!var3.endsWith("]")) {
         var3 = var3 + "]";
      }

      var1.setName(var3);
      Connection var4 = this.getConnection();

      try {
         if (!var2) {
            BasketDto var15 = this.getBasket(var3);
            if (var15 != null && !var15.getId().equals(var1.getId())) {
               throw new IllegalArgumentException("Name must be unique");
            }

            PreparedStatement var16 = var4.prepareStatement("UPDATE STOCK_GROUP set NAME=?, DESC=? where ID=?");

            try {
               BasketDto var17 = this.getBasket(var1.getId());
               var16.setString(1, var3);
               if (var1.getDesc() == null) {
                  var16.setNull(2, 12);
               } else {
                  var16.setString(2, var1.getDesc());
               }

               var16.setInt(3, var1.getId());
               var16.execute();
               var1.setCount(var17.getCount());
               var1.setTotal(var17.getTotal());
            } catch (Throwable var12) {
               if (var16 != null) {
                  try {
                     var16.close();
                  } catch (Throwable var10) {
                     var12.addSuppressed(var10);
                  }
               }

               throw var12;
            }

            if (var16 != null) {
               var16.close();
            }
         } else {
            if (!var1.isSystem() && this.getBasket(var3) != null) {
               throw new IllegalArgumentException("Name must be unique");
            }

            PreparedStatement var5 = var4.prepareStatement("INSERT INTO STOCK_GROUP (NAME, DESC, SYSTEM)values(?,?, ?)", 1);

            try {
               var5.setString(1, var3);
               if (var1.getDesc() == null) {
                  var5.setNull(2, 12);
               } else {
                  var5.setString(2, var1.getDesc());
               }

               var5.setBoolean(3, var1.isSystem());
               var5.execute();
               ResultSet var6 = var5.getGeneratedKeys();
               if (var6.next()) {
                  int var7 = var6.getInt(1);
                  var1.setId(var7);
               }
            } catch (Throwable var13) {
               if (var5 != null) {
                  try {
                     var5.close();
                  } catch (Throwable var11) {
                     var13.addSuppressed(var11);
                  }
               }

               throw var13;
            }

            if (var5 != null) {
               var5.close();
            }
         }

         this.groupsMap.put(var1.getId(), var1);
         DataManager.createGroupAlias(var3, var1.getId());
      } catch (Throwable var14) {
         if (var4 != null) {
            try {
               var4.close();
            } catch (Throwable var9) {
               var14.addSuppressed(var9);
            }
         }

         throw var14;
      }

      if (var4 != null) {
         var4.close();
      }
   }

   public BasketDto getBasket(String var1) {
      try {
         Connection var2 = this.getConnection();

         BasketDto var5;
         label74: {
            Object var11;
            try {
               PreparedStatement var3 = var2.prepareStatement("SELECT * FROM STOCK_GROUP where NAME=?");

               label76: {
                  try {
                     var3.setString(1, var1);
                     ResultSet var4 = var3.executeQuery();
                     if (!var4.next()) {
                        break label76;
                     }

                     var5 = this.toGroupDto(var4);
                  } catch (Throwable var8) {
                     if (var3 != null) {
                        try {
                           var3.close();
                        } catch (Throwable var7) {
                           var8.addSuppressed(var7);
                        }
                     }

                     throw var8;
                  }

                  if (var3 != null) {
                     var3.close();
                  }
                  break label74;
               }

               if (var3 != null) {
                  var3.close();
               }

               var11 = null;
            } catch (Throwable var9) {
               if (var2 != null) {
                  try {
                     var2.close();
                  } catch (Throwable var6) {
                     var9.addSuppressed(var6);
                  }
               }

               throw var9;
            }

            if (var2 != null) {
               var2.close();
            }

            return (BasketDto)var11;
         }

         if (var2 != null) {
            var2.close();
         }

         return var5;
      } catch (Exception var10) {
         Log.error("", var10);
         throw new RuntimeException(L.t("Error while reading data", new Object[0]));
      }
   }

   public Set<Integer> getGroupsForStock(String var1) {
      return this.groupsForStocks.get(var1);
   }

   public Set<String> updateGroupsOfSymbols() {
      Log.info("Updating stockgroup's from-to dates");
      HashMap var1 = new HashMap();
      String[] var2 = this.getAllSymbolsArray();

      for (int var4 : this.stocksOfGroup.keySet()) {
         var1.put(var4, new BasketOfStocksManager.MinMax());
      }

      HashSet var19 = new HashSet();
      Map var20 = this.getDataInfosMap();

      for (int var5 = 0; var5 < var2.length; var5++) {
         String var6 = var2[var5];
         DataInfo var7 = (DataInfo)var20.get(var6);
         if (var7 != null) {
            Set var8 = this.getGroupsForStock(var6);
            if (var8 != null && var7.dateFrom != 0L) {
               for (Integer var10 : var8) {
                  BasketDto var11 = this.groupsMap.get(var10);
                  if (var11 == null) {
                     Log.error("Stockgroup: " + var10 + " was not found!");
                  } else {
                     DataInfo var12 = DataManager.getDataInfo("History", var11.getName());
                     if (var12 != null) {
                        BasketOfStocksManager.MinMax var13 = (BasketOfStocksManager.MinMax)var1.get(var11.getId());
                        if (var13 == null) {
                           var13 = new BasketOfStocksManager.MinMax();
                           var1.put(var11.getId(), var13);
                        }

                        StockDto var14 = this.quickStockMap.get(var10 + "_" + var6);
                        if (var14 != null) {
                           long var15 = Math.max(var7.dateFrom, var14.getDateFrom());
                           long var17 = var14.getDateTo() == -1L ? var7.dateTo : Math.min(var7.dateTo, var14.getDateTo());
                           if (var13.min == 0L || var13.min > var15) {
                              var13.min = var15;
                           }

                           if (var13.max == 0L || var13.max < var17) {
                              var13.max = var17;
                           }
                        }
                     }
                  }
               }
            }
         }
      }

      for (Integer var22 : var1.keySet()) {
         BasketDto var23 = this.groupsMap.get(var22);
         DataInfo var24 = DataManager.getDataInfo("History", var23.getName());
         Log.debug("Evaluating min/max dates of stock group range: {}", var24.symbol);
         BasketOfStocksManager.MinMax var25 = (BasketOfStocksManager.MinMax)var1.get(var22);
         if (var25.min != var24.dateFrom || var25.max != var24.dateTo) {
            Log.debug("Updating stock group {} range: {} - {}", new Object[]{var24.symbol, SQTime.formatDate(var25.min), SQTime.formatDate(var25.max)});
            DataManager.updateData("History", var24.symbol, var25.min, var25.max, 1000000, 0L, 1, "D1", "Etc/UCT");
            var19.add(var24.symbol);
         }
      }

      return var19;
   }

   private Map<String, DataInfo> getDataInfosMap() {
      try {
         DataManager.get();
         ArrayList var1 = DataManager.listSafe();
         HashMap var2 = new HashMap();

         for (DataInfo var4 : var1) {
            if (var4.symbol != null) {
               var2.put(var4.symbol, var4);
            }
         }

         return var2;
      } catch (Exception var5) {
         throw new RuntimeException(L.t("Error while getting datainfos", new Object[0]), var5);
      }
   }

   private String[] getAllSymbolsArray() {
      try {
         DataManager.get();
         ArrayList var1 = DataManager.listSafe();
         return var1.stream().map(var0 -> var0.symbol).collect(Collectors.toList()).toArray(new String[0]);
      } catch (Exception var2) {
         throw new RuntimeException(L.t("Error while getting datainfos", new Object[0]), var2);
      }
   }

   public void updateGroupRange(Integer var1) {
      BasketDto var2 = this.getBasket(var1);
      DataInfo var3 = DataManager.getDataInfo("History", var2.getName());
      if (var3 == null) {
         Log.warn("Stock's group alias symbol {} doesn't exist", var2.getName());
      } else {
         LinkedList var4 = new LinkedList<>(this.getStocks(var1));
         long var5 = 0L;
         long var7 = 0L;

         try {
            Map var9 = this.getDataInfosMap();

            for (StockDto var11 : var4) {
               String var12 = var11.getTicker();
               DataInfo var13 = (DataInfo)var9.get(var12);
               StockDto var14 = this.quickStockMap.get(var1 + "_" + var12);
               if (var14 != null && var13 != null) {
                  long var15 = Math.max(var13.dateFrom, var14.getDateFrom());
                  long var17 = var14.getDateTo() == -1L ? var13.dateTo : Math.min(var13.dateTo, var14.getDateTo());
                  if (var5 == 0L || var5 > var15) {
                     var5 = var15;
                  }

                  if (var7 == 0L || var7 < var17) {
                     var7 = var17;
                  }
               }
            }

            if (var5 != var3.dateFrom || var7 != var3.dateTo) {
               Log.debug("Updating stock group {} range: {} - {}", new Object[]{var2.getName(), SQTime.formatDate(var5), SQTime.formatDate(var7)});
               DataManager.updateData("History", var2.getName(), var5, var7, 1000000, 0L, 1, "D1", "Etc/UCT");
            }
         } catch (Exception var19) {
            Log.error("Error while updating group range", var19);
         }
      }
   }

   public void saveCustomStocks(int var1, List<StockDto> var2) {
      this.customStocksOfGroup.put(var1, var2);
   }

   public void saveStocks(int var1, List<StockDto> var2) {
      try {
         Connection var3 = this.getConnection();

         try {
            try {
               var3.setAutoCommit(false);
               PreparedStatement var4 = var3.prepareStatement("delete from stock where basket_id=?");

               try {
                  var4.setInt(1, var1);
                  var4.executeUpdate();
                  this.stocksOfGroup.put(var1, new LinkedList<>());

                  for (StockDto var6 : var2) {
                     PreparedStatement var7 = var3.prepareStatement("INSERT INTO STOCK (BASKET_ID, TICKER, DATE_FROM, DATE_TO) values(?,?,?,?)");

                     try {
                        var7.setInt(1, var1);
                        var7.setString(2, var6.getTicker());
                        var7.setLong(3, var6.getDateFrom() == null ? -1L : var6.getDateFrom());
                        var7.setLong(4, var6.getDateTo() == null ? -1L : var6.getDateTo());
                        var7.executeUpdate();
                        this.stocksOfGroup.get(var1).add(var6);
                        this.quickStockMap.put(var1 + "_" + var6.getTicker(), var6);
                        String var8 = var6.getTicker();
                        Set var9 = this.groupsForStocks.get(var8);
                        if (var9 == null) {
                           var9 = new HashSet();
                           this.groupsForStocks.put(var8, var9);
                        }

                        var9.add(var1);
                     } catch (Throwable var13) {
                        if (var7 != null) {
                           try {
                              var7.close();
                           } catch (Throwable var12) {
                              var13.addSuppressed(var12);
                           }
                        }

                        throw var13;
                     }

                     if (var7 != null) {
                        var7.close();
                     }
                  }
               } catch (Throwable var14) {
                  if (var4 != null) {
                     try {
                        var4.close();
                     } catch (Throwable var11) {
                        var14.addSuppressed(var11);
                     }
                  }

                  throw var14;
               }

               if (var4 != null) {
                  var4.close();
               }

               var3.commit();
            } catch (Exception var15) {
               var3.rollback();
               throw var15;
            }
         } catch (Throwable var16) {
            if (var3 != null) {
               try {
                  var3.close();
               } catch (Throwable var10) {
                  var16.addSuppressed(var10);
               }
            }

            throw var16;
         }

         if (var3 != null) {
            var3.close();
         }
      } catch (Exception var17) {
         Log.error("", var17);
         throw new RuntimeException(L.t("Error while saving data", new Object[0]));
      }

      this.updateGroupRange(var1);
   }

   public List<StockDto> getStocks(int var1) {
      if (var1 < 0) {
         return this.customStocksOfGroup.get(var1);
      }

      if (this.stocksOfGroup == null) {
         this.performLoadGroups();
      }

      List var2 = this.stocksOfGroup.get(var1);
      return var2 == null ? new LinkedList<>() : var2;
   }

   public List<StockDto> getStocks(int var1, List<String> var2) {
      List var3 = this.getStocks(var1);
      if (var2 == null) {
         return var3;
      }

      LinkedList var4 = new LinkedList();

      for (StockDto var6 : var3) {
         if (var2.contains(var6.getTicker())) {
            var4.add(var6);
         }
      }

      return var4;
   }

   public synchronized BasketDto deleteGroup(int var1) throws Exception {
      BasketDto var2 = this.getBasket(var1);
      if (var2 == null) {
         throw new RuntimeException(L.t("Group was not found.", new Object[0]));
      }

      this.sqlCommand("delete from stock where basket_id=" + var1);
      this.sqlCommand("DELETE FROM STOCK_GROUP WHERE ID=" + var1);
      DataManager.deleteGroupAlias(var1, var2.getName());
      this.groupsMap.remove(var1);
      this.stocksOfGroup.remove(var1);
      return var2;
   }

   private Map<String, BasketDto> getSystemGroupsByName() {
      return this.getGroups().stream().filter(var0 -> var0.isSystem()).collect(Collectors.toMap(BasketDto::getName, var0 -> (BasketDto)var0));
   }

   private synchronized void performLoadGroups() {
      try {
         Connection var1 = this.getConnection();

         try {
            this.groupsMap = new LinkedHashMap<>();
            this.stocksOfGroup = new HashMap<>();
            this.groupsForStocks = new HashMap<>();
            this.quickStockMap = new HashMap<>();
            Statement var2 = var1.createStatement();

            try {
               ResultSet var3 = var2.executeQuery("SELECT * FROM STOCK_GROUP ORDER BY NAME");

               try {
                  while (var3.next()) {
                     BasketDto var4 = this.toGroupDto(var3);
                     this.groupsMap.put(var4.getId(), var4);
                  }
               } catch (Throwable var14) {
                  if (var3 != null) {
                     try {
                        var3.close();
                     } catch (Throwable var13) {
                        var14.addSuppressed(var13);
                     }
                  }

                  throw var14;
               }

               if (var3 != null) {
                  var3.close();
               }
            } catch (Throwable var15) {
               if (var2 != null) {
                  try {
                     var2.close();
                  } catch (Throwable var12) {
                     var15.addSuppressed(var12);
                  }
               }

               throw var15;
            }

            if (var2 != null) {
               var2.close();
            }

            var2 = var1.createStatement();

            try {
               ResultSet var22 = var2.executeQuery("SELECT * FROM STOCK");

               try {
                  while (var22.next()) {
                     int var24 = var22.getInt("BASKET_ID");
                     List var5 = this.stocksOfGroup.get(var24);
                     if (var5 == null) {
                        var5 = new LinkedList();
                        this.stocksOfGroup.put(var24, var5);
                     }

                     StockDto var6 = this.toStockDto(var22);
                     var5.add(var6);
                     this.quickStockMap.put(var24 + "_" + var6.getTicker(), var6);
                     String var7 = var6.getTicker();
                     Set var8 = this.groupsForStocks.get(var7);
                     if (var8 == null) {
                        var8 = new HashSet();
                        this.groupsForStocks.put(var7, var8);
                     }

                     var8.add(var24);
                  }
               } catch (Throwable var16) {
                  if (var22 != null) {
                     try {
                        var22.close();
                     } catch (Throwable var11) {
                        var16.addSuppressed(var11);
                     }
                  }

                  throw var16;
               }

               if (var22 != null) {
                  var22.close();
               }
            } catch (Throwable var17) {
               if (var2 != null) {
                  try {
                     var2.close();
                  } catch (Throwable var10) {
                     var17.addSuppressed(var10);
                  }
               }

               throw var17;
            }

            if (var2 != null) {
               var2.close();
            }

            for (Integer var23 : this.stocksOfGroup.keySet()) {
               List var25 = this.stocksOfGroup.get(var23);
               long var26 = var25.stream().filter(var0 -> var0.isActive()).count();
               BasketDto var27 = this.groupsMap.get(var23);
               var27.setCount((int)var26);
               this.groupsMap.get(var23).setTotal(var25.size());
            }
         } catch (Throwable var18) {
            if (var1 != null) {
               try {
                  var1.close();
               } catch (Throwable var9) {
                  var18.addSuppressed(var9);
               }
            }

            throw var18;
         }

         if (var1 != null) {
            var1.close();
         }
      } catch (Exception var19) {
         Log.error("", var19);
         throw new RuntimeException(L.t("Error while reading data", new Object[0]));
      }
   }

   public synchronized List<BasketDto> getGroups() {
      if (this.groupsMap == null) {
         this.performLoadGroups();
      }

      return this.groupsMap.values().stream().collect(Collectors.toList());
   }

   public BasketDto getSystemBasket(String var1) {
      try {
         Connection var2 = this.getConnection();

         BasketDto var6;
         label74: {
            Object var12;
            try {
               PreparedStatement var3 = var2.prepareStatement("SELECT * FROM STOCK_GROUP where NAME=? AND SYSTEM=?");

               label76: {
                  try {
                     var3.setString(1, var1);
                     var3.setBoolean(2, true);
                     ResultSet var4 = var3.executeQuery();
                     if (!var4.next()) {
                        break label76;
                     }

                     BasketDto var5 = new BasketDto();
                     var5.setId(var4.getInt("ID"));
                     var5.setName(var4.getString("NAME"));
                     var5.setDesc(var4.getString("DESC"));
                     var5.setSystem(var4.getBoolean("SYSTEM"));
                     var6 = var5;
                  } catch (Throwable var9) {
                     if (var3 != null) {
                        try {
                           var3.close();
                        } catch (Throwable var8) {
                           var9.addSuppressed(var8);
                        }
                     }

                     throw var9;
                  }

                  if (var3 != null) {
                     var3.close();
                  }
                  break label74;
               }

               if (var3 != null) {
                  var3.close();
               }

               var12 = null;
            } catch (Throwable var10) {
               if (var2 != null) {
                  try {
                     var2.close();
                  } catch (Throwable var7) {
                     var10.addSuppressed(var7);
                  }
               }

               throw var10;
            }

            if (var2 != null) {
               var2.close();
            }

            return (BasketDto)var12;
         }

         if (var2 != null) {
            var2.close();
         }

         return var6;
      } catch (Exception var11) {
         Log.error("", var11);
         throw new RuntimeException(L.t("Error while reading data", new Object[0]));
      }
   }

   public BasketDto getBasket(Integer var1) {
      if (var1 < 0) {
         return this.customGroupsMap.get(var1);
      }

      if (this.groupsMap == null) {
         this.performLoadGroups();
      }

      return this.groupsMap.get(var1);
   }

   private BasketDto toGroupDto(ResultSet var1) throws SQLException {
      BasketDto var2 = new BasketDto();
      var2.setId(var1.getInt("ID"));
      var2.setName(var1.getString("NAME"));
      var2.setDesc(var1.getString("DESC"));
      var2.setSystem(var1.getBoolean("SYSTEM"));
      var2.setCount(0);
      return var2;
   }

   private StockDto toStockDto(ResultSet var1) throws SQLException {
      StockDto var2 = new StockDto();
      var2.setId(var1.getInt("ID"));
      var2.setTicker(var1.getString("TICKER"));
      var2.setDateFrom(var1.getLong("DATE_FROM"));
      var2.setDateTo(var1.getLong("DATE_TO"));
      return var2;
   }

   private void checkDataAliases() {
      try {
         List var1 = getInstance().getGroups();

         for (int var2 = 0; var2 < var1.size(); var2++) {
            BasketDto var3 = (BasketDto)var1.get(var2);
            DataManager.createGroupAlias(var3.getName(), var3.getId());
         }
      } catch (Exception var4) {
         Log.error("Error while checking Data aliases", var4);
      }
   }

   public BasketDto createCustomGroup(String var1, int var2, String var3, boolean var4, String var5) throws Exception {
      try {
         if (var5 == null) {
            var5 = "local";
         }

         BasketDto var6 = new BasketDto();
         var6.setId(var2);
         var6.setName("group=" + var1);
         var6.setDesc(String.format("userID=%s, symbols=%s", var5, var3));
         List var7;
         if (var4) {
            var7 = new ArrayList();
            StockDto var8 = new StockDto();
            var8.setId(0);
            var8.setTicker(var3);
            var8.setDateFrom(-1L);
            var8.setDateTo(-1L);
            var7.add(var8);
         } else {
            var7 = getInstance().readStocksFromCsv(var3, "");
         }

         getInstance().saveCustomStocks(var6.getId(), var7);
         this.customGroupsMap.put(var2, var6);
         return var6;
      } catch (Exception var9) {
         throw new Exception("Faile to create custom group - " + var9.getMessage(), var9);
      }
   }

   public static int getNumberOfSymbolsInGroup(Set<String> var0, BasketDto var1) {
      List var2 = getInstance().getStocks(var1.getId());
      int var3 = 0;

      for (StockDto var5 : var2) {
         String var6 = var5.getTicker();
         if (var0.contains(var6) || var0.contains(var5.getTicker())) {
            var3++;
         }
      }

      return var3;
   }

   public static int getDownloaded(Map<String, DataInfo> var0, Integer var1) {
      List var2 = getInstance().getStocks(var1);
      int var3 = 0;

      for (StockDto var5 : var2) {
         String var6 = var5.getTicker();
         DataInfo var7 = (DataInfo)var0.get(var6);
         if (var7 == null) {
            var7 = (DataInfo)var0.get(var5.getTicker());
         }

         if (var7 != null && var7.dateTo > 0L) {
            var3++;
         }
      }

      return var3;
   }

   public static boolean isReadyForUse(int var0, int var1) {
      return var0 >= var1 - 3;
   }

   private class MinMax {
      public long min;
      public long max;

      private MinMax() {
      }
   }
}
