/*
 * Decompiled with CFR 0.152.
 */
package com.strategyquant.lib.app.impl;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.Context;
import ch.qos.logback.core.joran.spi.JoranException;
import com.strategyquant.lib.L;
import com.strategyquant.lib.L88OaFjjon.G8SyrBEfO8;
import com.strategyquant.lib.L88OaFjjon.V571hfnsHw;
import com.strategyquant.lib.L88OaFjjon.V7hkcR2dQd;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.app.IMainApp;
import com.strategyquant.lib.app.IMainWindow;
import com.strategyquant.lib.app.ISQStatusBar;
import com.strategyquant.lib.app.MainApp;
import com.strategyquant.lib.app.SQSplashScreen;
import com.strategyquant.lib.app.webserver.MainAppWebServer;
import com.strategyquant.lib.hw.OperatingSystem;
import com.strategyquant.lib.snippets.CustomClassesLoader;
import com.strategyquant.lib.snippets.ProtectedSnippets;
import com.strategyquant.lib.snippets.SnippetsCompiler;
import com.strategyquant.lib.snippets.SnippetsUtils;
import com.strategyquant.lib.snippets.compile.CompilationResult;
import com.strategyquant.lib.snippets.compile.SQStructure;
import com.strategyquant.lib.snippets.compile.indicators.IndicatorsBuilder;
import com.strategyquant.security.IDataProtector;
import com.strategyquant.security.SQDataProtector;
import java.awt.EventQueue;
import java.io.File;
import java.io.IOException;
import java.util.Base64;
import java.util.TimeZone;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class MainAppStandardImpl
implements IMainApp,
V7hkcR2dQd {
    public static final Logger Log = LoggerFactory.getLogger((String)"MainAppStandardImpl");
    protected String appPath = null;
    protected int appRelease = -1;
    private String appMode = null;
    protected TimeZone timeZone = TimeZone.getTimeZone("GMT");
    private String productCode;
    private String productName;
    private String licenserProductCode;
    private String productShortcut;
    private static String logFileName = "log_config_sqtrader.xml";
    protected ISQStatusBar mainStatusBar = null;
    private IMainWindow mainWindow;
    protected static boolean alreadyStarted = false;
    private SQSplashScreen splashScreen;
    protected static String[] appArgs;
    protected static boolean runInConsole;
    private static String DetectFirstRunFile;
    protected static String VersionFile;
    protected int AppVersion = -1;
    protected int AppBuildNumber = -1;
    private V571hfnsHw v571hfnsHw = null;
    private IDataProtector dataProtector;

    public MainAppStandardImpl(String string, String string2, String string3, IMainWindow iMainWindow, String string4) {
        if (alreadyStarted) {
            return;
        }
        this.productCode = string;
        this.mainWindow = iMainWindow;
        this.productName = string2;
        this.licenserProductCode = string3;
        this.productShortcut = string4;
        this.dataProtector = new SQDataProtector();
        try {
            G8SyrBEfO8.getInstance().g5Tq3ZdPHp(string3);
        }
        catch (Exception exception) {
            Log.error("Failed to init app - " + exception.getMessage());
            MainApp.exitJVM("Failed to init app - " + exception.getMessage());
        }
        System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.Jdk14Logger");
        this.initDataPath();
        VersionFile = this.getDataPath() + "internal/" + string + ".dat";
        try {
            Log.debug("Initializing log engine...");
            this.initLogEngine();
        }
        catch (JoranException joranException) {
            joranException.printStackTrace();
            MainApp.showErrorDialog("Cannot initialize log engine!", "Reason: \n" + joranException.getMessage());
            MainApp.exitJVM("Cannot initialize log engine! " + joranException.getMessage());
        }
        Log.debug(String.format("Starting %s...", string2));
        this.checkAnotherInstanceRunning();
        this.createUserFolders();
    }

    @Override
    public void startApp() {
        try {
            Log.debug("Console mode: " + (this.isRunInConsole() ? "YES" : "NO"));
            if (!this.isRunInConsole()) {
                OperatingSystem operatingSystem = new OperatingSystem();
                if (operatingSystem.isUnix()) {
                    this.splashScreen = new SQSplashScreen();
                    MainApp.drawSplashProgress("Starting...");
                } else {
                    EventQueue.invokeLater(() -> {
                        this.splashScreen = new SQSplashScreen();
                        MainApp.drawSplashProgress("Starting...");
                    });
                }
            }
            this.initLangs();
            this.startGUI();
            boolean bl = this.initApp();
            if (!bl) {
                return;
            }
            if (MainApp.checkProduct("SQUANT") && !this.v571hfnsHw().gWfGtoRYJG()) {
                throw new Exception("Missing license.");
            }
            if (this.mainWindow != null) {
                this.mainWindow.initWebServer();
            }
            this.hideSplash();
            this.afterInit();
            if (this.mainWindow != null) {
                this.mainWindow.setTitle();
            }
        }
        catch (Exception exception) {
            this.hideSplash();
            Log.error("Failed to start application. Exc.", (Throwable)exception);
            MainApp.exitJVM("Failed to start application - " + exception.getMessage());
        }
    }

    private void initLangs() {
        try {
            String string = MainApp.getDataPath() + "internal/langs";
            File file = new File(string);
            if (!file.exists()) {
                file.mkdirs();
            }
            L.loadAvailableLangs(new File(string));
            String string2 = MainApp.settings().get("language");
            if (string2 != null) {
                L.loadLangFileToMap(string2);
            }
        }
        catch (Exception exception) {
            Log.error("Error while loading langs.", (Throwable)exception);
        }
    }

    private void hideSplash() {
        if (this.splashScreen != null) {
            this.splashScreen.hide();
        }
    }

    protected void initDataPath() {
        File file = new File("");
        this.appPath = file.getAbsolutePath();
        this.appPath = this.appPath.replaceAll(Pattern.quote("\\"), "/");
        if (!this.isRelease() && this.appPath.contains("projects")) {
            this.appPath = this.appPath + "/../../../work_directory/StrategyQuant";
        }
        this.appPath = this.appPath + "/";
        System.setProperty("user.dir", this.appPath);
        String string = this.appPath + "internal/web/" + this.productCode;
        File file2 = new File(string);
        try {
            String string2;
            String string3 = string2 = file2.getCanonicalPath();
        }
        catch (IOException iOException) {
            iOException.printStackTrace();
        }
        if (!file2.exists()) {
            file2.mkdirs();
        }
    }

    protected void initLogEngine() throws JoranException {
        LoggerContext loggerContext = (LoggerContext)LoggerFactory.getILoggerFactory();
        JoranConfigurator joranConfigurator = new JoranConfigurator();
        joranConfigurator.setContext((Context)loggerContext);
        loggerContext.reset();
        if (MainAppStandardImpl.runInConsole()) {
            joranConfigurator.doConfigure(this.appPath + "internal/web/" + this.productCode + "/log_config_console.xml");
        } else {
            joranConfigurator.doConfigure(this.appPath + "internal/web/" + this.productCode + "/log_config.xml");
        }
    }

    protected void checkWritable() {
        if (!SQUtils.checkWorkDirectoryIsWritable(this.getDataPath())) {
            String string = "";
            OperatingSystem operatingSystem = new OperatingSystem();
            string = operatingSystem.isWindows() ? L.t("Please reinstall it to another directory, for example C:\\Trading.", new Object[0]) : (operatingSystem.isMac() ? L.t("Please reinstall it into Applications folder.", new Object[0]) : L.t("Please reinstall it to another directory, for example /usr/local.", new Object[0]));
            MainApp.showErrorDialog(L.t("Installation Error", new Object[0]), L.t("The program installation directory '%s' is read-only!", this.getDataPath()) + "\n" + L.t("%s will not work correctly, because it wouldn't be able to write changes to settings or database.", this.getProductName()) + "\n\n" + string);
            MainApp.exitJVM(String.format("The program installation directory '%s' is read-only!", this.getDataPath()));
        }
    }

    @Override
    public String getAppVersion() {
        if (this.AppVersion == -1) {
            try {
                this.AppVersion = G8SyrBEfO8.getInstance().getProductVersion();
            }
            catch (Exception exception) {
                this.AppVersion = 0;
                Log.error("Cannot parse Version number. Exc.", (Throwable)exception);
            }
            try {
                String string = SQUtils.fileToString(new File(this.getDataPath() + "internal/web/" + this.productCode + "/build.dat")).trim();
                this.AppBuildNumber = Integer.valueOf(string);
            }
            catch (Exception exception) {
                this.AppBuildNumber = 0;
                Log.debug("Cannot parse Build number. Exc.", (Throwable)exception);
            }
        }
        return this.AppVersion + "." + this.AppBuildNumber;
    }

    @Override
    public String getDataPath() {
        return this.appPath;
    }

    @Override
    public String getProduct() {
        return this.productCode;
    }

    @Override
    public String getMode() {
        if (this.appMode == null) {
            File file;
            File file2 = new File("");
            File file3 = new File(file2.getAbsolutePath() + "/development.txt");
            this.appMode = file3.exists() ? "Development" : ((file = new File(file2.getAbsolutePath() + "/testing.txt")).exists() ? "Testing" : "Release");
        }
        return this.appMode;
    }

    @Override
    public boolean isRelease() {
        if (this.appRelease == -1) {
            this.getMode();
            this.appRelease = this.appMode.equals("Development") ? 0 : 1;
        }
        return this.appRelease == 1;
    }

    protected void checkSnippets() throws Exception {
        Object object;
        File file;
        File file2 = new File(MainApp.getDataPath() + "/user/extend/Code/MetaTrader4/CustomFunctions/CustomFunctions.mq4");
        if (!file2.getParentFile().exists()) {
            file2.getParentFile().mkdirs();
        }
        if (!file2.exists()) {
            file2.createNewFile();
        }
        if (!(file = new File(MainApp.getDataPath() + "/user/extend/Code/MetaTrader5/CustomFunctions/CustomFunctions.mq5")).getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
        if (!file.exists()) {
            file.createNewFile();
        }
        ProtectedSnippets.getInstance();
        SnippetsUtils.removeOldSnippets();
        SnippetsUtils.synchronizeSnippets();
        try {
            Log.debug("Generating Indicators.java");
            IndicatorsBuilder indicatorsBuilder = new IndicatorsBuilder();
            indicatorsBuilder.run();
        }
        catch (Exception exception) {
            Log.error("Generating Indicators.java failed. Exc.", (Throwable)exception);
        }
        boolean bl = false;
        try {
            if (new File(SQStructure.SNIPPETS_JAR_PATH).exists()) {
                object = new CustomClassesLoader("Columns/Databanks");
                ((CustomClassesLoader)object).createInstanceThrow("NetProfit");
            }
        }
        catch (UnsupportedClassVersionError unsupportedClassVersionError) {
            Log.info("Checking snippets - UnsupportedClassVersionError - snippets are compiled with different java version. Deleting Snippets.jar...");
            if (new File(SQStructure.SNIPPETS_HASH_PATH).delete()) {
                Log.info("Snippets.jar has been successfully deleted.");
            }
            bl = true;
        }
        catch (Exception exception) {
        }
        catch (Error error) {
            // empty catch block
        }
        try {
            Log.debug("Compiling Snippets.jar");
            object = SnippetsCompiler.getInstance();
            CompilationResult compilationResult = ((SnippetsCompiler)object).run(false);
            boolean bl2 = compilationResult.isRecompiled();
            if (bl2) {
                MainApp.snippetsClassLoader = null;
            }
            if (bl) {
                Log.info(L.tsq("Different version of Java detected, Snippets were recompiled.") + "\n" + L.tsq("Please restart StrategyQuant for these changes to take effect."));
                if (!runInConsole) {
                    MainApp.showErrorDialog(L.t("Error", new Object[0]), L.t("Different version of Java detected, Snippets were recompiled.", new Object[0]) + "\n" + L.t("Please restart StrategyQuant for these changes to take effect.", new Object[0]) + "\n" + L.t("Clicking on OK will close the program, please start it again.", new Object[0]));
                }
                MainApp.exitJVM("Different version of Java detected, Snippets were recompiled.");
            }
        }
        catch (Exception exception) {
            Log.error("Compiling Snippets.jar failed. Exc.", (Throwable)exception);
        }
    }

    protected void checkUpdates() {
    }

    public void onProgress(int n, long l, long l2) {
        System.out.println("Downloaded " + n + "% - " + l + " from " + l2);
    }

    protected String getProductCode() {
        return this.productCode;
    }

    public void startGUI() throws Exception {
        Log.debug("Starting GUI...");
        if (this.mainWindow == null) {
            Log.debug("No main window");
            return;
        }
        try {
            Log.debug("Initializing main window ...");
            this.mainWindow.initialize();
        }
        catch (Throwable throwable) {
            Log.error("Start GuI error:", throwable);
            this.mainWindow.destroy();
            throw new Exception("Start GUI error:", throwable);
        }
    }

    protected static void setLogFileName(String string) {
        logFileName = string;
    }

    @Override
    public V571hfnsHw v571hfnsHw() {
        if (this.v571hfnsHw == null) {
            this.v571hfnsHw = new V571hfnsHw(G8SyrBEfO8.getInstance().getProduct(), G8SyrBEfO8.getInstance().getLicenseType(), G8SyrBEfO8.getInstance().getLicenseEdition(), G8SyrBEfO8.getInstance().getLicenseOwner(), G8SyrBEfO8.getInstance().getEmail(), G8SyrBEfO8.getInstance().getLicenseCode(), G8SyrBEfO8.getInstance().getValidUntil(), G8SyrBEfO8.getInstance().getValidUntilSec(), G8SyrBEfO8.getInstance().getHardwareID(), G8SyrBEfO8.getInstance().getBroker(), G8SyrBEfO8.getInstance().verified(), G8SyrBEfO8.getInstance().getDataTrialExpires(), G8SyrBEfO8.getInstance().isSpecialTrial());
        }
        return this.v571hfnsHw;
    }

    @Override
    public void eRdAkc3Yu4() {
        this.v571hfnsHw = null;
    }

    @Override
    public SQSplashScreen _getSplashScreen() {
        return this.splashScreen;
    }

    public static boolean runInConsole() {
        return runInConsole;
    }

    public void generateAppVersionFileForUpdater() {
        String string;
        V571hfnsHw v571hfnsHw = this.v571hfnsHw();
        String string2 = string = v571hfnsHw.gWfGtoRYJG() ? Base64.getEncoder().encodeToString(v571hfnsHw.fSECzwVwpK().getBytes()) : "";
        if (this.AppVersion > 0) {
            SQUtils.stringToFile(new File(VersionFile), this.AppVersion + "\n" + string);
        }
    }

    @Override
    public TimeZone getLocalTimeZone() {
        return this.timeZone;
    }

    @Override
    public void beforeExit() {
        File file = new File(DetectFirstRunFile);
        if (file.exists()) {
            file.delete();
        }
    }

    public void exitApp() {
        this.beforeExit();
        if (this.mainWindow != null) {
            this.mainWindow.destroy();
        }
    }

    @Override
    public boolean isFirstRun() {
        return new File(DetectFirstRunFile).exists();
    }

    @Override
    public String getProductName() {
        return this.productName;
    }

    @Override
    public boolean initWebServer() {
        return false;
    }

    protected void checkAnotherInstanceRunning() {
        Log.debug("Checking if another instance is running...");
        if (this.anotherInstanceIsAlreadyRunning()) {
            if (MainAppStandardImpl.runInConsole()) {
                Log.info(L.t("It seems another instance of %s is running, %s can run only with one instance at once.", this.productName, this.productShortcut) + "\n" + L.t("If you need to run multiple %s instances you can create multiple installations (different installation folders).", this.productShortcut));
            } else {
                MainApp.showErrorDialog("Warning", L.t("It seems another instance of %s is running, %s can run only with one instance at once.", this.productName, this.productShortcut) + "\n" + L.t("If you need to run multiple %s instances you can create multiple installations (different installation folders).", this.productShortcut));
            }
            MainApp.exitJVM(String.format("Another instance of %s is running.", this.productName));
        }
    }

    private boolean anotherInstanceIsAlreadyRunning() {
        if ("BACKTESTNODE".equals(this.getProductCode())) {
            return false;
        }
        try {
            MainAppWebServer mainAppWebServer = new MainAppWebServer(this.getProductCode());
            mainAppWebServer.createServer();
            return false;
        }
        catch (Error | Exception throwable) {
            Log.error("Preventing multiple instances: " + throwable.getMessage(), throwable);
            return true;
        }
    }

    private void createUserFolders() {
        File file;
        Log.debug("Creating user folders...");
        File file2 = new File(SQStructure.USER_LIBS);
        if (!file2.exists()) {
            file2.mkdirs();
        }
        if (!(file = new File(SQStructure.USER_EXTEND)).exists()) {
            file.mkdirs();
        }
    }

    @Override
    public void reloadApp() {
    }

    @Override
    public void rsq3UErJhC(String string) throws Exception {
        G8SyrBEfO8 g8SyrBEfO8 = G8SyrBEfO8.getInstance();
        g8SyrBEfO8.updateLicense(string);
    }

    protected abstract boolean initApp() throws Exception;

    protected abstract void afterInit() throws Exception;

    @Override
    public IDataProtector getDataProtector() {
        return this.dataProtector;
    }

    @Override
    public void setDataProtector(IDataProtector iDataProtector) {
        this.dataProtector = iDataProtector;
    }

    static {
        runInConsole = false;
        DetectFirstRunFile = MainApp.getDataPath() + "internal/run.dat";
        VersionFile = null;
    }
}

