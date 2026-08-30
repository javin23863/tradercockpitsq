package com.strategyquant.datalib.data.io;

import com.strategyquant.lib.time.SQTimeOld;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

public class DataCsvWriter {
   protected PrintWriter writer;
   protected String fileName;
   protected String timeframe;

   public DataCsvWriter(String var1, String var2) {
      this.fileName = var2;
      this.timeframe = var1;
   }

   public void openFile() throws Exception {
      try {
         File var1 = new File(this.fileName);
         if (!var1.getParentFile().exists()) {
            var1.getParentFile().mkdirs();
            var1.createNewFile();
         }

         this.writer = new PrintWriter(new BufferedWriter(new FileWriter(var1, StandardCharsets.UTF_8)));
      } catch (FileNotFoundException var2) {
         throw new Exception("File not found: " + this.fileName);
      }
   }

   public void closeFile() {
      this.writer.close();
   }

   public void write(VersatileData var1) throws IOException {
      if (var1.type == 1) {
         this.writeTickData(var1);
      } else {
         this.writeOHLCData(var1);
      }
   }

   private void writeOHLCData(VersatileData var1) {
      SQTimeOld var2 = new SQTimeOld(var1.time);
      this.writer
         .println(var2.formatDate() + "," + var2.formatTime() + "," + var1.open + "," + var1.high + "," + var1.low + "," + var1.close + "," + var1.volume);
   }

   private void writeTickData(VersatileData var1) {
      SQTimeOld var2 = new SQTimeOld(var1.time);
      this.writer.println(var2.toString() + "," + var1.ask + "," + var1.bid + "," + var1.volume);
   }
}
