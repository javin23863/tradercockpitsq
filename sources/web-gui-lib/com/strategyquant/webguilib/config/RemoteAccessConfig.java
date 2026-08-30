package com.strategyquant.webguilib.config;

import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.XMLUtil;
import com.strategyquant.lib.constants.SQPaths;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import org.jdom2.Element;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RemoteAccessConfig {
   public static final Logger Log = LoggerFactory.getLogger(RemoteAccessConfig.class);
   public static final String passwordHashFile = "remote_pwd";
   public static final String defaultPassword = "SQ-admin";
   private boolean remoteAccess;
   private boolean remoteAccessEnabled;
   private boolean requirePassword;
   private String password;
   private int passwordLength;
   private boolean secureConnection;
   private int token = -1;
   private static RemoteAccessConfig instance;

   private RemoteAccessConfig() {
      this.loadSettings();
      instance = this;
   }

   public static RemoteAccessConfig getInstance() {
      if (instance == null) {
         new RemoteAccessConfig();
      }

      return instance;
   }

   private void loadSettings() {
      try {
         File var1 = new File(SQPaths.remoteSettingsFile);
         if (var1.exists()) {
            Element var2 = XMLUtil.fileToXmlElement(var1);
            if (var2 != null) {
               String var3 = var2.getAttributeValue("enabled");
               if (var3 != null && var3.equals("true")) {
                  this.remoteAccess = true;
               } else {
                  this.remoteAccess = false;
               }

               Element var4 = var2.getChild("Password");
               if (var4 != null) {
                  try {
                     File var5 = new File(SQPaths.settingsDirPath + "/" + "remote_pwd");
                     if (var5.exists()) {
                        this.password = SQUtils.fileToString(var5);
                        this.passwordLength = 8;

                        try {
                           this.passwordLength = Integer.parseInt(var4.getAttributeValue("length"));
                        } catch (Exception var7) {
                        }
                     } else {
                        this.password = null;
                     }
                  } catch (Exception var8) {
                     Log.error("Cannot read remote password", var8);
                     this.password = null;
                  }

                  if (this.password == null) {
                     this.passwordLength = 0;
                     this.requirePassword = false;
                  } else {
                     String var10 = var4.getAttributeValue("require");
                     if (var10 != null && var10.equals("true")) {
                        this.requirePassword = true;
                     } else {
                        this.requirePassword = false;
                     }
                  }
               }

               Element var11 = var2.getChild("Connection");
               if (var11 != null) {
                  String var6 = var11.getAttributeValue("secure");
                  if (var6 != null && var6.equals("true")) {
                     this.secureConnection = true;
                  } else {
                     this.secureConnection = false;
                  }
               }
            }
         }
      } catch (Exception var9) {
         Log.error("Cannot load remote access settings", var9);
      }
   }

   public void saveSettings() {
      BufferedWriter var1 = null;

      try {
         File var2 = new File(SQPaths.remoteSettingsFile);
         var1 = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(var2), StandardCharsets.UTF_8));
         XMLOutputter var3 = new XMLOutputter(Format.getPrettyFormat());
         var3.output(this.createSettings(), var1);
         var1.close();
      } catch (Exception var12) {
         Log.error("Saving settings failed.", var12);
      } finally {
         if (var1 != null) {
            try {
               var1.close();
            } catch (Exception var11) {
               Log.error("Failed to close BufferedWriter", var11);
            }
         }
      }
   }

   private Element createSettings() {
      Element var1 = new Element("Remote-Access");
      var1.setAttribute("enabled", String.valueOf(this.remoteAccess));
      Element var2 = new Element("Password");
      var2.setAttribute("require", String.valueOf(this.requirePassword));
      var2.setAttribute("length", String.valueOf(this.passwordLength));
      if (this.password != null) {
         SQUtils.stringToFile(SQPaths.settingsDirPath + "/" + "remote_pwd", this.password);
      }

      Element var3 = new Element("Connection");
      var3.setAttribute("secure", String.valueOf(this.secureConnection));
      var1.addContent(var2);
      var1.addContent(var3);
      return var1;
   }

   public boolean isRemoteAccess() {
      return this.remoteAccessEnabled && this.remoteAccess;
   }

   public void setRemoteAccess(boolean var1) {
      if (var1 != this.remoteAccess) {
         this.remoteAccess = var1;
         this.recreateToken();
      }
   }

   public boolean isPasswordRequired() {
      return this.requirePassword;
   }

   public void setRequirePassword(boolean var1) {
      if (var1 != this.requirePassword) {
         this.requirePassword = var1;
         this.recreateToken();
      }
   }

   public String getPassword() {
      return this.password;
   }

   public void setPassword(String var1) throws Exception {
      if (var1 == null) {
         this.passwordLength = 0;
         this.requirePassword = false;
      } else {
         this.passwordLength = var1.length();
         this.password = this.getMD5(var1);
         this.recreateToken();
      }
   }

   public boolean isSecureConnection() {
      return this.secureConnection;
   }

   public void setSecureConnection(boolean var1) {
      if (var1 != this.secureConnection) {
         this.secureConnection = var1;
      }
   }

   public int getToken() {
      if (this.token == -1) {
         this.token = new Date().toString().hashCode();
      }

      return this.token;
   }

   public void recreateToken() {
      this.token = new Date().toString().hashCode();
   }

   public int getPasswordLength() {
      return this.passwordLength;
   }

   private String getMD5(String var1) throws Exception {
      return SQUtils.md5CheckSum(var1.getBytes());
   }

   public boolean passwordValid(String var1) throws Exception {
      String var2 = this.getMD5(var1);
      return this.password.equals(var2);
   }

   public void enableRemoteAccess() {
      this.remoteAccessEnabled = true;
   }

   public void forceRemoteAccessEnabled() {
      this.remoteAccessEnabled = true;
      this.remoteAccess = true;
   }
}
