package com.strategyquant.gridlib.compute.performer;

import com.strategyquant.gridlib.TopicIdent;
import com.strategyquant.gridlib.client.GridJob;
import com.strategyquant.gridlib.client.GridMessage;
import com.strategyquant.gridlib.compute.JobResult;
import com.strategyquant.gridlib.compute.common.ExecuteOptions;
import com.strategyquant.gridlib.config.Config;
import com.strategyquant.gridlib.message.JmsConnectionInfo;
import com.strategyquant.gridlib.message.JmsPerformer;
import com.strategyquant.gridlib.message.MessageKind;
import com.strategyquant.gridlib.message.MessageProperties;
import com.strategyquant.gridlib.topology.GridTopology;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.zip.DataFormatException;
import javax.jms.BytesMessage;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JmsComputePerformer implements IComputePerformer {
   private static final Logger LOGGER = LoggerFactory.getLogger(JmsComputePerformer.class);
   private JmsPerformer statisticsReciever;
   private JmsPerformer jobSender;
   private JmsPerformer messageSender;
   private JmsPerformer reciever;
   private List<IJobStatusChangedHandler> handlers = new LinkedList<>();
   private Config config;
   private GridTopology gridTopology;
   protected String serverTarget;

   public JmsComputePerformer(JmsConnectionInfo var1, Config var2) throws JMSException {
      this.config = var2;
      this.gridTopology = new GridTopology();
      this.jobSender = new JmsPerformer(true, TopicIdent.registerTaskTopic, var1);
      this.messageSender = new JmsPerformer(TopicIdent.messagesTopic, var1);
      this.reciever = new JmsPerformer(TopicIdent.taskResultsTopic, var1);
      this.statisticsReciever = new JmsPerformer(TopicIdent.statisticsTopic, var1);
      this.initListeners();
      this.statisticsReciever.sendSimpleMessage(MessageKind.refreshTopology, null, new HashMap<>());
   }

   private void initListeners() throws JMSException {
      this.reciever.prepareMessageClient(new MessageListener() {
         public void onMessage(Message var1) {
            try {
               JmsComputePerformer.this.handleTaskComputedMessage(var1);
            } catch (Throwable var3) {
               JmsComputePerformer.LOGGER.error("", var3);
            }
         }
      }, MessageKind.resultOfTask);
      this.statisticsReciever
         .prepareMessageClient(
            new MessageListener() {
               public void onMessage(Message var1) {
                  try {
                     JmsComputePerformer.this.serverTarget = var1.getStringProperty(MessageProperties.senderIdent.name());
                     ObjectMessage var2 = (ObjectMessage)var1;
                     JmsComputePerformer.this.gridTopology = (GridTopology)var2.getObject();
                     JmsComputePerformer.LOGGER
                        .debug(
                           "Got grid topology changed. Nodes:"
                              + JmsComputePerformer.this.gridTopology.getNodes().size()
                              + ". Total cores:"
                              + JmsComputePerformer.this.gridTopology.getTotalCores()
                        );
                  } catch (JMSException var3) {
                     JmsComputePerformer.LOGGER.error("", var3);
                  }
               }
            },
            MessageKind.sendingTopology
         );
   }

   @Override
   public void execute(GridJob<?> var1, String var2, ExecuteOptions var3) throws JMSException, IOException {
      JmsJobInfo var4 = new JmsJobInfo(var1.getClass().getName(), var1.getParameters(), var1.getFlags());
      byte[] var5 = JmsHelper.serializeAndCompress(var4, this.config.isUseJavaSerialization(), this.config.isCompressData());
      HashMap var6 = new HashMap();
      var6.put(MessageProperties.jobId, var1.getJobId());
      var6.put(MessageProperties.jobGroupId, var2);
      var6.put(MessageProperties.zippedContent, String.valueOf(this.config.isCompressData()));
      var6.put(MessageProperties.jvmSerialization, String.valueOf(this.config.isUseJavaSerialization()));
      this.jobSender.sendLargeMessage(new ByteArrayInputStream(var5), this.serverTarget, var6, MessageKind.registerTask);
   }

   private void handleTaskComputedMessage(Message var1) throws JMSException, InterruptedException, IOException, ClassNotFoundException, DataFormatException {
      String var2 = var1.getStringProperty(MessageProperties.jobId.name());
      String var3 = var1.getStringProperty(MessageProperties.jobGroupId.name());
      boolean var4 = Boolean.parseBoolean(var1.getStringProperty(MessageProperties.jvmSerialization.name()));
      boolean var5 = Boolean.parseBoolean(var1.getStringProperty(MessageProperties.zippedContent.name()));
      if (var2 == null) {
         LOGGER.debug("Message without jobId");
      } else {
         JobResult var6 = null;

         try {
            if (!(var1 instanceof BytesMessage)) {
               LOGGER.debug("Unexpected message format");
               return;
            }

            var6 = (JobResult)JmsHelper.handleBytesMessage(var1, var4, var5);
         } catch (Throwable var8) {
            var6 = new JobResult();
            var6.setSuccess(false);
            var6.setErrorMessage(ExceptionUtils.getStackTrace(var8));
         }

         this.callHandler(var2, var3, var6);
      }
   }

   private void callHandler(String var1, String var2, JobResult<Serializable> var3) {
      for (IJobStatusChangedHandler var5 : this.handlers) {
         var5.onJobFinished(var1, var2, var3);
      }
   }

   @Override
   public void close() throws JMSException {
      this.jobSender.close();
      this.statisticsReciever.close();
      this.reciever.close();
      this.messageSender.close();
      this.handlers.clear();
   }

   @Override
   public void addJobFinishedHandler(IJobStatusChangedHandler var1) {
      this.handlers.add(var1);
   }

   @Override
   public void commit() throws JMSException {
      this.jobSender.commit();
   }

   @Override
   public void pause(String var1) throws JMSException {
      JmsHelper.pause(this.messageSender, var1);
   }

   @Override
   public void stop(String var1) throws JMSException {
      JmsHelper.stop(this.messageSender, var1);
   }

   @Override
   public void restart(String var1) throws JMSException {
      JmsHelper.restart(this.messageSender, var1);
   }

   @Override
   public boolean sendMessageToJob(String var1, String var2, GridMessage var3) throws JMSException, IOException {
      JmsHelper.sendMessageToJob(this.messageSender, this.serverTarget, var1, var2, this.config.isUseJavaSerialization(), this.config.isCompressData(), var3);
      return true;
   }

   @Override
   public GridTopology getGridTopology() {
      return this.gridTopology;
   }

   @Override
   public String getGridDescriptions() {
      return null;
   }

   @Override
   public void setProgress(String var1, String var2, int var3) {
   }

   @Override
   public void setGroupRestrictions(String var1, int var2) {
   }

   @Override
   public void check() {
   }

   @Override
   public void setJobWaiting(String var1, String var2, boolean var3) {
   }

   @Override
   public int getComputedThread() {
      return 0;
   }

   @Override
   public int getUsedComputedThreads() {
      return 0;
   }

   @Override
   public void setUsedComputedThreads(int var1) {
   }

   @Override
   public void pause(String var1, String var2) throws Exception {
   }

   @Override
   public void stop(String var1, String var2) throws Exception {
   }

   @Override
   public void restart(String var1, String var2) throws Exception {
   }
}
