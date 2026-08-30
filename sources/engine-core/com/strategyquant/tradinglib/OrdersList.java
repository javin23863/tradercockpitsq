package com.strategyquant.tradinglib;

import com.strategyquant.datalib.data.DataManager;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.tradinglib.strategy.LiveOrderObj;
import it.unimi.dsi.fastutil.bytes.Byte2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ByteOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap.Entry;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap.FastEntrySet;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.ObjectListIterator;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Collections;
import java.util.Comparator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OrdersList implements Externalizable {
   public static final Logger Log = LoggerFactory.getLogger("OrdersList");
   private static final String ORDER_FILE_FORMAT = "SQOrderFileFormat:";
   private static final String ConstantAll = "*";
   private ObjectArrayList<Order> list;
   private String identifier;

   public OrdersList(String var1) {
      this.identifier = var1;
      this.list = new ObjectArrayList();
   }

   public OrdersList(String var1, int var2) {
      this.identifier = var1;
      this.list = new ObjectArrayList(var2);
   }

   public void add(Order var1) {
      this.list.add(var1);
   }

   public ObjectListIterator<Order> listIterator() {
      return this.list.listIterator();
   }

   public ObjectListIterator<Order> listIterator(int var1) {
      return this.list.listIterator(var1);
   }

   public void add(LiveOrderObj var1) {
      Order var2 = new Order(var1);
      this.add(var2);
   }

   public Order get(int var1) {
      return (Order)this.list.get(var1);
   }

   public void shuffle() {
      Collections.shuffle(this.list);
   }

   public void clear() {
      this.list.clear();
      this.list.trim();
   }

   public int size() {
      return this.list.size();
   }

   public void addAll(OrdersList var1) {
      this.list.addAll(var1.list);
   }

   public void replaceWithList(OrdersList var1) {
      this.list.clear();
      this.list = var1.list;
   }

   public OrdersList filter(String var1, byte var2, byte var3) {
      return this.filter(var1, var2, var3, false, false, false);
   }

   public OrdersList filter(String var1, byte var2, byte var3, boolean var4) {
      return this.filter(var1, var2, var3, var4, false, false);
   }

   public OrdersList filterWithClone(String var1, byte var2, byte var3) {
      return this.filter(var1, var2, var3, false, true, false);
   }

   public OrdersList filterExcludingControlOrders(String var1, byte var2, byte var3, boolean var4) {
      return this.filter(var1, var2, var3, var4, true, true);
   }

   private OrdersList filter(String var1, byte var2, byte var3, boolean var4, boolean var5, boolean var6) {
      OrdersList var7 = new OrdersList("FilteredOL");
      String var8 = "*";
      if (!var1.equals("Portfolio") && !DataManager.isGroupAlias(var1)) {
         var8 = var1;
      }

      boolean var9 = !var8.equals("*");
      int var10 = var8.hashCode();
      boolean var11 = var2 != 0;
      boolean var12 = var3 != 127;

      for (int var13 = 0; var13 < this.size(); var13++) {
         Order var14 = (Order)this.list.get(var13);
         if ((var9 ? var14 != null && var14.SetupName != null && var14.SetupName.hashCode() == var10 : var14.IsInPortfolio != 0)
            && (!var11 || !var14.isLong() || var2 != -1)
            && (!var14.isShort() || var2 != 1)
            && (
               !var12
                  || (
                     var3 == 10
                        ? var14.SampleType == 10 || var14.SampleType == 11 || var14.SampleType >= 40 && var14.SampleType <= 50
                        : (
                           var3 == 40
                              ? var14.SampleType >= 40 && var14.SampleType <= 50
                              : (var3 == 20 ? var14.SampleType >= 20 && var14.SampleType <= 30 : var3 == var14.SampleType)
                        )
                  )
            )
            && (!var4 || var14.isFilledOrder())
            && (!var6 || var14.isRealOrder())) {
            if (var5) {
               var7.add(new Order(var14));
            } else {
               var7.add(var14);
            }
         }
      }

      return var7;
   }

   public boolean isEmpty() {
      return this.list.isEmpty();
   }

   public OrdersList clone() {
      OrdersList var1 = new OrdersList(String.format("%s.cloned", this.identifier));
      var1.addAll(this);
      return var1;
   }

   public OrdersList cloneDeep() {
      OrdersList var1 = new OrdersList(String.format("%s.cloned", this.identifier));

      for (int var3 = 0; var3 < this.size(); var3++) {
         Order var2 = this.get(var3);
         var1.add(new Order(var2));
      }

      return var1;
   }

   public void remove(int var1) {
      this.list.remove(var1);
   }

   @Override
   public void writeExternal(ObjectOutput var1) throws IOException {
      this.serialize(var1);
   }

   @Override
   public void readExternal(ObjectInput var1) throws IOException, ClassNotFoundException {
      this.deserialize(var1);
   }

   public void serialize(ObjectOutput var1) throws IOException {
      var1.writeUTF(String.format("%s%d", "SQOrderFileFormat:", 11));
      var1.writeInt(0);
      var1.writeInt(0);
      var1.writeInt(0);
      var1.writeInt(0);
      var1.writeInt(0);
      var1.writeInt(0);
      var1.writeInt(1);
      Int2ObjectOpenHashMap var3 = new Int2ObjectOpenHashMap();

      for (int var4 = 0; var4 < this.size(); var4++) {
         Order var2 = this.get(var4);
         this.prepareStringsCache(var2, var3);
      }

      Int2ByteOpenHashMap var7 = this.writeStringCache(var1, var3);

      for (int var5 = 0; var5 < this.size(); var5++) {
         Order var6 = this.get(var5);
         this.writeOrderFormat8(var1, var6, var3, var7);
      }

      var1.flush();
   }

   private Int2ByteOpenHashMap writeStringCache(ObjectOutput var1, Int2ObjectOpenHashMap<String> var2) throws IOException {
      var1.writeInt(var2.size());
      Int2ByteOpenHashMap var3 = null;
      if (var2.size() < 120) {
         var3 = new Int2ByteOpenHashMap();
         FastEntrySet var4 = var2.int2ObjectEntrySet();
         byte var5 = 1;
         ObjectIterator var6 = var4.iterator();

         while (var6.hasNext()) {
            Entry var7 = (Entry)var6.next();
            var3.put(var7.getIntKey(), var5);
            var5++;
            var1.writeUTF((String)var7.getValue());
         }
      } else {
         FastEntrySet var8 = var2.int2ObjectEntrySet();
         ObjectIterator var9 = var8.iterator();

         while (var9.hasNext()) {
            Entry var10 = (Entry)var9.next();
            var1.writeInt(var10.getIntKey());
            var1.writeUTF((String)var10.getValue());
         }
      }

      return var3;
   }

   private void prepareStringsCache(Order var1, Int2ObjectOpenHashMap<String> var2) {
      if (var1.Symbol != null) {
         int var3 = SQUtils.betterHashCode(var1.Symbol);
         if (!var2.containsKey(var3)) {
            var2.put(var3, var1.Symbol);
         }
      }

      if (var1.SetupName != null) {
         int var4 = SQUtils.betterHashCode(var1.SetupName);
         if (!var2.containsKey(var4)) {
            var2.put(var4, var1.SetupName);
         }
      }

      if (var1.StrategyName != null) {
         int var5 = SQUtils.betterHashCode(var1.StrategyName);
         if (!var2.containsKey(var5)) {
            var2.put(var5, var1.StrategyName);
         }
      }

      if (var1.Comment != null) {
         int var6 = SQUtils.betterHashCode(var1.Comment);
         if (!var2.containsKey(var6)) {
            var2.put(var6, var1.Comment);
         }
      }
   }

   private void writeOrderFormat8(ObjectOutput var1, Order var2, Int2ObjectOpenHashMap<String> var3, Int2ByteOpenHashMap var4) throws IOException {
      this.writeUTFUsingMap(var1, var2.Symbol, var3, var4);
      this.writeUTFUsingMap(var1, var2.SetupName, var3, var4);
      this.writeUTFUsingMap(var1, var2.StrategyName, var3, var4);
      this.writeUTFUsingMap(var1, var2.Comment, var3, var4);
      var1.writeInt(var2.Ticket);
      var1.writeInt(var2.Order);
      var1.writeByte(var2.Type);
      var1.writeByte(var2.CloseType);
      var1.writeByte(var2.SampleType);
      var1.writeLong(var2.OriginalOpenTime);
      var1.writeByte(var2.OriginalType);
      var1.writeFloat(var2.Size);
      var1.writeFloat(var2.OriginalPrice);
      var1.writeLong(var2.OpenTime);
      var1.writeFloat(var2.OpenPrice);
      var1.writeLong(var2.CloseTime);
      var1.writeFloat(var2.ClosePrice);
      var1.writeFloat(var2.StopLoss);
      var1.writeFloat(var2.TakeProfit);
      var1.writeShort(var2.BarsInTrade);
      var1.writeFloat(var2.PL);
      var1.writeFloat(var2.PctPL);
      var1.writeFloat(var2.PctPL_TWR);
      var1.writeFloat(var2.PipsPL);
      var1.writeFloat(var2.DD);
      var1.writeFloat(var2.PctDD);
      var1.writeFloat(var2.PipsDD);
      var1.writeFloat(var2.CommSwap);
      var1.writeBoolean(var2.CommSwapApplied);
      var1.writeFloat(var2.MAE);
      var1.writeFloat(var2.PipsMAE);
      var1.writeFloat(var2.MFE);
      var1.writeFloat(var2.PipsMFE);
      var1.writeInt(var2.Duration);
      var1.writeFloat(var2.AccountBalance);
      var1.writeFloat(var2.PctAccountBalance);
      var1.writeFloat(var2.PipsAccountBalance);
      var1.writeInt(var2.MagicNumber);
      var1.writeByte(var2.IsInPortfolio);
      var1.writeFloat(var2.Extra1);
      var1.writeFloat(var2.SlippageInMoney);
      var1.writeByte(var2.ExitIndex);
      var1.writeFloat(var2.ATROnOpen);
   }

   private void writeUTFUsingMap(ObjectOutput var1, String var2, Int2ObjectOpenHashMap<String> var3, Int2ByteOpenHashMap var4) throws IOException {
      if (var4 == null) {
         if (var2 == null) {
            var1.writeInt(0);
         } else {
            var1.writeInt(SQUtils.betterHashCode(var2));
         }
      } else if (var2 == null) {
         var1.writeByte(0);
      } else {
         var1.writeByte(var4.get(SQUtils.betterHashCode(var2)));
      }
   }

   public void deserialize(ObjectInput var1) throws IOException {
      if (var1.available() > 0) {
         String var2 = var1.readUTF();
         int var3;
         if (var2.startsWith("SQOrderFileFormat:")) {
            var3 = Integer.parseInt(var2.replace("SQOrderFileFormat:", ""));
         } else {
            var3 = 0;
         }

         int[] var4 = new int[7];
         if (var3 >= 10) {
            for (int var5 = 0; var5 < 7; var5++) {
               var4[var5] = var1.readInt();
            }
         }

         if (var3 >= 5) {
            this.loadOrdersUsingFormat10(var1, var2, var3, var4);
         } else {
            this.loadOrdersUsingPreviousFormats(var1, var2, var3);
         }
      }
   }

   private void loadOrdersUsingFormat10(ObjectInput var1, String var2, int var3, int[] var4) throws IOException {
      int var5 = var1.readInt();
      Int2ObjectOpenHashMap var6 = null;
      Byte2ObjectOpenHashMap var7 = null;
      if (var5 < 120) {
         var7 = new Byte2ObjectOpenHashMap();

         for (int var8 = 0; var8 < var5; var8++) {
            String var9 = this.intern(var1.readUTF());
            var7.put((byte)(var8 + 1), var9);
         }
      } else {
         var6 = new Int2ObjectOpenHashMap();

         for (int var12 = 0; var12 < var5; var12++) {
            int var14 = var1.readInt();
            String var10 = this.intern(var1.readUTF());
            var6.put(var14, var10);
         }
      }

      while (var1.available() > 0) {
         Order var13;
         if (var3 >= 7) {
            var13 = this.loadOrderFormat10(var1, var6, var7, var3, var4);
         } else {
            var13 = this.loadOrderFormat5(var1, var6, var7, var3);
         }

         if (var3 < 9) {
            var13.CommSwap = -var13.CommSwap;
         }

         if (var13.SampleType == 20) {
            var13.SampleType = 21;
         }

         this.add(var13);
         Object var11 = null;
      }
   }

   private Order loadOrderFormat10(ObjectInput var1, Int2ObjectOpenHashMap<String> var2, Byte2ObjectOpenHashMap<String> var3, int var4, int[] var5) throws IOException {
      Order var6 = new Order();
      var6.Symbol = this.readUTFUsingMap(var1, var2, var3);
      var6.SetupName = this.readUTFUsingMap(var1, var2, var3);
      var6.StrategyName = this.readUTFUsingMap(var1, var2, var3);
      var6.Comment = this.readUTFUsingMap(var1, var2, var3);
      var6.Ticket = var1.readInt();
      var6.Order = var1.readInt();
      var6.Type = var1.readByte();
      var6.CloseType = var1.readByte();
      var6.SampleType = var1.readByte();
      var6.OriginalOpenTime = var1.readLong();
      var6.OriginalType = var1.readByte();
      var6.Size = var1.readFloat();
      var6.OriginalPrice = var1.readFloat();
      var6.OpenTime = var1.readLong();
      var6.OpenPrice = var1.readFloat();
      var6.CloseTime = var1.readLong();
      var6.ClosePrice = var1.readFloat();
      var6.StopLoss = var1.readFloat();
      var6.TakeProfit = var1.readFloat();
      var6.BarsInTrade = var1.readShort();
      var6.PL = var1.readFloat();
      var6.PctPL = var1.readFloat();
      var6.PctPL_TWR = var1.readFloat();
      var6.PipsPL = var1.readFloat();
      var6.DD = var1.readFloat();
      var6.PctDD = var1.readFloat();
      var6.PipsDD = var1.readFloat();
      var6.CommSwap = var1.readFloat();
      var6.CommSwapApplied = var1.readBoolean();
      var6.MAE = var1.readFloat();
      var6.PipsMAE = var1.readFloat();
      var6.MFE = var1.readFloat();
      var6.PipsMFE = var1.readFloat();
      var1.readInt();
      var6.Duration = this.getDuration(var6);
      var6.AccountBalance = var1.readFloat();
      var6.PctAccountBalance = var1.readFloat();
      var6.PipsAccountBalance = var1.readFloat();
      var6.MagicNumber = var1.readInt();
      var6.IsInPortfolio = var1.readByte();
      var6.Extra1 = var1.readFloat();
      var6.SlippageInMoney = var1.readFloat();
      if (var4 >= 8) {
         var6.ExitIndex = var1.readByte();
      }

      if (var4 >= 10 && var4 >= 10) {
         this.readAdditionalData(var4, 0, var5, var1, var6, var2, var3);
         this.readAdditionalData(var4, 1, var5, var1, var6, var2, var3);
         this.readAdditionalData(var4, 2, var5, var1, var6, var2, var3);
         this.readAdditionalData(var4, 3, var5, var1, var6, var2, var3);
         this.readAdditionalData(var4, 4, var5, var1, var6, var2, var3);
         this.readAdditionalData(var4, 5, var5, var1, var6, var2, var3);
         this.readAdditionalData(var4, 6, var5, var1, var6, var2, var3);
      }

      return var6;
   }

   private void readAdditionalData(
      int var1, int var2, int[] var3, ObjectInput var4, Order var5, Int2ObjectOpenHashMap<String> var6, Byte2ObjectOpenHashMap<String> var7
   ) throws IOException {
      for (int var8 = 0; var8 < var3[var2]; var8++) {
         if (var2 == 0) {
            this.readUTFUsingMap(var4, var6, var7);
         } else if (var2 == 1) {
            var4.readByte();
         } else if (var2 == 2) {
            var4.readInt();
         } else if (var2 == 3) {
            var4.readLong();
         } else if (var2 == 4) {
            var4.readBoolean();
         } else if (var2 == 5) {
            var4.readDouble();
         } else {
            if (var2 != 6) {
               throw new IOException("Unrecognized index: " + var2);
            }

            if (var8 == 0) {
               var5.ATROnOpen = var4.readFloat();
            } else {
               var4.readFloat();
            }
         }
      }
   }

   private Order loadOrderFormat5(ObjectInput var1, Int2ObjectOpenHashMap<String> var2, Byte2ObjectOpenHashMap<String> var3, int var4) throws IOException {
      Order var5 = new Order();
      var5.Symbol = this.readUTFUsingMap(var1, var2, var3);
      var5.SetupName = this.readUTFUsingMap(var1, var2, var3);
      var5.StrategyName = this.readUTFUsingMap(var1, var2, var3);
      var5.Comment = this.readUTFUsingMap(var1, var2, var3);
      var5.Ticket = var1.readInt();
      var5.Order = var1.readInt();
      var5.Type = var1.readByte();
      var5.CloseType = var1.readByte();
      var5.SampleType = var1.readByte();
      var5.OriginalOpenTime = var1.readLong();
      var5.OriginalType = var1.readByte();
      var5.Size = (float)var1.readDouble();
      var5.OriginalPrice = (float)var1.readDouble();
      var5.OpenTime = var1.readLong();
      var5.OpenPrice = (float)var1.readDouble();
      var5.CloseTime = var1.readLong();
      var5.ClosePrice = (float)var1.readDouble();
      var5.StopLoss = (float)var1.readDouble();
      var5.TakeProfit = (float)var1.readDouble();
      var5.BarsInTrade = var1.readShort();
      var5.PL = var1.readFloat();
      var5.PctPL = var1.readFloat();
      var5.PctPL_TWR = var1.readFloat();
      var5.PipsPL = var1.readFloat();
      var5.DD = var1.readFloat();
      var5.PctDD = var1.readFloat();
      var5.PipsDD = var1.readFloat();
      var5.CommSwap = var1.readFloat();
      var5.CommSwapApplied = var1.readBoolean();
      var5.MAE = var1.readFloat();
      var1.readFloat();
      var5.PipsMAE = var1.readFloat();
      var5.MFE = var1.readFloat();
      var1.readFloat();
      var5.PipsMFE = var1.readFloat();
      var1.readInt();
      var5.Duration = this.getDuration(var5);
      var5.AccountBalance = var1.readFloat();
      var5.PctAccountBalance = var1.readFloat();
      var5.PipsAccountBalance = var1.readFloat();
      var1.readFloat();
      var5.MagicNumber = var1.readInt();
      var5.IsInPortfolio = var1.readByte();
      var5.Extra1 = var1.readFloat();
      if (var4 == 6) {
         var5.SlippageInMoney = var1.readFloat();
      }

      return var5;
   }

   private String readUTFUsingMap(ObjectInput var1, Int2ObjectOpenHashMap<String> var2, Byte2ObjectOpenHashMap<String> var3) throws IOException {
      if (var3 == null) {
         int var5 = var1.readInt();
         return var5 == 0 ? null : (String)var2.get(var5);
      } else {
         byte var4 = var1.readByte();
         return var4 == 0 ? null : (String)var3.get(var4);
      }
   }

   private void loadOrdersUsingPreviousFormats(ObjectInput var1, String var2, int var3) throws IOException {
      while (var1.available() > 0) {
         Order var4 = this.loadOrder(var1, var2, var3);
         this.add(var4);
         var2 = null;
      }
   }

   private Order loadOrder(ObjectInput var1, String var2, int var3) throws IOException {
      if (var3 == 4) {
         return this.loadOrderFormat4(var1);
      } else if (var3 == 3) {
         return this.loadOrderFormat3(var1);
      } else {
         return var3 == 2 ? this.loadOrderFormat2(var1) : this.loadOrderFormat01(var1, var2, var3);
      }
   }

   private Order loadOrderFormat4(ObjectInput var1) throws IOException {
      Order var2 = new Order();
      var2.Symbol = this.readUTF(var1);
      var2.SetupName = this.readUTF(var1);
      var2.StrategyName = this.readUTF(var1);
      var2.Comment = this.readUTF(var1);
      var2.Ticket = var1.readInt();
      var2.Order = var1.readInt();
      var2.Type = var1.readByte();
      var2.CloseType = var1.readByte();
      var2.SampleType = var1.readByte();
      var2.OriginalOpenTime = var1.readLong();
      var2.OriginalType = var1.readByte();
      var2.Size = (float)var1.readDouble();
      var2.OriginalPrice = (float)var1.readDouble();
      var2.OpenTime = var1.readLong();
      var2.OpenPrice = (float)var1.readDouble();
      var2.CloseTime = var1.readLong();
      var2.ClosePrice = (float)var1.readDouble();
      var2.StopLoss = (float)var1.readDouble();
      var2.TakeProfit = (float)var1.readDouble();
      var2.BarsInTrade = var1.readShort();
      var2.PL = var1.readFloat();
      var2.PctPL = var1.readFloat();
      var2.PctPL_TWR = var1.readFloat();
      var2.PipsPL = var1.readFloat();
      var2.DD = var1.readFloat();
      var2.PctDD = var1.readFloat();
      var2.PipsDD = var1.readFloat();
      var2.CommSwap = var1.readFloat();
      var2.CommSwapApplied = var1.readBoolean();
      var2.MAE = var1.readFloat();
      var1.readFloat();
      var2.PipsMAE = var1.readFloat();
      var2.MFE = var1.readFloat();
      var1.readFloat();
      var2.PipsMFE = var1.readFloat();
      var1.readInt();
      var2.Duration = this.getDuration(var2);
      var2.AccountBalance = var1.readFloat();
      var2.PctAccountBalance = var1.readFloat();
      var2.PipsAccountBalance = var1.readFloat();
      var1.readFloat();
      var2.MagicNumber = var1.readInt();
      var2.IsInPortfolio = var1.readByte();
      var2.Extra1 = var1.readFloat();
      return var2;
   }

   private String readUTF(ObjectInput var1) throws IOException {
      byte var2 = var1.readByte();
      return var2 == 0 ? null : this.intern(var1.readUTF());
   }

   private Order loadOrderFormat3(ObjectInput var1) throws IOException {
      Order var2 = new Order();
      var2.Symbol = this.intern(var1.readUTF());
      var2.SetupName = this.intern(var1.readUTF());
      var2.StrategyName = this.intern(var1.readUTF());
      var2.Comment = this.intern(var1.readUTF());
      var2.Ticket = var1.readInt();
      var2.Order = var1.readInt();
      var2.Type = var1.readByte();
      var2.CloseType = var1.readByte();
      var2.SampleType = var1.readByte();
      var2.OriginalOpenTime = var1.readLong();
      var2.OriginalType = var1.readByte();
      var2.Size = (float)var1.readDouble();
      var2.OriginalPrice = (float)var1.readDouble();
      var2.OpenTime = var1.readLong();
      var2.OpenPrice = (float)var1.readDouble();
      var2.CloseTime = var1.readLong();
      var2.ClosePrice = (float)var1.readDouble();
      var2.StopLoss = (float)var1.readDouble();
      var2.TakeProfit = (float)var1.readDouble();
      var2.BarsInTrade = var1.readShort();
      var2.PL = var1.readFloat();
      var2.PctPL = var1.readFloat();
      var2.PctPL_TWR = var1.readFloat();
      var2.PipsPL = var1.readFloat();
      var2.DD = var1.readFloat();
      var2.PctDD = var1.readFloat();
      var2.PipsDD = var1.readFloat();
      var2.CommSwap = var1.readFloat();
      var2.CommSwapApplied = var1.readBoolean();
      var2.MAE = var1.readFloat();
      var1.readFloat();
      var2.PipsMAE = var1.readFloat();
      var2.MFE = var1.readFloat();
      var1.readFloat();
      var2.PipsMFE = var1.readFloat();
      var1.readInt();
      var2.Duration = this.getDuration(var2);
      var2.AccountBalance = var1.readFloat();
      var2.PctAccountBalance = var1.readFloat();
      var2.PipsAccountBalance = var1.readFloat();
      var1.readFloat();
      var2.MagicNumber = var1.readInt();
      var2.IsInPortfolio = var1.readByte();
      var2.Extra1 = var1.readFloat();
      var1.readFloat();
      var1.readFloat();
      var1.readUTF();
      return var2;
   }

   private Order loadOrderFormat2(ObjectInput var1) throws IOException {
      Order var2 = new Order();
      var2.Symbol = this.intern(var1.readUTF());
      var2.SetupName = this.intern(var1.readUTF());
      var2.StrategyName = this.intern(var1.readUTF());
      var2.Comment = this.intern(var1.readUTF());
      var2.Ticket = var1.readInt();
      var2.Order = var1.readInt();
      var2.Type = var1.readByte();
      var2.CloseType = var1.readByte();
      var2.SampleType = var1.readByte();
      var2.OriginalOpenTime = var1.readLong();
      var2.OriginalType = var1.readByte();
      var2.Size = (float)var1.readDouble();
      var2.OriginalPrice = (float)var1.readDouble();
      var2.OpenTime = var1.readLong();
      var2.OpenPrice = (float)var1.readDouble();
      var2.CloseTime = var1.readLong();
      var2.ClosePrice = (float)var1.readDouble();
      var2.StopLoss = (float)var1.readDouble();
      var2.TakeProfit = (float)var1.readDouble();
      var2.BarsInTrade = var1.readShort();
      var2.PL = var1.readFloat();
      var2.PctPL = var1.readFloat();
      var2.PctPL_TWR = var1.readFloat();
      var2.PipsPL = var1.readFloat();
      var2.DD = var1.readFloat();
      var2.PctDD = var1.readFloat();
      var2.PipsDD = var1.readFloat();
      var2.CommSwap = var1.readFloat();
      var2.CommSwapApplied = var1.readBoolean();
      var2.MAE = var1.readFloat();
      var1.readFloat();
      var2.PipsMAE = var1.readFloat();
      var2.MFE = var1.readFloat();
      var1.readFloat();
      var2.PipsMFE = var1.readFloat();
      var1.readInt();
      var2.Duration = this.getDuration(var2);
      var2.AccountBalance = var1.readFloat();
      var2.PctAccountBalance = var1.readFloat();
      var2.PipsAccountBalance = var1.readFloat();
      var1.readFloat();
      var2.MagicNumber = var1.readInt();
      var2.IsInPortfolio = var1.readByte();
      return var2;
   }

   private Order loadOrderFormat01(ObjectInput var1, String var2, int var3) throws IOException {
      Order var4 = new Order();
      if (var3 == 0 && var2 != null) {
         var4.Symbol = var2;
      } else {
         var4.Symbol = this.intern(var1.readUTF());
      }

      var4.SetupName = this.intern(var1.readUTF());
      var4.StrategyName = this.intern(var1.readUTF());
      var4.Comment = this.intern(var1.readUTF());
      var4.Ticket = var1.readInt();
      var4.Order = var1.readInt();
      var4.Type = var1.readByte();
      var4.CloseType = var1.readByte();
      var4.SampleType = var1.readByte();
      var4.OriginalOpenTime = var1.readLong();
      var4.OriginalType = var1.readByte();
      var4.Size = (float)var1.readDouble();
      var4.OriginalPrice = (float)var1.readDouble();
      var4.OpenTime = var1.readLong();
      var4.OpenPrice = (float)var1.readDouble();
      var4.CloseTime = var1.readLong();
      var4.ClosePrice = (float)var1.readDouble();
      var4.StopLoss = (float)var1.readDouble();
      var4.TakeProfit = (float)var1.readDouble();
      var4.BarsInTrade = var1.readShort();
      var4.PL = (float)var1.readDouble();
      var4.PctPL = (float)var1.readDouble();
      var4.PctPL_TWR = (float)var1.readDouble();
      var4.PipsPL = (float)var1.readDouble();
      var4.DD = (float)var1.readDouble();
      var4.PctDD = (float)var1.readDouble();
      var4.PipsDD = (float)var1.readDouble();
      var4.CommSwap = (float)var1.readDouble();
      var4.CommSwapApplied = var1.readBoolean();
      var4.MAE = (float)var1.readDouble();
      var1.readDouble();
      var4.PipsMAE = (float)var1.readDouble();
      var4.MFE = (float)var1.readDouble();
      var1.readDouble();
      var4.PipsMFE = (float)var1.readDouble();
      var1.readLong();
      var4.Duration = this.getDuration(var4);
      var4.AccountBalance = (float)var1.readDouble();
      var4.PctAccountBalance = (float)var1.readDouble();
      var4.PipsAccountBalance = (float)var1.readDouble();
      var1.readDouble();
      var4.Extra1 = (float)var1.readDouble();
      var1.readDouble();
      var1.readDouble();
      var1.readDouble();
      var1.readDouble();
      var1.readUTF();
      var1.readUTF();
      var1.readUTF();
      if (var3 > 0) {
         var4.MagicNumber = var1.readInt();
      }

      return var4;
   }

   private String intern(String var1) {
      return var1 != null ? var1.intern() : null;
   }

   private int getDuration(Order var1) {
      long var2 = (var1.CloseTime - var1.OpenTime) / 1000L;
      return (int)(var2 < 2147483647L ? var2 : 2147483647L);
   }

   public boolean contains(Order var1) {
      return this.list.contains(var1);
   }

   public void sort(Comparator<Order> var1) {
      this.list.sort(var1);
   }

   public void removeEndOfTest() {
      ObjectListIterator var1 = this.list.iterator();

      while (var1.hasNext()) {
         Order var2 = (Order)var1.next();
         if (var2.CloseType == 4) {
            var1.remove();
         }
      }
   }

   public void removeById(long var1) {
      ObjectListIterator var3 = this.list.iterator();

      while (var3.hasNext()) {
         Order var4 = (Order)var3.next();
         if (var4.OrderId == var1) {
            var3.remove();
         }
      }
   }
}
