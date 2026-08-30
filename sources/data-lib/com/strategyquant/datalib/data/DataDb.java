package com.strategyquant.datalib.data;

import com.strategyquant.lib.db.DbBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class DataDb extends DbBase {
   public static final Logger Log = LoggerFactory.getLogger(DataDb.class);

   public DataDb(String var1) {
      super("DataDb", "data.db", var1);
      this.initDatabase();
   }

   public abstract void initDatabase();
}
