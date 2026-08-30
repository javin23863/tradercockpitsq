package com.strategyquant.plugin.Task.impl.AutomaticPortfolioBuilder;

import com.strategyquant.lib.L;
import com.strategyquant.lib.XMLUtil;
import com.strategyquant.lib.app.MainApp;
import com.strategyquant.plugin.Task.impl.AutomaticPortfolioBuilder.bruteForce.BFPortfolioMasterComputer;
import com.strategyquant.plugin.Task.impl.AutomaticPortfolioBuilder.bruteForce.PortfolioMasterComputer;
import com.strategyquant.plugin.Task.impl.AutomaticPortfolioBuilder.genetic.GPortfolioMasterComputer;
import com.strategyquant.tradinglib.Databank;
import com.strategyquant.tradinglib.ResultsGroup;
import com.strategyquant.tradinglib.databank.RecordNotFoundException;
import com.strategyquant.tradinglib.portfolioMaster.PortfolioMasterSettings;
import com.strategyquant.tradinglib.portfolioMaster.PortfolioMasterUtils;
import com.strategyquant.tradinglib.project.ProgressEngine;
import com.strategyquant.tradinglib.project.SQProject;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import org.jdom2.Content;
import org.jdom2.Element;

public class AutomaticPortfolioBuilder {
   private static final String LockName = "AutomaticPortfolioBuilder";

   public void start(SQProject var1, ProgressEngine var2, PortfolioMasterSettings var3) throws Exception {
      if (var3.databankSource == null) {
         throw new Exception(L.t("No input databank set.", new Object[0]));
      }

      if (var3.databankTarget == null) {
         throw new Exception(L.t("No output databank set.", new Object[0]));
      }

      if (var3.fitnessFunction == null) {
         throw new Exception(L.t("No fitness function set.", new Object[0]));
      }

      if (var3.conditions == null) {
         throw new Exception(L.t("No custom conditions set.", new Object[0]));
      }

      ArrayList var4 = var3.getKeys();
      int var5 = var4.size();
      if (var5 == 0) {
         throw new Exception(L.t("You have to select at least 2 simple strategies to create portfolio.", new Object[0]));
      }

      var3.numberOfPossiblePortfolios = PortfolioMasterUtils.calculateMaxNumberOfPortfolios(var5, var3.minStrategiesInPortfolio, var3.maxStrategiesInPortfolio);
      if (var3.numberOfPossiblePortfolios.compareTo(BigInteger.valueOf(0L)) == 0) {
         throw new Exception(L.t("Please check your settings. Number of possible portfolios must be greater than 0.", new Object[0]));
      }

      ArrayList var6 = this.getClonedStrategies(var4, var3.databankSource);
      var2.printToLog(L.t("Number of possible portfolios: " + var3.numberOfPossiblePortfolios.toString(), new Object[0]));
      boolean var7 = var3.useBruteForce;
      if (var3.numberOfPossiblePortfolios.compareTo(BigInteger.valueOf(500L)) < 0 && !var7) {
         var2.printToLog(L.t("The number of possible portfolios is very small, Genetic approach doesn't make sense in this case.", new Object[0]));
         var7 = true;
      }

      if (MainApp.v571hfnsHw().bDm88fRJA3()) {
         throw new Exception(L.t("PortfolioMaster is available only in full version, not in this special trial.", new Object[0]));
      }

      PortfolioMasterComputer var8 = null;
      if (var7) {
         var8 = new BFPortfolioMasterComputer(var6, var1, var2, var3);
      } else {
         var8 = new GPortfolioMasterComputer(var6, var1, var2, var3);
      }

      var8.execute();
   }

   private ArrayList<ResultsGroup> getClonedStrategies(ArrayList<String> var1, Databank var2) throws Exception {
      try {
         ArrayList var3 = new ArrayList();

         for (int var4 = 0; var4 < var1.size(); var4++) {
            String var5 = (String)var1.get(var4);
            ResultsGroup var6 = null;

            try {
               var6 = var2.getLocked(var5, "AutomaticPortfolioBuilder");
               Element var7 = var6.getStrategyXml();
               Element var8 = var7.getChild("Strategy").getChild("MoneyManagement");
               List var9 = XMLUtil.stringToElement(var6.getLastSettings()).getChild("RiskMoneyManagement").getChild("MoneyManagement").getChildren();
               String var10 = "";
               String var11 = "";
               List var12 = new ArrayList();

               for (int var14 = 0; var14 < var9.size(); var14++) {
                  Element var15 = (Element)var9.get(var14);
                  if (var15.getName().equals("Method")) {
                     if ("true".equals(var15.getAttribute("use").getValue())) {
                        var11 = var15.getAttributeValue("type");
                        List var16 = var15.getChildren("Params");
                        Element var13 = (Element)var16.get(0);
                        var12 = var13.getChildren();
                        break;
                     }
                  } else if (var15.getName().equals("InitialCapital") && var15.getContentSize() >= 1) {
                     var10 = ((Content)var15.getContent().get(0)).getValue();
                  }
               }

               Element var26 = new Element("MoneyManagement");
               var26.setAttribute("type", var11);
               Element var27 = new Element("Params");
               var26.addContent(var27);

               for (Element var17 : var12) {
                  addParam(var27, var17.getAttributeValue("key"), var17.getAttributeValue("className"), var17.getText());
               }

               Element var29 = var8.getParentElement();
               int var30 = var29.indexOf(var8);
               var29.setContent(var30, var26);
               var6.setStrategyXml(var7);
               var3.add(var6.clone(true));
            } catch (RecordNotFoundException var22) {
               throw new Exception(L.t("Strategy '%s' not found.", new Object[]{var5}));
            } finally {
               if (var6 != null) {
                  var6.releaseLock("AutomaticPortfolioBuilder");
               }
            }
         }

         return var3;
      } catch (Exception var24) {
         throw new Exception(L.t("Failed to get strategies - %s", new Object[]{var24.getMessage()}));
      }
   }

   private static void addParam(Element var0, String var1, String var2, String var3) {
      Element var4 = new Element("Param");
      var4.setAttribute("key", var1);
      var4.setAttribute("className", var2);
      var4.setText(var3);
      var0.addContent(var4);
   }
}
