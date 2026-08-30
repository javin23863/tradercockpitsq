package com.strategyquant.plugin.DataManager.impl.Data.csvexport.format;

import com.strategyquant.datalib.data.io.VersatileData;
import com.strategyquant.lib.L;
import com.strategyquant.lib.settings.IXMLAble;
import com.strategyquant.plugin.DataManager.impl.Data.csvexport.items.AbstractItem;
import com.strategyquant.plugin.DataManager.impl.Data.csvexport.items.Items;
import com.strategyquant.plugin.DataManager.impl.Data.csvexport.items.TextItem;
import com.strategyquant.plugin.DataManager.impl.Data.csvexport.items.Volume;
import java.util.ArrayList;
import java.util.regex.Pattern;
import org.jdom2.Element;
import org.json.JSONObject;

public class Format implements IXMLAble {
   public String name;
   public String separator;
   public String header;
   public String items;
   public boolean includeHeader;
   public boolean predefined;
   private ArrayList<AbstractItem> itemsList = new ArrayList<>();
   private int volumeColumns = 0;

   public Format(String var1, String var2, String[] var3, String[] var4, boolean var5, boolean var6) throws Exception {
      this.name = var1;
      this.separator = var2;
      this.includeHeader = var5;
      this.predefined = var6;
      if (var3.length != var4.length) {
         throw new Exception(L.t("Header length <> Format length. Different delimiters!?", new Object[0]));
      }

      this.header = "";

      for (int var7 = 0; var7 < var3.length; var7++) {
         if (var7 < var3.length - 1) {
            this.header = this.header + var3[var7] + var2;
         } else {
            this.header = this.header + var3[var7];
         }
      }

      this.items = "";

      for (int var12 = 0; var12 < var4.length; var12++) {
         String var13 = var4[var12];
         var13 = var13.replaceAll(Pattern.quote("["), "");
         var13 = var13.replaceAll(Pattern.quote("]"), "");
         String var8;
         String var9;
         if (var13.contains(":")) {
            String[] var10 = var13.split(":", 2);
            var8 = var10[0];
            var9 = var10[1];
         } else {
            var8 = var13;
            var9 = null;
         }

         AbstractItem var11 = Items.get().findByKey(var8);
         if (var11 != null) {
            var11 = var11.clone();
            if (var9 != null) {
               var11.setFormat(var9);
            }

            this.itemsList.add(var11);
            if (var11 instanceof Volume) {
               this.volumeColumns++;
            }
         } else {
            var11 = new TextItem();
            var11.value = var13;
            this.itemsList.add(var11);
            if (var11 instanceof Volume) {
               this.volumeColumns++;
            }
         }

         if (var12 < var4.length - 1) {
            this.items = this.items + "[" + var13 + "]" + var2;
         } else {
            this.items = this.items + "[" + var13 + "]";
         }
      }

      if (this.volumeColumns > 1) {
         for (int var18 = 0; var18 < this.itemsList.size(); var18++) {
            if (this.itemsList.get(var18) instanceof Volume) {
               this.itemsList.get(var18).count = this.volumeColumns;
            }
         }
      }
   }

   public JSONObject toJson() {
      return new JSONObject()
         .put("name", this.name)
         .put("header", this.header)
         .put("items", this.items)
         .put("includeHeader", this.includeHeader)
         .put("predefined", this.predefined);
   }

   public void setFromXML(Element var1) throws Exception {
   }

   public Element getXML() {
      return null;
   }

   public String printHeader() {
      return this.header.replaceAll(Pattern.quote("[Tab]"), "\t");
   }

   public String printData(String var1, VersatileData var2, int var3) {
      String var4 = "";

      for (int var5 = 0; var5 < this.itemsList.size(); var5++) {
         if (var5 < this.itemsList.size() - 1) {
            var4 = var4 + this.itemsList.get(var5).printValue(var1, var2, var3) + this.separator;
         } else {
            var4 = var4 + this.itemsList.get(var5).printValue(var1, var2, var3);
         }
      }

      return var4.replaceAll(Pattern.quote("[Tab]"), "\t");
   }
}
