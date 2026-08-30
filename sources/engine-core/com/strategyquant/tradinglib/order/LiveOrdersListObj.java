package com.strategyquant.tradinglib.order;

import com.strategyquant.tradinglib.ILiveOrder;
import java.util.ArrayList;
import java.util.Iterator;

public class LiveOrdersListObj implements ILiveOrdersList {
   protected ArrayList<ILiveOrder> list = new ArrayList<>();

   @Override
   public Iterator<ILiveOrder> iterator() {
      return new Iterator<ILiveOrder>() {
         private final Iterator<ILiveOrder> iter = LiveOrdersListObj.this.list.iterator();

         @Override
         public boolean hasNext() {
            return this.iter.hasNext();
         }

         public ILiveOrder next() {
            return this.iter.next();
         }

         @Override
         public void remove() {
            throw new UnsupportedOperationException("No changes allowed");
         }
      };
   }

   @Override
   public int size() {
      return this.list.size();
   }

   public void add(ILiveOrder var1) {
      this.list.add(var1);
   }

   public void addAll(ArrayList<ILiveOrder> var1) {
      this.list.addAll(var1);
   }

   public void addAll(LiveOrdersListObj var1) {
      for (ILiveOrder var3 : var1) {
         this.list.add(var3);
      }
   }

   public void remove(ILiveOrder var1) {
      this.list.remove(var1);
   }

   public ILiveOrder get(int var1) {
      return this.list.get(var1);
   }

   public Iterator<ILiveOrder> iteratorMutable() {
      return new Iterator<ILiveOrder>() {
         private final Iterator<ILiveOrder> iter = LiveOrdersListObj.this.list.iterator();

         @Override
         public boolean hasNext() {
            return this.iter.hasNext();
         }

         public ILiveOrder next() {
            return this.iter.next();
         }

         @Override
         public void remove() {
            this.iter.remove();
         }
      };
   }
}
