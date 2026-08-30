package com.strategyquant.plugin.Loader.impl.SQ4;

import com.strategyquant.lib.L;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.SettingsMap;
import com.strategyquant.lib.XMLUtil;
import com.strategyquant.lib.app.MainApp;
import com.strategyquant.lib.whitelabel.AbstractBroker;
import com.strategyquant.pluginlib.SQPluginManager;
import com.strategyquant.pluginlib.annotations.Category;
import com.strategyquant.pluginlib.annotations.License;
import com.strategyquant.pluginlib.annotations.Name;
import com.strategyquant.pluginlib.annotations.ShortDesc;
import com.strategyquant.tradinglib.OrdersList;
import com.strategyquant.tradinglib.Result;
import com.strategyquant.tradinglib.ResultsGroup;
import com.strategyquant.tradinglib.crosscheck.ICrossCheck;
import com.strategyquant.tradinglib.dailyEquity.DailyEquity;
import com.strategyquant.tradinglib.optimization.OptimizationProfile;
import com.strategyquant.tradinglib.results.file.ILoaderPlugin;
import com.strategyquant.tradinglib.strategy.xml.SQ3StrategyConverter;
import com.strategyquant.tradinglib.strategy.xml.StrategyFixer;
import it.unimi.dsi.fastutil.longs.Long2FloatRBTreeMap;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Enumeration;
import java.util.List;
import java.util.UUID;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.FileHeader;
import net.xeoh.plugins.base.annotations.PluginImplementation;
import net.xeoh.plugins.base.annotations.meta.Author;
import org.jdom2.Element;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Author(name = "Mark Fric")
@Name(name = "SQXLoaderPlugin plugin")
@Category(name = "File Loader")
@License(text = "")
@ShortDesc(text = "Short description")
@PluginImplementation
public class SQ4LoaderPlugin implements ILoaderPlugin {
   public static final Logger Log = LoggerFactory.getLogger(SQ4LoaderPlugin.class);
   public static final String[] extensions = new String[]{"sq4", "sqw", "sqx"};
   private XMLOutputter xmlOutputter = new XMLOutputter(Format.getPrettyFormat());

   public String getProduct() {
      return "any";
   }

   public int getPreferredPosition() {
      return 0;
   }

   public void initPlugin() throws Exception {
   }

   public String[] getFileExtensions() {
      return extensions;
   }

   public ResultsGroup load(String var1, boolean var2, String var3, String var4) throws Exception {
      return !var1.endsWith("sq4") && !var1.endsWith("sqx") ? this.loadSQWFile(var1, var2, var4) : this.loadSQ4File(var1, var2, var3, var4);
   }

   private ResultsGroup loadSQWFile(String var1, boolean var2, String var3) throws Exception {
      Log.debug("Loading SQW file {}", var1);
      ResultsGroup var4 = null;

      try {
         File var5 = new File(var1);
         String var6 = SQUtils.stripExtension(var5.getName());
         var4 = new ResultsGroup(var6);
         var4.setResultFileName(var1);
         String var7 = SQUtils.fileToString(var5);
         if (var7 == null) {
            throw new Exception(L.t("Cannot load strategy XML from the file.", new Object[0]));
         }

         var7 = MainApp.getDataProtector().decryptBase64(var7, var3);
         Element var8 = XMLUtil.stringToElement(var7);
         if (var8 != null) {
            String var9 = var8.getAttributeValue("version");

            try {
               if (var9 != null && Double.parseDouble(var9) < 3.8) {
                  var8 = SQ3StrategyConverter.getInstance().convert(var8);
               }

               var8 = StrategyFixer.fixStrategy(XMLUtil.elementToString(var8));
            } catch (Exception var11) {
               Log.error("Modifying XML config failed", var11);
            }
         }

         var4.addSubresult(var6, new SettingsMap());
         var4.computeAllStats();
         var4.portfolio().addStrategyXml(var8);
         var4.updated = true;
         return var4;
      } catch (Exception var12) {
         throw var12;
      }
   }

