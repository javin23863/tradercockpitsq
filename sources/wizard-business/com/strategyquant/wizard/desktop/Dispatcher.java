package com.strategyquant.wizard.desktop;

import com.strategyquant.wizard.desktop.loader.CustomIndicatorLoader;
import com.strategyquant.wizard.desktop.loader.ErrorReportSender;
import com.strategyquant.wizard.desktop.loader.FavoriteItemsLoader;
import com.strategyquant.wizard.desktop.loader.LicenceLoader;
import com.strategyquant.wizard.desktop.loader.RecentFilesLoader;
import com.strategyquant.wizard.desktop.loader.ShareLoader;
import com.strategyquant.wizard.desktop.loader.StrategyLoader;
import com.strategyquant.wizard.desktop.loader.TransformEngineLoader;
import com.strategyquant.wizard.desktop.utils.FileUtils;
import com.strategyquant.wizard.desktop.utils.JsonUtils;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;

public class Dispatcher {
   public static final String SEPARATOR_PATTERN = "#;#";
   private TransformEngineLoader transformEngineLoader = new TransformEngineLoader(null);
   private RecentFilesLoader recentFilesLoader = new RecentFilesLoader();
   private StrategyLoader strategyLoader = new StrategyLoader(null);
   private LicenceLoader licenceLoader = new LicenceLoader();
   private CustomIndicatorLoader customIndicatorLoader = new CustomIndicatorLoader(null);
   private FavoriteItemsLoader favoriteItemsLoader = new FavoriteItemsLoader();
   private ErrorReportSender errorReportSender = new ErrorReportSender(null);
   private ShareLoader shareLoader = new ShareLoader();
   private Runnable initializedRunnable;

   public void setInitializedRunnable(Runnable var1) {
      this.initializedRunnable = var1;
   }

   public void dispose() {
      this.transformEngineLoader.dispose();
      this.strategyLoader.dispose();
      this.customIndicatorLoader.dispose();
      this.errorReportSender.dispose();
   }

   public String share(String var1, String var2, String var3, String var4) {
      return this.shareLoader.share(var1, var2, var3, var4);
   }

   public String execute(String var1) throws UnsupportedEncodingException, IOException, UnsupportedFlavorException {
      String[] var2 = var1.split("#;#");
      String var3 = var2[0];
      if (var3.equalsIgnoreCase("getAppVersion")) {
         return this.getVersion();
      } else if (var3.equalsIgnoreCase("verifyShare")) {
         return this.verifyShare(var2[1]);
      } else if (var3.equalsIgnoreCase("notifyShare")) {
         return this.notifyShare(var2[1], var2[2]);
      } else if (var3.equalsIgnoreCase("loadShared")) {
         return this.loadShared(var2[1], var2[2]);
      } else if (var3.equalsIgnoreCase("saveShared")) {
         return this.saveShared(var2[1], var2[2], var2[3]);
      } else if (var3.equalsIgnoreCase("transformCode")) {
         return this.transformCode(var2[1], var2[2]);
      } else if (var3.equalsIgnoreCase("loadTransformEngines")) {
         return this.loadTransformEngines();
      } else if (var3.equalsIgnoreCase("saveTransformedCode")) {
         this.saveTransformedCode(var2[1], var2[2], var2[3]);
         return "";
      } else if (var3.equalsIgnoreCase("loadFavorites")) {
         return this.loadFavorites();
      } else if (var3.equalsIgnoreCase("saveRecentFiles")) {
         return this.saveRecentFiles(var2[1], var2[2], var2[3]);
      } else if (var3.equalsIgnoreCase("loadRecentFiles")) {
         return this.loadRecentFiles(var2[1]);
      } else if (var3.equalsIgnoreCase("errorReport")) {
         return String.valueOf(this.errorReport(var2[1], var2[2]));
      } else if (var3.equalsIgnoreCase("exit")) {
         this.exit();
         return "";
      } else if (var3.equalsIgnoreCase("loadLicense")) {
         return this.loadLicense(var2[1]);
      } else if (var3.equalsIgnoreCase("loadFile")) {
         return this.loadFile(var2[1]);
      } else if (var3.equalsIgnoreCase("showDialogAndLoadFile")) {
         return this.showDialogAndLoadFile();
      } else if (var3.equalsIgnoreCase("saveFile")) {
         return this.saveFile(var2[1], var2[2], Boolean.parseBoolean(var2[3]));
      } else if (var3.equalsIgnoreCase("loadCloudFileList")) {
         return this.loadCloudFileList(var2[1], var2[2]);
      } else if (var3.equalsIgnoreCase("createFolder")) {
         return this.createFolder(var2[1], var2[2]);
      } else if (var3.equalsIgnoreCase("loadCloudFileContent")) {
         return this.loadCloudFileContent(var2[1], var2[2]);
      } else if (var3.equalsIgnoreCase("saveCloudFileContent")) {
         return this.saveCloudFileContent(var2[1], var2[2], var2[3]);
      } else if (var3.equalsIgnoreCase("importCustomIndicator")) {
         return this.importCustomIndicator();
      } else if (var3.equalsIgnoreCase("loadCustomIndicators")) {
         return this.importCustomIndicator(var2[1]);
      } else if (var3.equalsIgnoreCase("copyToClipboard")) {
         this.copyToClipboard(var2[1]);
         return "";
      } else if (var3.equalsIgnoreCase("paste")) {
         return this.paste(var2[1]);
      } else if (var3.equalsIgnoreCase("initializationOver")) {
         this.initializationOver();
         return "";
      } else {
         return null;
      }
   }

