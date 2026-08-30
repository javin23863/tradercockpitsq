package com.strategyquant.plugin.Results.impl.RobustnessTests.views;

import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.XMLUtil;
import com.strategyquant.lib.app.MainApp;
import com.strategyquant.tradinglib.DatabankColumn;
import com.strategyquant.tradinglib.databank.DatabankColumns;
import com.strategyquant.tradinglib.databank.DatabankTableColumnEntry;
import com.strategyquant.tradinglib.databank.DatabankTableView;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import org.jdom2.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RTViewsManager {
   private static final Logger Log = LoggerFactory.getLogger(RTViewsManager.class);
   private HashMap<String, DatabankTableView> views = new HashMap<>();
   private final String viewsFolder;
   private final String fileExtension = "vw";
   private String type;

   public RTViewsManager(String var1, String var2) {
      this.type = var1;
      this.viewsFolder = var2;
      this.loadViews();
   }

   public void loadViews() {
      File var1 = new File(this.viewsFolder);
      if (!var1.exists()) {
         var1.mkdirs();
      } else {
         File[] var2 = var1.listFiles();
         if (var2 != null) {
            for (File var6 : var2) {
               try {
                  String var7 = SQUtils.fileToString(var6);
                  this._addView(var7, false);
               } catch (Exception var8) {
                  Log.error("Cannot load View from file '" + var6.getName() + "'");
               }
            }
         }
      }

      DatabankTableView var9 = this.createDefaultView();
      this.views.put("Default", var9);
   }

   public HashMap<String, DatabankTableView> getViews() {
      return this.views;
   }

   public DatabankTableView getView(String var1) throws Exception {
      if (this.views.containsKey(var1)) {
         return this.views.get(var1);
      } else {
         throw new Exception("View with name '" + var1 + "' doesn't exist.");
      }
   }

   public void addView(String var1) throws Exception {
      this._addView(var1, true);
   }

   private void _addView(String var1, boolean var2) throws Exception {
      DatabankTableView var3 = this.getTableView(var1);
      if (this.views.containsKey(var3.name)) {
         throw new Exception("View with name '" + var3.name + "' already exists.");
      }

      this.views.put(var3.name, var3);
      if (var2) {
         File var4 = new File(this.getViewFilePath(var3.name));
         SQUtils.stringToFile(var4, var1);
      }
   }

   public DatabankTableView updateView(String var1) throws Exception {
      DatabankTableView var2 = this.getTableView(var1);
      Element var3 = XMLUtil.stringToElement(var1);
      String var4 = var3.getAttributeValue("originalName");
      String var5 = var3.getAttributeValue("name");
      boolean var6 = var4 != null && !var4.equals(var5);
      if (var6) {
         this.views.remove(var4);
         File var7 = new File(this.getViewFilePath(var4));
         var7.delete();
      }

      if (!var6 && !this.views.containsKey(var2.name)) {
         throw new Exception("Databank view with name '" + var2.name + "' doesn't exist.");
      }

      this.views.put(var2.name, var2);
      File var8 = new File(this.getViewFilePath(var2.name));
      SQUtils.stringToFile(var8, var1);
      return var2;
   }

   public void removeView(String var1) throws Exception {
      if (this.views.containsKey(var1)) {
         this.views.remove(var1);
         File var2 = new File(this.getViewFilePath(var1));
         if (!var2.delete()) {
            throw new Exception("Cannot delete file '" + var2.getName() + "'");
         }
      } else {
         throw new Exception("View with name '" + var1 + "' doesn't exist.");
      }
   }

   public DatabankTableView createDefaultView() {
      DatabankTableView var1 = new DatabankTableView();
      var1.name = "Default - Main data";
      String[] var2 = new String[]{"NetProfit", "Drawdown", "ReturnDDRatio", "RExpectancy"};
      List var3 = DatabankColumns.get().cloneAvailableClasses();

      for (String var7 : var2) {
         for (DatabankColumn var9 : var3) {
            if (var9.getClass().getSimpleName().equals(var7)) {
               var1.columns.add(new DatabankTableColumnEntry(var9, "portfolio", (byte)0, (byte)10, (byte)127));
               break;
            }
         }
      }

      return var1;
   }

   private DatabankTableView getTableView(String var1) throws Exception {
      DatabankTableView var2 = new DatabankTableView();
      Element var3 = XMLUtil.stringToElement(SQUtils.removeUTF8BOM(var1));
      var2.setFromXML(var3);
      return var2;
   }

   private String getViewFilePath(String var1) {
      return this.viewsFolder + "/" + var1 + "." + "vw";
   }

   public DatabankTableView getSelectedView() {
      String var1 = MainApp.settings().get(this.type);
      if (var1 != null) {
         try {
            return this.getView(var1);
         } catch (Exception var4) {
            Log.error("Cannot load selected RT view. Using default view...");
         }
      }

      try {
         return this.getView("Default");
      } catch (Exception var3) {
         Log.error("Cannot load default RT view");
         return null;
      }
   }
}
