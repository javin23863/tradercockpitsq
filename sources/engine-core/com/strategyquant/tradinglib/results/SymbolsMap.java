package com.strategyquant.tradinglib.results;

import com.strategyquant.datalib.InstrumentInfo;
import com.strategyquant.datalib.data.DataManager;
import com.strategyquant.lib.settings.IXMLAble;
import com.strategyquant.lib.utils.ISQCloneable;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import org.jdom2.Element;

public class SymbolsMap implements IXMLAble, Serializable, ISQCloneable<SymbolsMap> {
   private ArrayList<SymbolInfo> aSymbolInfo = new ArrayList<>();
   private ArrayList<String> aSymbols = new ArrayList<>();
   private HashMap<String, Integer> symbolIndex = new HashMap<>();
   private HashMap<Integer, Integer> symbolHashIndex = new HashMap<>();

   public synchronized void add(String var1, String var2, InstrumentInfo var3) {
      if (!this.contains(var1)) {
         this.aSymbolInfo.add(new SymbolInfo(var1, var2, var3));
         this.aSymbols.add(var1);
         this.symbolIndex.put(var1, this.aSymbolInfo.size() - 1);
         this.symbolHashIndex.put(var1.hashCode(), this.aSymbolInfo.size() - 1);
      }
   }

   public boolean contains(String var1) {
      return this.symbolIndex.containsKey(var1);
   }

   public ArrayList<String> list() {
      return this.aSymbols;
   }

   public SymbolInfo get(String var1) {
      return !this.contains(var1) ? null : this.aSymbolInfo.get(this.symbolIndex.get(var1));
   }

   public SymbolInfo get(int var1) {
      return !this.symbolHashIndex.containsKey(var1) ? null : this.aSymbolInfo.get(this.symbolHashIndex.get(var1));
   }

   public void add(SymbolsMap var1) {
      for (String var3 : var1.list()) {
         SymbolInfo var4 = var1.get(var3);
         this.add(var4.Symbol(), var4.Instrument(), var4.InstrumentInfo());
      }
   }

   public Element getXML() {
      Element var1 = new Element("SymbolsMap");

      for (String var3 : this.symbolIndex.keySet()) {
         SymbolInfo var4 = this.get(var3);
         InstrumentInfo var5 = null;
         if (var4 != null) {
            var5 = var4.InstrumentInfo();
            Element var6 = new Element("SymbolInfo");
            var6.setAttribute("symbolName", var3);
            if (var4.Instrument() != null) {
               var6.setAttribute("instrumentName", var4.Instrument());
            }

            if (var5 != null) {
               Element var7 = var5.getXML();
               var6.addContent(var7);
            }

            var1.addContent(var6);
         }
      }

      return var1;
   }

   public void setFromXML(Element var1) {
      this.aSymbolInfo.clear();
      this.aSymbols.clear();
      this.symbolIndex.clear();
      this.symbolHashIndex.clear();

      for (Element var3 : var1.getChildren("SymbolInfo")) {
         String var4 = var3.getAttributeValue("symbolName").intern();
         String var5 = var3.getAttributeValue("instrumentName").intern();
         Element var6 = var3.getChild("InstrumentInfo");
         if (var6 != null) {
            InstrumentInfo var7 = new InstrumentInfo();
            var7.setFromXML(var6);
            this.add(var4, var5, var7);
         }
      }
   }

   public SymbolsMap getClone() throws Exception {
      SymbolsMap var1 = new SymbolsMap();

      for (SymbolInfo var3 : this.aSymbolInfo) {
         var1.aSymbolInfo.add(var3.getClone());
      }

      var1.aSymbols.addAll(this.aSymbols);
      var1.symbolHashIndex.putAll(this.symbolHashIndex);
      var1.symbolIndex.putAll(this.symbolIndex);
      return var1;
   }

   public InstrumentInfo getDefaultInstrumentInfo() {
      if (this.aSymbolInfo.size() == 1) {
         return this.aSymbolInfo.get(0).InstrumentInfo();
      }

      for (SymbolInfo var2 : this.aSymbolInfo) {
         if (DataManager.isGroupAlias(var2.Symbol())) {
            return var2.InstrumentInfo();
         }
      }

      return null;
   }
}
