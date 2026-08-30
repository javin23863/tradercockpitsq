package com.strategyquant.gridlib.message;

import com.strategyquant.gridlib.TopicIdent;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.jms.BytesMessage;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.jms.Topic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JmsPerformer implements AutoCloseable {
   private static final Logger LOGGER = LoggerFactory.getLogger(JmsPerformer.class);
   private List<MessageConsumer> consumers = new LinkedList<>();
   private MessageProducer producer;
   private JmsConnection connection;
   private Session session;
   private Topic topic;

   public JmsPerformer(TopicIdent var1, JmsConnectionInfo var2) throws JMSException {
      this(false, var1, var2);
   }

   public JmsPerformer(boolean var1, TopicIdent var2, JmsConnectionInfo var3) throws JMSException {
      this.connection = new JmsConnection(var3);
      this.session = this.connection.createSession(var1);
      this.topic = this.connection.createTopic(var2.name());
   }

   public MessageKind getMessageKind(Message var1) throws JMSException {
      String var2 = var1.getStringProperty(MessageProperties.messageKind.name());
      return MessageKind.valueOf(var2);
   }

   @Override
   public void close() throws JMSException {
      for (MessageConsumer var2 : this.consumers) {
         var2.close();
      }

      if (this.session != null) {
         this.session.close();
      }

      this.connection.close();
   }

   public void prepareMessageClient(MessageListener var1, MessageKind var2) throws JMSException {
      this.prepareMessageClient(var1, MessageProperties.messageKind.name() + "='" + var2.name() + "'");
   }

   public void prepareMessageClient(MessageListener var1, String var2) throws JMSException {
      LOGGER.debug("Preparing ArtemisMQ consumer...");
      String var3 = MessageProperties.targetIdent.name() + " is null OR " + MessageProperties.targetIdent.name() + "='" + this.connection.getIdent() + "'";
      String var4 = var3;
      if (var2 != null && !var2.isEmpty()) {
         var4 = "(" + var4 + ") AND (" + var2 + ")";
      }

      LOGGER.debug("selector:" + var4);
      MessageConsumer var5 = this.session.createConsumer(this.topic, var4);
      var5.setMessageListener(var1);
      LOGGER.debug("ArtemisMQ reciever consumer");
      this.consumers.add(var5);
   }

   private void ensureProducer(Topic var1) throws JMSException {
      if (this.producer == null) {
         LOGGER.debug("Preparing producer...");
         this.producer = this.session.createProducer(var1);
         this.producer.setDisableMessageID(true);
         this.producer.setDisableMessageTimestamp(true);
         this.producer.setDeliveryMode(1);
         LOGGER.debug("Producer prepared");
      }
   }

   private void fillProperties(Message var1, Map<MessageProperties, String> var2) throws JMSException {
      for (MessageProperties var4 : var2.keySet()) {
         String var5 = (String)var2.get(var4);
         var1.setStringProperty(var4.name(), var5);
      }
   }

   public void sendMessageFile(File var1, String var2, String var3) throws JMSException, IOException {
      BytesMessage var4 = this.session.createBytesMessage();
      FileInputStream var5 = new FileInputStream(var1);
      BufferedInputStream var6 = new BufferedInputStream(var5);
      var4.setObjectProperty("JMS_AMQ_InputStream", var6);
      var4.setStringProperty(MessageProperties.folder.name(), var3);
      this.sendMessage(var4, var2, MessageKind.sendingFileData);
      var6.close();
   }

   public void sendLargeMessage(InputStream var1, String var2, Map<MessageProperties, String> var3, MessageKind var4) throws JMSException, IOException {
      BytesMessage var5 = this.session.createBytesMessage();
      BufferedInputStream var6 = new BufferedInputStream(var1);
      var5.setObjectProperty("JMS_AMQ_InputStream", var6);
      this.fillProperties(var5, var3);
      this.sendMessage(var5, var2, var4);
      var6.close();
   }

   private synchronized void sendMessage(Message var1, String var2, MessageKind var3) throws JMSException {
      this.ensureProducer(this.topic);
      LOGGER.debug("Sending message " + var3 + " for target:" + var2 + "... ");
      if (var2 != null) {
         var1.setStringProperty(MessageProperties.targetIdent.name(), var2);
      }

      var1.setStringProperty(MessageProperties.messageKind.name(), var3.name());
      var1.setStringProperty(MessageProperties.senderIdent.name(), this.connection.getIdent());
      this.producer.send(var1);
      LOGGER.debug("Message sent");
   }

   public void sendSimpleMessage(MessageKind var1, String var2, Map<MessageProperties, String> var3) throws JMSException {
      TextMessage var4 = this.session.createTextMessage();
      this.fillProperties(var4, var3);
      this.sendMessage(var4, var2, var1);
   }

   public void sendTextMessage(MessageKind var1, String var2, String var3) throws JMSException {
      TextMessage var4 = this.session.createTextMessage();
      var4.setText(var3);
      this.sendMessage(var4, var2, var1);
   }

   public void sendObjectMessage(MessageKind var1, String var2, Map<MessageProperties, String> var3, Serializable var4) throws JMSException {
      ObjectMessage var5 = this.session.createObjectMessage(var4);
      this.fillProperties(var5, var3);
      this.sendMessage(var5, var2, var1);
   }

   public void commit() throws JMSException {
      this.session.commit();
   }
}
