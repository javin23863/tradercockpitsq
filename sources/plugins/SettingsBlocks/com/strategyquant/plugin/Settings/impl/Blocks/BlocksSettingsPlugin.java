package com.strategyquant.plugin.Settings.impl.Blocks;

import com.strategyquant.lib.L;
import com.strategyquant.lib.XMLUtil;
import com.strategyquant.lib.volumeProfile.VolumeProfileBlocks;
import com.strategyquant.lib.volumeProfile.VolumeProfileSubscription;
import com.strategyquant.pluginlib.annotations.Category;
import com.strategyquant.pluginlib.annotations.License;
import com.strategyquant.pluginlib.annotations.Name;
import com.strategyquant.pluginlib.annotations.ShortDesc;
import com.strategyquant.tradinglib.blocks.CustomBlocks;
import com.strategyquant.tradinglib.blocks.random.BlockDefinition;
import com.strategyquant.tradinglib.blocks.random.BlocksConfig;
import com.strategyquant.tradinglib.servlet.IServletPlugin;
import com.strategyquant.tradinglib.task.settings.ISettingTabPlugin;
import com.strategyquant.tradinglib.task.settings.TaskSettingsData;
import com.strategyquant.tradinglib.task.settings.blocks.ConfigVerifier;
import com.strategyquant.tradinglib.taskImpl.ISQTask;
import java.util.HashMap;
import java.util.List;
import net.xeoh.plugins.base.annotations.PluginImplementation;
import net.xeoh.plugins.base.annotations.meta.Author;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.jdom2.Element;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Author(name = "Tomas Brynda")
@Name(name = "Blocks servlet plugin")
@Category(name = "Settings")
@License(text = "")
@ShortDesc(text = "Short description")
@PluginImplementation
public class BlocksSettingsPlugin implements ISettingTabPlugin, IServletPlugin {
   public static final Logger Log = LoggerFactory.getLogger(BlocksSettingsPlugin.class);
   private static final String tabTitle = "Building Blocks";
   private ServletContextHandler dataContext;
   private BlocksServlet servlet;

   public Handler getHandler() {
      if (this.dataContext == null) {
         this.servlet = new BlocksServlet();
         this.dataContext = new ServletContextHandler(1);
         this.dataContext.setContextPath("/blocks/");
         this.dataContext.addServlet(new ServletHolder(this.servlet), "/*");
      }

      return this.dataContext;
   }

   public String getProduct() {
      return "SQUANT";
   }

   public int getPreferredPosition() {
      return 30;
   }

   public void initPlugin() throws Exception {
   }

   private void fixSettings(Element var1) {
      this.fixCustomBlocks(var1);
   }

   private void fixCustomBlocks(Element var1) {
      try {
         JSONArray var2 = CustomBlocks.getAsBuildingBlocks();
         HashMap var3 = new HashMap();

         for (int var4 = 0; var4 < var2.length(); var4++) {
            JSONObject var5 = var2.getJSONObject(var4);
            String var6 = var5.getString("key");
            var3.put(var6, var5);
         }

         Element var21 = XMLUtil.tryAddElement(var1, this.getSettingName());
         Element var22 = XMLUtil.tryAddElement(var21, "BuildingBlocks");
         List var23 = var22.getChildren("Block");

         for (int var7 = 0; var7 < var23.size(); var7++) {
            Element var8 = (Element)var23.get(var7);
            String var9 = var8.getAttributeValue("key");
            if (var9 != null && var9.startsWith("CBlock_")) {
               JSONObject var10 = (JSONObject)var3.get(var9);
               if (var10 != null) {
                  Element var11 = var8.getChild("Generated");
                  if (var11 != null) {
                     List var12 = var11.getChildren("Param");
                     JSONArray var13 = var10.getJSONArray("params");
                     if (var12.size() != var13.length()) {
                        Log.info("Fixing CB with key '" + var9 + "' - updating params, expected count: " + var13.length() + ", existing count: " + var12.size());
                        var11.removeChildren("Param");

                        for (int var14 = 0; var14 < var13.length(); var14++) {
                           JSONObject var15 = var13.getJSONObject(var14);
                           Element var16 = new Element("Param");

                           for (String var18 : var15.keySet()) {
                              String var19 = var15.get(var18).toString();
                              var16.setAttribute(var18, var19);
                           }

                           var11.addContent(var16);
                        }
                     }
                  }
               }
            }
         }
      } catch (Exception var20) {
         Log.error("Error while fixing Custom blocks in settings.", var20);
      }
   }

