package com.strategyquant.gridlib.sync.guard;

import java.io.IOException;
import javax.jms.JMSException;

public interface IFolderGuard {
   String getFolder();

   void updateHash() throws IOException, JMSException;

   void close() throws JMSException;
}
