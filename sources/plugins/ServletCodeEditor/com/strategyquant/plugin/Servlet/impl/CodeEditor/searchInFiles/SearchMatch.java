package com.strategyquant.plugin.Servlet.impl.CodeEditor.searchInFiles;

public class SearchMatch {
   private final int line;
   private final String text;

   public SearchMatch(int var1, String var2) {
      this.line = var1;
      this.text = var2;
   }

   public int getLine() {
      return this.line;
   }

   public String getText() {
      return this.text;
   }
}
