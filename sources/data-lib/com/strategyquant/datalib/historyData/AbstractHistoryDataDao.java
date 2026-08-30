package com.strategyquant.datalib.historyData;

import com.strategyquant.datalib.historyData.dto.AliasDto;
import com.strategyquant.datalib.historyData.dto.TickerDto;
import com.strategyquant.model.IdName;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.h2.jdbcx.JdbcConnectionPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractHistoryDataDao {
   public static final Logger Log = LoggerFactory.getLogger(AbstractHistoryDataDao.class);
   private static final String SQL_GET_ALL_MARKETS = "select id, code from market order by code";
   private static final String SQL_GET_ALL_TICKERS = "select t.timeframe,t.id,t.ticker,t.name,t.market_id,m.code,t.date_from,t.date_to,t.date_from,t.date_to,t.name,commodity_id tickerName,type.name typeName,alias_ticker_id from ticker as t left outer join market as m on m.id=t.market_id left outer join ticker_type as type on type.id=t.type_id";
   private JdbcConnectionPool pool;
   private Map<String, String> tickersExchnageMap = null;

   public AbstractHistoryDataDao(String var1, String var2, String var3) {
      this.pool = JdbcConnectionPool.create("jdbc:h2:file:" + var3, var1, var2);
   }

   public void createIndexes() throws SQLException {
      this.createIndex("CREATE INDEX IDX_MARKET_CODE ON MARKET (code)");
      this.createIndex("CREATE INDEX IDX_TICKER_TICKER ON TICKER (TICKER)");
      this.createIndex("CREATE INDEX IDX_TICKER_NAME ON TICKER (NAME)");
      this.createIndex("CREATE INDEX IDX_TICKER_MARKET_ID ON TICKER (MARKET_ID)");
      this.createIndex("CREATE INDEX IDX_TICKER_MARKET_TICKER_ID ON TICKER_MARKET (TICKER_ID)");
      this.createIndex("CREATE INDEX IDX_TICKER_MARKET_MARKET_ID ON TICKER_MARKET (MARKET_ID)");
      this.createIndex("CREATE INDEX IDX_ALIAS_ALIAS ON ALIAS(ALIAS)");
      this.createForeignKey("ALTER TABLE TICKER ADD FOREIGN KEY (TYPE_ID) REFERENCES TICKER_TYPE(ID)");
      this.createForeignKey("ALTER TABLE TICKER_PROPERTIES ADD FOREIGN KEY (TICKER_ID) REFERENCES TICKER(ID)");
      this.createForeignKey("ALTER TABLE TICKER_MARKET ADD FOREIGN KEY (MARKET_ID) REFERENCES MARKET(ID)");
      this.createForeignKey("ALTER TABLE TICKER_MARKET ADD FOREIGN KEY (TICKER_ID) REFERENCES TICKER(ID)");
   }

   public List<AliasDto> getAliasesForAlias(Collection<String> var1) throws SQLException {
      Connection var2 = this.getConnection();

      LinkedList var15;
      try {
         Statement var3 = var2.createStatement();

         try {
            String var4 = this.getInStatement("upper(alias)", var1, false);
            String var5 = "select original, alias from alias where " + var4;
            ResultSet var6 = var3.executeQuery(var5);
            LinkedList var7 = new LinkedList();

            while (var6.next()) {
               String var8 = var6.getString("original");
               String var9 = var6.getString("alias");
               AliasDto var10 = new AliasDto();
               var7.add(var10);
               var10.setAlias(var9);
               var10.setOriginal(var8);
            }

            var15 = var7;
         } catch (Throwable var13) {
            if (var3 != null) {
               try {
                  var3.close();
               } catch (Throwable var12) {
                  var13.addSuppressed(var12);
               }
            }

            throw var13;
         }

         if (var3 != null) {
            var3.close();
         }
      } catch (Throwable var14) {
         if (var2 != null) {
            try {
               var2.close();
            } catch (Throwable var11) {
               var14.addSuppressed(var11);
            }
         }

         throw var14;
      }

      if (var2 != null) {
         var2.close();
      }

      return var15;
   }

   public Map<String, String> getAliasesMapForAlias(Collection<String> var1) throws SQLException {
      try {
         List var2 = this.getAliasesForAlias(var1);
         HashMap var3 = new HashMap();

         for (AliasDto var5 : var2) {
            var3.put(var5.getOriginal(), var5.getAlias());
         }

         return var3;
      } catch (Exception var6) {
         Log.error("Error while loading aliases", var6);
         return new HashMap<>();
      }
   }

   public Date getChangeDate(String var1) {
      try {
         Connection var2 = this.getConnection();

         Date var5;
         label79: {
            try {
               PreparedStatement var3 = var2.prepareStatement("select change_date from ticker where ticker=?");

               label74: {
                  try {
                     var3.setString(1, var1);
                     ResultSet var4 = var3.executeQuery();
                     if (!var4.next()) {
                        break label74;
                     }

                     var5 = var4.getDate("change_date");
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
                  break label79;
               }

               if (var3 != null) {
                  var3.close();
               }
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

            return null;
         }

         if (var2 != null) {
            var2.close();
         }

         return var5;
      } catch (Exception var10) {
         Log.error("Error while checking change_date value", var10);
         return null;
      }
   }

   public Map<String, Date> getChangeDate() {
      HashMap var1 = new HashMap();

      try {
         Connection var2 = this.getConnection();

         try {
            PreparedStatement var3 = var2.prepareStatement("select ticker, change_date from ticker");

            try {
               ResultSet var4 = var3.executeQuery();

               while (var4.next()) {
                  var1.put(var4.getString("ticker"), var4.getDate("change_date"));
               }
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
      } catch (Exception var10) {
         Log.error("Error while checking change_date value", var10);
      }

      return var1;
   }

   public void dispose() {
      this.pool.dispose();
   }

   protected void createForeignKey(String var1) throws SQLException {
      this.sqlCommand(var1);
   }

   protected void createIndex(String var1) throws SQLException {
      this.sqlCommand(var1);
   }

   private void sqlCommand(String var1) throws SQLException {
      Connection var2 = this.getConnection();

      try {
         Statement var3 = var2.createStatement();

         try {
            var3.executeUpdate(var1);
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
   }

   protected Connection getConnection() throws SQLException {
      return this.pool.getConnection();
   }

   public List<IdName> getAllMarkets() throws SQLException {
      Connection var1 = this.getConnection();

      LinkedList var11;
      try {
         PreparedStatement var2 = var1.prepareStatement("select id, code from market order by code");

         try {
            ResultSet var3 = var2.executeQuery();
            LinkedList var4 = new LinkedList();

            while (var3.next()) {
               Long var5 = var3.getLong("id");
               String var6 = var3.getString("code");
               var4.add(new IdName(var5, var6));
            }

            var11 = var4;
         } catch (Throwable var9) {
            if (var2 != null) {
               try {
                  var2.close();
               } catch (Throwable var8) {
                  var9.addSuppressed(var8);
               }
            }

            throw var9;
         }

         if (var2 != null) {
            var2.close();
         }
      } catch (Throwable var10) {
         if (var1 != null) {
            try {
               var1.close();
            } catch (Throwable var7) {
               var10.addSuppressed(var7);
            }
         }

         throw var10;
      }

      if (var1 != null) {
         var1.close();
      }

      return var11;
   }

   protected String getTickerSql(String var1, TickerFilterDto var2, Set<String> var3) throws SQLException {
      StringBuilder var4 = new StringBuilder(var1);
      boolean var5 = false;
      if (var2.getMarketId() != null && !var2.getMarketId().equals(0L)) {
         var4.append(" where t.market_id=" + var2.getMarketId());
         var5 = true;
      }

      boolean var6 = var2.getNames() != null && var2.getNames().length > 0;
      if (var6) {
         var4.append(var5 ? " and " : " where ");
         var4.append("(");
         if (!var2.isExactMatch()) {
            if (var2.isSearchInTicker()) {
               String[] var9 = var2.getNames();
               if (!var3.isEmpty()) {
                  LinkedList var8 = new LinkedList<>(Arrays.asList(var9));
                  var8.addAll(var3);
                  var9 = var8.toArray(new String[0]);
               }

               var4.append(this.getInStatement("upper(t.ticker)", var9, true));
            }

            if (var2.isSearchInName() && var2.isSearchInTicker()) {
               var4.append(" OR ");
            }

            if (var2.isSearchInName()) {
               var4.append(this.getInStatement("upper(t.name)", var2.getNames(), true));
            }
         } else {
            var4.append("t.ticker in(");

            for (int var7 = 0; var7 < var2.getNames().length; var7++) {
               var4.append("'" + var2.getNames()[var7].trim() + "'");
               if (var7 != var2.getNames().length - 1) {
                  var4.append(",");
               }
            }

            var4.append(")");
         }

         var4.append(")");
      }

      if (!var5 && !var6) {
         var4.append(" where 1=1 ");
      }

      this.appendTickersSql(var4, var2);
      var4.append(" order by t.ticker");
      return var4.toString();
   }

   protected void appendTickersSql(StringBuilder var1, TickerFilterDto var2) {
   }

   protected String getInStatement(String var1, String[] var2, boolean var3) {
      StringBuilder var4 = new StringBuilder();
      var4.append("(");

      for (int var5 = 0; var5 < var2.length; var5++) {
         if (var4.length() > 1) {
            var4.append(" OR ");
         }

         if (var3) {
            var4.append(var1 + " like '%" + var2[var5].toUpperCase().trim() + "%'");
         } else {
            var4.append(var1 + " = '" + var2[var5].toUpperCase().trim() + "'");
         }
      }

      var4.append(")");
      return var4.toString();
   }

   protected String getInStatement(String var1, Collection<String> var2, boolean var3) {
      StringBuilder var4 = new StringBuilder();
      var4.append("(");

      for (String var6 : var2) {
         if (var4.length() > 1) {
            var4.append(" OR ");
         }

         if (var3) {
            var4.append(var1 + " like '%" + var6.toUpperCase().trim() + "%'");
         } else {
            var4.append(var1 + " = '" + var6.toUpperCase().trim() + "'");
         }
      }

      var4.append(")");
      return var4.toString();
   }

   private TickerDto getTicker(ResultSet var1) throws SQLException {
      TickerDto var2 = new TickerDto();
      var2.setId(var1.getLong("id"));
      var2.setTicker(var1.getString("ticker"));
      var2.setName(var1.getString("name"));
      var2.setMarketId(var1.getLong("market_id"));
      var2.setMarketName(var1.getString("code"));
      var2.setDateFrom(var1.getDate("date_from"));
      var2.setDateTo(var1.getDate("date_to"));
      var2.setType(var1.getString("typeName"));
      var2.setCommodityId(var1.getLong("commodity_id"));
      String var3 = var1.getString("timeframe");
      var2.setEod(var3.equals("D"));
      if (var1.wasNull()) {
         var2.setCommodityId(null);
      }

      var2.setAliasTickerId(var1.getLong("alias_ticker_id"));
      if (var1.wasNull()) {
         var2.setAliasTickerId(null);
      }

      return var2;
   }

   public TickerDto getTicker(String var1, String var2) throws SQLException {
      Connection var3 = this.getConnection();

      TickerDto var6;
      label73: {
         try {
            PreparedStatement var4 = var3.prepareStatement(var1 + " where t.ticker=?");

            label75: {
               try {
                  var4.setString(1, var2);
                  ResultSet var5 = var4.executeQuery();
                  if (!var5.next()) {
                     var6 = null;
                     break label75;
                  }

                  var6 = this.getTicker(var5);
               } catch (Throwable var9) {
                  if (var4 != null) {
                     try {
                        var4.close();
                     } catch (Throwable var8) {
                        var9.addSuppressed(var8);
                     }
                  }

                  throw var9;
               }

               if (var4 != null) {
                  var4.close();
               }
               break label73;
            }

            if (var4 != null) {
               var4.close();
            }
         } catch (Throwable var10) {
            if (var3 != null) {
               try {
                  var3.close();
               } catch (Throwable var7) {
                  var10.addSuppressed(var7);
               }
            }

            throw var10;
         }

         if (var3 != null) {
            var3.close();
         }

         return var6;
      }

      if (var3 != null) {
         var3.close();
      }

      return var6;
   }

   public TickerDto getTicker(String var1) throws SQLException {
      return this.getTicker(
         "select t.timeframe,t.id,t.ticker,t.name,t.market_id,m.code,t.date_from,t.date_to,t.date_from,t.date_to,t.name,commodity_id tickerName,type.name typeName,alias_ticker_id from ticker as t left outer join market as m on m.id=t.market_id left outer join ticker_type as type on type.id=t.type_id",
         var1
      );
   }

   public TickerDto getTicker(String var1, Long var2) throws SQLException {
      Connection var3 = this.getConnection();

      TickerDto var6;
      label73: {
         try {
            PreparedStatement var4 = var3.prepareStatement(var1 + " where t.id=?");

            label75: {
               try {
                  var4.setLong(1, var2);
                  ResultSet var5 = var4.executeQuery();
                  if (!var5.next()) {
                     var6 = null;
                     break label75;
                  }

                  var6 = this.getTicker(var5);
               } catch (Throwable var9) {
                  if (var4 != null) {
                     try {
                        var4.close();
                     } catch (Throwable var8) {
                        var9.addSuppressed(var8);
                     }
                  }

                  throw var9;
               }

               if (var4 != null) {
                  var4.close();
               }
               break label73;
            }

            if (var4 != null) {
               var4.close();
            }
         } catch (Throwable var10) {
            if (var3 != null) {
               try {
                  var3.close();
               } catch (Throwable var7) {
                  var10.addSuppressed(var7);
               }
            }

            throw var10;
         }

         if (var3 != null) {
            var3.close();
         }

         return var6;
      }

      if (var3 != null) {
         var3.close();
      }

      return var6;
   }

   public TickerDto getTicker(Long var1) throws SQLException {
      return this.getTicker(
         "select t.timeframe,t.id,t.ticker,t.name,t.market_id,m.code,t.date_from,t.date_to,t.date_from,t.date_to,t.name,commodity_id tickerName,type.name typeName,alias_ticker_id from ticker as t left outer join market as m on m.id=t.market_id left outer join ticker_type as type on type.id=t.type_id",
         var1
      );
   }

   public final List<TickerDto> getTickers(String var1, TickerFilterDto var2) throws SQLException {
      String[] var3 = var2.getNames();
      if (var3 == null) {
         var3 = new String[0];
      }

      Connection var4 = this.getConnection();

      LinkedList var16;
      try {
         Statement var5 = var4.createStatement();

         try {
            boolean var6 = !var2.isExactMatch() && var2.isSearchInTicker();
            Map var7 = var6 && var3.length > 0 ? this.getAliasesMapForAlias(Arrays.asList(var3)) : new HashMap();
            String var8 = this.getTickerSql(var1, var2, var7.keySet());
            if (var2.getNames() != null && var2.getNames().length == 1 && var2.getNames()[0].equals("EVERYTHING")) {
               var8 = var1;
            }

            ResultSet var9 = var5.executeQuery(var8);
            LinkedList var10 = new LinkedList();

            while (var9.next()) {
               TickerDto var11 = this.getTicker(var9);
               var10.add(var11);
            }

            this.modifyTickers(var10, var7);
            var16 = var10;
         } catch (Throwable var14) {
            if (var5 != null) {
               try {
                  var5.close();
               } catch (Throwable var13) {
                  var14.addSuppressed(var13);
               }
            }

            throw var14;
         }

         if (var5 != null) {
            var5.close();
         }
      } catch (Throwable var15) {
         if (var4 != null) {
            try {
               var4.close();
            } catch (Throwable var12) {
               var15.addSuppressed(var12);
            }
         }

         throw var15;
      }

      if (var4 != null) {
         var4.close();
      }

      return var16;
   }

   public List<TickerDto> getTickers(TickerFilterDto var1) throws SQLException {
      return this.getTickers(
         "select t.timeframe,t.id,t.ticker,t.name,t.market_id,m.code,t.date_from,t.date_to,t.date_from,t.date_to,t.name,commodity_id tickerName,type.name typeName,alias_ticker_id from ticker as t left outer join market as m on m.id=t.market_id left outer join ticker_type as type on type.id=t.type_id",
         var1
      );
   }

   protected void updateNameByTimeframe(TickerDto var1) {
      if (var1.isEod()) {
         var1.setName(var1.getName() + " - [Daily]");
      }
   }

   protected void modifyTickers(List<TickerDto> var1, Map<String, String> var2) throws SQLException {
      for (TickerDto var4 : var1) {
         this.updateNameByTimeframe(var4);
         String var5 = (String)var2.get(var4.getTicker());
         if (var5 != null) {
            var4.setName(var4.getName() + " (" + var5 + ")");
         }
      }
   }

   public synchronized String getExchangeForTicker(String var1) throws SQLException {
      if (this.tickersExchnageMap == null) {
         this.tickersExchnageMap = new HashMap<>();
         List var2 = this.getTickers(new TickerFilterDto());
         var2.stream().forEach(var1x -> this.tickersExchnageMap.put(var1x.getTicker(), var1x.getMarketName()));
      }

      return var1 == null ? null : this.tickersExchnageMap.get(var1);
   }
}
