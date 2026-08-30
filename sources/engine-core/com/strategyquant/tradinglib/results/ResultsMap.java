package com.strategyquant.tradinglib.results;

import com.strategyquant.lib.SettingsMap;
import com.strategyquant.lib.settings.IXMLAble;
import com.strategyquant.tradinglib.Result;
import com.strategyquant.tradinglib.ResultsGroup;
import com.strategyquant.tradinglib.strategy.OutOfSample;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntIterator;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.jdom2.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResultsMap implements IXMLAble, Serializable {
   public static final Logger Log = LoggerFactory.getLogger("ResultsMap");
   private final ArrayList<String> resultKeys = new ArrayList<>();
   final List<String> unmodifiableResultKeys = Collections.unmodifiableList(this.resultKeys);
   private Int2ObjectOpenHashMap<Result> results = new Int2ObjectOpenHashMap();
   private String mainResultKey = null;
   private ResultsGroup resultsGroup;

   public ResultsMap(ResultsGroup var1) {
      this.resultsGroup = var1;
   }

   public ResultsMap clone(ResultsGroup var1) {
      ResultsMap var2 = new ResultsMap(var1);
      var1.updated = true;
      var2.resultKeys.addAll(this.resultKeys);
      var2.results.clear();
      IntIterator var3 = this.results.keySet().iterator();

      while (var3.hasNext()) {
         int var4 = (Integer)var3.next();
         Result var5 = (Result)this.results.get(var4);
         var2.results.put(var4, var5.clone(var1));
      }

      var2.mainResultKey = this.mainResultKey;
      return var2;
   }

   public void removeResult(String var1, boolean var2) {
      int var3 = var1.hashCode();
      if (this.results.containsKey(var3)) {
         Result var4 = (Result)this.results.get(var3);
         if (var2) {
            var4.clear();
         }

         this.resultKeys.remove(var1);
         this.results.remove(var3);
         if (this.mainResultKey.equals(var1)) {
            this.mainResultKey = null;
         }

         this.resultsGroup.updated = true;
      }
   }

   public Result get(String var1) {
      if (var1 == null || var1.equals("*")) {
         var1 = "Portfolio";
      }

      if (this.results.size() == 0) {
         throw new IllegalArgumentException("ResultGroup doesn't contain any results!");
      } else {
         int var2 = var1.hashCode();
         if (this.resultKeys.contains(var1)) {
            return (Result)this.results.get(var2);
         } else if (var1.equals("Portfolio")) {
            return this.isPortfolio() ? (Result)this.results.get(var2) : (Result)this.results.get(this.getMainResultKey().hashCode());
         } else {
            var1 = var1.replace("/*", "");
            if (this.resultKeys.contains(var1)) {
               return (Result)this.results.get(var2);
            } else {
               throw new IllegalArgumentException("ResultGroup doesn't contain result with key '" + var1 + "'!");
            }
         }
      }
   }

   public void addResult(String var1, SettingsMap var2) {
      this.addResult(var1, var2, null);
   }

   public void addResult(String var1, SettingsMap var2, Result var3) {
      int var4 = var1.hashCode();
      if (this.results.containsKey(var4)) {
         Result var5 = (Result)this.results.get(var4);
         var5.clear();
      }

      if (this.mainResultKey == null) {
         this.mainResultKey = var1;
      }

      if (var3 == null) {
         var3 = new Result(var1, this.resultsGroup, var2);
      }

      this.resultKeys.add(var1);
      if (var1.equals("Portfolio") && this.resultKeys.size() > 1) {
         ArrayList var7 = new ArrayList();
         var7.add("Portfolio");

         for (int var6 = 0; var6 < this.resultKeys.size() - 1; var6++) {
            var7.add(this.resultKeys.get(var6));
         }

         this.resultKeys.clear();
         this.resultKeys.addAll(var7);
      }

      this.results.put(var4, var3);
      this.resultsGroup.updated = true;
   }

   public boolean hasResult(String var1) {
      return this.resultKeys.contains(var1);
   }

   public List<String> keys() {
      return this.unmodifiableResultKeys;
   }

   public List<String> additionalMarketKeys() {
      ArrayList var1 = new ArrayList();

      for (int var2 = 0; var2 < this.unmodifiableResultKeys.size(); var2++) {
         String var3 = this.unmodifiableResultKeys.get(var2);
         if (var3.startsWith("AdditionalMarket")) {
            var1.add(var3);
         }
      }

      return var1;
   }

   public boolean isPortfolio() {
      return this.resultKeys.contains("Portfolio");
   }

   public Element getXML() {
      Element var1 = new Element("ResultsMap");
      Element var2 = new Element("Results");

      for (int var3 = 0; var3 < this.resultKeys.size(); var3++) {
         String var4 = this.resultKeys.get(var3);
         Result var5 = (Result)this.results.get(var4.hashCode());
         Element var6 = var5.getXML();
         var2.addContent(var6);
      }

      var1.addContent(var2);
      return var1;
   }

   public void setFromXML(Element var1) {
      this.results.clear();
      this.resultKeys.clear();
      Element var2 = var1.getChild("Results");
      if (var2 != null) {
         List var3 = var2.getChildren("Result");

         for (int var4 = 0; var4 < var3.size(); var4++) {
            Element var5 = (Element)var3.get(var4);
            Result var6 = new Result(this.resultsGroup);
            var6.setFromXML(var5);
            String var7 = var6.getResultKey();
            this.results.put(var7.hashCode(), var6);
            this.resultKeys.add(var7);
         }
      }

      this.resultsGroup.updated = true;
   }

   public void computeAllStats(SettingsMap var1, OutOfSample var2) throws Exception {
      IntIterator var3 = this.results.keySet().iterator();

      while (var3.hasNext()) {
         int var4 = (Integer)var3.next();
         Result var5 = (Result)this.results.get(var4);
         var5.computeAllStats(var1, var2);
      }
   }

   public void clear(boolean var1, boolean var2) {
      if (this.results != null) {
         IntIterator var3 = this.results.keySet().iterator();

         while (var3.hasNext()) {
            int var4 = (Integer)var3.next();
            Result var5 = (Result)this.results.get(var4);
            var5.clear(var1, var2);
         }
      }

      this.resultsGroup.updated = true;
   }

   public void clearTempData() {
      if (this.results != null) {
         IntIterator var1 = this.results.keySet().iterator();

         while (var1.hasNext()) {
            int var2 = (Integer)var1.next();
            Result var3 = (Result)this.results.get(var2);
            var3.clear(true, true);
         }
      }

      this.resultsGroup.updated = true;
   }

   public String getMainResultKey() {
      if (this.mainResultKey == null) {
         if (this.results.size() == 0) {
            throw new IllegalArgumentException("ResultGroup doesn't contain any results!");
         }

         for (int var1 = 0; var1 < this.results.size(); var1++) {
            String var2 = this.resultKeys.get(var1);
            if (var2.startsWith("Main:")) {
               this.mainResultKey = var2;
               break;
            }
         }

         if (this.mainResultKey == null) {
            for (int var4 = 0; var4 < this.results.size(); var4++) {
               String var5 = this.resultKeys.get(var4);
               if (!var5.startsWith("Portfolio") && !var5.startsWith("AdditionalMarket") && !var5.startsWith("CrossCheck")) {
                  Result var3 = (Result)this.results.get(var5.hashCode());
                  if (!var3.isSpecial()) {
                     this.mainResultKey = var5;
                     break;
                  }
               }
            }
         }

         if (this.mainResultKey == null) {
            this.mainResultKey = this.resultKeys.get(0);
         }
      }

      return this.mainResultKey;
   }

   public void removeUnsavableSettings() {
      IntIterator var1 = this.results.keySet().iterator();

      while (var1.hasNext()) {
         int var2 = (Integer)var1.next();
         Result var3 = (Result)this.results.get(var2);
         var3.removeUnsavableSettings();
      }

      this.resultsGroup.updated = true;
   }

   public int size() {
      return this.resultKeys.size();
   }
}