   private ResultsGroup loadSQ4File(String var1, boolean var2, String var3, String var4) throws Exception {
      Log.debug("Loading file {}", var1);
      String var5 = null;

      try {
         ZipFile var6 = new ZipFile(var1);
         if (var6.isEncrypted()) {
            AbstractBroker var7 = MainApp.v571hfnsHw().inygRmSMx7();
            if (var7 == null || var7.getPassword() == null) {
               throw new Exception(L.t("Cannot read strategy file - it is broker-locked", new Object[0]));
            }

            Serializable var8 = MainApp.getDataPath() + "/internal/tmp/";
            var5 = var8 + UUID.randomUUID();
            List var9 = var6.getFileHeaders();
            String var10 = ((FileHeader)var9.get(0)).getFileName();
            var6.setPassword(var7.getPassword());
            var6.extractAll(var8);
            new File(var8 + var10).renameTo(new File(var5));
         }
      } catch (ZipException var25) {
         Log.error("Cannot read strategy file", var25);
         throw new Exception(L.t("Cannot read strategy file", new Object[0]) + " - " + var25.getMessage());
      }

      JarFile var26 = null;
      ResultsGroup var27 = null;

      try {
         var26 = new JarFile(var5 == null ? var1 : var5);
         File var29 = new File(var1);
         String var30 = SQUtils.stripExtension(var29.getName());
         var27 = new ResultsGroup(var30);
         var27.setResultFileName(var1);
         JarEntry var31 = var26.getJarEntry("version.txt");
         int var11 = this.recognizeVersion(var26, var31);
         Log.debug("Version recognized to: {}", var11);
         JarEntry var12 = var26.getJarEntry("strategy_Portfolio.xml");
         JarEntry var13 = var26.getJarEntry("settings.xml");
         Element var14 = null;
         if (var12 == null && var13 == null) {
            throw new Exception(L.t("Cannot load ResultsGroup from file. Cannot find any settings inside the file.", new Object[0]));
         }

         if (var12 != null) {
            String var15 = SQUtils.inputStreamToString(var26.getInputStream(var12));
            if (var15 != null) {
               var15 = MainApp.getDataProtector().decryptBase64(var15, var4);

               try {
                  var14 = StrategyFixer.fixStrategy(var15);
               } catch (Exception var22) {
                  Log.error("Modifying XML config failed", var22);
               }
            }
         }

         this.loadOrders(var27.orders(), var26);
         if (var13 != null) {
            Element var33 = XMLUtil.readXmlFile(var26.getInputStream(var13));
            this.fixOldSettingsResultKeys(var33);
            var27.setFromXML(var33);
         } else {
            var27.addSubresult(var30, new SettingsMap());
            var27.computeAllStats();
         }

         this.loadLastSettings(var27, var26);
         this.loadOptimizationProfile(var27, var26);
         this.loadResultsData(var27, var26);
         this.loadCrossCheckData(var27, var26);
         if (var14 != null) {
            var27.portfolio().addStrategyXml(var14);
         }

         if (var11 == 0) {
            Log.debug("Recomputing daily equity");
            Long2FloatRBTreeMap var34 = var27.recomputeMergedDailyEquity();
            if (var34 != null) {
               var27.portfolio().setWorstDailyEquity(var34);
            }
         }

         var27.updated = true;
         return var27;
      } catch (Exception var23) {
         throw var23;
      } finally {
         if (var26 != null) {
            var26.close();
         }

         if (var5 != null) {
            Files.delete(new File(var5).toPath());
         }
      }
   }

   private int recognizeVersion(JarFile var1, JarEntry var2) {
      if (var2 == null) {
         return 0;
      }

      try {
         BufferedReader var3 = new BufferedReader(new InputStreamReader(var1.getInputStream(var2), StandardCharsets.UTF_8));
         StringBuilder var4 = new StringBuilder();

         String var5;
         while ((var5 = var3.readLine()) != null) {
            var4.append(var5);
         }

         var3.close();
         return Integer.parseInt(var4.toString());
      } catch (Exception var6) {
         Log.error("Cannot recognize version");
         return 0;
      }
   }

   private void fixOldSettingsResultKeys(Element var1) {
   }

