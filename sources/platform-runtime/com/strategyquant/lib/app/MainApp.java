/*
 * Decompiled with CFR 0.152.
 */
package com.strategyquant.lib.app;

import com.strategyquant.lib.L;
import com.strategyquant.lib.L88OaFjjon.V571hfnsHw;
import com.strategyquant.lib.app.AppSettings;
import com.strategyquant.lib.app.IMainApp;
import com.strategyquant.lib.app.LicenseInfo;
import com.strategyquant.lib.app.SQSplashScreen;
import com.strategyquant.lib.app.impl.IConsoleImpl;
import com.strategyquant.lib.app.webserver.MainAppWebSocket;
import com.strategyquant.lib.snippets.SnippetsCompilerUtils;
import com.strategyquant.lib.snippets.compile.SQStructure;
import com.strategyquant.security.IDataProtector;
import java.awt.Color;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.TimeZone;
import java.util.concurrent.locks.StampedLock;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class MainApp {
    public static boolean AppLoaded = false;
    public static long TimeAppStarted = System.currentTimeMillis();
    public static final Logger Log = LoggerFactory.getLogger(MainApp.class);
    public static final Color COLOR_BLUISH = new Color(218, 234, 248);
    private static final String DESKTOP_INSTANCE = "DesktopInstance";
    private static HashMap<String, IMainApp> instancesMap = new HashMap();
    private static IMainApp desktopInstance = null;
    private static AppSettings settings = new AppSettings();
    public static URLClassLoader snippetsClassLoader = null;
    private static boolean requestsEnabled = false;
    private static boolean multiInstance = false;
    private static boolean exiting = false;
    private static StampedLock lock = new StampedLock();
    public static String OnlyInProVersionTitle = L.t("Only in Pro version", new Object[0]);
    public static boolean CLIWebServeActive = false;
    public static boolean CLIInteractiveMode = false;
    public static long TimezoneDiff = 0L;
    public static IConsoleImpl cli = null;
    public static MainAppWebSocket electronWS = null;
    public static LicenseInfo license = new LicenseInfo();
    private static boolean brEdition = false;
    private static File brEditionFile = null;

    public static void addInstance(IMainApp iMainApp) {
        desktopInstance = iMainApp;
    }

    public static void addInstance(String string, IMainApp iMainApp) {
        if (string == DESKTOP_INSTANCE) {
            desktopInstance = iMainApp;
        } else {
            instancesMap.put(string, iMainApp);
        }
    }

    private static IMainApp getInstance() {
        return desktopInstance;
    }

    private static IMainApp getInstance(String string) {
        return instancesMap.get(string);
    }

    public static void start() {
        TimezoneDiff = Calendar.getInstance().get(15) + Calendar.getInstance().get(16);
        MainApp.getInstance().startApp();
    }

    public static String getDataPath() {
        if (MainApp.getInstance() == null) {
            String string = System.getProperty("user.dir");
            if (!string.endsWith("/") && !string.endsWith("\\")) {
                string = string + "/";
            }
            return string;
        }
        return MainApp.getInstance().getDataPath();
    }

    public static String getAppVersion() {
        return MainApp.getInstance().getAppVersion();
    }

    public static String printAppVersion(boolean bl) {
        return MainApp.printAppVersion(null, bl);
    }

    public static String printAppVersion(String string, boolean bl) {
        String string2 = MainApp.getInstance().getAppVersion();
        if (!bl) {
            try {
                String[] stringArray = string2.split("\\.");
                string2 = stringArray[0].trim();
            }
            catch (Exception exception) {
                // empty catch block
            }
        }
        if (MainApp.checkProduct("QDM")) {
            return string == null ? "Build " + string2 : string + " Build " + string2;
        }
        return string == null ? "X Build " + string2 : "X " + string + " Build " + string2;
    }

    public static String printFullAppVersion() {
        String string = MainApp.printAppVersion(null, true);
        if (MainApp.checkProduct("BACKTESTNODE")) {
            return "AW" + string;
        }
        if (MainApp.checkProduct("SQUANT")) {
            return "SQ" + string;
        }
        return MainApp.getProduct() + string;
    }

    public static String getProduct() {
        return MainApp.getInstance().getProduct();
    }

    public static String getProductName() {
        return MainApp.getInstance().getProductName();
    }

    public static String getProductName(boolean bl) {
        return MainApp.getInstance().getProductName().replaceAll("\\s", "");
    }

    public static boolean isRelease() {
        if (MainApp.getInstance() == null) {
            return true;
        }
        return MainApp.getInstance().isRelease();
    }

    public static String getMode() {
        return MainApp.getInstance().getMode();
    }

    public static boolean is32BitVersion() {
        File file = new File("");
        File file2 = new File(file.getAbsolutePath() + "/j32");
        return file2.exists();
    }

    public static V571hfnsHw v571hfnsHw() {
        return MainApp.getInstance().v571hfnsHw();
    }

    public static void rsq3UErJhC(String string) throws Exception {
        MainApp.getInstance().rsq3UErJhC(string);
    }

    public static void clearLicenseInfo() {
        MainApp.getInstance().eRdAkc3Yu4();
    }

    public static boolean runInConsoleWithoutActiveWebserver() {
        if (MainApp.checkProduct("TESTAPP")) {
            return true;
        }
        return MainApp.getInstance().isRunInConsole() && !CLIWebServeActive;
    }

    public static boolean runInConsoleInInteractiveMode() {
        return MainApp.getInstance().isRunInConsole() && CLIInteractiveMode;
    }

    public static boolean runInConsole() {
        return MainApp.getInstance().isRunInConsole();
    }

    public static void drawSplashProgress(String string) {
        SQSplashScreen sQSplashScreen = MainApp.getSplashScreen();
        if (sQSplashScreen != null) {
            sQSplashScreen.drawSplashProgress(string);
        }
    }

    public static SQSplashScreen getSplashScreen() {
        IMainApp iMainApp = MainApp.getInstance();
        return iMainApp != null ? iMainApp._getSplashScreen() : null;
    }

    public static TimeZone getLocalTimeZone() {
        return MainApp.getInstance().getLocalTimeZone();
    }

    public static void beforeExit() {
        exiting = true;
        MainApp.getInstance().beforeExit();
    }

    public static void exitApp() {
        MainApp.exitApp(true);
    }

    public static void exitApp(boolean bl) {
        MainApp.getInstance().exitApp(bl);
    }

    public static void exitJVM(String string) {
        MainApp.exitJVM(string, 0);
    }

    public static void exitJVM(String string, int n) {
        Log.info("Exit app - " + string);
        if (electronWS != null) {
            electronWS.sendMessage("exit", new JSONObject());
        }
        System.exit(n);
    }

    public static AppSettings settings() {
        return settings;
    }

    public static synchronized URLClassLoader getSnippetsClassLoader() throws MalformedURLException {
        if (snippetsClassLoader == null) {
            ArrayList<File> arrayList = new ArrayList<File>();
            arrayList.add(new File(SQStructure.SNIPPETS_JAR_PATH));
            SnippetsCompilerUtils.loadJarFileDependencies(SQStructure.USER_LIBS, arrayList);
            URL[] uRLArray = new URL[arrayList.size()];
            for (int i = 0; i < arrayList.size(); ++i) {
                uRLArray[i] = arrayList.get(i).toURI().toURL();
            }
            snippetsClassLoader = new URLClassLoader(uRLArray, MainApp.class.getClassLoader());
        }
        return snippetsClassLoader;
    }

    public static void enableRequests() {
        long l = lock.writeLock();
        requestsEnabled = true;
        lock.unlock(l);
    }

    public static boolean requestsEnabled() {
        long l = lock.readLock();
        try {
            boolean bl = requestsEnabled;
            return bl;
        }
        finally {
            lock.unlock(l);
        }
    }

    public static void setMultiInstance(boolean bl) {
        multiInstance = bl;
    }

    public static boolean isMultiInstance() {
        return multiInstance;
    }

    public static boolean isExiting() {
        return exiting;
    }

    public static String getFrameTitle() {
        return MainApp.getInstance().getFrameTitle();
    }

    public static boolean checkProduct(String string) {
        return MainApp.getProduct().equals(string);
    }

    public static boolean isFirstRun() {
        return MainApp.getInstance().isFirstRun();
    }

    public static boolean initWebServer() {
        return MainApp.getInstance().initWebServer();
    }

    public static void reloadApp() {
        MainApp.getInstance().reloadApp();
    }

    public static boolean isBrazilianEdition() {
        if (brEditionFile == null) {
            brEditionFile = new File(MainApp.getDataPath() + "/internal/web/SQUANT/br.edition");
            brEdition = brEditionFile.exists();
        }
        return brEdition;
    }

    public static IDataProtector getDataProtector() {
        return MainApp.getInstance().getDataProtector();
    }

    public static final boolean isBacktestNode() {
        return "BACKTESTNODE".equals(MainApp.getProduct());
    }

    public static void showErrorDialog(String string, String string2) {
        Log.error(string + " - " + string2);
        if (electronWS == null) {
            return;
        }
        electronWS.showErrorDialog(string, string2);
    }

    public static void showErrorDialog(String string) {
        MainApp.showErrorDialog(L.t("Error", new Object[0]), string);
    }

    public static void showInfoDialog(String string, String string2) {
        Log.error(string + " - " + string2);
        if (electronWS == null) {
            return;
        }
        electronWS.showInfoDialog(string, string2);
    }

    public static void showWarningDialog(String string) {
        MainApp.showWarningDialog(L.t("Warning", new Object[0]), string);
    }

    public static void showWarningDialog(String string, String string2) {
        Log.error(string + " - " + string2);
        if (electronWS == null) {
            return;
        }
        electronWS.showWarningDialog(string, string2);
    }

    public static void showInfoDialog(String string) {
        MainApp.showInfoDialog(L.t("Info", new Object[0]), string);
    }

    public static int awaitUserConfirmation(String string, String string2) {
        if (electronWS == null) {
            return 0;
        }
        return electronWS.awaitConfirmation(string, string2);
    }

    public static void sendMessage(String string, JSONObject jSONObject) {
        if (electronWS == null) {
            return;
        }
        electronWS.sendMessage(string, jSONObject);
    }

    public static void sendMessage(String string, String string2) {
        if (electronWS == null) {
            return;
        }
        electronWS.sendMessage(string, string2);
    }
}

