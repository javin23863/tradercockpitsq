package com.strategyquant.tradinglib.project.websocket;

import com.strategyquant.tradinglib.task.settings.buildmode.JSONAble;

public class DataUpdateTypes extends JSONAble {
   public static final String ADD = "add";
   public static final String UPDATE = "update";
   public static final String RENAME = "rename";
   public static final String REMOVE = "remove";
   public static final String CLEAR = "clear";
   public static final String DOWNLOAD = "download";
   public static final String DOWNLOAD_SINGLE = "download_single";
   public static final String IMPORT = "import";
   public static final String LIST = "list";
   public static final String SHOW = "show";
}
