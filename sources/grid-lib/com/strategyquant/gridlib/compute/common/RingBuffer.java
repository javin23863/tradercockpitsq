package com.strategyquant.gridlib.compute.common;

import java.util.Iterator;
import java.util.NoSuchElementException;

public class RingBuffer<T> implements Iterable<T> {
   private T[] data;
   private int size;
   private int writePos = 0;
   private int oldest = 0;

   public RingBuffer(int var1) {
      this.size = var1;
      this.data = (T[])(new Object[var1]);
   }

   public int getSize() {
      return this.size;
   }

   public void add(T var1) {
      if (this.data[this.writePos] != null) {
         this.oldest++;
         if (this.oldest == this.size) {
            this.oldest = 0;
         }
      }

      this.data[this.writePos] = (T)var1;
      this.writePos++;
      if (this.writePos == this.size) {
         this.writePos = 0;
      }
   }

   public void clear() {
      for (int var1 = 0; var1 < this.size; var1++) {
         this.data[var1] = null;
      }

      this.oldest = 0;
      this.writePos = 0;
   }

   @Override
   public Iterator<T> iterator() {
      return new RingBuffer.IteratorImpl(this.oldest);
   }

   private class IteratorImpl implements Iterator<T> {
      private int current;
      private int returned = 0;

      public IteratorImpl(int nullx) {
         this.current = nullx;
      }

      @Override
      public boolean hasNext() {
         return RingBuffer.this.data[this.current] != null && this.returned != RingBuffer.this.data.length;
      }

      @Override
      public T next() {
         if (!this.hasNext()) {
            throw new NoSuchElementException();
         }

         Object var1 = RingBuffer.this.data[this.current];
         this.current++;
         if (this.current == RingBuffer.this.data.length) {
            this.current = 0;
         }

         this.returned++;
         return (T)var1;
      }
   }
}
