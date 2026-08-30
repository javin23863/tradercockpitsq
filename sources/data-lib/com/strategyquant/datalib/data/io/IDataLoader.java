package com.strategyquant.datalib.data.io;

import com.strategyquant.lib.historyData.ICryptable;

public interface IDataLoader extends ICanSeek, ICryptable {
   boolean hasNextTick() throws Exception;

   long nextTickTime() throws Exception;

   void getNextTick(VersatileData var1) throws Exception;

   void open() throws Exception;

   void close() throws Exception;

   boolean isOHLCData();

   int getDecimalPlaces();

   long getDateFrom();

   long getTotalRecords() throws Exception;

   String getDataFilePath();

   boolean isCrypted();
}
