package com.strategyquant.plugin.Servlet.impl.CodeEditor.templates;

import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.app.MainApp;
import com.strategyquant.plugin.Servlet.impl.CodeEditor.CodeEditorServletException;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Templates {
   public static final Logger Log = LoggerFactory.getLogger(Templates.class);
   private final ArrayList<Template> availableTemplates = new ArrayList<>();
   public static final String TEMPLATES_PATH = MainApp.getDataPath() + "internal/web/" + "SQEDITOR" + "/templates/";
   public static final String CODE_BLOCKS_TPL_PATH = TEMPLATES_PATH + "/CodeBlocks.tpl";
   public static final String PSEUDO_CODE_BLOCKS_TPL_PATH = TEMPLATES_PATH + "/PseudoCodeBlocks.tpl";
   public static final String EMPTY_FILE_PATH = TEMPLATES_PATH + "/EmptyFile.tpl";

   public Templates() {
      this.loadAvailableTemplates();
   }

   public List<Template> getAvailableTemplates() {
      return this.availableTemplates;
   }

   public void loadAvailableTemplates() {
      try {
         this.availableTemplates.clear();
         this.loadTemplates(new File(TEMPLATES_PATH + "SQ"));
      } catch (Exception var2) {
         Log.error("Exc. ", var2);
      }
   }

   private void loadTemplates(File var1) {
      if (var1.isDirectory()) {
         File[] var2 = var1.listFiles();
         if (var2 != null) {
            for (File var6 : var2) {
               if (var6.isDirectory()) {
                  this.loadTemplates(var6);
               } else {
                  String var7 = SQUtils.getExtension(var6.getName());
                  if (var7.equalsIgnoreCase("tpl")) {
                     try {
                        this.availableTemplates.add(new Template(var6));
                     } catch (Exception var9) {
                        Log.error(String.format("Template '%s' could not be loaded. Reason: %s", var6.getAbsolutePath(), var9.getMessage()), var9);
                     }
                  }
               }
            }
         }
      }
   }

   public Template findTemplateByPath(String var1) throws CodeEditorServletException {
      for (Template var3 : this.availableTemplates) {
         if (var3.getTemplateFileAbsolutePath().equals(var1)) {
            return var3;
         }
      }

      throw new CodeEditorServletException(String.format("Template '%s' doesn't exist.", var1));
   }

   public Template findTemplateByFileName(String var1) throws CodeEditorServletException {
      for (Template var3 : this.availableTemplates) {
         String var4 = SQUtils.stripExtension(var3.getTemplateFileName());
         if (var4.equals(var1)) {
            return var3;
         }
      }

      throw new CodeEditorServletException("Template doesn't exist.");
   }
}
