package com.strategyquant.gridlib.client;

import com.strategyquant.gridlib.TopicIdent;
import com.strategyquant.gridlib.message.JmsConnectionInfo;
import com.strategyquant.gridlib.message.JmsPerformer;
import com.strategyquant.gridlib.message.MessageKind;
import com.strategyquant.gridlib.message.MessageProperties;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ListenerManager implements AutoCloseable {
   private static final Logger LOGGER = LoggerFactory.getLogger(ListenerManager.class);
   private ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
   private Map<String, IGridMessageListener> listeners = new HashMap<>();
   private JmsPerformer sender;
   private JmsPerformer reciever;
   private ConcurrentMap<String, Boolean> resultMap = new ConcurrentHashMap<>();

   public ListenerManager(JmsConnectionInfo var1, boolean var2) throws JMSException {
      if (!var2) {
         this.prepareListener(var1);
      }
   }

   private void prepareListener(JmsConnectionInfo var1) throws JMSException {
      this.sender = new JmsPerformer(TopicIdent.listenerTopic, var1);
      this.reciever = new JmsPerformer(TopicIdent.listenerTopic, var1);
      this.reciever.prepareMessageClient(new MessageListener() {
         public void onMessage(Message var1) {
            try {
               String var2 = var1.getStringProperty(MessageProperties.jobGroupId.name());
               boolean var3 = var1.getBooleanProperty(MessageProperties.registerResult.name());
               ListenerManager.this.resultMap.put(var2, var3);
            } catch (JMSException var4) {
               ListenerManager.LOGGER.error("", var4);
            }
         }
      }, MessageKind.registerListenerResult);
   }

   public void registerMessageListener(String var1, IGridMessageListener var2) {
      try {
         if (this.lock.writeLock().tryLock(200L, TimeUnit.MILLISECONDS)) {
            try {
               if (this.listeners.containsKey(var1)) {
                  throw new IllegalArgumentException("Listener for group: " + var1 + " is already registered!");
               }

               this.listeners.put(var1, var2);
               if (this.sender != null) {
                  HashMap var3 = new HashMap();
                  var3.put(MessageProperties.jobGroupId, var1);

                  try {
                     this.sender.sendSimpleMessage(MessageKind.registerListener, null, var3);
                  } catch (JMSException var9) {
                     LOGGER.error("", var9);
                     throw new IllegalArgumentException("Checking unique of listener failed!");
                  }

                  while (!this.resultMap.containsKey(var1)) {
                  }

                  Boolean var4 = this.resultMap.get(var1);
                  this.resultMap.remove(var1);
                  if (!var4) {
                     throw new IllegalArgumentException("Listener for group: " + var1 + " is already registered on another node!");
                  }
               }
            } finally {
               this.lock.writeLock().unlock();
            }
         }
      } catch (InterruptedException var11) {
         LOGGER.error("", var11);
      }
   }

   public void removeMessageListener(String var1) {
      try {
         if (this.lock.writeLock().tryLock(100L, TimeUnit.MILLISECONDS)) {
            try {
               this.listeners.remove(var1);
               if (this.sender != null) {
                  HashMap var2 = new HashMap();
                  var2.put(MessageProperties.jobGroupId, var1);

                  try {
                     this.sender.sendSimpleMessage(MessageKind.unregisterListener, null, var2);
                  } catch (JMSException var8) {
                     LOGGER.error("", var8);
                     throw new IllegalArgumentException("Checking unique of listener failed!");
                  }
               }
            } finally {
               this.lock.writeLock().unlock();
            }
         }
      } catch (InterruptedException var10) {
         LOGGER.error("Interrupted wait for lock", var10);
      }
   }

   public boolean isRegisteredMessageListener(String var1) {
      this.lock.readLock().lock();

      try {
         return this.listeners.containsKey(var1);
      } finally {
         this.lock.readLock().unlock();
      }
   }

   public boolean sendToListener(String var1, GridMessage var2) {
      boolean var3 = var2.getMessageID() != 6;

      try {
         boolean var4;
         if (var3) {
            this.lock.writeLock().lock();
            var4 = true;
         } else {
            var4 = this.lock.writeLock().tryLock(100L, TimeUnit.MILLISECONDS);
         }

         if (var4) {
            IGridMessageListener var5 = null;

            try {
               var5 = this.listeners.get(var1);
            } finally {
               this.lock.writeLock().unlock();
            }

            if (var5 != null) {
               try {
                  var5.messageReceived(var2);
               } catch (Exception var10) {
                  LOGGER.error("Error while handling finish message", var10);
               }
            }

            return var5 != null;
         }
      } catch (InterruptedException var12) {
         LOGGER.error("", var12);
      }

      return false;
   }

   @Override
   public void close() throws Exception {
      if (this.sender != null) {
         this.sender.close();
      }

      if (this.reciever != null) {
         this.reciever.close();
      }

      this.lock.writeLock().lock();

      try {
         this.listeners.clear();
      } finally {
         this.lock.writeLock().unlock();
      }
   }
}
