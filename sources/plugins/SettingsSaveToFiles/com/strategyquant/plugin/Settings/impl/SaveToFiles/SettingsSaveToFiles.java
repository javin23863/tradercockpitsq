package com.strategyquant.plugin.Settings.impl.SaveToFiles;

import com.strategyquant.lib.L;
import com.strategyquant.lib.XMLUtil;
import com.strategyquant.lib.sourcecode.SourceCodeGenerator;
import com.strategyquant.lib.sourcecode.SourceCodeGenerators;
import com.strategyquant.pluginlib.annotations.Category;
import com.strategyquant.pluginlib.annotations.License;
import com.strategyquant.pluginlib.annotations.Name;
import com.strategyquant.pluginlib.annotations.ShortDesc;
import com.strategyquant.tradinglib.Databank;
import com.strategyquant.tradinglib.project.ProjectConfigHelper;
import com.strategyquant.tradinglib.task.settings.ISettingTabPlugin;
import com.strategyquant.tradinglib.task.settings.TaskSettingsData;
import com.strategyquant.tradinglib.taskImpl.ISQTask;
import java.io.File;
import net.xeoh.plugins.base.annotations.PluginImplementation;
import net.xeoh.plugins.base.annotations.meta.Author;
import org.jdom2.Element;
import org.json.JSONArray;
import org.json.JSONObject;

@Author(name = "Tomas Brynda")
@Name(name = "Settings SaveToFiles plugin")
@Category(name = "Settings")
@License(text = "")
@ShortDesc(text = "Short description")
@PluginImplementation
public class SettingsSaveToFiles implements ISettingTabPlugin {
   private static final String tabTitle = "Save to files";

   public String getProduct() {
      return "SQUANT";
   }

   public int getPreferredPosition() {
      return 50;
   }

   public void initPlugin() throws Exception {
   }

