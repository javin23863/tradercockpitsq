package com.strategyquant.pluginlib;

import com.strategyquant.pluginlib.annotations.Category;
import com.strategyquant.pluginlib.annotations.DownloadUrl;
import com.strategyquant.pluginlib.annotations.License;
import com.strategyquant.pluginlib.annotations.LongDesc;
import com.strategyquant.pluginlib.annotations.Mandatory;
import com.strategyquant.pluginlib.annotations.Name;
import com.strategyquant.pluginlib.annotations.PageUrl;
import com.strategyquant.pluginlib.annotations.ShortDesc;
import java.io.File;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map.Entry;
import net.xeoh.plugins.base.PluginInformation;
import net.xeoh.plugins.base.PluginManager;
import net.xeoh.plugins.base.annotations.meta.Author;
import net.xeoh.plugins.base.annotations.meta.Version;
import net.xeoh.plugins.base.impl.PluginManagerFactory;
import net.xeoh.plugins.base.options.AddPluginsFromOption;
import net.xeoh.plugins.base.options.GetPluginOption;
import net.xeoh.plugins.base.util.PluginManagerUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SQPluginManager {
   public static final Logger Log = LoggerFactory.getLogger("SQPluginManager");
   public static final String PLUGIN_CATEGORY_OTHERS = "Others";
   private PluginManager pm = null;
   private static PluginManagerUtil pmu;
   private PluginInformation pi;
   private String product = null;
   private HashMap<Class, ArrayList> loadedPluginsMap = new HashMap<>();
   private HashMap<Class, ArrayList> availablePluginsMap = new HashMap<>();
   private static SQPluginManager instance;
   private static String workDirectory = null;

   public static SQPluginManager getInstance() {
      if (instance == null) {
         instance = new SQPluginManager();
      }

      return instance;
   }

   public static void setWorkDirectory(String var0) {
      workDirectory = var0;
   }

   public HashMap<Class, ArrayList> getAvailablePlugins() {
      return this.availablePluginsMap;
   }

   public static <P extends ISQPlugin> String getPluginName(Class<P> var0) {
      String var1 = var0.toString();
      int var2 = var1.lastIndexOf(".");
      return var2 > 0 ? var1.substring(var2 + 1) : var1;
   }

   public static void initForProduct(String var0) throws Exception {
      initForProduct(var0, null);
   }

   public static void initForProduct(String var0, String var1) throws Exception {
      SQPluginManager var2 = getInstance();
      var2.pm = PluginManagerFactory.createPluginManager();
      var2.pi = (PluginInformation)var2.pm.getPlugin(PluginInformation.class, new GetPluginOption[0]);
      pmu = new PluginManagerUtil(var2.pm);
      loadFromFolder("internal/plugins", var1);
      loadFromFolder("user/extend/Plugins", var1);
      var2.product = var0;
   }

   private static void loadFromFolder(String var0, String var1) throws Exception {
      String var2;
      if (workDirectory == null) {
         var2 = var0;
      } else {
         var2 = workDirectory + var0;
      }

      if (var1 != null && !var1.equalsIgnoreCase("")) {
         var2 = var2 + "/" + var1;
      }

      instance.pm.addPluginsFrom(new File(var2).toURI(), new AddPluginsFromOption[0]);

      for (String var4 : DirManager.listAllSubdirectoryPaths(var2)) {
         instance.pm.addPluginsFrom(new File(var4).toURI(), new AddPluginsFromOption[0]);
      }
   }

   public static <P extends ISQPlugin> ArrayList<P> getPlugins(Class<P> var0) {
      return getInstance()._getPlugins(var0);
   }

   private <P extends ISQPlugin> ArrayList<P> _getPlugins(Class<P> var1) {
      return !this.loadedPluginsMap.containsKey(var1) ? new ArrayList<>() : this.loadedPluginsMap.get(var1);
   }

   public static <P extends ISQPlugin> void loadPlugins(Class<P> var0) throws Exception {
      getInstance()._loadPlugins(var0);
   }

   private <P extends ISQPlugin> void _loadPlugins(Class<P> var1) throws Exception {
      if (!this.loadedPluginsMap.containsKey(var1)) {
         ArrayList var2 = (ArrayList)pmu.getPlugins(var1);
         ArrayList var3 = new ArrayList();
         ArrayList var4 = new ArrayList();
         Collections.sort(var2, ISQPlugin.PositionComparator);
         StringBuilder var5 = new StringBuilder();
         StringBuilder var6 = new StringBuilder();
         var5.append("Loading plugins extending ");
         var5.append(var1.getSimpleName());
         var5.append(": ");

         for (int var7 = 0; var7 < var2.size(); var7++) {
            ISQPlugin var8 = (ISQPlugin)var2.get(var7);
            String var9 = getPluginName(var8);
            var5.append(var7 == 0 ? "" : ", ");
            var5.append(var9);
            if (var8.getProduct() == null) {
               var6.append("Plugin '");
               var6.append(var9);
               var6.append("' has no product defined, it is skipped!\n");
            } else if (!this.product.equals("TESTAPP") && !var8.getProduct().equals("any") && !var8.getProduct().contains(this.product)) {
               var6.append("Plugin '");
               var6.append(var9);
               var6.append("' is created for another product, not '");
               var6.append(this.product);
               var6.append("'.\n");
            } else {
               var3.add(var8);
               var4.add(var8);
            }
         }

         if (Log.isDebugEnabled()) {
            Log.debug(var5.toString());
            if (var6.length() > 0) {
               Log.debug(var6.toString());
            }
         }

         this.loadedPluginsMap.put(var1, var3);
         this.availablePluginsMap.put(var1, var4);

         for (ISQPlugin var11 : var3) {
            var11.initPlugin();
         }
      }
   }

   public static String getPluginAuthor(ISQPlugin var0) {
      Class var1 = var0.getClass();
      Annotation var2 = var1.getAnnotation(Author.class);
      if (var2 instanceof Author) {
         Author var3 = (Author)var2;
         return var3.name();
      } else {
         return "Unknown";
      }
   }

   public static String getLicense(ISQPlugin var0) {
      Class var1 = var0.getClass();
      Annotation var2 = var1.getAnnotation(License.class);
      if (var2 instanceof License) {
         License var3 = (License)var2;
         return var3.text();
      } else {
         return "Unknown";
      }
   }

   public static boolean isPluginMandatory(ISQPlugin var0) {
      Class var1 = var0.getClass();
      Annotation var2 = var1.getAnnotation(Mandatory.class);
      if (var2 instanceof Mandatory) {
         Mandatory var3 = (Mandatory)var2;
         return var3.value();
      } else {
         return false;
      }
   }

   public static String getPluginName(ISQPlugin var0) {
      Class var1 = var0.getClass();
      Annotation var2 = var1.getAnnotation(Name.class);
      if (var2 instanceof Name) {
         Name var3 = (Name)var2;
         return var3.name();
      } else {
         return var0.getClass().getSimpleName();
      }
   }

   public static String getPluginCategory(ISQPlugin var0) {
      Class var1 = var0.getClass();
      Annotation var2 = var1.getAnnotation(Category.class);
      if (var2 instanceof Category) {
         Category var3 = (Category)var2;
         return var3.name();
      } else {
         return "Others";
      }
   }

   public static String getPluginShortDesc(ISQPlugin var0) {
      Class var1 = var0.getClass();
      Annotation var2 = var1.getAnnotation(ShortDesc.class);
      if (var2 instanceof ShortDesc) {
         ShortDesc var3 = (ShortDesc)var2;
         return var3.text();
      } else {
         return "";
      }
   }

   public static String getPluginLongDesc(ISQPlugin var0) {
      Class var1 = var0.getClass();
      Annotation var2 = var1.getAnnotation(LongDesc.class);
      if (var2 instanceof LongDesc) {
         LongDesc var3 = (LongDesc)var2;
         return var3.text();
      } else {
         return "";
      }
   }

   public static String getPluginPageUrl(ISQPlugin var0) {
      Class var1 = var0.getClass();
      Annotation var2 = var1.getAnnotation(PageUrl.class);
      if (var2 instanceof PageUrl) {
         PageUrl var3 = (PageUrl)var2;
         return var3.url();
      } else {
         return "";
      }
   }

   public static String getPluginDownloadUrl(ISQPlugin var0) {
      Class var1 = var0.getClass();
      Annotation var2 = var1.getAnnotation(DownloadUrl.class);
      if (var2 instanceof DownloadUrl) {
         DownloadUrl var3 = (DownloadUrl)var2;
         return var3.url();
      } else {
         return "";
      }
   }

   public static int getPluginVersion(ISQPlugin var0) {
      Class var1 = var0.getClass();
      Annotation var2 = var1.getAnnotation(Version.class);
      if (var2 instanceof Version) {
         Version var3 = (Version)var2;
         return var3.version();
      } else {
         return 0;
      }
   }

   public static String printPluginVersion(ISQPlugin var0) {
      int var1 = getPluginVersion(var0);
      return formatVersion(var1);
   }

   public static ISQPlugin findPluginByName(String var0) {
      if (var0 == null) {
         return null;
      }

      for (Entry var2 : getInstance().availablePluginsMap.entrySet()) {
         for (Object var4 : (ArrayList)var2.getValue()) {
            if (var4 instanceof ISQPlugin && var0.equals(getPluginName((ISQPlugin)var4))) {
               return (ISQPlugin)var4;
            }
         }
      }

      return null;
   }

   public static String formatVersion(int var0) {
      try {
         String var1 = "";
         String var2 = String.valueOf(var0);
         if (var2.length() == 5) {
            var2 = "0" + var2;
         } else if (var2.length() != 6) {
            throw new Exception("Incorrect version definition. Version=" + var0);
         }

         var1 = Integer.parseInt(var2.substring(0, 2)) + "";
         var1 = var1 + "." + Integer.parseInt(var2.substring(2, 4));
         return var1 + "." + Integer.parseInt(var2.substring(4, 6));
      } catch (Exception var3) {
         Log.error("Exc.", var3);
         return "0";
      }
   }

   public static <P extends ISQPlugin> ArrayList<P> getAllAvailablePlugins(Class<P> var0) {
      return (ArrayList<P>)pmu.getPlugins(var0);
   }
}