   private void disableVPBlocksIfInactive(Element var1) {
      if (!VolumeProfileSubscription.getInstance().isActive()) {
         for (Element var3 : var1.getChildren("Block")) {
            String var4 = var3.getAttributeValue("key");
            if (var4 != null) {
               String var5 = var4.contains(".") ? var4.substring(var4.lastIndexOf(".") + 1) : var4;
               if (VolumeProfileBlocks.contains(var5)) {
                  var3.setAttribute("use", "false");
               }
            }
         }
      }
   }

   public void readSettings(String var1, ISQTask var2, Element var3, TaskSettingsData var4) {
      BlocksConfig var5 = null;
      this.fixSettings(var3);

      Element var7;
      Element var8;
      Element var9;
      try {
         Element var6 = XMLUtil.tryAddElement(var3, this.getSettingName());
         var7 = XMLUtil.tryAddElement(var6, "BuildingBlocks");
         var8 = XMLUtil.tryAddElement(var6, "OrderTypes");
         var9 = XMLUtil.tryAddElement(var6, "ExitTypes");
         Element var10 = XMLUtil.tryAddElement(var6, "CustomData");
         this.disableVPBlocksIfInactive(var7);
         var4.addParam("BuildingBlocks", var7);
         var4.addParam("OrderTypes", var8);
         var4.addParam("ExitTypes", var9);
         var4.addParam("CustomData", var10);
      } catch (Exception var14) {
         Log.error("Cannot load blocks config from XML file.", var14);
         var4.addError("Building Blocks", null, "Cannot load blocks config from XML file. Error: " + var14.getMessage());
         return;
      }

      try {
         var5 = BlocksConfig.getConfig();
         ConfigVerifier.verifyConfig(var3, var5, var4);
      } catch (Exception var13) {
         Log.error("Cannot load blocks config from XML file.", var13);
         var4.addError("Building Blocks", null, "Cannot load blocks config from XML file. Error: " + var13.getMessage());
         return;
      }

      try {
         var4.addParam("BlocksConfig", var3);
         var4.addParam("BlocksDesciption", this.generateBlockDescription(var7, var8, var9));
      } catch (Exception var12) {
         Log.error("Error while converting simple blocks.", var12);
         var4.addError("Building Blocks", null, "Error while converting simple blocks. Error: " + var12.getMessage());
      }
   }

   private String generateBlockDescription(Element var1, Element var2, Element var3) {
      int var4 = XMLUtil.countElements("Block", var1, 0);
      int var5 = XMLUtil.countElements("Block", var3, 0);
      return L.t("%d entry blocks, %d exit types", new Object[]{var4, var5});
   }

   public String getSettingName() {
      return "Blocks";
   }

   public String getName() {
      return L.tsq("Building blocks");
   }

   public void getStrategyConfigSettings(Element var1, JSONArray var2) throws Exception {
      Element var3 = var1.getChild("Blocks");
      if (var3 != null) {
         Element var4 = var1.getChild("BuildingBlocks");
         if (var4 != null) {
            Element var5 = var1.getChild("OrderTypes");
            if (var5 != null) {
               Element var6 = var1.getChild("ExitTypes");
               if (var6 != null) {
                  Element var7 = var1.getChild("CustomData");
                  if (var7 != null) {
                     BlocksConfig var8 = BlocksConfig.getConfig();
                     this.addBlocks(var4, var2, var8);
                     this.addBlocks(var5, var2, var8);
                     this.addBlocks(var6, var2, var8);
                     this.addBlocks(var7, var2, var8);
                  }
               }
            }
         }
      }
   }

   private void addBlocks(Element var1, JSONArray var2, BlocksConfig var3) {
      for (Element var5 : var1.getChildren("Block")) {
         String var6 = var5.getAttributeValue("key");
         BlockDefinition var7 = var3.getBlock(var6);
         if (!XMLUtil.elementIsNot(var5, "use")) {
            String var8 = var7 != null ? var7.getName() : var6;
            var2.put(new JSONObject().put("key", var8).put("value", L.t("yes", new Object[0])));
         }
      }
   }

   public JSONObject getInitializationData() throws Exception {
      JSONObject var1 = new JSONObject();
      var1.put("stockpickerDefaultBlocks", this.servlet.loadStockpickerDefaultBlocks());
      return var1;
   }
}