   public String verifyShare(String var1) {
      return this.shareLoader.verifyShare(var1);
   }

   public String notifyShare(String var1, String var2) {
      return this.shareLoader.notifyShare(var1, var2);
   }

   public String loadShared(String var1, String var2) {
      return this.shareLoader.loadShared(var1, var2);
   }

   public String saveShared(String var1, String var2, String var3) {
      return this.shareLoader.saveShared(var1, var2, var3);
   }

   public String transformCode(String var1, String var2) {
      return this.transformEngineLoader.transformCode(var1, var2);
   }

   public void initializationOver() {
      if (this.initializedRunnable != null) {
         this.initializedRunnable.run();
      }
   }

   public String loadTransformEngines() {
      return this.transformEngineLoader.load();
   }

   public void saveTransformedCode(String var1, String var2, String var3) {
      this.transformEngineLoader.save(var1, var2, var3);
   }

   public String loadFavorites() {
      return this.favoriteItemsLoader.getFavoriteItems();
   }

   public String saveRecentFiles(String var1, String var2, String var3) {
      return this.recentFilesLoader.saveRecentFiles(var1, var2, var3);
   }

   public String loadRecentFiles(String var1) {
      return this.recentFilesLoader.loadRecentFiles(var1);
   }

   public boolean errorReport(String var1, String var2) {
      return this.errorReportSender.send(var1, var2);
   }

   public String loadLicense(String var1) {
      return this.licenceLoader.loadLicense(var1);
   }

   public void exit() {
      this.dispose();
   }

   public String loadFile(String var1) {
      return this.strategyLoader.loadFile(var1);
   }

   public String showDialogAndLoadFile() {
      return this.strategyLoader.loadFileWithName();
   }

   public String saveFile(String var1, String var2, boolean var3) {
      try {
         return this.strategyLoader.saveFile(var1, var2, var3);
      } catch (IOException var5) {
         return JsonUtils.getErrorResonse(var5.getMessage());
      }
   }

   public String loadCloudFileList(String var1, String var2) {
      return this.strategyLoader.loadCloudFileList(var1, var2);
   }

   public String createFolder(String var1, String var2) {
      return this.strategyLoader.createCloudFolder(var1, var2);
   }

   public String loadCloudFileContent(String var1, String var2) {
      return this.strategyLoader.loadCloudFile(var1, var2);
   }

   public String saveCloudFileContent(String var1, String var2, String var3) {
      return this.strategyLoader.saveCloudFile(var1, var2, var3);
   }

   public String importCustomIndicator() {
      return this.customIndicatorLoader.loadFile();
   }

   public String importCustomIndicator(String var1) {
      return this.customIndicatorLoader.loadFile(var1);
   }

   public void copyToClipboard(String var1) {
      Clipboard var2 = Toolkit.getDefaultToolkit().getSystemClipboard();
      var2.setContents(new StringSelection(var1), null);
   }

   public String paste(String var1) throws UnsupportedFlavorException, IOException {
      Transferable var2 = Toolkit.getDefaultToolkit().getSystemClipboard().getContents(this);
      if (var2 != null && var2.isDataFlavorSupported(DataFlavor.stringFlavor)) {
         String var3 = (String)var2.getTransferData(DataFlavor.stringFlavor);
         if (var3 != null) {
            var3 = var3.replace("\n", "").replace("\r", "").replace("\t", "");
         }

         return "{\"result\":\"success\",\"content\":\"" + var3 + "\"}";
      } else {
         return null;
      }
   }

   public String getVersion() {
      File var1 = new File(FileUtils.getAppFolder() + "version");

      try {
         return new String(Files.readAllBytes(var1.toPath()));
      } catch (IOException var3) {
         return "{\"version\":\"unknown\"}";
      }
   }
}
