package com.strategyquant.tradinglib.project.websocket.channels;

import com.strategyquant.tradinglib.project.SQProject;

public interface IProjectChannel {
   Object getData(SQProject var1);

   String getChannelName();
}
