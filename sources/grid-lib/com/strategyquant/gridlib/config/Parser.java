package com.strategyquant.gridlib.config;

import com.strategyquant.gridlib.message.sync.FolderConfigs;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.util.LinkedList;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;

public class Parser {
   public Config parse(File var1) throws UnsupportedEncodingException, IOException, JDOMException {
      SAXBuilder var2 = new SAXBuilder();
      var2.setIgnoringElementContentWhitespace(true);
      var2.setIgnoringBoundaryWhitespace(true);
      String var3 = new String(Files.readAllBytes(var1.toPath()), "UTF-8");
      var3 = var3.replaceAll(">\\s*<", "><");
      ByteArrayInputStream var4 = new ByteArrayInputStream(var3.getBytes("UTF-8"));
      Document var5 = var2.build(var4);
      Element var6 = var5.getRootElement();
      Config var7 = new Config();
      var7.setCompressData(getBoolean(var6, "compressData", true));
      var7.setUseAffinity(getBoolean(var6, "useAffinity", true));
      var7.setUseJavaSerialization(getBoolean(var6, "useJavaSerialization", true));
      var7.setServerLocation(getString(var6, "serverLocation", null));
      var7.setFoldersWithJars(this.getFoldersWithJars(var6));
      var7.setFolderConfigs(this.getFolderConfigs(var6));
      return var7;
   }

   private FolderConfigs getFolderConfigs(Element var1) {
      FolderConfigs var2 = new FolderConfigs();
      var2.setFolderConfig(new LinkedList<>());
      Element var3 = var1.getChild("syncFolders");
      if (var3 != null) {
         var2.setDataFolderRoot(var3.getAttributeValue("path"));

         for (Element var6 : var3.getChildren()) {
            if (var6.getName().equalsIgnoreCase("folder")) {
               SingleFolderConfig var7 = new SingleFolderConfig();
               var7.setFolder(var6.getAttributeValue("name"));
               var7.setSolidFolder("true".equalsIgnoreCase(var6.getAttributeValue("solid")));
               var2.getFolderConfig().add(var7);
            }
         }
      }

      return var2;
   }

   private String[] getFoldersWithJars(Element var1) {
      LinkedList var2 = new LinkedList();
      Element var3 = var1.getChild("libraryFolders");
      if (var3 != null) {
         for (Element var6 : var3.getChildren()) {
            if (var6.getName().equalsIgnoreCase("library")) {
               var2.add(var6.getValue());
            }
         }
      }

      return var2.toArray(new String[0]);
   }

   private static String getString(Element var0, String var1, String var2) {
      Element var3 = var0.getChild(var1);
      return var3 == null ? var2 : var3.getValue();
   }

   private static boolean getBoolean(Element var0, String var1, boolean var2) {
      Element var3 = var0.getChild(var1);
      return var3 == null ? var2 : Boolean.parseBoolean(var3.getValue());
   }
}
