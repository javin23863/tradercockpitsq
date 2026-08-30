package com.strategyquant.datalib.historyData;

import com.strategyquant.datalib.historyData.dto.CommodityDto;
import com.strategyquant.datalib.historyData.dto.TickerDto;
import com.strategyquant.lib.app.MainApp;
import com.strategyquant.model.IdName;
import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class FuturesHistoryDataDao extends AbstractHistoryDataDao {
   public static final Long BMF_ID = 100000L;
   private static final String SQL_GET_ALL_BR_TICKERS = "select t.timeframe,t.id,t.ticker,t.name,100000 as market_id,'BMF' as code,t.date_from,t.date_to,t.date_from,t.date_to,t.name,commodity_id tickerName,type.name typeName,alias_ticker_id from br_ticker as t left outer join ticker_type as type on type.id=t.type_id ";
   private static final Map<String, String> MONTH_LETTERS = new HashMap<>();
   private static final Set<String> BR_SYMBOLS = new HashSet<>();

   public FuturesHistoryDataDao(String var1) {
      super("sq", "hE8+-%:b&]^c#[b#j;+=*R[N~hrtbn$", var1 + File.separator + "data_futures");
   }

   @Override
   public List<IdName> getAllMarkets() throws SQLException {
      List var1 = super.getAllMarkets();
      if (MainApp.isBrazilianEdition()) {
         var1.add(0, new IdName(BMF_ID, "BMF"));
      }

      return var1;
   }

   @Override
   public void createIndexes() throws SQLException {
      super.createIndexes();
      this.createIndex("CREATE INDEX IDX_COMMODITY_CODE ON COMMODITY (code)");
      this.createForeignKey("ALTER TABLE TICKER ADD FOREIGN KEY (COMMODITY_ID) REFERENCES COMMODITY(ID)");
      if (MainApp.isBrazilianEdition()) {
         this.createForeignKey("ALTER TABLE BR_TICKER ADD FOREIGN KEY (COMMODITY_ID) REFERENCES COMMODITY(ID)");
      }
   }

   public CommodityDto getCommodity(Long var1) throws SQLException {
      Connection var2 = this.getConnection();

      CommodityDto var6;
      label73: {
         CommodityDto var11;
         try {
            PreparedStatement var3 = var2.prepareStatement(
               "select id,name,code,point_value,tick_step,tick_Size,order_size_multi,order_size_step from commodity where id=?"
            );

            label75: {
               try {
                  var3.setLong(1, var1);
                  ResultSet var4 = var3.executeQuery();
                  if (!var4.next()) {
                     var11 = null;
                     break label75;
                  }

                  var11 = new CommodityDto();
                  var11.setId(var4.getLong("id"));
                  var11.setName(var4.getString("name"));
                  var11.setCode(var4.getString("code"));
                  var11.setPointValue(var4.getBigDecimal("point_value"));
                  var11.setTickStep(var4.getBigDecimal("tick_step"));
                  var11.setTickSize(var4.getBigDecimal("tick_size"));
                  var11.setOrderSizeMulti(var4.getBigDecimal("order_size_multi"));
                  var11.setOrderSizeStep(var4.getBigDecimal("order_size_step"));
                  var6 = var11;
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
               break label73;
            }

            if (var3 != null) {
               var3.close();
            }
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

         return var11;
      }

      if (var2 != null) {
         var2.close();
      }

      return var6;
   }

   @Override
   public List<TickerDto> getTickers(TickerFilterDto var1) throws SQLException {
      List var2 = super.getTickers(var1);
      boolean var3 = BMF_ID.equals(var1.getMarketId());
      if (MainApp.isBrazilianEdition() && (var1.getMarketId() == null || var1.getMarketId().equals(0L) || var3)) {
         var1.setMarketId(null);
         var1.setOnlyContFutures(false);
         List var4 = this.getTickers(
            "select t.timeframe,t.id,t.ticker,t.name,100000 as market_id,'BMF' as code,t.date_from,t.date_to,t.date_from,t.date_to,t.name,commodity_id tickerName,type.name typeName,alias_ticker_id from br_ticker as t left outer join ticker_type as type on type.id=t.type_id ",
            var1
         );
         if (var3) {
            return var4;
         }

         var4.addAll(var2);
         return var4;
      } else {
         return var2;
      }
   }

   private List<TickerDto> filterMainBr(List<TickerDto> var1) {
      LinkedList var2 = new LinkedList();

      for (TickerDto var4 : var1) {
         String var5 = var4.getTicker();
         if (var5.startsWith("@@")) {
            var5 = var5.substring(2);
         } else if (var5.startsWith("@")) {
            var5 = var5.substring(1);
         }

         if (var5.endsWith(".D")) {
            var5 = var5.substring(0, var5.length() - 2);
         }

         if (BR_SYMBOLS.contains(var5)) {
            var2.add(var4);
         }
      }

      return var2;
   }

   @Override
   public TickerDto getTicker(Long var1) throws SQLException {
      if (MainApp.isBrazilianEdition()) {
         TickerDto var2 = this.getTicker(
            "select t.timeframe,t.id,t.ticker,t.name,100000 as market_id,'BMF' as code,t.date_from,t.date_to,t.date_from,t.date_to,t.name,commodity_id tickerName,type.name typeName,alias_ticker_id from br_ticker as t left outer join ticker_type as type on type.id=t.type_id ",
            var1
         );
         if (var2 != null) {
            return var2;
         }
      }

      return super.getTicker(var1);
   }

   @Override
   public TickerDto getTicker(String var1) throws SQLException {
      if (MainApp.isBrazilianEdition()) {
         TickerDto var2 = this.getTicker(
            "select t.timeframe,t.id,t.ticker,t.name,100000 as market_id,'BMF' as code,t.date_from,t.date_to,t.date_from,t.date_to,t.name,commodity_id tickerName,type.name typeName,alias_ticker_id from br_ticker as t left outer join ticker_type as type on type.id=t.type_id ",
            var1
         );
         if (var2 != null) {
            return var2;
         }
      }

      return super.getTicker(var1);
   }

   @Override
   protected void modifyTickers(List<TickerDto> var1, Map<String, String> var2) throws SQLException {
      if (!var1.isEmpty()) {
         Set var3 = var1.stream().map(TickerDto::getCommodityId).collect(Collectors.toSet());
         List var4 = this.getCommodities(var3);
         Map var5 = var4.stream().collect(Collectors.toMap(CommodityDto::getId, CommodityDto::getCode));
         Map var6 = var4.stream().collect(Collectors.toMap(CommodityDto::getId, CommodityDto::getName));

         for (TickerDto var8 : var1) {
            if (var8.getName() == null && var8.getCommodityId() != null) {
               String var9 = (String)var6.get(var8.getCommodityId());
               var8.setName(var9);
            }

            if (MainApp.isBrazilianEdition()) {
               String var11 = var8.getName();
               String var10 = var8.getTicker();
               if (var10.endsWith("_D1")) {
                  var11 = var11 + " - Diario";
               } else {
                  var11 = var11 + " - 1 Minuto";
               }

               var8.setName(var11);
            } else {
               this.updateFundamentalName(var8);
               this.updateNameByTimeframe(var8);
               this.updateAliases(var8, var2, var5);
            }
         }
      }
   }

   private void updateAliases(TickerDto var1, Map<String, String> var2, Map<Long, String> var3) {
      String var4 = (String)var3.get(var1.getCommodityId());
      String var5 = (String)var2.get(var4);
      if (var5 != null) {
         var1.setName(var1.getName() + " (" + var5 + ")");
      }
   }

   @Override
   protected void appendTickersSql(StringBuilder var1, TickerFilterDto var2) {
      super.appendTickersSql(var1, var2);
      if (var2.isOnlyContFutures()) {
         var1.append("AND t.ticker like '@%' ");
      }
   }

   private void updateFundamentalName(TickerDto var1) {
      if (!var1.getTicker().startsWith("@")) {
         int var2 = var1.getTicker().length();
         String var3;
         if (var1.isEod()) {
            var3 = Character.toString(var1.getTicker().charAt(var2 - 5));
         } else {
            var3 = Character.toString(var1.getTicker().charAt(var2 - 3));
         }

         String var4 = MONTH_LETTERS.get(var3);
         if (var4 != null && var1.getName() != null) {
            try {
               String var5 = var1.isEod() ? var1.getTicker().substring(var2 - 4, var2 - 2) : var1.getTicker().substring(var2 - 2, var2);
               String var6 = var1.getName() + " [" + var4 + var5 + "]";
               var1.setName(var6);
            } catch (Exception var7) {
            }
         }
      }
   }

   public CommodityDto getCommodityForFuture(String var1) throws SQLException {
      Connection var2 = this.getConnection();

      CommodityDto var5;
      label73: {
         try {
            PreparedStatement var3 = var2.prepareStatement(
               "select c.id,c.name,c.code,c.point_value,c.tick_step,c.tick_Size,c.order_size_multi from ticker t join commodity c on c.id=t.commodity_id where t.ticker=?"
            );

            label75: {
               try {
                  var3.setString(1, var1);
                  ResultSet var4 = var3.executeQuery();
                  if (!var4.next()) {
                     var5 = null;
                     break label75;
                  }

                  var5 = this.getCommodity(var4);
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
               break label73;
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

         return var5;
      }

      if (var2 != null) {
         var2.close();
      }

      return var5;
   }

   private CommodityDto getCommodity(ResultSet var1) throws SQLException {
      CommodityDto var2 = new CommodityDto();
      var2.setId(var1.getLong("id"));
      var2.setName(var1.getString("name"));
      var2.setCode(var1.getString("code"));
      var2.setPointValue(var1.getBigDecimal("point_value"));
      var2.setTickStep(var1.getBigDecimal("tick_step"));
      var2.setTickSize(var1.getBigDecimal("tick_size"));
      var2.setOrderSizeMulti(var1.getBigDecimal("order_size_multi"));
      var2.setOrderSizeStep(var1.getBigDecimal("order_size_step"));
      return var2;
   }

   public List<CommodityDto> getCommoditiesForFutures(String[] var1) throws SQLException {
      LinkedList var2 = new LinkedList();
      if (var1.length == 0) {
         return var2;
      }

      Connection var3 = this.getConnection();

      try {
         Statement var4 = var3.createStatement();

         try {
            String var5 = this.getCommodityStatement("ticker", var1);
            ResultSet var6 = var4.executeQuery(var5);

            while (var6.next()) {
               var2.add(this.getCommodity(var6));
            }
         } catch (Throwable var11) {
            if (var4 != null) {
               try {
                  var4.close();
               } catch (Throwable var9) {
                  var11.addSuppressed(var9);
               }
            }

            throw var11;
         }

         if (var4 != null) {
            var4.close();
         }

         if (MainApp.isBrazilianEdition()) {
            var4 = var3.createStatement();

            try {
               String var14 = this.getCommodityStatement("br_ticker", var1);
               ResultSet var15 = var4.executeQuery(var14);

               while (var15.next()) {
                  var2.add(this.getCommodity(var15));
               }
            } catch (Throwable var10) {
               if (var4 != null) {
                  try {
                     var4.close();
                  } catch (Throwable var8) {
                     var10.addSuppressed(var8);
                  }
               }

               throw var10;
            }

            if (var4 != null) {
               var4.close();
            }
         }
      } catch (Throwable var12) {
         if (var3 != null) {
            try {
               var3.close();
            } catch (Throwable var7) {
               var12.addSuppressed(var7);
            }
         }

         throw var12;
      }

      if (var3 != null) {
         var3.close();
      }

      return var2;
   }

   public List<CommodityDto> getCommodities(Set<Long> var1) throws SQLException {
      LinkedList var2 = new LinkedList();
      if (var1.isEmpty()) {
         return var2;
      }

      Connection var3 = this.getConnection();

      LinkedList var7;
      try {
         Statement var4 = var3.createStatement();

         try {
            String var5 = this.getCommodityStatement(var1);
            ResultSet var6 = var4.executeQuery(var5);

            while (var6.next()) {
               var2.add(this.getCommodity(var6));
            }

            var7 = var2;
         } catch (Throwable var10) {
            if (var4 != null) {
               try {
                  var4.close();
               } catch (Throwable var9) {
                  var10.addSuppressed(var9);
               }
            }

            throw var10;
         }

         if (var4 != null) {
            var4.close();
         }
      } catch (Throwable var11) {
         if (var3 != null) {
            try {
               var3.close();
            } catch (Throwable var8) {
               var11.addSuppressed(var8);
            }
         }

         throw var11;
      }

      if (var3 != null) {
         var3.close();
      }

      return var7;
   }

   private String getCommodityStatement(Set<Long> var1) throws SQLException {
      StringBuilder var2 = new StringBuilder();
      var2.append("select c.id,c.name,c.code,c.point_value,c.tick_step,c.tick_Size,c.order_size_multi,c.order_size_step from commodity c where c.id in (");
      int var3 = 0;

      for (Long var5 : var1) {
         var2.append("'" + var5 + "'");
         if (var3 != var1.size() - 1) {
            var2.append(",");
         }

         var3++;
      }

      var2.append(")");
      return var2.toString();
   }

   private String getCommodityStatement(String var1, String[] var2) throws SQLException {
      StringBuilder var3 = new StringBuilder();
      var3.append(
         "select c.id,c.name,c.code,c.point_value,c.tick_step,c.tick_Size,c.order_size_multi,c.order_size_step from commodity c where c.id in (select t.commodity_id from "
            + var1
            + " t where commodity_id is not null and t.ticker in ("
      );

      for (int var4 = 0; var4 < var2.length; var4++) {
         var3.append("'" + var2[var4] + "'");
         if (var4 != var2.length - 1) {
            var3.append(",");
         }
      }

      var3.append("))");
      return var3.toString();
   }

   static {
      MONTH_LETTERS.put("F", "Jan");
      MONTH_LETTERS.put("G", "Feb");
      MONTH_LETTERS.put("H", "Mar");
      MONTH_LETTERS.put("J", "Apr");
      MONTH_LETTERS.put("K", "May");
      MONTH_LETTERS.put("M", "Jun");
      MONTH_LETTERS.put("N", "Jul");
      MONTH_LETTERS.put("Q", "Aug");
      MONTH_LETTERS.put("U", "Sep");
      MONTH_LETTERS.put("V", "Oct");
      MONTH_LETTERS.put("X", "Nov");
      MONTH_LETTERS.put("Z", "Dec");
      BR_SYMBOLS.add("WDO");
      BR_SYMBOLS.add("WIN");
      BR_SYMBOLS.add("DOL");
      BR_SYMBOLS.add("IND");
      BR_SYMBOLS.add("DI1");
      BR_SYMBOLS.add("CCM");
      BR_SYMBOLS.add("BGI");
   }
}
