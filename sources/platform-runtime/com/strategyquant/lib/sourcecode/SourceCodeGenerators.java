/*
 * Decompiled with CFR 0.152.
 */
package com.strategyquant.lib.sourcecode;

import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.app.MainApp;
import com.strategyquant.lib.snippets.compile.SQStructure;
import com.strategyquant.lib.sourcecode.SourceCodeGenerator;
import freemarker.cache.FileTemplateLoader;
import freemarker.cache.MultiTemplateLoader;
import freemarker.cache.TemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.ObjectWrapper;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SourceCodeGenerators {
    private static final Logger Log = LoggerFactory.getLogger(SourceCodeGenerators.class);
    public final List<SourceCodeGenerator> generators = new ArrayList<SourceCodeGenerator>();
    private static SourceCodeGenerators instance;
    private final Configuration cfgFreemaker;
    private final File fBuiltinCode = new File(SQStructure.getIndyCodeBuiltinDirPath());

    public static synchronized SourceCodeGenerators getInstance() {
        if (instance == null) {
            instance = new SourceCodeGenerators();
        }
        return instance;
    }

    private SourceCodeGenerators() {
        Object object;
        File file = new File(SQStructure.getIndyCodeUserDirPath());
        java.util.logging.Logger.getLogger("freemarker").setLevel(Level.OFF);
        this.cfgFreemaker = new Configuration(Configuration.VERSION_2_3_0);
        this.cfgFreemaker.setDefaultEncoding("UTF-8");
        try {
            SQUtils.ensureDirExists(file.getAbsolutePath());
            FileTemplateLoader fileTemplateLoader = new FileTemplateLoader(this.fBuiltinCode, true);
            FileTemplateLoader object2 = new FileTemplateLoader(file, true);
            object = new TemplateLoader[]{fileTemplateLoader, object2};
            MultiTemplateLoader multiTemplateLoader = new MultiTemplateLoader(object);
            this.cfgFreemaker.setTemplateLoader((TemplateLoader)multiTemplateLoader);
        }
        catch (IOException iOException) {
            Log.error("Exc.", (Throwable)iOException);
        }
        this.cfgFreemaker.setObjectWrapper((ObjectWrapper)new DefaultObjectWrapper());
        this.loadAvailableGenerators();
        for (SourceCodeGenerator sourceCodeGenerator : this.getAvailableGenerators()) {
            object = sourceCodeGenerator.codeDir.getName();
            SQUtils.ensureDirExists(new File(new File(file, (String)object), "blocks").getAbsolutePath());
        }
    }

    public List<SourceCodeGenerator> getAvailableGenerators() {
        return this.generators;
    }

    private void loadAvailableGenerators() {
        int n;
        File[] fileArray = this.fBuiltinCode.listFiles();
        if (fileArray != null) {
            for (File iterator : fileArray) {
                try {
                    if (!iterator.isDirectory() || iterator.getName().contains("global")) continue;
                    SourceCodeGenerator sourceCodeGenerator = new SourceCodeGenerator(iterator, this.cfgFreemaker);
                    this.generators.add(sourceCodeGenerator);
                    if (!MainApp.isBrazilianEdition() || !sourceCodeGenerator.getName().contains("MetaTrader5")) continue;
                    SourceCodeGenerator sourceCodeGenerator2 = new SourceCodeGenerator(iterator, this.cfgFreemaker);
                    String string = sourceCodeGenerator2.getName().replace("MetaTrader5", "MetaTrader5 (Brazil)");
                    sourceCodeGenerator2.setName(string);
                    sourceCodeGenerator2.setBrazilianVersion(true);
                    this.generators.add(sourceCodeGenerator2);
                }
                catch (Exception exception) {
                    Log.error(String.format("Error while loading '%s' generator.", iterator.getName()));
                }
            }
        }
        HashMap hashMap = new HashMap();
        for (SourceCodeGenerator sourceCodeGenerator : this.generators) {
            String string = sourceCodeGenerator.getName();
            n = hashMap.getOrDefault(string, 0);
            hashMap.put(string, n + 1);
        }
        for (Map.Entry entry : hashMap.entrySet()) {
            n = (Integer)entry.getValue();
            if (n <= 1) continue;
            this.addDirectoryToName((String)entry.getKey());
        }
        for (SourceCodeGenerator sourceCodeGenerator : this.generators) {
            Log.debug("{} - {} - {} - {} - {}", new Object[]{sourceCodeGenerator.getDirName(), sourceCodeGenerator.name, sourceCodeGenerator.extension[0], sourceCodeGenerator.extension[1], sourceCodeGenerator.description});
        }
    }

    private void addDirectoryToName(String string) {
        for (SourceCodeGenerator sourceCodeGenerator : this.generators) {
            if (!sourceCodeGenerator.getName().equalsIgnoreCase(string)) continue;
            sourceCodeGenerator.addDirectoryToName();
        }
    }

    private void registerSCType(SourceCodeGenerator sourceCodeGenerator) {
        this.generators.add(sourceCodeGenerator);
    }

    public SourceCodeGenerator getGeneratorFromName(String string) {
        for (SourceCodeGenerator sourceCodeGenerator : this.generators) {
            if (!sourceCodeGenerator.getName().equalsIgnoreCase(string)) continue;
            return sourceCodeGenerator;
        }
        return null;
    }

    public SourceCodeGenerator getPseudoSourceGenerator() {
        for (SourceCodeGenerator sourceCodeGenerator : this.generators) {
            if (!sourceCodeGenerator.getDirName().contains("PseudoCode")) continue;
            return sourceCodeGenerator;
        }
        return null;
    }

    public void refresh() {
        this.generators.clear();
        this.loadAvailableGenerators();
    }

    public int getPseudoCodeIndex() {
        for (int i = 0; i < this.generators.size(); ++i) {
            if (!this.generators.get(i).getName().contains("Pseudo")) continue;
            return i;
        }
        return 0;
    }
}

