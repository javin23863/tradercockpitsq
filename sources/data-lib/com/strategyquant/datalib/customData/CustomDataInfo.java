package com.strategyquant.datalib.customData;

import com.strategyquant.lib.XMLUtil;
import com.strategyquant.lib.settings.IXMLAble;
import java.io.Serializable;
import org.jdom2.Element;
import org.json.JSONArray;

public class CustomDataInfo implements Serializable, IXMLAble {
   public static final String Name = "name";
   public static final String CodeMT4 = "mt4";
   public static final String CodeMT5 = "mt5";
   public static final String CodeEL = "el";
   public int id;
   public String name;
   public int dataType;
   public int values;
   public JSONArray valuesObject;
   public String timeframe;
   public String filename;
   public long dateFrom = 0L;
   public long dateTo = 0L;
   public int rows = 0;
   public int totalDays;

   public String getValue(int var1, String var2) throws Exception {
      try {
         Object var3 = this.valuesObject.getJSONObject(var1).get(var2);
         return var3 instanceof String ? var3.toString() : null;
      } catch (Exception var4) {
         throw new Exception("Failed to return value.", var4);
      }
   }

   public void setValues(String var1) {
      this.valuesObject = new JSONArray(var1);
      this.values = 0;

      for (int var2 = 0; var2 < this.valuesObject.length(); var2++) {
         Object var3 = this.valuesObject.getJSONObject(var2).get("name");
         if (var3 instanceof String && var3.toString().length() > 0) {
            this.values++;
         }
      }
   }

   public Element getXML() {
      Element var1 = new Element("IndicatorDataInfo");
      XMLUtil.trySetAttr(var1, "name", this.name);
      XMLUtil.trySetAttr(var1, "type", this.dataType + "");
      XMLUtil.trySetAttr(var1, "count", this.values + "");
      XMLUtil.trySetAttr(var1, "values", this.valuesObject.toString());
      return var1;
   }

   public void setFromXML(Element var1) throws Exception {
      this.name = var1.getAttributeValue("name");
      this.dataType = XMLUtil.tryGetIntAttr(var1, "type");
      this.values = XMLUtil.tryGetIntAttr(var1, "count");
      this.valuesObject = new JSONArray(var1.getAttributeValue("values"));
   }
}
