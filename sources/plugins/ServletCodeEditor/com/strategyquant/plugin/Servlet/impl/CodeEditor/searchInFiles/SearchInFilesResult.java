package com.strategyquant.plugin.Servlet.impl.CodeEditor.searchInFiles;

import org.json.JSONArray;

public class SearchInFilesResult {
   private final JSONArray items;
   private final int totalMatches;

   SearchInFilesResult(JSONArray var1, int var2) {
      this.items = var1;
      this.totalMatches = var2;
   }

   public JSONArray getItems() {
      return this.items;
   }

   public int getTotalMatchesCount() {
      return this.totalMatches;
   }
}
