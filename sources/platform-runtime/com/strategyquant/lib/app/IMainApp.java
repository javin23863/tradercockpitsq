/*
 * Decompiled with CFR 0.152.
 */
package com.strategyquant.lib.app;

import com.strategyquant.lib.L88OaFjjon.V571hfnsHw;
import com.strategyquant.lib.app.SQSplashScreen;
import com.strategyquant.security.IDataProtector;
import java.util.TimeZone;

public interface IMainApp {
    public boolean isRelease();

    public String getMode();

    public String getDataPath();

    public void startApp();

    public void exitApp(boolean var1);

    public String getAppVersion();

    public String getProduct();

    public String getProductName();

    public boolean isRunInConsole();

    public V571hfnsHw v571hfnsHw();

    public void rsq3UErJhC(String var1) throws Exception;

    public SQSplashScreen _getSplashScreen();

    public TimeZone getLocalTimeZone();

    public void beforeExit();

    public String getFrameTitle();

    public boolean isFirstRun();

    public boolean initWebServer();

    public void reloadApp();

    public void eRdAkc3Yu4();

    public IDataProtector getDataProtector();

    public void setDataProtector(IDataProtector var1);
}

