package com.strategyquant.datalib.data.io.newDataFormat;

import java.io.IOException;

public interface IRandomAccessReader {
   boolean dataRemaining();

   byte readByte() throws IOException;

   void readBytes(byte[] var1) throws IOException;

   short readShort() throws IOException;

   int readInt() throws IOException;

   long readLong() throws IOException;

   String readUTF() throws IOException;

   double readDouble() throws IOException;

   float readFloat() throws IOException;

   void seek(long var1) throws IOException;

   long getPosition() throws IOException;

   long getLength() throws IOException;
}
