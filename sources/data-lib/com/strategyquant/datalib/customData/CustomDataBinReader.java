package com.strategyquant.datalib.customData;

import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

public class CustomDataBinReader {
   DataInputStream reader = null;
   private String fileName;
   public CustomData loadedData;

   public CustomDataBinReader(CustomDataInfo var1) {
      this.loadedData = new CustomData(var1.values);
      this.fileName = CustomDataManager.getDataFileName(var1.name);
   }

   public void open() throws Exception {
      try {
         this.reader = new DataInputStream(new FileInputStream(this.fileName));
      } catch (FileNotFoundException var2) {
         throw new Exception("File not found: " + this.fileName);
      }
   }

   public void close() throws Exception {
      try {
         if (this.reader != null) {
            this.reader.close();
            this.reader = null;
         }
      } catch (IOException var2) {
         throw new Exception("Cannot close file");
      }
   }

   public boolean hasNextData() throws Exception {
      if (this.reader == null) {
         throw new Exception("You have to call CustomDataBinReader.open() method first!");
      } else {
         return this.reader.available() > 0;
      }
   }

   public void loadData() throws Exception {
      this.loadedData.time = this.reader.readLong();

      for (int var1 = 0; var1 < this.loadedData.values.length; var1++) {
         this.loadedData.values[var1] = this.reader.readDouble();
      }
   }
}
