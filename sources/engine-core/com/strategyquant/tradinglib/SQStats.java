package com.strategyquant.tradinglib;

import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.settings.IXMLAble;
import com.strategyquant.lib.utils.ISQCloneable;
import com.strategyquant.tradinglib.results.StatsKeyCache;
import it.unimi.dsi.fastutil.ints.Int2FloatOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2LongOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap.Entry;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap.FastEntrySet;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.List;
import org.apache.commons.codec.binary.Base64;
import org.jdom2.Attribute;
import org.jdom2.DataConversionException;
import org.jdom2.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SQStats implements IXMLAble, ISQCloneable<SQStats> {
   public static final Logger Log = LoggerFactory.getLogger("SQStats");
   private int[] intValues = new int[StatsKeyCache.getIntsCount()];
   private float[] doubleValues = new float[StatsKeyCache.getDoublesCount()];
   private long[] longValues = new long[StatsKeyCache.getLongsCount()];
   private Int2IntOpenHashMap unknownIntsMap = null;
   private Int2FloatOpenHashMap unknownDoublesMap = null;
   private Int2LongOpenHashMap unknownLongsMap = null;
   private Int2ObjectOpenHashMap<Object> objectMap = null;

   public void set(String var1, int var2) {
      StatsKeyCache.StatInfo var3 = StatsKeyCache.getKey(var1);
      if (var3 != null) {
         switch (var3.type) {
            case 1:
               this.intValues[var3.index] = var2;
               return;
            case 2:
               this.longValues[var3.index] = var2;
               return;
            case 3:
               this.doubleValues[var3.index] = var2;
               return;
            default:
               throw new IllegalArgumentException("Shouldn't get here!");
         }
      } else {
         int var4 = StatsKeyCache.registerKey(var1, (byte)1);
         if (this.unknownIntsMap == null) {
            this.unknownIntsMap = new Int2IntOpenHashMap();
         }

         this.unknownIntsMap.put(var4, var2);
      }
   }

   public void set(String var1, Object var2) {
      int var3 = StatsKeyCache.registerKey(var1, (byte)4);
      if (this.objectMap == null) {
         this.objectMap = new Int2ObjectOpenHashMap();
      }

      this.objectMap.put(var3, var2);
   }

   public void set(String var1, long var2) {
      StatsKeyCache.StatInfo var4 = StatsKeyCache.getKey(var1);
      if (var4 != null) {
         switch (var4.type) {
            case 1:
               this.intValues[var4.index] = (int)var2;
               return;
            case 2:
               this.longValues[var4.index] = var2;
               return;
            case 3:
               this.doubleValues[var4.index] = (float)var2;
               return;
            default:
               throw new IllegalArgumentException("Shouldn't get here!");
         }
      } else {
         int var5 = StatsKeyCache.registerKey(var1, (byte)2);
         if (this.unknownLongsMap == null) {
            this.unknownLongsMap = new Int2LongOpenHashMap();
         }

         this.unknownLongsMap.put(var5, var2);
      }
   }

   public void set(String var1, double var2) {
      StatsKeyCache.StatInfo var4 = StatsKeyCache.getKey(var1);
      if (var4 != null) {
         switch (var4.type) {
            case 1:
               this.intValues[var4.index] = (int)var2;
               return;
            case 2:
               this.longValues[var4.index] = (long)var2;
               return;
            case 3:
               this.doubleValues[var4.index] = (float)var2;
               return;
            default:
               throw new IllegalArgumentException("Shouldn't get here!");
         }
      } else {
         int var5 = StatsKeyCache.registerKey(var1, (byte)3);
         if (this.unknownDoublesMap == null) {
            this.unknownDoublesMap = new Int2FloatOpenHashMap();
         }

         this.unknownDoublesMap.put(var5, (float)var2);
      }
   }

   public int getInt(String var1) {
      return this.getInt(var1, 0);
   }

   public int getInt(String var1, int var2) {
      StatsKeyCache.StatInfo var3 = StatsKeyCache.getKey(var1);
      if (var3 != null) {
         switch (var3.type) {
            case 1:
               return this.intValues[var3.index];
            case 2:
               return (int)this.longValues[var3.index];
            case 3:
               return (int)this.doubleValues[var3.index];
            default:
               throw new IllegalArgumentException("Shouldn't get here!");
         }
      } else {
         int var4 = var1.hashCode();
         if (this.unknownIntsMap != null && this.unknownIntsMap.containsKey(var4)) {
            return this.unknownIntsMap.get(var4);
         } else if (this.unknownDoublesMap != null && this.unknownDoublesMap.containsKey(var4)) {
            return (int)this.unknownDoublesMap.get(var4);
         } else {
            return this.unknownLongsMap != null && this.unknownLongsMap.containsKey(var4) ? (int)this.unknownLongsMap.get(var4) : var2;
         }
      }
   }

   public long getLong(String var1) {
      return this.getLong(var1, 0L);
   }

   public long getLong(String var1, long var2) {
      StatsKeyCache.StatInfo var4 = StatsKeyCache.getKey(var1);
      if (var4 != null) {
         switch (var4.type) {
            case 1:
               return this.intValues[var4.index];
            case 2:
               return this.longValues[var4.index];
            case 3:
               return (long)this.doubleValues[var4.index];
            default:
               throw new IllegalArgumentException("Shouldn't get here!");
         }
      } else {
         int var5 = var1.hashCode();
         if (this.unknownLongsMap != null && this.unknownLongsMap.containsKey(var5)) {
            return this.unknownLongsMap.get(var5);
         } else if (this.unknownDoublesMap != null && this.unknownDoublesMap.containsKey(var5)) {
            return (long)this.unknownDoublesMap.get(var5);
         } else {
            return this.unknownIntsMap != null && this.unknownIntsMap.containsKey(var5) ? this.unknownIntsMap.get(var5) : var2;
         }
      }
   }

   public double getDouble(String var1) {
      return this.getDouble(var1, 0.0);
   }

   public double getDouble(String var1, double var2) {
      StatsKeyCache.StatInfo var4 = StatsKeyCache.getKey(var1);
      if (var4 != null) {
         switch (var4.type) {
            case 1:
               return this.intValues[var4.index];
            case 2:
               return this.longValues[var4.index];
            case 3:
               return this.doubleValues[var4.index];
            default:
               throw new IllegalArgumentException("Shouldn't get here!");
         }
      } else {
         int var5 = var1.hashCode();
         if (this.unknownDoublesMap != null && this.unknownDoublesMap.containsKey(var5)) {
            return this.unknownDoublesMap.get(var5);
         } else if (this.unknownIntsMap != null && this.unknownIntsMap.containsKey(var5)) {
            return this.unknownIntsMap.get(var5);
         } else {
            return this.unknownLongsMap != null && this.unknownLongsMap.containsKey(var5) ? this.unknownLongsMap.get(var5) : var2;
         }
      }
   }

   public Element getXML() {
      try {
         return this.getXMLOptimizedFormat();
      } catch (IOException var2) {
         Log.error("Error loading stats", var2);
         return null;
      }
   }

   public void setFromXML(Element var1) {
      if (var1 != null) {
         this.clearData();
         String var2 = var1.getAttributeValue("e");
         if (var2 == null) {
            this.loadUsingOldFormat(var1);
         } else {
            try {
               this.loadUsingOptimizedFormat(var1);
            } catch (IOException var4) {
               Log.error("Error loading stats", var4);
            }
         }
      }
   }

   private void loadUsingOptimizedFormat(Element var1) throws IOException {
      String var2 = var1.getText();
      int var3 = 1;
      String var4 = var1.getAttributeValue("version");
      if (var4 != null) {
         var3 = Integer.parseInt(var4);
      }

      DataInputStream var5 = new DataInputStream(new ByteArrayInputStream(Base64.decodeBase64(var2)));

      while (var5.available() > 0) {
         byte var6 = var5.readByte();
         switch (var6) {
            case 1:
               byte var10 = var5.readByte();
               this.intValues[var10] = var5.readInt();
               break;
            case 2:
               byte var9 = var5.readByte();
               this.longValues[var9] = var5.readLong();
               break;
            case 3:
               byte var7 = var5.readByte();
               if (var3 > 1) {
                  this.doubleValues[var7] = var5.readFloat();
               } else {
                  this.doubleValues[var7] = (float)var5.readDouble();
               }
               break;
            case 101:
               String var12 = var5.readUTF();
               this.set(var12, var5.readInt());
               break;
            case 102:
               String var11 = var5.readUTF();
               this.set(var11, var5.readLong());
               break;
            case 103:
               String var8 = var5.readUTF();
               if (var3 > 1) {
                  this.set(var8, var5.readFloat());
               } else {
                  this.set(var8, var5.readDouble());
               }
               break;
            default:
               throw new IllegalArgumentException("Shouldn't get here!");
         }
      }
   }

   private void loadUsingOldFormat(Element var1) {
      List var4 = var1.getAttributes();

      for (int var5 = 0; var5 < var4.size(); var5++) {
         Attribute var6 = (Attribute)var4.get(var5);
         String var3 = var6.getName();
         StatsKeyCache.StatInfo var7 = StatsKeyCache.getShortKey(var3.hashCode());
         if (var7 != null) {
            switch (var7.type) {
               case 1:
                  this.intValues[var7.index] = (int)this.getAttrValue(var6, 1);
                  break;
               case 2:
                  this.longValues[var7.index] = (long)this.getAttrValue(var6, 2);
                  break;
               case 3:
                  this.doubleValues[var7.index] = (float)this.getAttrValue(var6, 3);
                  break;
               default:
                  throw new IllegalArgumentException("This shouldn't happen!");
            }
         }
      }

      for (Element var11 : var1.getChildren()) {
         String var2 = var11.getName();
         if (var11.getContentSize() > 0) {
            String var12 = var11.getAttributeValue("type");
            if (var12 == null) {
               throw new IllegalArgumentException("Unknown type '" + var12 + "'!");
            }

            switch (var12) {
               case "Integer":
                  this.set(var2, Integer.parseInt(var11.getValue()));
                  break;
               case "Long":
                  this.set(var2, Long.parseLong(var11.getValue()));
                  break;
               case "Double":
                  this.set(var2, Double.parseDouble(var11.getValue()));
                  break;
               default:
                  this.set(var2, var11.getValue());
            }
         }
      }
   }

   private double getAttrValue(Attribute var1, int var2) {
      try {
         switch (var2) {
            case 1:
               return var1.getIntValue();
            case 2:
               return var1.getLongValue();
            case 3:
               return var1.getDoubleValue();
            default:
               throw new IllegalArgumentException("Shouldn't get here!");
         }
      } catch (DataConversionException var5) {
         try {
            return var1.getDoubleValue();
         } catch (DataConversionException var4) {
            return -1.23456789E8;
         }
      }
   }

   public void clearData() {
      for (int var1 = 0; var1 < this.intValues.length; var1++) {
         this.intValues[var1] = 0;
      }

      for (int var2 = 0; var2 < this.doubleValues.length; var2++) {
         this.doubleValues[var2] = 0.0F;
      }

      for (int var3 = 0; var3 < this.longValues.length; var3++) {
         this.longValues[var3] = 0L;
      }

      if (this.unknownIntsMap != null) {
         this.unknownIntsMap.replaceAll((var0, var1x) -> 0);
      }

      if (this.unknownDoublesMap != null) {
         this.unknownDoublesMap.replaceAll((var0, var1x) -> 0.0F);
      }

      if (this.unknownLongsMap != null) {
         this.unknownLongsMap.replaceAll((var0, var1x) -> 0L);
      }
   }

   public void copyValuesFrom(SQStats var1) {
      for (int var2 = 0; var2 < this.intValues.length; var2++) {
         this.intValues[var2] = var1.intValues[var2];
      }

      for (int var3 = 0; var3 < this.doubleValues.length; var3++) {
         this.doubleValues[var3] = var1.doubleValues[var3];
      }

      for (int var4 = 0; var4 < this.longValues.length; var4++) {
         this.longValues[var4] = var1.longValues[var4];
      }

      if (var1.unknownIntsMap != null) {
         if (this.unknownIntsMap == null) {
            this.unknownIntsMap = new Int2IntOpenHashMap();
         }

         this.unknownIntsMap.putAll(var1.unknownIntsMap);
      }

      if (var1.unknownIntsMap != null) {
         if (this.unknownIntsMap == null) {
            this.unknownIntsMap = new Int2IntOpenHashMap();
         }

         this.unknownIntsMap.putAll(var1.unknownIntsMap);
      }

      if (var1.unknownDoublesMap != null) {
         if (this.unknownDoublesMap == null) {
            this.unknownDoublesMap = new Int2FloatOpenHashMap();
         }

         this.unknownDoublesMap.putAll(var1.unknownDoublesMap);
      }

      if (var1.unknownLongsMap != null) {
         if (this.unknownLongsMap == null) {
            this.unknownLongsMap = new Int2LongOpenHashMap();
         }

         this.unknownLongsMap.putAll(var1.unknownLongsMap);
      }
   }

   public void copyValueFrom(SQStats var1, String var2) {
      StatsKeyCache.StatInfo var3 = StatsKeyCache.getKey(var2);
      if (var3 != null) {
         switch (var3.type) {
            case 1:
               this.intValues[var3.index] = var1.intValues[var3.index];
               return;
            case 2:
               this.longValues[var3.index] = var1.longValues[var3.index];
               return;
            case 3:
               this.doubleValues[var3.index] = var1.doubleValues[var3.index];
               return;
            default:
               throw new IllegalArgumentException("Shouldn't get here!");
         }
      } else {
         int var4 = StatsKeyCache.registerKey(var2, (byte)4);
         if ((this.unknownDoublesMap == null || !this.unknownDoublesMap.containsKey(var4))
            && (var1.unknownDoublesMap == null || !var1.unknownDoublesMap.containsKey(var4))) {
            if ((this.unknownIntsMap == null || !this.unknownIntsMap.containsKey(var4))
               && (var1.unknownIntsMap == null || !var1.unknownIntsMap.containsKey(var4))) {
               if ((this.unknownLongsMap == null || !this.unknownLongsMap.containsKey(var4))
                  && (var1.unknownLongsMap == null || !var1.unknownLongsMap.containsKey(var4))) {
                  if (Log.isDebugEnabled()) {
                  }
               } else {
                  if (this.unknownLongsMap == null) {
                     this.unknownLongsMap = new Int2LongOpenHashMap();
                  }

                  this.unknownLongsMap.put(var4, var1.getLong(var2));
               }
            } else {
               if (this.unknownIntsMap == null) {
                  this.unknownIntsMap = new Int2IntOpenHashMap();
               }

               this.unknownIntsMap.put(var4, var1.getInt(var2));
            }
         } else {
            if (this.unknownDoublesMap == null) {
               this.unknownDoublesMap = new Int2FloatOpenHashMap();
            }

            this.unknownDoublesMap.put(var4, (float)var1.getDouble(var2));
         }
      }
   }

   public boolean containsKey(String var1) {
      StatsKeyCache.StatInfo var2 = StatsKeyCache.getKey(var1);
      if (var2 != null) {
         return true;
      } else {
         int var3 = var1.hashCode();
         if (this.unknownDoublesMap != null && this.unknownDoublesMap.containsKey(var3)) {
            return true;
         } else {
            return this.unknownIntsMap != null && this.unknownIntsMap.containsKey(var3)
               ? true
               : this.unknownLongsMap != null && this.unknownLongsMap.containsKey(var3);
         }
      }
   }

   public SQStats getClone() throws Exception {
      SQStats var1 = new SQStats();

      for (int var2 = 0; var2 < this.intValues.length; var2++) {
         var1.intValues[var2] = this.intValues[var2];
      }

      for (int var6 = 0; var6 < this.doubleValues.length; var6++) {
         var1.doubleValues[var6] = this.doubleValues[var6];
      }

      for (int var7 = 0; var7 < this.longValues.length; var7++) {
         var1.longValues[var7] = this.longValues[var7];
      }

      if (this.unknownIntsMap != null) {
         if (var1.unknownIntsMap == null) {
            var1.unknownIntsMap = new Int2IntOpenHashMap();
         }

         var1.unknownIntsMap.putAll(this.unknownIntsMap);
      }

      if (this.unknownDoublesMap != null) {
         if (var1.unknownDoublesMap == null) {
            var1.unknownDoublesMap = new Int2FloatOpenHashMap();
         }

         var1.unknownDoublesMap.putAll(this.unknownDoublesMap);
      }

      if (this.unknownLongsMap != null) {
         if (var1.unknownLongsMap == null) {
            var1.unknownLongsMap = new Int2LongOpenHashMap();
         }

         var1.unknownLongsMap.putAll(this.unknownLongsMap);
      }

      if (this.objectMap != null) {
         IntIterator var8 = this.objectMap.keySet().iterator();

         while (var8.hasNext()) {
            int var3 = (Integer)var8.next();
            Object var4 = this.objectMap.get(var3);
            if (var4 != null) {
               Object var5 = SQUtils.cloneObject(var4);
               if (var5 != null) {
                  if (var1.objectMap == null) {
                     var1.objectMap = new Int2ObjectOpenHashMap();
                  }

                  var1.objectMap.put(var3, var5);
               }
            }
         }
      }

      return var1;
   }

   public Element getXMLOptimizedFormat() throws IOException {
      Element var1 = new Element("SQStats");
      var1.setAttribute("version", "2");
      ByteArrayOutputStream var2 = new ByteArrayOutputStream();
      DataOutputStream var3 = new DataOutputStream(var2);
      ObjectIterator var4 = StatsKeyCache.defaultKeysMap.int2ObjectEntrySet().iterator();

      while (var4.hasNext()) {
         Entry var5 = (Entry)var4.next();
         StatsKeyCache.StatInfo var6 = (StatsKeyCache.StatInfo)var5.getValue();
         switch (var6.type) {
            case 1:
               var3.writeByte(1);
               var3.writeByte(var6.index);
               var3.writeInt(this.intValues[var6.index]);
               break;
            case 2:
               var3.writeByte(2);
               var3.writeByte(var6.index);
               var3.writeLong(this.longValues[var6.index]);
               break;
            case 3:
               var3.writeByte(3);
               var3.writeByte(var6.index);
               var3.writeFloat(this.doubleValues[var6.index]);
               break;
            default:
               throw new IllegalArgumentException("Shouldn't get here!");
         }
      }

      if (this.unknownIntsMap != null) {
         var4 = this.unknownIntsMap.int2IntEntrySet().iterator();

         while (var4.hasNext()) {
            it.unimi.dsi.fastutil.ints.Int2IntMap.Entry var14 = (it.unimi.dsi.fastutil.ints.Int2IntMap.Entry)var4.next();
            int var17 = var14.getIntKey();
            int var7 = var14.getIntValue();
            var3.writeByte(101);
            String var8 = StatsKeyCache.getUnknownKeyName(var17);
            var3.writeUTF(var8);
            var3.writeInt(var7);
         }
      }

      if (this.unknownDoublesMap != null) {
         var4 = this.unknownDoublesMap.int2FloatEntrySet().iterator();

         while (var4.hasNext()) {
            it.unimi.dsi.fastutil.ints.Int2FloatMap.Entry var15 = (it.unimi.dsi.fastutil.ints.Int2FloatMap.Entry)var4.next();
            int var18 = var15.getIntKey();
            float var20 = var15.getFloatValue();
            var3.writeByte(103);
            String var22 = StatsKeyCache.getUnknownKeyName(var18);
            var3.writeUTF(var22);
            var3.writeFloat(var20);
         }
      }

      if (this.unknownLongsMap != null) {
         var4 = this.unknownLongsMap.int2LongEntrySet().iterator();

         while (var4.hasNext()) {
            it.unimi.dsi.fastutil.ints.Int2LongMap.Entry var16 = (it.unimi.dsi.fastutil.ints.Int2LongMap.Entry)var4.next();
            int var19 = var16.getIntKey();
            long var21 = var16.getLongValue();
            var3.writeByte(102);
            String var9 = StatsKeyCache.getUnknownKeyName(var19);
            var3.writeUTF(var9);
            var3.writeLong(var21);
         }
      }

      var3.flush();
      String var13 = Base64.encodeBase64String(var2.toByteArray());
      var1.setAttribute("e", "b64");
      var1.setText(var13);
      return var1;
   }

   public void serialize(ObjectOutput var1) throws IOException {
      var1.writeInt(2);
      FastEntrySet var2 = StatsKeyCache.defaultKeysMap.int2ObjectEntrySet();
      IntSet var3 = this.unknownIntsMap == null ? null : this.unknownIntsMap.keySet();
      IntSet var4 = this.unknownDoublesMap == null ? null : this.unknownDoublesMap.keySet();
      IntSet var5 = this.unknownLongsMap == null ? null : this.unknownLongsMap.keySet();
      int var6 = var2.size() + (var3 == null ? 0 : var3.size()) + (var4 == null ? 0 : var4.size()) + (var5 == null ? 0 : var5.size());
      var1.writeInt(var6);
      ObjectIterator var7 = var2.iterator();

      while (var7.hasNext()) {
         Entry var8 = (Entry)var7.next();
         StatsKeyCache.StatInfo var9 = (StatsKeyCache.StatInfo)var8.getValue();
         switch (var9.type) {
            case 1:
               var1.writeByte(1);
               var1.writeByte(var9.index);
               var1.writeInt(this.intValues[var9.index]);
               break;
            case 2:
               var1.writeByte(2);
               var1.writeByte(var9.index);
               var1.writeLong(this.longValues[var9.index]);
               break;
            case 3:
               var1.writeByte(3);
               var1.writeByte(var9.index);
               var1.writeFloat(this.doubleValues[var9.index]);
               break;
            default:
               throw new IllegalArgumentException("Shouldn't get here!");
         }
      }

      if (var3 != null) {
         var7 = this.unknownIntsMap.int2IntEntrySet().iterator();

         while (var7.hasNext()) {
            it.unimi.dsi.fastutil.ints.Int2IntMap.Entry var16 = (it.unimi.dsi.fastutil.ints.Int2IntMap.Entry)var7.next();
            int var19 = var16.getIntKey();
            int var10 = var16.getIntValue();
            var1.writeByte(101);
            String var11 = StatsKeyCache.getUnknownKeyName(var19);
            SQUtils.writeUTF(var1, var11);
            var1.writeInt(var10);
         }
      }

      if (var4 != null) {
         var7 = this.unknownDoublesMap.int2FloatEntrySet().iterator();

         while (var7.hasNext()) {
            it.unimi.dsi.fastutil.ints.Int2FloatMap.Entry var17 = (it.unimi.dsi.fastutil.ints.Int2FloatMap.Entry)var7.next();
            int var20 = var17.getIntKey();
            float var22 = var17.getFloatValue();
            var1.writeByte(103);
            String var24 = StatsKeyCache.getUnknownKeyName(var20);
            SQUtils.writeUTF(var1, var24);
            var1.writeFloat(var22);
         }
      }

      if (var5 != null) {
         var7 = this.unknownLongsMap.int2LongEntrySet().iterator();

         while (var7.hasNext()) {
            it.unimi.dsi.fastutil.ints.Int2LongMap.Entry var18 = (it.unimi.dsi.fastutil.ints.Int2LongMap.Entry)var7.next();
            int var21 = var18.getIntKey();
            long var23 = var18.getLongValue();
            var1.writeByte(102);
            String var12 = StatsKeyCache.getUnknownKeyName(var21);
            SQUtils.writeUTF(var1, var12);
            var1.writeLong(var23);
         }
      }
   }

   public void deserialize(ObjectInput var1) throws IOException {
      int var2 = var1.readInt();
      if (var2 != 1 && var2 != 2) {
         throw new IllegalArgumentException("Incorrect format " + var2);
      }

      int var6 = var1.readInt();

      for (int var7 = 0; var7 < var6; var7++) {
         byte var3 = var1.readByte();
         switch (var3) {
            case 1:
               byte var9 = var1.readByte();
               this.intValues[var9] = var1.readInt();
               break;
            case 2:
               byte var8 = var1.readByte();
               this.longValues[var8] = var1.readLong();
               break;
            case 3:
               byte var4 = var1.readByte();
               if (var2 == 1) {
                  this.doubleValues[var4] = (float)var1.readDouble();
               } else {
                  this.doubleValues[var4] = var1.readFloat();
               }
               break;
            case 101:
               String var11 = SQUtils.readUTFIntern(var1);
               this.set(var11, var1.readInt());
               break;
            case 102:
               String var10 = SQUtils.readUTFIntern(var1);
               this.set(var10, var1.readLong());
               break;
            case 103:
               String var5 = SQUtils.readUTFIntern(var1);
               if (var2 == 1) {
                  this.set(var5, var1.readDouble());
               } else {
                  this.set(var5, var1.readFloat());
               }
               break;
            default:
               throw new IllegalArgumentException("Shouldn't get here!");
         }
      }
   }
}
