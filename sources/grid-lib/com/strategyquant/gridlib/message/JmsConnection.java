package com.strategyquant.gridlib.message;

import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.Session;
import javax.jms.Topic;
import org.apache.activemq.artemis.api.jms.ActiveMQJMSClient;
import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JmsConnection {
   private static final Logger LOGGER = LoggerFactory.getLogger(JmsConnection.class);
   private String ident;
   private Connection connection;
   private ActiveMQConnectionFactory factory;
   private String target;

   public JmsConnection(JmsConnectionInfo var1) throws JMSException {
      this.ident = var1.getIdent();
      this.target = var1.getTarget();
      this.initClient();
   }

   public Session createSession(boolean var1) throws JMSException {
      return this.connection.createSession(var1, 1);
   }

   protected String getIdent() {
      return this.ident;
   }

   private void initClient() throws JMSException {
      int var1 = 102400;
      if (!this.target.startsWith("tcp") && !this.target.startsWith("vm")) {
         this.target = "tcp://" + this.target;
      }

      LOGGER.debug("Connecting to ActiveMQ engine (" + this.target + ")...");
      this.factory = new ActiveMQConnectionFactory(this.target);
      this.factory.setMinLargeMessageSize(var1);
      this.connection = this.factory.createConnection();
      this.connection.start();
      LOGGER.debug("ActiveMQ connected");
   }

   public Topic createTopic(String var1) {
      return ActiveMQJMSClient.createTopic(var1);
   }

   public void close() throws JMSException {
      if (this.connection != null) {
         LOGGER.debug("Closing JMS connection...");
         this.connection.close();
         LOGGER.debug("JMS connection closed");
      }

      if (this.factory != null) {
         LOGGER.debug("Closing JMS factory...");
         this.factory.close();
         LOGGER.debug("JMS factory closed...");
      }
   }
}
