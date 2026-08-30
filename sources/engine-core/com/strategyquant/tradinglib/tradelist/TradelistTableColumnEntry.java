package com.strategyquant.tradinglib.tradelist;

import com.strategyquant.lib.L;
import com.strategyquant.lib.settings.IXMLAble;
import com.strategyquant.tradinglib.Order;
import com.strategyquant.tradinglib.TradelistColumn;
import org.jdom2.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TradelistTableColumnEntry implements IXMLAble {
   public static final Logger Log = LoggerFactory.getLogger("TableColumnEntry");
   public TradelistColumn tableColumn;

   public TradelistTableColumnEntry() {
   }

   public TradelistTableColumnEntry(TradelistColumn var1) {
      this.tableColumn = var1;
   }

   public String getColumnEntryName() {
      return this.tableColumn.getName();
   }

   public Object getValue(Order var1) {
      try {
         return this.tableColumn.getValue(var1);
      } catch (Exception var3) {
         Log.error("Error processing databank column '" + this.tableColumn.getClass().getSimpleName(), var3);
         return "N/A";
      }
   }

   public Element getXML() {
      Element var1 = new Element("Column");
      var1.setAttribute("class", this.tableColumn.getClass().getSimpleName());
      var1.setAttribute("name", this.tableColumn.getName());
      return var1;
   }

   public void setFromXML(Element var1) throws Exception {
      String var2 = var1.getAttributeValue("class");
      this.tableColumn = (TradelistColumn)TradelistColumnsList.get().createNew(var2);
      if (this.tableColumn == null) {
         throw new Exception(L.t("Invalid databank column '%s'.", new Object[]{var2}));
      }
   }
}
