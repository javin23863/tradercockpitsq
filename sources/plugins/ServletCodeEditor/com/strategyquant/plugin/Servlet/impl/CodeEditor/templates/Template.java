package com.strategyquant.plugin.Servlet.impl.CodeEditor.templates;

import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.snippets.compile.SQStructure;
import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Template {
   private final File fileTemplate;
   private String name;
   private String description;
   private String fileExt = "java";

   public Template(File var1) throws TemplateException {
      this.fileTemplate = var1;
      this.parse();
   }

   public String getDescription() {
      return this.description;
   }

   public String getName() {
      return this.name;
   }

   public String getExtension() {
      return this.fileExt;
   }

   public String getTemplateFileAbsolutePath() {
      return this.fileTemplate.getAbsolutePath();
   }

   public String getTemplateFileDirectoryPath() {
      return this.fileTemplate.getParentFile().getAbsolutePath();
   }

   public String getTemplateFileName() {
      return this.fileTemplate.getName();
   }

   private void parse() throws TemplateException {
      String var1;
      try {
         var1 = SQUtils.fileToString(this.fileTemplate);
      } catch (Exception var3) {
         throw new TemplateException("Unable to read template file");
      }

      int var2 = var1.indexOf("</definition>");
      if (var2 == -1) {
         throw new TemplateException("Missing tag <definition></definition>");
      }

      var1 = var1.substring(0, var2);
      this.name = this.parseTextBetweenTags("name", var1);
      this.description = this.parseTextBetweenTags("description", var1);
      this.fileExt = this.parseTextBetweenTags("extension", var1);
   }

   private String parseTextBetweenTags(String var1, String var2) throws TemplateException {
      Pattern var3 = Pattern.compile("<" + var1 + ">.*</" + var1 + ">");
      Matcher var4 = var3.matcher(var2);
      if (var4.find()) {
         String var5 = var4.group().trim();
         var5 = var5.replaceAll("<" + var1 + ">", "");
         return var5.replaceAll("</" + var1 + ">", "");
      } else {
         throw new TemplateException("Missing tag <" + var1 + "></" + var1 + ">");
      }
   }

   public String createNewFile(String var1, String var2, String var3) throws TemplateException {
      File var5 = new File(var2);
      if (!var5.exists() && !var5.mkdirs()) {
         throw new TemplateException("Unable to create directory path");
      }

      String var4;
      if (var5.isDirectory()) {
         var4 = var5.getAbsolutePath() + '/' + var1 + "." + this.fileExt;
      } else {
         var4 = var5.getParentFile().getAbsolutePath() + '/' + var1 + "." + this.fileExt;
      }

      File var6 = new File(var4);
      if (var6.exists()) {
         throw new TemplateException("File with this name already exists.");
      }

      String var7 = this.generateContent(var1, var6, var3);

      try {
         SQUtils.stringToFile(var6, var7);
      } catch (Exception var9) {
         throw new TemplateException(String.format("Unable to write file '%s'. Exc. %s", var6.getPath(), var9.toString()), var9);
      }

      return var6.getAbsolutePath();
   }

   private String generateContent(String var1, File var2, String var3) throws TemplateException {
      String var4;
      try {
         var4 = SQUtils.fileToString(this.fileTemplate);
      } catch (Exception var6) {
         throw new TemplateException(String.format("Unable to read template file '%s'. Exc. %s", var2.getPath(), var6.toString()), var6);
      }

      var4 = removeDefinition(var4);
      var4 = var4.replace("[class_name]", var1);
      if (var3 != null) {
         var4 = var4.replace("[indicator_name]", var3);
      }

      String var5 = SQUtils.trimFilePath(var2.getParentFile().getAbsolutePath(), SQStructure.getSnippetsUserDirPath());
      var5 = var5.replace("/", ".").replace(".java", "");
      return var4.replace("[package]", var5);
   }

   private static String removeDefinition(String var0) {
      String var1 = "</definition>";
      int var2 = var0.indexOf(var1);
      if (var2 != -1) {
         var0 = var0.substring(var2 + var1.length());
      }

      return var0.trim();
   }
}
