package com.strategyquant.datalib.customData;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class CustomDataBinWriter {
   protected DataOutputStream writer = null;
   private String fileName;

   public void setFileName(String var1) {
      this.fileName = var1;
   }

   public String getFileName() {
      return this.fileName;
   }

   public void open() throws Exception {
      try {
         File var1 = new File(this.fileName);
         if (!var1.exists()) {
            var1.getParentFile().mkdirs();
         }

         this.writer = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(this.fileName)));
      } catch (FileNotFoundException var2) {
         throw new Exception("File not found: " + this.fileName);
      }
   }

   public void close() throws Exception {
      try {
         if (this.writer != null) {
            this.writer.flush();
            this.writer.close();
            this.writer = null;
         }
      } catch (IOException var2) {
         throw new Exception("Cannot close file");
      }
   }

   public void writeData(CustomData var1) throws Exception {
      if (this.writer == null) {
         throw new Exception("You have to call CustomDataBinWriter.open() method first!");
      }

      this.writer.writeLong(var1.time);

      for (int var2 = 0; var2 < var1.values.length; var2++) {
         this.writer.writeDouble(var1.values[var2]);
      }
   }
}
