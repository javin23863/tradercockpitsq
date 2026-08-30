package com.strategyquant.datalib.data.io;

import com.strategyquant.datalib.ChartDef;
import com.strategyquant.datalib.DataInfo;
import com.strategyquant.datalib.data.io.newDataFormat.DataBinReaderNew;
import com.strategyquant.lib.SQTime;
import java.io.File;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BinaryDataLoader implements IDataLoader {
   public static final Logger Log = LoggerFactory.getLogger("BinaryDataLoader");
   private String filePath;
   private DataBinReaderNew reader;
   private boolean eof = false;
   private int dataType;
   private int connectionHash;
   private int symbolHash;
   private ChartDef chartDef;
   private boolean dataInBuffer;
   private int specialId = -1;
   private long dateFrom;
   private DataInfo dataInfo;

   public BinaryDataLoader(String var1, ChartDef var2, int var3, DataInfo var4) {
      this.filePath = var1;
      this.connectionHash = var2.getConnectionHash();
      this.symbolHash = var2.getSymbolHash();
      this.chartDef = var2;
      this.dataType = var3;
      this.dataInfo = var4;
   }

   @Override
   public void open() throws Exception {
      File var1 = new File(this.filePath);
      if (var1.exists() && !var1.isDirectory()) {
         this.reader = DataBinReaderNew.getInstance(this.dataType, this.dataInfo.symbolInfo);
         this.reader.setFileName(this.filePath);
         this.reader.openFile();
         this.dataInBuffer = false;
         long var2 = System.currentTimeMillis();
         this.loadLinesUntilFromDate();
         long var4 = System.currentTimeMillis();
         Log.debug("BinaryDataLoader -  loadLinesUntilFromDate TOOK: " + (var4 - var2) + " ms");
      } else {
         throw new Exception("BinaryDataLoader - Data file '" + this.filePath + "' doesn't exist!");
      }
   }

   private void loadLinesUntilFromDate() throws Exception {
      boolean var1 = false;
      this.eof = false;

      while (this.reader.readData()) {
         long var2 = this.reader.tickData.time;
         if (var2 >= this.chartDef.getLoadedHistoryFrom()) {
            var1 = true;
            this.dataInBuffer = true;
            this.dateFrom = var2;
            Log.debug("History from: " + SQTime.toFullDateMinuteString(var2));
            Log.debug("History to: " + SQTime.toFullDateMinuteString(this.chartDef.getHistoryTo()));
            break;
         }
      }

      if (!var1) {
         this.eof = true;
      }
   }

   @Override
   public void close() throws Exception {
      if (this.reader != null) {
         this.reader.closeFile();
      }
   }

   @Override
   public boolean hasNextTick() throws Exception {
      if (this.reader == null) {
         throw new Exception("You have to call DataLoader.open() method first!");
      }

      if (this.eof) {
         return false;
      }

      if (this.dataInBuffer) {
         return true;
      }

      if (this.reader.readData()) {
         if (this.chartDef.getHistoryTo() > 0L && this.reader.tickData.time > this.chartDef.getHistoryTo()) {
            return false;
         }

         this.dataInBuffer = true;
         return true;
      } else {
         return false;
      }
   }

   @Override
   public long nextTickTime() throws Exception {
      return this.reader.tickData.time;
   }

   @Override
   public void getNextTick(VersatileData var1) throws Exception {
      if (this.reader == null) {
         throw new Exception("You have to call DataLoader.open() method first!");
      }

      if (this.dataType == 1) {
         var1.set(
            2,
            this.reader.tickData.time,
            this.connectionHash,
            this.symbolHash,
            this.reader.tickData.open,
            this.reader.tickData.high,
            this.reader.tickData.low,
            this.reader.tickData.close,
            this.reader.tickData.volume
         );
      } else {
         var1.set(
            1, this.reader.tickData.time, this.connectionHash, this.symbolHash, this.reader.tickData.ask, this.reader.tickData.bid, this.reader.tickData.volume
         );
      }

      this.dataInBuffer = false;
   }

   @Override
   public boolean isOHLCData() {
      return this.dataType == 1;
   }

   @Override
   public long getDateFrom() {
      return this.dateFrom;
   }

   @Override
   public int getDecimalPlaces() {
      return this.dataInfo.decimals;
   }

   @Override
   public void seek(int var1) throws Exception {
      if (this.reader instanceof ICanSeek) {
         ((ICanSeek)this.reader).seek(var1);
         this.dataInBuffer = false;
      }
   }

   @Override
   public long getTotalRecords() throws Exception {
      return this.reader.getTotalRecords();
   }

   @Override
   public String getDataFilePath() {
      return this.filePath;
   }

   @Override
   public boolean isCrypted() {
      return this.reader.isCrypted();
   }
}
