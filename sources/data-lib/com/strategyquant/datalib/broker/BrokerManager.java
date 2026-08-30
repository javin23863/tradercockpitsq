package com.strategyquant.datalib.broker;

import com.strategyquant.datalib.basket.BasketBrokerDev;
import com.strategyquant.datalib.data.DataDb;
import com.strategyquant.lib.L;
import com.strategyquant.lib.SQTime;
import com.strategyquant.lib.app.MainApp;
import com.strategyquant.lib.constants.SQPaths;
import com.strategyquant.lib.utils.Pair;
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
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BrokerManager extends DataDb {
   public static final String DEFAULT_BROKER = "SQ default";
   private static final Logger Log = LoggerFactory.getLogger(BrokerManager.class);
   public static final String NO_BROKER = "No filter";
   private static final String BROKERS_VERSION = "brokers.version";
   private static final String version = "v2";
   private static final String insertBrokerSQL = "INSERT INTO BROKER (NAME, DESC, SYSTEM, MT_USE, MT_TIMEZONE, POSTFIX, STOCKPICKER_USE)values(?,?,?,?,?,?,?)";
   private static final String updateBrokerSQL = "UPDATE BROKER set NAME=?, DESC=?, MT_USE=?, MT_TIMEZONE=?, POSTFIX=?, STOCKPICKER_USE=? where ID=?";
   private static final String insertStockSql = "INSERT INTO BROKER_STOCK (BROKER_ID, TICKER) values(?,?)";
   private LinkedHashMap<Integer, BrokerDto> brokerMap;
   private Map<Integer, List<String>> stocksOfBroker;
   private static BrokerManager instance = null;
   private static String url;
   private static String version_url;

   public static void init(String var0) throws Exception {
      if (instance != null) {
         throw new Exception("BrokerManager.init() called more than once!");
      }

      instance = new BrokerManager(var0);
      String var1 = BasketBrokerDev.getInstance().isDev() ? "_dev" : "";
      if (MainApp.isBrazilianEdition()) {
         url = "http://autoupdate.strategyquant.com/brokers_br_v2" + var1 + "/brokers.zip";
         version_url = "http://autoupdate.strategyquant.com/brokers_br_v2" + var1 + "/brokers.version";
      } else {
         url = "http://autoupdate.strategyquant.com/brokers_v2" + var1 + "/brokers.zip";
         version_url = "http://autoupdate.strategyquant.com/brokers_v2" + var1 + "/brokers.version";
      }
   }

   public void synhronizeAsync(final Runnable var1) {
      boolean var2 = MainApp.runInConsole() && !MainApp.CLIWebServeActive;
      if (!var2) {
         new Thread(new Runnable() {
            @Override
            public void run() {
               try {
                  BrokerManager.instance.sync(var1);
               } catch (Throwable var2x) {
                  BrokerManager.Log.error("Error while sync stock group definition", var2x);
               }
            }
         }).start();
      }
   }

   private BrokerManager(String var1) {
      super(var1);
   }

   public static BrokerManager getInstance() {
      return instance;
   }

   private void sync(Runnable var1) throws Exception {
      long var2 = System.currentTimeMillis();
      String var4 = this.getNewAvailableVersion();
      if (var4 != null) {
         byte[] var5 = this.download(url);
         Pair var6 = this.unzip(var5);
         String var7 = (String)var6.getA();
         if (var7 == null) {
            return;
         }

         List var8 = this.parse(var7);
         Map var9 = (Map)var6.getB();
         Map var10 = this.getSystemBrokersByName();

         for (BrokerDto var12 : var8) {
            try {
               Log.debug("Syncing broker: {}", var12.getName());
               if (!var12.isMtUse() && MainApp.checkProduct("QDM")) {
                  Log.info("Skipping broker: {}", var12.getName());
               } else {
                  String var13 = (String)var9.get(var12.getName());
                  BrokerDto var14 = (BrokerDto)var10.get(var12.getName());
                  if (var14 != null) {
                     var12.setId(var14.getId());
                  } else {
                     var12.setSystem(true);
                  }

                  this.saveBroker(var12);
                  if (var13 != null) {
                     this.importFromCsv(var13, var12.getId(), "");
                  } else {
                     this.saveStocks(var12.getId(), new LinkedList<>());
                  }
               }
            } catch (Exception var15) {
               Log.error("Syncing broker: " + var12.getName() + " failed", var15);
            }
         }

         this.saveLatestVersion(var4);
         this.performLoadBrokers();
         if (var1 != null) {
            var1.run();
         }
      }

      long var16 = System.currentTimeMillis() - var2;
      Log.info("Syncing brokers finished in {}ms", var16);
   }

   private List<BrokerDto> parse(String var1) {
      JSONArray var2 = new JSONArray(var1);
      LinkedList var3 = new LinkedList();

      for (int var4 = 0; var4 < var2.length(); var4++) {
         JSONObject var5 = var2.getJSONObject(var4);
         BrokerDto var6 = new BrokerDto();
         var3.add(var6);
         var6.setName(var5.getString("name"));
         var6.setDesc(var5.getString("description"));
         var6.setStockPickerUse(var5.getBoolean("spUse"));
         var6.setMtUse(var5.getBoolean("mtUse"));
         if (var6.isMtUse()) {
            var6.setPostfix(var5.getString("postfix"));
            var6.setMtTimezone(var5.getString("timezone"));
         }
      }

      return var3;
   }

   private void saveLatestVersion(String var1) throws IOException {
      Log.debug("Updating data version file");
      File var2 = new File(SQPaths.dataDirPath, "brokers.version");
      Files.write(var2.toPath(), var1.getBytes(), StandardOpenOption.CREATE);
   }

   private Pair unzip(byte[] var1) throws IOException {
      HashMap var2 = new HashMap();
      String var3 = null;
      byte[] var4 = new byte[4096];
      ZipInputStream var5 = new ZipInputStream(new ByteArrayInputStream(var1));

      try {
         for (ZipEntry var6 = var5.getNextEntry(); var6 != null; var6 = var5.getNextEntry()) {
            String var7 = var6.getName();
            if (var7.endsWith(".csv")) {
               ByteArrayOutputStream var15 = new ByteArrayOutputStream();

               int var16;
               while ((var16 = var5.read(var4)) > 0) {
                  var15.write(var4, 0, var16);
               }

               var15.close();
               byte[] var17 = var15.toByteArray();
               String var11 = var7.substring(0, var7.length() - 4);
               var2.put(var11, new String(var17));
            } else if (var7.equals("config.json")) {
               ByteArrayOutputStream var8 = new ByteArrayOutputStream();

               int var9;
               while ((var9 = var5.read(var4)) > 0) {
                  var8.write(var4, 0, var9);
               }

               var8.close();
               byte[] var10 = var8.toByteArray();
               var3 = new String(var10);
            }
         }

         return new Pair(var3, var2);
      } finally {
         var5.closeEntry();
         var5.close();
      }
   }

   private String getCurrentDownloadedVersion() throws IOException {
      File var1 = new File(SQPaths.dataDirPath, "brokers.version");
      return !var1.exists() ? null : new String(Files.readAllBytes(var1.toPath()));
   }

   private String getNewAvailableVersion() throws IOException {
      String var1 = this.getCurrentDownloadedVersion();
      Log.debug("Current version file: {}", var1);
      byte[] var2 = this.download(version_url + "?time=" + new Date().getTime());
      if (var2 == null) {
         return null;
      }

      String var3 = new String(var2);
      Log.debug("Available version file: {}", var3);
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

   public List<BrokerStockDto> readStocksFromCsv(String var1, String var2) throws Exception {
      String[] var3 = var1.split("\n");
      ArrayList var4 = new ArrayList(var3.length);

      for (String var8 : var3) {
         var8 = var8.trim();
         if (!var8.isEmpty()) {
            String[] var9 = var8.split(";");
            BrokerStockDto var10 = new BrokerStockDto();
            var10.setTicker(var2 + var9[0]);
            var4.add(var10);
         }
      }

      return var4;
   }

   private long parseDate(String var1) {
      try {
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
      List var4 = this.readStocksFromCsv(var1, var3);
      BrokerDto var5 = this.getBroker(var2);
      if (var5 == null) {
         throw new Exception("Broker was not found.");
      }

      this.saveStocks(var2, var4);
   }

   public List<BrokerStockDto> updateStocksFromStr(String var1, Integer var2, String var3) throws Exception {
      List var4 = this.readStocksFromCsv(var1, var3);
      BrokerDto var5 = this.getBroker(var2);
      if (var5 == null) {
         throw new Exception("Broker was not found.");
      }

      this.saveStocks(var2, var4);
      List var6 = this.stocksOfBroker.get(var5.getId());
      long var7 = var6 == null ? 0L : var6.size();
      var5.setCustomizedStocks((int)var7);
      return var4;
   }

   @Override
   public void initDatabase() {
      try {
         if (!this.tableExists("BROKER")) {
            String var1 = "CREATE TABLE BROKER (ID INTEGER PRIMARY KEY AUTOINCREMENT, NAME VARCHAR(50) NOT NULL,SYSTEM BOOLEAN NOT NULL,DESC VARCHAR(250))";
            this.sqlCommand(var1);
            Log.debug("Broker table created successfully");
         }

         if (!this.tableExists("BROKER_STOCK")) {
            String var3 = "CREATE TABLE BROKER_STOCK (ID INTEGER PRIMARY KEY AUTOINCREMENT, TICKER VARCHAR(50) NOT NULL,BROKER_ID INTEGER NOT NULL,FOREIGN KEY(BROKER_ID) REFERENCES BROKER(ID))";
            this.sqlCommand(var3);
            Log.debug("Broker's stock table created successfully");
         }

         if (!this.columnExists("BROKER", "STOCKPICKER_USE")) {
            String var4 = "ALTER TABLE BROKER ADD STOCKPICKER_USE INTEGER DEFAULT 0";
            this.sqlCommand(var4);
         }

         if (!this.columnExists("BROKER", "MT_USE")) {
            String var5 = "ALTER TABLE BROKER ADD MT_USE INTEGER DEFAULT 0";
            this.sqlCommand(var5);
         }

         if (!this.columnExists("BROKER", "MT_TIMEZONE")) {
            String var6 = "ALTER TABLE BROKER ADD MT_TIMEZONE VARCHAR(100)";
            this.sqlCommand(var6);
         }

         if (!this.columnExists("BROKER", "POSTFIX")) {
            String var7 = "ALTER TABLE BROKER ADD POSTFIX VARCHAR(100)";
            this.sqlCommand(var7);
         }
      } catch (Exception var2) {
         Log.error("DB error: Cannot create baskets table.", var2);
      }
   }

   public BrokerDto getBroker(String var1) {
      try {
         Connection var2 = this.getConnection();

         BrokerDto var5;
         label74: {
            Object var11;
            try {
               PreparedStatement var3 = var2.prepareStatement("SELECT * FROM BROKER where NAME=?");

               label76: {
                  try {
                     var3.setString(1, var1);
                     ResultSet var4 = var3.executeQuery();
                     if (!var4.next()) {
                        break label76;
                     }

                     var5 = this.toBrokerDto(var4);
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

            return (BrokerDto)var11;
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

   public synchronized void saveBroker(BrokerDto var1) throws Exception {
      boolean var2 = var1.getId() == null;
      if (var1.getName() == null) {
         throw new IllegalArgumentException("Broker's name must be set");
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
            BrokerDto var15 = this.getBroker(var3);
            if (var15 != null && !var15.getId().equals(var1.getId())) {
               throw new IllegalArgumentException("Name must be unique");
            }

            PreparedStatement var16 = var4.prepareStatement(
               "UPDATE BROKER set NAME=?, DESC=?, MT_USE=?, MT_TIMEZONE=?, POSTFIX=?, STOCKPICKER_USE=? where ID=?"
            );

            try {
               var16.setString(1, var3);
               if (var1.getDesc() == null) {
                  var16.setNull(2, 12);
               } else {
                  var16.setString(2, var1.getDesc());
               }

               var16.setInt(3, var1.isMtUse() ? 1 : 0);
               var16.setString(4, var1.getMtTimezone());
               var16.setString(5, var1.getPostfix());
               var16.setInt(6, var1.isStockPickerUse() ? 1 : 0);
               var16.setInt(7, var1.getId());
               var16.execute();
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
            if (!var1.isSystem() && this.getBroker(var3) != null) {
               throw new IllegalArgumentException("Name must be unique");
            }

            PreparedStatement var5 = var4.prepareStatement(
               "INSERT INTO BROKER (NAME, DESC, SYSTEM, MT_USE, MT_TIMEZONE, POSTFIX, STOCKPICKER_USE)values(?,?,?,?,?,?,?)", 1
            );

            try {
               var5.setString(1, var3);
               if (var1.getDesc() == null) {
                  var5.setNull(2, 12);
               } else {
                  var5.setString(2, var1.getDesc());
               }

               var5.setBoolean(3, var1.isSystem());
               var5.setInt(4, var1.isMtUse() ? 1 : 0);
               var5.setString(5, var1.getMtTimezone());
               var5.setString(6, var1.getPostfix());
               var5.setInt(7, var1.isStockPickerUse() ? 1 : 0);
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

         this.brokerMap.put(var1.getId(), var1);
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

      this.refresh();
   }

   public void saveStocks(int var1, List<BrokerStockDto> var2) {
      try {
         Connection var3 = this.getConnection();

         try {
            try {
               var3.setAutoCommit(false);
               PreparedStatement var4 = var3.prepareStatement("delete from broker_stock where broker_id=?");

               try {
                  var4.setInt(1, var1);
                  var4.executeUpdate();
                  this.stocksOfBroker.put(var1, new LinkedList<>());

                  for (BrokerStockDto var6 : var2) {
                     PreparedStatement var7 = var3.prepareStatement("INSERT INTO BROKER_STOCK (BROKER_ID, TICKER) values(?,?)");

                     try {
                        var7.setInt(1, var1);
                        var7.setString(2, var6.getTicker());
                        var7.executeUpdate();
                        this.stocksOfBroker.get(var1).add(var6.getTicker());
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
   }

   public List<String> getStocks(int var1) {
      if (this.stocksOfBroker == null) {
         this.performLoadBrokers();
      }

      List var2 = this.stocksOfBroker.get(var1);
      return var2 == null ? new LinkedList<>() : var2;
   }

   public synchronized BrokerDto deleteBroker(int var1, boolean var2) throws Exception {
      BrokerDto var3 = this.getBroker(var1);
      if (var3 == null) {
         throw new RuntimeException(L.t("Broker was not found.", new Object[0]));
      }

      if (!var2 && this.isUsedByMt(var1)) {
         throw new RuntimeException(L.t("Broker is used and can't be deleted.", new Object[0]));
      }

      this.sqlCommand("delete from broker_stock where broker_id=" + var1);
      this.sqlCommand("DELETE FROM broker WHERE ID=" + var1);
      this.brokerMap.remove(var1);
      this.stocksOfBroker.remove(var1);
      return var3;
   }

   public boolean isUsedByMt(int var1) throws SQLException {
      Connection var2 = this.getConnection();

      boolean var19;
      label163: {
         label164: {
            try {
               PreparedStatement var17;
               label170: {
                  label167: {
                     var17 = var2.prepareStatement("SELECT * FROM instruments where broker_id=?");

                     try {
                        var17.setInt(1, var1);
                        ResultSet var4 = var17.executeQuery();

                        label125: {
                           try {
                              if (var4.next()) {
                                 var19 = true;
                                 break label125;
                              }
                           } catch (Throwable var12) {
                              if (var4 != null) {
                                 try {
                                    var4.close();
                                 } catch (Throwable var11) {
                                    var12.addSuppressed(var11);
                                 }
                              }

                              throw var12;
                           }

                           if (var4 != null) {
                              var4.close();
                           }
                           break label167;
                        }

                        if (var4 != null) {
                           var4.close();
                        }
                     } catch (Throwable var13) {
                        if (var17 != null) {
                           try {
                              var17.close();
                           } catch (Throwable var10) {
                              var13.addSuppressed(var10);
                           }
                        }

                        throw var13;
                     }

                     if (var17 != null) {
                        var17.close();
                     }
                     break label163;
                  }

                  if (var17 != null) {
                     var17.close();
                  }

                  var17 = var2.prepareStatement("SELECT * FROM sessions where broker_id=?");

                  try {
                     var17.setInt(1, var1);
                     ResultSet var18 = var17.executeQuery();

                     label147: {
                        try {
                           if (var18.next()) {
                              var19 = true;
                              break label147;
                           }
                        } catch (Throwable var14) {
                           if (var18 != null) {
                              try {
                                 var18.close();
                              } catch (Throwable var9) {
                                 var14.addSuppressed(var9);
                              }
                           }

                           throw var14;
                        }

                        if (var18 != null) {
                           var18.close();
                        }
                        break label170;
                     }

                     if (var18 != null) {
                        var18.close();
                     }
                  } catch (Throwable var15) {
                     if (var17 != null) {
                        try {
                           var17.close();
                        } catch (Throwable var8) {
                           var15.addSuppressed(var8);
                        }
                     }

                     throw var15;
                  }

                  if (var17 != null) {
                     var17.close();
                  }
                  break label164;
               }

               if (var17 != null) {
                  var17.close();
               }
            } catch (Throwable var16) {
               if (var2 != null) {
                  try {
                     var2.close();
                  } catch (Throwable var7) {
                     var16.addSuppressed(var7);
                  }
               }

               throw var16;
            }

            if (var2 != null) {
               var2.close();
            }

            return false;
         }

         if (var2 != null) {
            var2.close();
         }

         return var19;
      }

      if (var2 != null) {
         var2.close();
      }

      return var19;
   }

   private Map<String, BrokerDto> getSystemBrokersByName() {
      return this.getAllBrokers().stream().filter(var0 -> var0.isSystem()).collect(Collectors.toMap(BrokerDto::getName, var0 -> (BrokerDto)var0));
   }

   public void refresh() {
      this.performLoadBrokers();
   }

   private synchronized void performLoadBrokers() {
      try {
         Connection var1 = this.getConnection();

         try {
            this.brokerMap = new LinkedHashMap<>();
            this.stocksOfBroker = new HashMap<>();
            Statement var2 = var1.createStatement();

            try {
               ResultSet var3 = var2.executeQuery("SELECT * FROM BROKER ORDER BY NAME");

               try {
                  while (var3.next()) {
                     BrokerDto var4 = this.toBrokerDto(var3);
                     this.brokerMap.put(var4.getId(), var4);
                  }
               } catch (Throwable var16) {
                  if (var3 != null) {
                     try {
                        var3.close();
                     } catch (Throwable var15) {
                        var16.addSuppressed(var15);
                     }
                  }

                  throw var16;
               }

               if (var3 != null) {
                  var3.close();
               }
            } catch (Throwable var17) {
               if (var2 != null) {
                  try {
                     var2.close();
                  } catch (Throwable var14) {
                     var17.addSuppressed(var14);
                  }
               }

               throw var17;
            }

            if (var2 != null) {
               var2.close();
            }

            var2 = var1.createStatement();

            try {
               ResultSet var30 = var2.executeQuery("SELECT * FROM BROKER_STOCK");

               try {
                  while (var30.next()) {
                     int var34 = var30.getInt("BROKER_ID");
                     List var5 = this.stocksOfBroker.get(var34);
                     if (var5 == null) {
                        var5 = new LinkedList();
                        this.stocksOfBroker.put(var34, var5);
                     }

                     BrokerStockDto var6 = this.toStockDto(var30);
                     var5.add(var6.getTicker());
                  }
               } catch (Throwable var18) {
                  if (var30 != null) {
                     try {
                        var30.close();
                     } catch (Throwable var13) {
                        var18.addSuppressed(var13);
                     }
                  }

                  throw var18;
               }

               if (var30 != null) {
                  var30.close();
               }
            } catch (Throwable var19) {
               if (var2 != null) {
                  try {
                     var2.close();
                  } catch (Throwable var12) {
                     var19.addSuppressed(var12);
                  }
               }

               throw var19;
            }

            if (var2 != null) {
               var2.close();
            }

            for (Integer var31 : this.stocksOfBroker.keySet()) {
               List var35 = this.stocksOfBroker.get(var31);
               this.brokerMap.get(var31).setCustomizedStocks(var35.size());
            }

            var2 = var1.createStatement();

            try {
               ResultSet var32 = var2.executeQuery("SELECT BROKER_ID, count(*) as CNT FROM instruments where broker_id<>-1 group by broker_id");

               try {
                  while (var32.next()) {
                     int var36 = var32.getInt("BROKER_ID");
                     int var38 = var32.getInt("CNT");
                     BrokerDto var40 = this.brokerMap.get(var36);
                     if (var40 != null) {
                        var40.setCustomizedInstruments(var38);
                     }
                  }
               } catch (Throwable var20) {
                  if (var32 != null) {
                     try {
                        var32.close();
                     } catch (Throwable var11) {
                        var20.addSuppressed(var11);
                     }
                  }

                  throw var20;
               }

               if (var32 != null) {
                  var32.close();
               }
            } catch (Throwable var21) {
               if (var2 != null) {
                  try {
                     var2.close();
                  } catch (Throwable var10) {
                     var21.addSuppressed(var10);
                  }
               }

               throw var21;
            }

            if (var2 != null) {
               var2.close();
            }

            var2 = var1.createStatement();

            try {
               ResultSet var33 = var2.executeQuery("SELECT BROKER_ID, count(*) as CNT FROM sessions where broker_id<>-1 group by broker_id");

               try {
                  while (var33.next()) {
                     int var37 = var33.getInt("BROKER_ID");
                     int var39 = var33.getInt("CNT");
                     BrokerDto var41 = this.brokerMap.get(var37);
                     if (var41 != null) {
                        var41.setCustomizedSessions(var39);
                     }
                  }
               } catch (Throwable var22) {
                  if (var33 != null) {
                     try {
                        var33.close();
                     } catch (Throwable var9) {
                        var22.addSuppressed(var9);
                     }
                  }

                  throw var22;
               }

               if (var33 != null) {
                  var33.close();
               }
            } catch (Throwable var23) {
               if (var2 != null) {
                  try {
                     var2.close();
                  } catch (Throwable var8) {
                     var23.addSuppressed(var8);
                  }
               }

               throw var23;
            }

            if (var2 != null) {
               var2.close();
            }
         } catch (Throwable var24) {
            if (var1 != null) {
               try {
                  var1.close();
               } catch (Throwable var7) {
                  var24.addSuppressed(var7);
               }
            }

            throw var24;
         }

         if (var1 != null) {
            var1.close();
         }
      } catch (Exception var25) {
         Log.error("", var25);
         throw new RuntimeException(L.t("Error while reading data", new Object[0]));
      }
   }

   public synchronized List<BrokerDto> getAllBrokers() {
      if (this.brokerMap == null) {
         this.performLoadBrokers();
      }

      return this.brokerMap.values().stream().collect(Collectors.toList());
   }

   public BrokerDto getSystemBroker(String var1) {
      try {
         Connection var2 = this.getConnection();

         BrokerDto var6;
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

                     BrokerDto var5 = new BrokerDto();
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

            return (BrokerDto)var12;
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

   public synchronized BrokerDto getBroker(Integer var1) {
      if (this.brokerMap == null) {
         this.performLoadBrokers();
      }

      return var1.equals(-1) ? this.getDefaultBroker() : this.brokerMap.get(var1);
   }

   public BrokerDto getDefaultBroker() {
      BrokerDto var1 = new BrokerDto();
      var1.setId(-1);
      var1.setName("SQ default");
      var1.setPostfix("");
      return var1;
   }

   public String getBrokerName(int var1) {
      BrokerDto var2 = getInstance().getBroker(var1);
      return var2 == null ? "SQ default" : var2.getName();
   }

   private BrokerDto toBrokerDto(ResultSet var1) throws SQLException {
      BrokerDto var2 = new BrokerDto();
      var2.setId(var1.getInt("ID"));
      var2.setName(var1.getString("NAME"));
      var2.setDesc(var1.getString("DESC"));
      var2.setSystem(var1.getBoolean("SYSTEM"));
      var2.setMtTimezone(var1.getString("MT_TIMEZONE"));
      var2.setMtUse(var1.getInt("MT_USE") == 1);
      var2.setStockPickerUse(var1.getInt("STOCKPICKER_USE") == 1);
      var2.setPostfix(var1.getString("POSTFIX"));
      return var2;
   }

   private BrokerStockDto toStockDto(ResultSet var1) throws SQLException {
      BrokerStockDto var2 = new BrokerStockDto();
      var2.setId(var1.getInt("ID"));
      var2.setTicker(var1.getString("TICKER"));
      return var2;
   }

   public String getBrokerDependantName(String var1, int var2) {
      if (var2 != -1) {
         BrokerDto var3 = getInstance().getBroker(var2);
         if (var3 == null) {
            throw new RuntimeException(L.t("Selected broker doesn't exists", new Object[0]));
         }

         var1 = var1 + var3.getPostfix();
      }

      return var1;
   }

   public boolean checkBrokerExists(String var1) {
      if (this.brokerMap == null) {
         this.performLoadBrokers();
      }

      for (BrokerDto var3 : this.brokerMap.values()) {
         if (var3.getName().equals(var1)) {
            return true;
         }
      }

      return false;
   }
}
