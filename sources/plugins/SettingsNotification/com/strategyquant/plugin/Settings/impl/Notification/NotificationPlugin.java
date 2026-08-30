package com.strategyquant.plugin.Settings.impl.Notification;

import com.strategyquant.lib.L;
import com.strategyquant.lib.XMLUtil;
import com.strategyquant.lib.app.MainApp;
import com.strategyquant.lib.utils.EmailSender;
import com.strategyquant.pluginlib.annotations.Category;
import com.strategyquant.pluginlib.annotations.License;
import com.strategyquant.pluginlib.annotations.Name;
import com.strategyquant.pluginlib.annotations.ShortDesc;
import com.strategyquant.tradinglib.servlet.IServletPlugin;
import com.strategyquant.tradinglib.task.settings.ISettingTabPlugin;
import com.strategyquant.tradinglib.task.settings.TaskSettingsData;
import com.strategyquant.tradinglib.taskImpl.ISQTask;
import net.xeoh.plugins.base.annotations.PluginImplementation;
import net.xeoh.plugins.base.annotations.meta.Author;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.jdom2.Element;
import org.json.JSONArray;
import org.json.JSONObject;

@Author(name = "Tomas Brynda")
@Name(name = "Settings Notification plugin")
@Category(name = "Settings")
@License(text = "")
@ShortDesc(text = "Short description")
@PluginImplementation
public class NotificationPlugin implements ISettingTabPlugin, IServletPlugin {
   private ServletContextHandler dataContext;
   private NotificationServlet servlet;

   public String getProduct() {
      return "SQUANT";
   }

   public int getPreferredPosition() {
      return 50;
   }

   public void initPlugin() throws Exception {
   }

   public Handler getHandler() {
      if (this.dataContext == null) {
         this.servlet = new NotificationServlet();
         this.dataContext = new ServletContextHandler(1);
         this.dataContext.setContextPath("/notification/");
         this.dataContext.addServlet(new ServletHolder(this.servlet), "/*");
      }

      return this.dataContext;
   }

   public void readSettings(String var1, ISQTask var2, Element var3, TaskSettingsData var4) {
      Element var5 = null;

      try {
         var5 = XMLUtil.getChildElem(var3, this.getSettingName());
      } catch (Exception var10) {
         var4.addError(this.getSettingName(), null, var10.getMessage());
         return;
      }

      try {
         var4.addParam("Type", XMLUtil.getChildElem(var5, "Type").getText());
         var4.addParam("Email", XMLUtil.getChildElem(var5, "Email").getText());
         var4.addParam("PauseProject", Boolean.parseBoolean(XMLUtil.getChildElem(var5, "Wait").getText()));
         String var6 = (String)var4.getParams().get("Type");
         String var7 = (String)var4.getParams().get("Email");
         if (var6.toLowerCase().equals("email")) {
            if (EmailSender.isValidEmailAddress(var7)) {
               var4.addParam("SMTPServer", MainApp.settings().get("SMTPServer", ""));
               var4.addParam("SMTPPort", Integer.parseInt(MainApp.settings().get("SMTPPort", "25")));
               var4.addParam("SMTPUsername", MainApp.settings().get("SMTPUsername", ""));
               var4.addParam("SMTPPassword", MainApp.settings().get("SMTPPassword", ""));
               var4.addParam("SMTPEmailFrom", MainApp.settings().get("SMTPEmailFrom", ""));
               var4.addParam("SMTPUseSSL", Boolean.parseBoolean(MainApp.settings().get("SMTPUseSSL", "false")));
               String var8 = (String)var4.getParams().get("SMTPServer");
               if (var8.trim().isEmpty()) {
                  var4.addError(
                     this.getSettingName(),
                     "smtp",
                     L.t("To enable email notifications, you need to configure email server on Global setting -> SMTP server panel.", new Object[0])
                  );
               }
            } else {
               var4.addError(this.getSettingName(), "email", L.t("Email is not valid.", new Object[0]));
            }
         }
      } catch (Exception var9) {
         var4.addError(this.getSettingName(), null, "Cannot load Notification settings. " + var9.getMessage());
      }
   }

   public void getStrategyConfigSettings(Element var1, JSONArray var2) throws Exception {
   }

   public String getSettingName() {
      return "Notification";
   }

   public String getName() {
      return L.tsq("Notification");
   }

   public JSONObject getInitializationData() throws Exception {
      JSONObject var1 = new JSONObject();
      var1.put("types", this.servlet.getTypes());
      return var1;
   }
}
