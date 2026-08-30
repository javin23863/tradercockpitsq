package com.strategyquant.datalib.customData;

import com.strategyquant.lib.db.DbBase;

public abstract class CustomDataDb extends DbBase {
   public CustomDataDb(String var1) {
      super("CustomDataDb", "customdata.db", var1);
      this.initDatabase();
   }

   public abstract void initDatabase();
}
