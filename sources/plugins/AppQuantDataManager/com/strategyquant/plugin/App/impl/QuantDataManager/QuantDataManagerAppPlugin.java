package com.strategyquant.plugin.App.impl.QuantDataManager;

import com.strategyquant.lib.app.MainApp;
import com.strategyquant.pluginlib.annotations.Category;
import com.strategyquant.pluginlib.annotations.License;
import com.strategyquant.pluginlib.annotations.Name;
import com.strategyquant.pluginlib.annotations.ShortDesc;
import com.strategyquant.qdm.QDM;
import com.strategyquant.qdm.banners.Banner;
import com.strategyquant.qdm.banners.Banners;
import com.strategyquant.tradinglib.plugindef.app.IAppPlugin;
import com.strategyquant.tradinglib.project.websocket.DataToSend;
import com.strategyquant.tradinglib.project.websocket.SQWebSocketManager;
import com.strategyquant.tradinglib.servlet.IServletPlugin;
import com.strategyquant.webguilib.BrowserGUI;
import net.xeoh.plugins.base.annotations.PluginImplementation;
import net.xeoh.plugins.base.annotations.meta.Author;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Author(name = "Tomas Brynda")
@Name(name = "QuantDataManager")
@Category(name = "App")
@License(text = "")
@ShortDesc(text = "Short description")
@PluginImplementation
public class QuantDataManagerAppPlugin implements IAppPlugin, IServletPlugin {
   public static final Logger Log = LoggerFactory.getLogger(QuantDataManagerAppPlugin.class);
   private ServletContextHandler dataContext;
   public static long timeOfLastDisplayedPopupBanner = System.currentTimeMillis();
   private Thread t = null;

   public String getName() {
      return "QuantDataManager";
   }

   public String getProduct() {
      return "QDM";
   }

   public int getPreferredPosition() {
      return 0;
   }

   public void initPlugin() throws Exception {
      if (!MainApp.runInConsole()) {
         this.startBannerPublisher();
      }
   }

   public String getContextPath() {
      return "/qdm";
   }

   public String getAppCode() {
      return "QDM";
   }

   public String getTooltip() {
      return "QuantDataManager";
   }

   public String getProject() {
      return null;
   }

   public String getDefaultTaskType() {
      return null;
   }

   public String getDefaultTaskName() {
      return null;
   }

   private void startBannerPublisher() {
      if (this.t == null) {
         this.t = new Thread() {
            @Override
            public void run() {
               DataToSend var1 = new DataToSend();
               var1.setName("qdmBanner");
               long var2 = 0L;

               while (true) {
                  while (BrowserGUI.getInstance() == null || !BrowserGUI.getInstance().isAppLoaded()) {
                     try {
                        Thread.sleep(1000L);
                     } catch (InterruptedException var8) {
                     }
                  }

                  if (!MainApp.v571hfnsHw().tsPZHq7yG3()) {
                     JSONObject var6 = new JSONObject();
                     Banners var4 = QDM.getInstance().banners;
                     if ((System.currentTimeMillis() - var2) / 60000L >= var4.topBannersInMinutes) {
                        Banner var5 = var4.getNext("top");
                        if (var5 != null) {
                           var6.put("top", var5.toJSON());
                           var2 = System.currentTimeMillis();
                        }
                     }

                     if (QuantDataManagerAppPlugin.timeOfLastDisplayedPopupBanner > 0L
                        && (System.currentTimeMillis() - QuantDataManagerAppPlugin.timeOfLastDisplayedPopupBanner) / 60000L >= var4.popupBannersInMinutes) {
                        Banner var9 = var4.getNext("popup");
                        if (var9 != null) {
                           System.out.println("top banner: " + var9.toJSON().toString());
                           var6.put("popup", var9.toJSON());
                           QuantDataManagerAppPlugin.timeOfLastDisplayedPopupBanner = -1L;
                        }
                     }

                     var1.setDataObject(var6);
                     SQWebSocketManager.addToDataQueue(var1, new String[]{"QDM"});
                  }

                  try {
                     Thread.sleep(10000L);
                  } catch (InterruptedException var7) {
                  }
               }
            }
         };
         this.t.start();
      }
   }

   public Handler getHandler() {
      if (this.dataContext == null) {
         this.dataContext = new ServletContextHandler(1);
         this.dataContext.setContextPath("/qdm/");
         this.dataContext.addServlet(new ServletHolder(new QuantDataManagerServlet()), "/*");
      }

      return this.dataContext;
   }
}
