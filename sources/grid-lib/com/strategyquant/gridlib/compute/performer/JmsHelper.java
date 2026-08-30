package com.strategyquant.gridlib.compute.performer;

import com.strategyquant.gridlib.client.GridMessage;
import com.strategyquant.gridlib.message.JmsPerformer;
import com.strategyquant.gridlib.message.MessageKind;
import com.strategyquant.gridlib.message.MessageProperties;
import com.strategyquant.gridlib.utils.BeanUtils;
import com.strategyquant.gridlib.utils.CompressionUtils;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.zip.DataFormatException;
import javax.jms.JMSException;
import javax.jms.Message;
import org.nustaq.serialization.FSTConfiguration;

public class JmsHelper {
   private static final FSTConfiguration fstConf = FSTConfiguration.createDefaultConfiguration();

   public static byte[] serializeAndCompress(Serializable var0, boolean var1, boolean var2) throws IOException {
      byte[] var3;
      if (var1) {
         var3 = BeanUtils.serialize(var0);
      } else {
         var3 = fstConf.asByteArray(var0);
      }

      if (var2) {
         var3 = CompressionUtils.compress(var3);
      }

      return var3;
   }

   public static Serializable handleBytesMessage(Message var0, boolean var1, boolean var2) throws IOException, JMSException, ClassNotFoundException, DataFormatException {
      ByteArrayOutputStream var3 = new ByteArrayOutputStream();

      Serializable var7;
      try {
         BufferedOutputStream var4 = new BufferedOutputStream(var3);

         try {
            var0.setObjectProperty("JMS_AMQ_SaveStream", var4);
            byte[] var5 = var3.toByteArray();
            Serializable var6 = decompressAndDeserialize(var5, var1, var2);
            var7 = var6;
         } catch (Throwable var10) {
            try {
               var4.close();
            } catch (Throwable var9) {
               var10.addSuppressed(var9);
            }

            throw var10;
         }

         var4.close();
      } catch (Throwable var11) {
         try {
            var3.close();
         } catch (Throwable var8) {
            var11.addSuppressed(var8);
         }

         throw var11;
      }

      var3.close();
      return var7;
   }

   public static Serializable decompressAndDeserialize(byte[] var0, boolean var1, boolean var2) throws ClassNotFoundException, IOException, DataFormatException {
      if (var2) {
         var0 = CompressionUtils.decompress(var0);
      }

      return var1 ? BeanUtils.deserializeObject(var0) : (Serializable)fstConf.asObject(var0);
   }

   public static void pause(JmsPerformer var0, String var1) throws JMSException {
      HashMap var2 = new HashMap();
      var2.put(MessageProperties.jobGroupId, var1);
      var0.sendSimpleMessage(MessageKind.pauseTasks, null, var2);
   }

   public static void stop(JmsPerformer var0, String var1) throws JMSException {
      HashMap var2 = new HashMap();
      var2.put(MessageProperties.jobGroupId, var1);
      var0.sendSimpleMessage(MessageKind.stopTasks, null, var2);
   }

   public static void restart(JmsPerformer var0, String var1) throws JMSException {
      HashMap var2 = new HashMap();
      var2.put(MessageProperties.jobGroupId, var1);
      var0.sendSimpleMessage(MessageKind.restartTasks, null, var2);
   }

   public static void sendMessageToJob(JmsPerformer var0, String var1, String var2, String var3, boolean var4, boolean var5, GridMessage var6) throws JMSException, IOException {
      byte[] var7 = serializeAndCompress(var6, var4, var5);
      sendMessageToJob(var0, var1, var2, var3, var4, var5, var7);
   }

   public static void sendMessageToJob(JmsPerformer var0, String var1, String var2, String var3, boolean var4, boolean var5, byte[] var6) throws JMSException, IOException {
      HashMap var7 = new HashMap();
      var7.put(MessageProperties.jobId, var2);
      var7.put(MessageProperties.jobGroupId, var3);
      var7.put(MessageProperties.zippedContent, String.valueOf(var5));
      var7.put(MessageProperties.jvmSerialization, String.valueOf(var4));
      var0.sendLargeMessage(new ByteArrayInputStream(var6), var1, var7, MessageKind.sendCustomJobMessage);
   }
}
