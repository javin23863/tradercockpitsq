package com.strategyquant.datalib.historyData;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class StockHistoryDataDao extends AbstractHistoryDataDao {
   public StockHistoryDataDao(String var1) {
      super("sq", "hE8+-%:b&]^c#[b#j;+=*R[N~hrtbn$", var1 + File.separator + "data_stock");
   }

   public Map<String, List<TickerRenameInfo>> getTickerRenamed() {
      HashMap var1 = new HashMap();

      try {
         Connection var2 = this.getConnection();

         try {
            PreparedStatement var3 = var2.prepareStatement("select ticker_from,ticker_to,change_date from ticker_renamed");

            try {
               ResultSet var4 = var3.executeQuery();

               while (var4.next()) {
                  TickerRenameInfo var5 = new TickerRenameInfo();
                  var5.setDate(var4.getDate("change_date").getTime());
                  var5.setTickerFrom(var4.getString("ticker_from"));
                  var5.setTickerTo(var4.getString("ticker_to"));
                  List var6 = (List)var1.get(var5.getTickerFrom());
                  if (var6 == null) {
                     var6 = new LinkedList();
                     var1.put(var5.getTickerFrom(), var6);
                  }

                  var6.add(var5);
               }
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
      } catch (Exception var11) {
         Log.error("Error while checking change_date value", var11);
      }

      var1.values().stream().forEach(var1x -> var1x.sort(new Comparator<TickerRenameInfo>() {
         public int compare(TickerRenameInfo var1, TickerRenameInfo var2) {
            return var1.getDate().compareTo(var2.getDate());
         }
      }));
      return var1;
   }
}