   private void loadLastSettings(ResultsGroup var1, JarFile var2) throws IOException, Exception {
      try {
         JarEntry var3 = var2.getJarEntry("lastSettings.xml");
         if (var3 != null) {
            String var4 = SQUtils.inputStreamToString(var2.getInputStream(var3));
            var4 = SQUtils.optimizeLastSettings(var4);
            var1.setLastSettings(var4.intern());
         }
      } catch (Exception var5) {
         Log.error("Problem loading last setting ", var5);
      }
   }

   private void loadOptimizationProfile(ResultsGroup var1, JarFile var2) throws IOException, Exception {
      try {
         JarEntry var3 = var2.getJarEntry("optimizationProfile.xml");
         if (var3 != null) {
            Element var4 = XMLUtil.readXmlFile(var2.getInputStream(var3));
            OptimizationProfile var5 = new OptimizationProfile();
            var5.setFromXML(var4);
            var1.setOptimizationProfile(var5);
         }

         JarEntry var8 = var2.getJarEntry("optimizationProfile.bin");
         if (var8 != null) {
            OptimizationProfile var9 = new OptimizationProfile();
            ObjectInputStream var6 = new ObjectInputStream(var2.getInputStream(var8));
            var9.deserialize(var6);
            var6.close();
            var1.setOptimizationProfile(var9);
         }
      } catch (Exception var7) {
         Log.error("Problem loading optimization profile ", var7);
      }
   }

   private void loadCrossCheckData(ResultsGroup var1, JarFile var2) throws Exception {
      for (ICrossCheck var4 : SQPluginManager.getPlugins(ICrossCheck.class)) {
         try {
            var4.loadDataFromFile(var1, var2);
         } catch (Exception var6) {
            Log.error("Loading crosscheck '" + var4.getName() + "' settings failed", var6);
         }
      }
   }

   private void loadResultsData(ResultsGroup var1, JarFile var2) throws Exception {
      Enumeration var3 = var2.entries();

      while (var3.hasMoreElements()) {
         String var4 = ((JarEntry)var3.nextElement()).getName();
         if (var4.startsWith("Results/")) {
            int var5 = var4.lastIndexOf("/");
            if (var5 > 8) {
               String var6 = var4.substring(8, var5);

               try {
                  Result var7 = var1.subResult(SQUtils.correctDirnameToResultKey(var6));
                  if (var7 == null) {
                     throw new Exception(L.t("Result '%s' is null!", new Object[]{var6}));
                  }

                  if (var4.endsWith("dailyEquity.csv")) {
                     this.loadDailyEquityDataOldFormat(var7, var6, var2);
                  }

                  if (var4.endsWith("dailyEquity.bin")) {
                     this.loadDailyEquityData(var7, var6, var2);
                  }
               } catch (IllegalArgumentException var8) {
                  Log.error("Cannot find result", var8);
               }
            }
         }
      }
   }

   private void loadDailyEquityDataOldFormat(Result var1, String var2, JarFile var3) throws Exception {
      JarEntry var4 = var3.getJarEntry("Results/" + var2 + "/dailyEquity.csv");
      if (var4 != null) {
         DailyEquity.loadCsv(var1, var3.getInputStream(var4));
      }
   }

   private void loadDailyEquityData(Result var1, String var2, JarFile var3) throws Exception {
      JarEntry var4 = var3.getJarEntry("Results/" + var2 + "/dailyEquity.bin");
      if (var4 != null) {
         if (var4.getSize() > 0L) {
            DailyEquity.load(var1, var3.getInputStream(var4));
         }
      }
   }

   private void loadOrders(OrdersList var1, JarFile var2) throws Exception {
      JarEntry var3 = var2.getJarEntry("orders.bin");

      try {
         if (var3 != null && var3.getSize() > 10L) {
            long var4 = System.currentTimeMillis();
            ObjectInputStream var6 = new ObjectInputStream(var2.getInputStream(var3));
            var1.deserialize(var6);
            var6.close();
            long var7 = System.currentTimeMillis();
         }
      } catch (Exception var9) {
         Log.error("Error loading orders", var9);
      }
   }

   public ResultsGroup finishLoad(ResultsGroup var1) throws Exception {
      return var1;
   }
}
