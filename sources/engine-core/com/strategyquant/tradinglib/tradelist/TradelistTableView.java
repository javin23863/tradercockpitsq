package com.strategyquant.tradinglib.tradelist;

import com.strategyquant.lib.settings.IXMLAble;
import com.strategyquant.tradinglib.Order;
import com.strategyquant.tradinglib.OrdersList;
import com.strategyquant.tradinglib.TradelistColumn;
import java.util.ArrayList;
import org.jdom2.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TradelistTableView implements IXMLAble {
   private static final Logger Log = LoggerFactory.getLogger(TradelistTableView.class);
   public ArrayList<TradelistTableColumnEntry> columns = new ArrayList<>();
   public static final String FILE_EXTENSION = "vw";
   public static final String DEFAULT_VIEW_NAME = "Default";
   public static final int extraColumnsCount = 5;
   public String name = "?";
   public long lastChanged = 0L;

   public TradelistTableView() {
   }

   public TradelistTableView(String var1) {
      this.name = var1;
   }

   @Override
   public String toString() {
      return this.name;
   }

   public Element getXML() {
      Element var1 = new Element("DatabankView");
      var1.setAttribute("name", this.name);
      Element var2 = new Element("Columns");
      var1.addContent(var2);

      for (TradelistTableColumnEntry var4 : this.columns) {
         var2.addContent(var4.getXML());
      }

      return var1;
   }

   public void setFromXML(Element var1) {
      this.name = var1.getAttributeValue("name");
      this.columns.clear();
      Element var2 = var1.getChild("Columns");
      if (var2 != null) {
         for (Element var4 : var2.getChildren("Column")) {
            TradelistTableColumnEntry var5 = new TradelistTableColumnEntry();

            try {
               var5.setFromXML(var4);
               this.columns.add(var5);
            } catch (Exception var7) {
               Log.error(this.name + " - " + var7.getMessage());
            }
         }
      }
   }

   public ArrayList<Object[]> getData(OrdersList var1, int var2, int var3, boolean var4) throws Exception {
      ArrayList var5 = new ArrayList();
      int var6 = this.columns.size();
      new Order();

      for (int var7 = var2; var7 < var2 + var3 && var7 <= var1.size(); var7++) {
         Order var8 = var1.get(var7);
         var8.Order = var7 + 1;
         Object[] var9 = new Object[var6 + 5];

         for (int var11 = 0; var11 < var6; var11++) {
            TradelistTableColumnEntry var12 = this.columns.get(var11);

            try {
               TradelistColumn var13 = var12.tableColumn;
               Object var10;
               if (var13.getClass().getSimpleName().equals("Balance") && var8.AccountBalanceTemp != -1.0E8F) {
                  var10 = var8.AccountBalanceTemp;
               } else {
                  var10 = var13.getValue(var8);
               }

               boolean var14 = var4 || var13.getType().equals("Text");
               var9[var11] = var14 ? var13.getFormattedValue(var10) : var10;
            } catch (Exception var15) {
               Log.error("Cannot print tradelist column value. Exc.", var15);
               var9[var11] = "N/A";
            }
         }

         var9[var9.length - 5] = var8.Ticket;
         var9[var9.length - 4] = var8.OpenTime;
         var9[var9.length - 3] = var8.CloseTime;
         var9[var9.length - 2] = var8.SampleType;
         var9[var9.length - 1] = var8.Type;
         var5.add(var9);
      }

      return var5;
   }
}
