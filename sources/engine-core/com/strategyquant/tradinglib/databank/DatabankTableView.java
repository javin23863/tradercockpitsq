package com.strategyquant.tradinglib.databank;

import com.strategyquant.lib.settings.IXMLAble;
import java.util.ArrayList;
import org.jdom2.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DatabankTableView implements IXMLAble {
   private static final Logger Log = LoggerFactory.getLogger(DatabankTableView.class);
   public ArrayList<DatabankTableColumnEntry> columns = new ArrayList<>();
   public String name = "?";

   public DatabankTableView() {
   }

   public DatabankTableView(String var1) {
      this.name = var1;
   }

   @Override
   public String toString() {
      return this.name;
   }

   public String getName() {
      return this.name;
   }

   public Element getXML() {
      Element var1 = new Element("DatabankView");
      var1.setAttribute("name", this.name);
      Element var2 = new Element("Columns");
      var1.addContent(var2);

      for (DatabankTableColumnEntry var4 : this.columns) {
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
            DatabankTableColumnEntry var5 = new DatabankTableColumnEntry();

            try {
               var5.setFromXML(var4);
               this.columns.add(var5);
            } catch (Exception var7) {
               Log.error("View: " + this.name + " - cannot load column. Reason: " + var7.getMessage());
            }
         }
      }
   }
}
