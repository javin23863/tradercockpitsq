package com.strategyquant.datalib.data.io;

import com.strategyquant.datalib.data.io.columns.DefaultCol;
import java.util.ArrayList;

public class ImportDataInfo {
   public static final int DT_OHLC = 1;
   public static final int DT_TICK = 2;
   public String name = null;
   public String filePath = null;
   public String separator = null;
   public int skipRows;
   public int skipCols;
   public String dateFormat;
   public String timeFormat = null;
   public int rowCount;
   public String uniqImportString = "ABCDEFGH";
   public ArrayList<DefaultCol> columnTypes = new ArrayList<>();
   public int dataType = -1;
   public String timeframe;
   public int importFileRows;
   public long beginTimeNewFile;
   public long endTimeNewFile;
   public boolean reversedFile = false;
   public boolean hasTwoVolumes = false;
   public int errorHandling = 0;
   public String timezone;
   private String lastAsk = null;
   private String lastBid = null;

   public boolean isMT5TickImport() {
      return this.name != null && this.name.equals("MetaTrader5 Tick Data");
   }

   public void resetLastAskBid() {
      this.lastAsk = null;
      this.lastBid = null;
   }

   public String[] correctMT5TickData(String[] var1) {
      String[] var2 = new String[]{var1[0], var1[1], null, null};
      if (var1.length > 2 && !var1[2].isBlank()) {
         this.lastBid = var1[2];
      }

      if (var1.length > 3 && !var1[3].isBlank()) {
         this.lastAsk = var1[3];
      }

      var2[2] = this.lastBid;
      var2[3] = this.lastAsk;
      return var2[2] != null && var2[3] != null ? var2 : null;
   }
}
