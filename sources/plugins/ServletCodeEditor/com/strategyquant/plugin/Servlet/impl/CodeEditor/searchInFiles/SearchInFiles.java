package com.strategyquant.plugin.Servlet.impl.CodeEditor.searchInFiles;

import com.strategyquant.lib.SQUtils;
import com.strategyquant.plugin.Servlet.impl.CodeEditor.CodeEditorServletException;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SearchInFiles {
   private static final Logger Log = LoggerFactory.getLogger(SearchInFiles.class);
   private boolean matchCase;
   private boolean wholeWord;
   private boolean useRegex;
   private String searchString;
   private int index;
   private final HashMap<String, SearchMatches> matches = new HashMap<>();
   private int totalMatches;
   private static final String IMG_PAGING_PAGE = "paging_page.gif";
   private static final String IMG_MATCH = "ar_right_dis.gif";
   private static final String RESP_TAG_NAME = "name";
   private static final String RESP_TAG_CONTENT = "content";

   public synchronized SearchInFilesResult search(Map<String, String> var1, String var2, boolean var3, boolean var4, boolean var5) {
      this.matchCase = var3;
      this.wholeWord = var5;
      this.useRegex = var4;
      this.searchString = var2;
      if (!this.useRegex && !this.matchCase) {
         this.searchString = this.searchString.toLowerCase();
      }

      this.index = 0;
      this.matches.clear();
      this.totalMatches = 0;
      JSONArray var6 = new JSONArray();

      for (Entry var8 : var1.entrySet()) {
         File var9 = new File((String)var8.getKey());
         if (var9.exists()) {
            this.matchFiles(var9);
            this.generateTree(var9, var6, true, (String)var8.getValue());
         }
      }

      return new SearchInFilesResult(var6, this.totalMatches);
   }

   private JSONArray generateTree(File var1, JSONArray var2, boolean var3, String var4) {
      File[] var5 = var1.listFiles();
      if (var5 == null) {
         return var2;
      }

      JSONObject var6 = null;
      JSONArray var7 = null;

      for (File var11 : var5) {
         if (this.checkFile(var11)) {
            if (var3 && var6 == null) {
               var6 = new JSONObject();
               var6.put("id", this.index++);
               var6.put("text", var4);
               var7 = new JSONArray();
               var6.put("item", var7);
               var2.put(var6);
            }

            JSONObject var12 = new JSONObject();
            var12.put("id", this.index++);
            if (var11.isDirectory()) {
               var12.put("text", var11.getName());
               JSONArray var13 = this.generateTree(var11, new JSONArray(), false, null);
               var12.put("item", var13);
            } else {
               SearchMatches var24 = this.matches.get(var11.getAbsolutePath());
               this.totalMatches = this.totalMatches + var24.size();
               var12.put("text", var11.getName() + " (" + var24.size() + " matches)");
               var12.put("im0", "paging_page.gif");
               var12.put("im1", "paging_page.gif");
               var12.put("im2", "paging_page.gif");
               JSONArray var14 = new JSONArray();
               JSONObject var15 = new JSONObject();
               var15.put("name", "file");
               var15.put("content", var11.getAbsolutePath());
               var14.put(var15);
               JSONObject var16 = new JSONObject();
               var16.put("name", "line");
               var16.put("content", var24.get(0).getLine());
               var14.put(var16);
               var12.put("userdata", var14);
               JSONArray var17 = new JSONArray();

               for (SearchMatch var19 : var24) {
                  this.index++;
                  JSONObject var20 = new JSONObject();
                  var20.put("id", this.index);
                  var20.put("text", var19.getLine() + ": " + var19.getText());
                  var20.put("im0", "ar_right_dis.gif");
                  var20.put("im1", "ar_right_dis.gif");
                  var20.put("im2", "ar_right_dis.gif");
                  JSONArray var21 = new JSONArray();
                  JSONObject var22 = new JSONObject();
                  var22.put("name", "file");
                  var22.put("content", var11.getAbsolutePath());
                  JSONObject var23 = new JSONObject();
                  var23.put("name", "line");
                  var23.put("content", var19.getLine());
                  var21.put(var22);
                  var21.put(var23);
                  var20.put("userdata", var21);
                  var17.put(var20);
               }

               var12.put("item", var17);
            }

            if (var7 == null) {
               var2.put(var12);
            } else {
               var7.put(var12);
            }
         }
      }

      return var2;
   }

   private void matchFiles(File var1) {
      File[] var2 = var1.listFiles();
      if (var2 != null) {
         for (File var6 : var2) {
            if (var6.isDirectory()) {
               this.matchFiles(var6);
            } else {
               this.searchInFile(var6);
            }
         }
      }
   }

   private void searchInFile(File var1) {
      String var2 = var1.getAbsolutePath();

      try {
         String var4 = SQUtils.fileToString(var1);
         if (!this.matchCase) {
            var4 = var4.toLowerCase();
         }

         SearchMatches var3;
         if (this.useRegex) {
            var3 = this.doSearchRegex(var4);
         } else {
            var3 = this.doSearchNoRegex(var4);
         }

         if (!var3.isEmpty()) {
            this.matches.put(var2, var3);
         }
      } catch (Exception var5) {
         Log.error("Exc.", var5);
      }
   }

   private SearchMatches doSearchNoRegex(String var1) throws CodeEditorServletException {
      SearchMatches var2 = new SearchMatches();
      int var6 = 0;
      int var5 = this.searchString.length();

      while ((var6 = var1.indexOf(this.searchString, var6)) != -1) {
         if (this.wholeWord && !this.isWholeWord(var1, var6, var5)) {
            var6++;
         } else {
            SearchMatch var4 = this.getMatch(var1, var6);
            var2.add(var4);
            var6 += var5;
         }
      }

      return var2;
   }

   private SearchMatches doSearchRegex(String var1) throws CodeEditorServletException {
      SearchMatches var2 = new SearchMatches();
      int var4 = this.matchCase ? 0 : 66;
      Pattern var5 = Pattern.compile(this.searchString, var4);
      Matcher var6 = var5.matcher(var1);

      while (var6.find()) {
         int var7 = var6.start();
         int var8 = var6.end();
         if (!this.wholeWord || this.isWholeWord(var1, var7, var8 - var7)) {
            SearchMatch var3 = this.getMatch(var1, this.index);
            var2.add(var3);
         }
      }

      return var2;
   }

   private SearchMatch getMatch(String var1, int var2) throws CodeEditorServletException {
      int var3 = 0;
      int var5 = 0;

      int var4;
      while ((var4 = var1.indexOf("\n", var5) + 1) != 0) {
         var3++;
         if (var4 >= var2) {
            String var6 = var1.substring(var5, var4);
            return new SearchMatch(var3, var6.trim());
         }

         var5 = var4;
      }

      throw new CodeEditorServletException(String.format("No match found. Index: %d.", var2));
   }

   private boolean isWholeWord(CharSequence var1, int var2, int var3) {
      boolean var4;
      try {
         var4 = Character.isWhitespace(var1.charAt(var2 - 1));
      } catch (IndexOutOfBoundsException var8) {
         var4 = true;
      }

      boolean var5;
      try {
         var5 = Character.isWhitespace(var1.charAt(var2 + var3));
      } catch (IndexOutOfBoundsException var7) {
         var5 = true;
      }

      return var4 && var5;
   }

   private boolean checkFile(File var1) {
      return var1.isDirectory() ? this.checkFiles(var1) : this.matches.containsKey(var1.getAbsolutePath());
   }

   private boolean checkFiles(File var1) {
      File[] var2 = var1.listFiles();
      if (var2 == null) {
         return false;
      }

      for (File var6 : var2) {
         if (!var6.isDirectory()) {
            return this.matches.containsKey(var6.getAbsolutePath());
         }

         if (this.checkFiles(var6)) {
            return true;
         }
      }

      return false;
   }
}
