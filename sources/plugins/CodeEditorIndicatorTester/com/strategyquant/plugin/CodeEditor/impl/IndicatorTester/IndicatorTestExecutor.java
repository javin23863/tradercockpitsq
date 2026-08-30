package com.strategyquant.plugin.CodeEditor.impl.IndicatorTester;

import com.strategyquant.indicatorTester.IndicatorParameters;
import com.strategyquant.indicatorTester.IndicatorTestException;
import com.strategyquant.indicatorTester.IndicatorTester;
import com.strategyquant.indicatorTester.IndicatorTesterConfig;
import com.strategyquant.indicatorTester.IndicatorsLoader;
import com.strategyquant.lib.XMLUtil;
import com.strategyquant.tradinglib.project.websocket.DataToSend;
import com.strategyquant.tradinglib.project.websocket.SQWebSocketManager;
import com.strategyquant.tradinglib.project.websocket.SynchronizedWebSocketPublisher;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;
import org.jdom2.Element;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IndicatorTestExecutor extends SynchronizedWebSocketPublisher {
   private static final Logger Log = LoggerFactory.getLogger(IndicatorTestExecutor.class);
   private List<Element> testConfigs = new ArrayList<>();
   private IndicatorTester tester;
   private int testIndex = 0;
   private int runningStatus = 0;
   private boolean shouldStop = false;
   private boolean newUpdate = false;
   private int dataSource = 0;
   private DataToSend info = new DataToSend("IndicatorTesterStatus");
   private JSONObject status = new JSONObject();
   private ReentrantLock lock = new ReentrantLock();
   private static IndicatorTestExecutor instance;

   private IndicatorTestExecutor() {
      instance = this;
      this.info.setDataObject(this.status);
      SQWebSocketManager.getInstance().addAppPublisher("SQEDITOR", this);
   }

   public static IndicatorTestExecutor get() {
      return instance == null ? new IndicatorTestExecutor() : instance;
   }

   public void start() throws Exception {
      if (this.tester == null) {
         this.tester = new IndicatorTester();
      }

      if (this.getRunningStatus() == 1) {
         throw new Exception("Tests are already running");
      }

      this.loadTestsConfig();
      (new Thread() {
         @Override
         public void run() {
            IndicatorTestExecutor.this.shouldStop = false;
            IndicatorTestExecutor.this.setTestIndex(0);
            IndicatorTestExecutor.this.setRunningStatus(1);

            while (IndicatorTestExecutor.this.getTestIndex() < IndicatorTestExecutor.this.testConfigs.size() && !IndicatorTestExecutor.this.shouldStop) {
               IndicatorTestExecutor.this.setNewUpdate();
               IndicatorTestExecutor.this.runNextTest();
               IndicatorTestExecutor.this.incrementTestIndex();
            }

            IndicatorTestExecutor.this.setRunningStatus(IndicatorTestExecutor.this.shouldStop ? 4 : 3);
            IndicatorTestExecutor.this.setNewUpdate();
         }
      }).start();
   }

   public void stop() {
      this.shouldStop = true;
   }

   private void loadTestsConfig() throws Exception {
      try {
         Element var1 = IndicatorTesterConfig.getConfig();
         Element var2 = XMLUtil.getChildElem(var1, "Tests");
         this.testConfigs.clear();

         for (Element var4 : var2.getChildren("Test")) {
            if (XMLUtil.elementIs(var4, "use")) {
               this.testConfigs.add(var4);
            }
         }

         String var6 = var1.getChild("Engine").getText();
         String var7 = var1.getChild("BarsToReserve").getText();
         this.tester.init(var6);
         this.tester.setStartCompareIndex(Integer.parseInt(var7));
      } catch (Exception var5) {
         throw new Exception("Invalid tests config. ", var5);
      }
   }

   private void runNextTest() {
      try {
         this.performTest();
         this.testConfigs.get(this.getTestIndex()).setAttribute("result", "Passed");
         this.testConfigs.get(this.getTestIndex()).removeContent();
      } catch (IndicatorTestException var3) {
         String var2 = this.getResultMessage(var3);
         Log.error("Indicator test failed. " + var2, var3);
         this.testConfigs.get(this.getTestIndex()).setAttribute("result", var2);
         this.addErrors(this.testConfigs.get(this.getTestIndex()));
      }

      IndicatorTesterConfig.save(null, null);
   }

   private void addErrors(Element var1) {
      Element var2 = XMLUtil.tryAddElement(var1, "Errors");
      var2.removeContent();
      var2.setAttribute("showGrid", String.valueOf(this.tester.shouldShowGrid()));
      ArrayList var3 = this.tester.getDataErrors();

      for (int var4 = 0; var4 < var3.size(); var4++) {
         Element var5 = new Element("Error");
         var5.setText((String)var3.get(var4));
         var2.addContent(var5);
      }
   }

   private String getResultMessage(IndicatorTestException var1) {
      switch (var1.getCode()) {
         case 0:
            return "Skipped - File not found.";
         case 1:
            return "Skipped - " + this.tester.getErrorMsg();
         case 2:
            return "Skipped - Invalid config.";
         case 3:
         case 4:
            return "Failed - " + this.tester.getErrorMsg();
         case 5:
            return "Skipped - Empty file.";
         default:
            return "";
      }
   }

   private void performTest() throws IndicatorTestException {
      try {
         this.tester.reset();
         Element var1 = this.testConfigs.get(this.getTestIndex());
         String var2 = var1.getAttributeValue("indicator");
         String var3 = var1.getAttributeValue("fileName");
         int var4 = Integer.parseInt(var1.getAttributeValue("decimals"));
         int var5 = Integer.parseInt(var1.getAttributeValue("shift"));
         if (!this.tester.checkFileExists(var3)) {
            var1.setAttribute("exists", "no");
            String var12 = this.tester.getFilePath(var3);
            throw new IndicatorTestException(0, new Exception(var12));
         }

         var1.setAttribute("exists", "yes");
         String var6 = var1.getAttributeValue("parameters");
         String[] var7 = var6.isEmpty() ? null : var6.split(",");
         IndicatorParameters var8 = new IndicatorParameters(this.tester);
         Object[] var9 = var8.convertParams(var2, var7);
         this.tester.runTest(var3, this.dataSource, IndicatorsLoader.correctIndyName(var2), IndicatorsLoader.getBufferName(var2), var9, var4, var5);
      } catch (IndicatorTestException var10) {
         throw var10;
      } catch (Exception var11) {
         throw new IndicatorTestException(2, var11);
      }
   }

   public DataToSend getData() {
      this.lock.lock();

      try {
         if (!this.newUpdate) {
            return null;
         }

         double var1 = (double)this.testIndex / this.testConfigs.size() * 100.0;
         this.status.put("percent", var1);
         this.status.put("status", this.runningStatus);
         this.status.put("xmlConfig", XMLUtil.xmlToString(IndicatorTesterConfig.getConfig()));
         return this.info;
      } finally {
         this.newUpdate = false;
         this.lock.unlock();
      }
   }

   public void resetLastData() {
   }

   private int getTestIndex() {
      this.lock.lock();

      try {
         return this.testIndex;
      } finally {
         this.lock.unlock();
      }
   }

   private void setTestIndex(int var1) {
      this.lock.lock();
      this.testIndex = var1;
      this.lock.unlock();
   }

   private void incrementTestIndex() {
      this.lock.lock();
      this.testIndex++;
      this.lock.unlock();
   }

   public int getRunningStatus() {
      this.lock.lock();

      try {
         return this.runningStatus;
      } finally {
         this.lock.unlock();
      }
   }

   private void setRunningStatus(int var1) {
      this.lock.lock();
      this.runningStatus = var1;
      this.lock.unlock();
   }

   private void setNewUpdate() {
      this.lock.lock();
      this.newUpdate = true;
      this.lock.unlock();
   }
}