   public void readSettings(String var1, ISQTask var2, Element var3, TaskSettingsData var4) {
      Element var5 = XMLUtil.tryAddElement(var3, this.getSettingName());

      try {
         var4.addParam("OverwriteFiles", XMLUtil.getBoolean(var5, "OverwriteFiles", false));
         String var6 = XMLUtil.getChildElem(var5, "DestDirectorySqx").getText();
         boolean var7 = Boolean.parseBoolean(XMLUtil.getChildElem(var5, "SaveInSqxFormat").getText());
         if (var7) {
            if (var6.isEmpty()) {
               throw new Exception(L.t("SQX target directory is not set.", new Object[0]));
            }

            if (!new File(var6).exists()) {
               throw new Exception(L.t("SQX target directory doesn't exist.", new Object[0]));
            }
         }

         var4.addParam("SaveInSqxFormat", var7);
         var4.addParam("DestDirectorySqx", var6);
         String var8 = XMLUtil.getChildElem(var5, "DestDirectoryStr").getText();
         boolean var9 = Boolean.parseBoolean(XMLUtil.getChildElem(var5, "SaveInStrFormat").getText());
         if (var9) {
            if (var8.isEmpty()) {
               throw new Exception(L.t("STR (SQ3) target directory is not set.", new Object[0]));
            }

            if (!new File(var8).exists()) {
               throw new Exception(L.t("STR (SQ3) target directory doesn't exist.", new Object[0]));
            }
         }

         var4.addParam("SaveInStrFormat", var9);
         var4.addParam("DestDirectoryStr", var8);
         String var10 = XMLUtil.getChildElem(var5, "DestDirectoryDatabank").getText();
         boolean var11 = Boolean.parseBoolean(XMLUtil.getChildElem(var5, "ExportDatabank").getText());
         if (var11) {
            if (var10.isEmpty()) {
               throw new Exception(L.t("Export databank contents target directory is not set.", new Object[0]));
            }

            if (!new File(var10).exists()) {
               throw new Exception(L.t("Export databank contents target directory doesn't exist.", new Object[0]));
            }
         }

         var4.addParam("ExportDatabank", var11);
         var4.addParam("DestDirectoryDatabank", var10);
         String var12 = XMLUtil.getChildElem(var5, "DestDirectoryTrades").getText();
         boolean var13 = Boolean.parseBoolean(XMLUtil.getChildElem(var5, "ExportTrades").getText());
         if (var13) {
            if (var12.isEmpty()) {
               throw new Exception(L.t("Export trades target directory is not set.", new Object[0]));
            }

            if (!new File(var12).exists()) {
               throw new Exception(L.t("Export trades target directory doesn't exist.", new Object[0]));
            }
         }

         var4.addParam("ExportTrades", var13);
         var4.addParam("DestDirectoryTrades", var12);
         String var14 = XMLUtil.getChildElem(var5, "DestDirectoryHtml").getText();
         boolean var15 = Boolean.parseBoolean(XMLUtil.getChildElem(var5, "SaveInHtmlFormat").getText());
         if (var15) {
            if (var14.isEmpty()) {
               throw new Exception(L.t("Html target directory is not set.", new Object[0]));
            }

            if (!new File(var14).exists()) {
               throw new Exception(L.t("Html target directory doesn't exist.", new Object[0]));
            }
         }

         var4.addParam("SaveInHtmlFormat", var15);
         var4.addParam("DestDirectoryHtml", var14);
         String var16 = XMLUtil.getChildElem(var5, "DestDirectoryPdf").getText();
         boolean var17 = Boolean.parseBoolean(XMLUtil.getChildElem(var5, "SaveInPdfFormat").getText());
         if (var17) {
            if (var16.isEmpty()) {
               throw new Exception(L.t("PDF target directory is not set.", new Object[0]));
            }

            if (!new File(var16).exists()) {
               throw new Exception(L.t("PDF target directory doesn't exist.", new Object[0]));
            }
         }

         var4.addParam("SaveInPdfFormat", var17);
         var4.addParam("DestDirectoryPdf", var16);
         String var18 = XMLUtil.getChildElem(var5, "DestDirectorySC").getText();
         Element var19 = XMLUtil.getChildElem(var5, "SaveSourceCode");
         boolean var20 = Boolean.parseBoolean(var19.getText());
         if (var20) {
            if (var18.isEmpty()) {
               throw new Exception(L.t("Source Code target directory is not set.", new Object[0]));
            }

            if (!new File(var18).exists()) {
               throw new Exception(L.t("Source Code target directory doesn't exist.", new Object[0]));
            }
         }

         var4.addParam("SaveSourceCode", var20);
         var4.addParam("DestDirectorySC", var18);
         String var21 = XMLUtil.getAttr(var19, "type");
         if (!var21.equals("Strategy XML")) {
            SourceCodeGenerator var22 = SourceCodeGenerators.getInstance().getGeneratorFromName(var21);
            if (var22 == null) {
               throw new Exception(L.t("Invalid source code generator", new Object[0]));
            }
         }

         var4.addParam("SourceCodeType", var21);
         String var25 = XMLUtil.getAttr(var19, "format", null);
         if (var21.equals(SourceCodeGenerator.EasyLanguage)) {
            var4.addParam("SourceCodeFormat", var25);
         } else {
            var4.addParam("SourceCodeFormat", null);
         }

         Databank var23 = ProjectConfigHelper.getInputDatabank(var1, var5.getParentElement());
         if (var23 == null) {
            throw new Exception(L.t("Source databank not found", new Object[0]));
         }

         var4.addParam("DatabankSource", var23);
         var4.addParam("MNActive", XMLUtil.getBoolean(var5, "MNActive", false));
         var4.addParam("MNValue", XMLUtil.getInt(var5, "MNValue", 12345));
         var4.addParam("Data", XMLUtil.getNodeValue(var5, "Data", "all"));
         var4.addParam("Format", XMLUtil.getNodeValue(var5, "Format", "xlsx"));
         var4.addParam("UseComma", XMLUtil.getBoolean(var5, "UseComma", false));
         var4.addParam("SetNoteActive", XMLUtil.getBoolean(var5, "SetNoteActive", false));
         var4.addParam("SetNoteType", XMLUtil.getNodeValue(var5, "SetNoteType", "timeframe"));
         var4.addParam("SetNoteCustom", XMLUtil.getNodeValue(var5, "SetNoteCustom", ""));
      } catch (Exception var24) {
         var4.addError(this.getSettingName(), null, "Cannot load SaveToFiles settings. " + var24.getMessage());
      }
   }

   public void getStrategyConfigSettings(Element var1, JSONArray var2) throws Exception {
   }

   public String getSettingName() {
      return "SaveToFiles";
   }

   public String getName() {
      return L.tsq("Save to files");
   }

   public JSONObject getInitializationData() throws Exception {
      return null;
   }
}
