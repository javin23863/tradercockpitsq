package com.strategyquant.tradinglib.results.file;

import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.L88OaFjjon.V571hfnsHw;
import com.strategyquant.lib.app.MainApp;
import com.strategyquant.lib.whitelabel.AbstractBroker;
import com.strategyquant.pluginlib.SQPluginManager;
import com.strategyquant.pluginlib.program.IProgram;
import com.strategyquant.pluginlib.program.ProgramMethodDoesntExistException;
import com.strategyquant.tradinglib.OrdersList;
import com.strategyquant.tradinglib.Result;
import com.strategyquant.tradinglib.ResultsGroup;
import com.strategyquant.tradinglib.crosscheck.ICrossCheck;
import com.strategyquant.tradinglib.dailyEquity.DailyEquity;
import com.strategyquant.tradinglib.optimization.OptimizationProfile;
import com.strategyquant.tradinglib.results.SpecialValues;
import com.strategyquant.tradinglib.strategy.StrategyMerger;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.jar.Attributes.Name;
import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.model.ZipParameters;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileHandler implements IProgram {
   public static final Logger Log = LoggerFactory.getLogger("FileHandler");
   private ArrayList<ILoaderPlugin> loaderPlugins;
   private static final String xmlFileName = "settings.xml";
   private static final String ordersFileName = "orders.bin";
   private static final String dailyEquityFileName = "dailyEquity.bin";
   private static final String dailyEquityFileNameOld = "dailyEquity.old";
   private static final String versionFileName = "version.txt";
   private String lastSettingsFileName = "lastSettings.xml";
   private String optimizationProfileFileName = "optimizationProfile.bin";
   private ThreadLocal<FileHandlerVariables> localData = ThreadLocal.withInitial(() -> new FileHandlerVariables());

   public FileHandler() {
      this.loaderPlugins = SQPluginManager.getPlugins(ILoaderPlugin.class);
   }

   public Object call(String var1, Object... var2) throws Exception {
      switch (var1) {
         case "loadFile":
            if (var2.length == 1) {
               return this.loadFile((String)var2[0]);
            } else if (var2.length == 2) {
               return this.loadFile((String)var2[0], (Boolean)var2[1], null, null);
            } else if (var2.length == 3) {
               return this.loadFile((String)var2[0], (Boolean)var2[1], (String)var2[2], null);
            } else if (var2.length == 4) {
               return this.loadFile((String)var2[0], (Boolean)var2[1], (String)var2[2], (String)var2[3]);
            }
         default:
            throw new ProgramMethodDoesntExistException(var1);
         case "saveFile":
            Map var5 = null;
            if (var2.length == 4) {
               var5 = (Map)var2[3];
            }

            this.saveFile((ResultsGroup)var2[0], (String)var2[1], var5);
            return null;
         case "saveStrategyFile":
            this.saveStrategyFile((Element)var2[0], (String)var2[1]);
            return null;
      }
   }

   private ResultsGroup loadFile(String var1) throws Exception {
      return this.loadFile(var1, true, null, null);
   }

   private ResultsGroup loadFile(String var1, boolean var2, String var3, String var4) throws Exception {
      FileHandlerVariables var5 = this.localData.get();
      File var6 = new File(var1);
      if (var6.exists() && var6.isFile()) {
         var5.loadedResult = null;
         var5.loadedFilePath = var1;
         this.preloadFile(var2, var3, var4);
         return this.finishLoad();
      } else {
         throw new Exception("File '" + var1 + "' doesn't exist!");
      }
   }

   private ArrayList<ILoaderPlugin> getPossibleLoaders() {
      FileHandlerVariables var1 = this.localData.get();
      var1.loadingErrorMessages = null;
      var1.pluginLoaderUsed = null;
      String var2 = SQUtils.getExtension(var1.loadedFilePath);
      if (!var1.mapLoaders.containsKey(var2.hashCode())) {
         ArrayList var3 = new ArrayList();

         for (ILoaderPlugin var5 : this.loaderPlugins) {
            String[] var6 = var5.getFileExtensions();
            if (var6 != null && var6.length > 0) {
               for (int var7 = 0; var7 < var6.length; var7++) {
                  if (var6[var7].equalsIgnoreCase(var2)) {
                     var3.add(var5);
                  }
               }
            }
         }

         var1.mapLoaders.put(var2.hashCode(), var3);
      }

      return (ArrayList<ILoaderPlugin>)var1.mapLoaders.get(var2.hashCode());
   }

   private void preloadFile(boolean var1, String var2, String var3) throws MissingInstrumentException {
      FileHandlerVariables var4 = this.localData.get();
      var4.loadingErrorMessages = null;
      var4.pluginLoaderUsed = null;
      ArrayList var6 = this.getPossibleLoaders();
      if (var6 != null) {
         for (int var7 = 0; var7 < var6.size(); var7++) {
            ILoaderPlugin var8 = (ILoaderPlugin)var6.get(var7);
            String var5 = SQPluginManager.getPluginName(var8);

            try {
               var8.initPlugin();
               ResultsGroup var9 = var8.load(var4.loadedFilePath, var1, var2, var3);
               if (var9 != null) {
                  var4.loadedResult = var9;
                  var4.loadingErrorMessages = null;
                  var4.pluginLoaderUsed = var8;
                  return;
               }

               if (Log.isDebugEnabled()) {
                  Log.debug(" - Load using {} failed.", var5);
               }
            } catch (MissingInstrumentException var10) {
               throw var10;
            } catch (Exception var11) {
               if (Log.isDebugEnabled()) {
                  Log.debug(" - Load using {} failed with exception: {}", var5, var11.getMessage());
               }

               Log.error("Error loading file {}", var4.loadedFilePath, var11);
               Log.info(SQUtils.getStackTrace());
               if (var4.loadingErrorMessages == null) {
                  var4.loadingErrorMessages = "- error loading using " + var5 + " - " + var11.getMessage() + "\n";
               } else {
                  var4.loadingErrorMessages = var4.loadingErrorMessages + "- error loading using " + var5 + " - " + var11.getMessage() + "\n";
               }

               var4.lastLoaderExc = var11;
            }
         }
      }
   }

   private ResultsGroup finishLoad() throws Exception {
      FileHandlerVariables var1 = this.localData.get();
      if (var1.loadedResult == null) {
         if (var1.lastLoaderExc == null) {
            throw new Exception("No plugin loader was able to recognize file '" + var1.loadedFilePath + "'");
         } else {
            throw new Exception(var1.lastLoaderExc.getMessage());
         }
      } else {
         var1.loadedResult = var1.pluginLoaderUsed.finishLoad(var1.loadedResult);
         return var1.loadedResult;
      }
   }

   protected String getParam(Map<String, String[]> var1, String var2, String var3) throws Exception {
      return var1 != null && var1.containsKey(var2) ? ((String[])var1.get(var2))[0] : var3;
   }

   public void saveFile(ResultsGroup var1, String var2, Map<String, String[]> var3) throws Exception {
      String var4 = var1.specialValues().getString(SpecialValues.Note, "");
      boolean var5 = Boolean.parseBoolean(this.getParam(var3, "setNoteActive", "false"));
      if (var5) {
         String var6 = this.getParam(var3, "setNoteType", "");
         String var7 = this.getParam(var3, "setNoteCustomText", "");
         String var8 = null;
         String var9 = null;
         if (var1.mainResult().containsKey(SpecialValues.Symbol)) {
            var8 = var1.mainResult().getString(SpecialValues.Symbol);
         } else {
            var8 = var1.specialValues().getString(SpecialValues.Symbol, "N/A");
         }

         if (var1.mainResult().containsKey(SpecialValues.Timeframe)) {
            var9 = var1.mainResult().getString(SpecialValues.Timeframe);
         } else {
            var9 = var1.specialValues().getString(SpecialValues.Timeframe, "N/A");
         }

         switch (var6) {
            case "symbol":
               var1.specialValues().setString(SpecialValues.Note, var8);
               break;
            case "timeframe":
               var1.specialValues().setString(SpecialValues.Note, var9);
               break;
            case "symbolTimeframe":
               var1.specialValues().setString(SpecialValues.Note, var8 + "_" + var9);
               break;
            default:
               var1.specialValues().setString(SpecialValues.Note, var7);
         }
      }

      File var17 = new File(var2);
      if (!var17.getParentFile().exists()) {
         var17.getParentFile().mkdirs();
      }

      List var19 = null;
      if (var1.specialValues().containsKey(SpecialValues.IsMergedPortfolio)) {
         var19 = StrategyMerger.getSplitResults(var1);
      }

      Manifest var21 = new Manifest();
      var21.getMainAttributes().put(Name.MANIFEST_VERSION, "1.0");
      JarOutputStream var23 = new JarOutputStream(new BufferedOutputStream(new FileOutputStream(var17)), var21);

      try {
         this.writeSettingsFile(var1.getXML(), var23);
         this.writeStrategyFiles(var1, var23, this.getParam(var3, "UserSecretCode", null));
         if (var1.storesChartData()) {
            this.writeTradingChartDataFiles(var1, var23);
         }

         this.writeCrossCheckData(var1, var23);
         this.writeLastSettingsFile(var1.getLastSettings(), var23);
         this.writeOptimizationProfileFile(var1.getOptimizationProfile(), var23);
         this.writeDailyEquityFiles(var1, var23);
         this.writeOrdersFile(var1.orders(), var23);
         if (var5) {
            var1.specialValues().setString(SpecialValues.Note, var4);
         }

         this.writeVersionFile(var23, 1);
         if (var19 != null) {
            this.writeMergedStrategies(var19, var23);
         }

         var23.close();
      } catch (Exception var16) {
         var23.close();
         var17 = new File(var2);
         if (var17.exists()) {
            var17.delete();
         }

         throw var16;
      }

      V571hfnsHw var24 = MainApp.v571hfnsHw();
      if (var24 != null) {
         AbstractBroker var25 = MainApp.v571hfnsHw().inygRmSMx7();
         if (var25 != null && var25.getPassword() != null) {
            File var12 = new File(var2);
            File var13 = new File(var2 + "_tmp");
            ZipFile var14 = new ZipFile(var13);
            ZipParameters var15 = new ZipParameters();
            var15.setCompressionMethod(8);
            var15.setCompressionLevel(9);
            var15.setEncryptFiles(true);
            var15.setEncryptionMethod(99);
            var15.setAesKeyStrength(3);
            var15.setPassword(var25.getPassword());
            var14.addFile(var12, var15);
            var12.delete();
            var13.renameTo(var12);
         }
      }
   }

   private void writeVersionFile(JarOutputStream var1, int var2) throws Exception {
      JarEntry var3 = new JarEntry("version.txt");
      var1.putNextEntry(var3);
      BufferedWriter var4 = new BufferedWriter(new OutputStreamWriter(var1, StandardCharsets.UTF_8));
      String var5 = Integer.toString(var2);
      var4.write(var5);
      var4.flush();
   }

   private void writeCrossCheckData(ResultsGroup var1, JarOutputStream var2) throws Exception {
      for (ICrossCheck var4 : SQPluginManager.getPlugins(ICrossCheck.class)) {
         try {
            var4.saveDataToFile(var1, var2);
         } catch (Exception var6) {
            Log.error("Saving crosscheck settings failed", var6);
         }
      }
   }

   private void writeTradingChartDataFiles(ResultsGroup var1, JarOutputStream var2) throws Exception {
      for (String var4 : var1.getResultKeys()) {
         Result var5 = var1.subResult(var4);
         String var6 = var5.getStockChartPath();
         if (var6 != null) {
            File var7 = new File(var6);
            if (var7.exists()) {
               JarFile var8 = new JarFile(var7);

               try {
                  var4 = SQUtils.correctResultKeyToDirname(var4);
                  Enumeration var9 = var8.entries();

                  while (var9.hasMoreElements()) {
                     JarEntry var10 = (JarEntry)var9.nextElement();
                     String var11 = var10.getName();
                     if (!var11.startsWith("META-INF")) {
                        JarEntry var12 = new JarEntry(var10.getName());
                        InputStream var13 = var8.getInputStream(var10);
                        byte[] var14 = new byte[4096];
                        int var15 = 0;
                        var2.putNextEntry(var12);

                        while ((var15 = var13.read(var14)) != -1) {
                           var2.write(var14, 0, var15);
                        }
                     }
                  }
               } catch (Throwable var17) {
                  try {
                     var8.close();
                  } catch (Throwable var16) {
                     var17.addSuppressed(var16);
                  }

                  throw var17;
               }

               var8.close();
               var7.delete();
               var5.clear();
            }
         }
      }
   }

   private void writeDailyEquityFiles(ResultsGroup var1, JarOutputStream var2) throws Exception {
      for (String var4 : var1.getResultKeys()) {
         Result var5 = var1.subResult(var4);
         var4 = SQUtils.correctResultKeyToDirname(var4);
         JarEntry var6 = new JarEntry("Results/" + var4 + "/" + "dailyEquity.bin");
         var2.putNextEntry(var6);
         ObjectOutputStream var7 = new ObjectOutputStream(new BufferedOutputStream(var2));
         DailyEquity.serialize(var5, var7);
      }
   }

   private void writeStrategyFiles(ResultsGroup var1, JarOutputStream var2, String var3) throws Exception {
      for (String var5 : var1.getResultKeys()) {
         Result var6 = var1.subResult(var5);
         Element var7 = var6.getStrategyXml();
         if (var7 != null) {
            JarEntry var8 = new JarEntry("strategy_Portfolio.xml");
            var2.putNextEntry(var8);
            Document var9 = new Document(var7.clone());
            BufferedWriter var10 = new BufferedWriter(new OutputStreamWriter(var2, "UTF8"));
            XMLOutputter var11 = new XMLOutputter(Format.getPrettyFormat());
            String var12 = var11.outputString(var9);
            if (var3 != null) {
               var12 = MainApp.getDataProtector().encryptBase64(var12, var3);
            }

            var10.write(var12);
            var10.flush();
            return;
         }
      }
   }

   private void writeSettingsFile(Element var1, JarOutputStream var2) throws IOException {
      JarEntry var3 = new JarEntry("settings.xml");
      var2.putNextEntry(var3);
      Document var4 = new Document(var1);
      BufferedWriter var5 = new BufferedWriter(new OutputStreamWriter(var2, "UTF8"));
      XMLOutputter var6 = new XMLOutputter(Format.getRawFormat());
      var6.output(var4, var5);
   }

   private void writeLastSettingsFile(String var1, JarOutputStream var2) throws IOException {
      if (var1 != null) {
         JarEntry var3 = new JarEntry(this.lastSettingsFileName);
         var2.putNextEntry(var3);
         BufferedWriter var4 = new BufferedWriter(new OutputStreamWriter(var2, "UTF8"));
         var1 = SQUtils.optimizeLastSettings(var1);
         var4.write(var1);
         var4.flush();
      }
   }

   private void writeOrdersFile(OrdersList var1, JarOutputStream var2) throws Exception {
      JarEntry var3 = new JarEntry("orders.bin");
      var2.putNextEntry(var3);
      ObjectOutputStream var4 = new ObjectOutputStream(new BufferedOutputStream(var2));
      var1.serialize(var4);
   }

   private void writeOptimizationProfileFile(OptimizationProfile var1, JarOutputStream var2) throws IOException {
      if (var1 != null) {
         JarEntry var3 = new JarEntry(this.optimizationProfileFileName);
         var2.putNextEntry(var3);
         ObjectOutputStream var4 = new ObjectOutputStream(new BufferedOutputStream(var2));
         var1.serialize(var4);
      }
   }

   private void writeMergedStrategies(List<ResultsGroup> var1, JarOutputStream var2) {
      try {
         for (int var3 = 0; var3 < var1.size(); var3++) {
            ResultsGroup var4 = (ResultsGroup)var1.get(var3);
            File var5 = new File(StrategyMerger.TempPath + UUID.randomUUID());
            this.saveFile(var4, var5.getAbsolutePath(), null);
            BufferedInputStream var6 = null;
            JarEntry var7 = new JarEntry("PortfolioParts/" + var4.getName() + ".sqx");
            var2.putNextEntry(var7);

            try {
               var6 = new BufferedInputStream(new FileInputStream(var5));
               byte[] var8 = new byte[1024];

               int var9;
               while ((var9 = var6.read(var8)) != -1) {
                  var2.write(var8, 0, var9);
               }
            } finally {
               if (var6 != null) {
                  var6.close();
               }
            }

            try {
               var5.delete();
            } catch (Exception var14) {
               Log.error("Error while deleting temp file", var14);
            }
         }
      } catch (Exception var16) {
         Log.error("Failed to save merged strategies into ResultsGroup file", var16);
      }
   }

   public void saveStrategyFile(Element var1, String var2) throws Exception {
      File var3 = new File(var2);
      if (!var3.getParentFile().exists()) {
         var3.getParentFile().mkdirs();
      }

      Manifest var4 = new Manifest();
      var4.getMainAttributes().put(Name.MANIFEST_VERSION, "1.0");
      JarOutputStream var5 = new JarOutputStream(new BufferedOutputStream(new FileOutputStream(var3)), var4);

      try {
         JarEntry var6 = new JarEntry("strategy_Portfolio.xml");
         var5.putNextEntry(var6);
         BufferedWriter var7 = new BufferedWriter(new OutputStreamWriter(var5, "UTF8"));
         XMLOutputter var8 = new XMLOutputter(Format.getPrettyFormat());
         String var9 = var8.outputString(var1);
         var7.write(var9);
         var5.close();
      } catch (Exception var10) {
         var5.close();
         var3 = new File(var2);
         if (var3.exists()) {
            var3.delete();
         }

         throw var10;
      }
   }
}
