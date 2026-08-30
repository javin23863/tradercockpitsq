package com.strategyquant.tradinglib.tradelist;

import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.XMLUtil;
import com.strategyquant.lib.app.MainApp;
import com.strategyquant.lib.snippets.CustomClassesReg;
import com.strategyquant.lib.snippets.ICustomClasses;
import com.strategyquant.tradinglib.TradelistColumn;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.locks.StampedLock;
import org.jdom2.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TradelistViewsManager implements ICustomClasses {
   private static final Logger Log = LoggerFactory.getLogger(TradelistViewsManager.class);
   private static final String viewsFolder = MainApp.getDataPath() + "/user/settings/views/tradelists";
   private static final String fileExtension = "vw";
   private static final String defaultViewName = "Default";
   private HashMap<String, TradelistTableView> views = new HashMap<>();
   private StampedLock lock = new StampedLock();
   private static TradelistViewsManager instance;

   public static synchronized TradelistViewsManager getInstance() {
      if (instance == null) {
         instance = new TradelistViewsManager();
      }

      return instance;
   }

   private TradelistViewsManager() {
      CustomClassesReg.add(this);
      this.loadViews();
   }

   private void loadViews() {
      this.views.clear();
      File var1 = new File(getViewsFolder());
      if (!var1.exists()) {
         var1.mkdirs();
      } else {
         File[] var2 = var1.listFiles();
         if (var2 != null) {
            for (File var6 : var2) {
               try {
                  String var7 = SQUtils.fileToString(var6);
                  this.addView(var7, false);
               } catch (Exception var8) {
                  Log.error("Cannot load tradelist view from file '" + var6.getName() + "'");
               }
            }
         }
      }

      TradelistTableView var9 = createDefaultView();
      this.views.put("Default", var9);
   }

   public HashMap<String, TradelistTableView> getViews() {
      return this.views;
   }

   public TradelistTableView getView(String var1) throws Exception {
      if (this.views.containsKey(var1)) {
         return this.views.get(var1);
      } else {
         throw new Exception("View with name '" + var1 + "' doesn't exist.");
      }
   }

   public TradelistTableView addView(String var1, boolean var2) throws Exception {
      long var3 = this.lock.writeLock();

      try {
         TradelistTableView var5 = getTableView(var1);
         var5.lastChanged = System.currentTimeMillis();
         if (this.views.containsKey(var5.name)) {
            throw new Exception("Tradelist view with name '" + var5.name + "' already exists.");
         }

         this.views.put(var5.name, var5);
         if (var2) {
            File var6 = new File(getViewFilePath(var5.name));
            SQUtils.stringToFile(var6, var1);
         }

         return var5;
      } finally {
         this.lock.unlock(var3);
      }
   }

   public TradelistTableView updateView(String var1) throws Exception {
      long var2 = this.lock.writeLock();

      try {
         TradelistTableView var4 = getTableView(var1);
         var4.lastChanged = System.currentTimeMillis();
         Element var5 = XMLUtil.stringToElement(var1);
         String var6 = var5.getAttributeValue("originalName");
         String var7 = var5.getAttributeValue("name");
         boolean var8 = var6 != null && !var6.equals(var7);
         if (var8) {
            this.views.remove(var6);
            File var9 = new File(getViewFilePath(var6));
            var9.delete();
         }

         if (!var8 && !this.views.containsKey(var4.name)) {
            throw new Exception("Tradelist view with name '" + var4.name + "' doesn't exist.");
         }

         this.views.put(var4.name, var4);
         File var13 = new File(getViewFilePath(var4.name));
         SQUtils.stringToFile(var13, var1);
         return var4;
      } finally {
         this.lock.unlock(var2);
      }
   }

   public void removeView(String var1) throws Exception {
      long var2 = this.lock.writeLock();

      try {
         if (!this.views.containsKey(var1)) {
            throw new Exception("Tradelist view with name '" + var1 + "' doesn't exist.");
         }

         this.views.remove(var1);
         File var4 = new File(getViewFilePath(var1));
         if (!var4.delete()) {
            throw new Exception("Cannot delete file '" + var4.getName() + "'");
         }
      } finally {
         this.lock.unlock(var2);
      }
   }

   private static TradelistTableView getTableView(String var0) throws Exception {
      TradelistTableView var1 = new TradelistTableView();
      Element var2 = XMLUtil.stringToElement(SQUtils.removeUTF8BOM(var0));
      var1.setFromXML(var2);
      return var1;
   }

   private static String getViewFilePath(String var0) {
      return getViewsFolder() + "/" + var0 + "." + "vw";
   }

   protected static String getViewsFolder() {
      return viewsFolder;
   }

   public static TradelistTableView createDefaultView() {
      TradelistTableView var0 = new TradelistTableView();
      var0.name = "Default";
      String[] var1 = new String[]{
         "Ticket",
         "Symbol",
         "Type",
         "OpenTime",
         "OpenPrice",
         "Size",
         "CloseTime",
         "ClosePrice",
         "ProfitLoss",
         "Balance",
         "SampleType",
         "CloseType",
         "MAE",
         "MFE",
         "TimeInTrade",
         "Comment"
      };
      List var2 = TradelistColumnsList.get().cloneAvailableClasses();

      for (String var6 : var1) {
         for (TradelistColumn var8 : var2) {
            if (var8.getClass().getSimpleName().equals(var6)) {
               var0.columns.add(new TradelistTableColumnEntry(var8));
               break;
            }
         }
      }

      return var0;
   }

   public TradelistTableView getSelectedTradelistView() {
      long var1 = this.lock.writeLock();

      try {
         String var3 = MainApp.settings().get("tradelistView");
         if (var3 != null) {
            try {
               return this.getView(var3);
            } catch (Exception var10) {
               Log.error("Cannot load selected tradelist view. Using default view...");
               MainApp.settings().set("tradelistView", "Default");
            }
         }

         try {
            return this.getView("Default");
         } catch (Exception var9) {
            Log.error("Cannot load default tradelist view");
            return null;
         }
      } finally {
         this.lock.unlock(var1);
      }
   }

   public void reload() throws Exception {
      this.loadViews();
   }
}
