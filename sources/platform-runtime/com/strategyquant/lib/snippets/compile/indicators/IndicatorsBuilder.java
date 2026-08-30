/*
 * Decompiled with CFR 0.152.
 */
package com.strategyquant.lib.snippets.compile.indicators;

import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.snippets.SnippetsUtils;
import com.strategyquant.lib.snippets.compile.SQStructure;
import com.strategyquant.lib.snippets.compile.indicators.IndicatorBase;
import com.strategyquant.lib.snippets.compile.indicators.IndicatorParam;
import com.strategyquant.lib.snippets.compile.indicators.IndicatorParser;
import com.strategyquant.lib.utils.FileLoader;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IndicatorsBuilder {
    public static final Logger Log = LoggerFactory.getLogger(IndicatorsBuilder.class);
    private final String fileName;
    private final List<String> skipFiles = new ArrayList<String>();
    private final List<String> compiledIndicators = new ArrayList<String>();

    public IndicatorsBuilder() {
        this.fileName = "Indicators.java";
        this.skipFiles.add("Indicator.java");
        this.skipFiles.add("IndicatorAboveMA.java");
        this.skipFiles.add("IndicatorBelowMA.java");
        this.skipFiles.add("IndicatorCrossesAboveMA.java");
        this.skipFiles.add("IndicatorCrossesBelowMA.java");
        this.skipFiles.add("IndicatorMAComparisonBlockAbstract.java");
        this.skipFiles.add(this.fileName);
    }

    public synchronized void run() throws Exception {
        this.run(null);
    }

    public synchronized void run(File file) throws Exception {
        Object object;
        ArrayList arrayList2 = new ArrayList();
        String[] stringArray2 = SQStructure.getSnippetsSourceDirsWithSuffix("SQ/Blocks");
        for (String stringArray3 : stringArray2) {
            object = new ArrayList();
            for (String string : SQUtils.listSubdirectories(stringArray3)) {
                ((ArrayList)object).add(stringArray3 + string + "/");
            }
            Collections.sort(arrayList2);
            arrayList2.addAll(object);
        }
        if (file != null) {
            this.listCompiledIndicators();
        }
        TreeMap treeMap = new TreeMap();
        ArrayList<String> arrayList3 = new ArrayList<String>();
        for (String string : arrayList2) {
            this.parseFilesInDirectory(treeMap, arrayList3, string, file);
        }
        Object object2 = "package SQ.Internal;\n\n[imports]\n/**\n * this is a cache class that caches all indicators used in a trading setup\n * @author Mark Fric\n */\n public class Indicators extends IndicatorsObj {\n     private boolean hasZeroShift;\n\n     public void setHasZeroShift(boolean hasZeroShift) {\n         this.hasZeroShift = hasZeroShift;\n     }\n\n\t private StrategyBase Strategy;\n\t public Indicators(StrategyBase strategy) {\n\t\t this.Strategy = strategy;\n\t }\n\n[indicators]}";
        arrayList3 = new ArrayList(new HashSet(arrayList3));
        Collections.sort(arrayList3);
        String[] stringArray = new String[]{"com.strategyquant.tradinglib.indicator.IndicatorsObj", "com.strategyquant.tradinglib.indicator.IIndicatorsHolder", "com.strategyquant.tradinglib.indicator.IndicatorsCache"};
        for (String string : stringArray) {
            arrayList3.remove(string);
            arrayList3.add(0, string);
        }
        object = new StringBuilder();
        for (String string : arrayList3) {
            ((StringBuilder)object).append("import ").append(string).append(";\n");
        }
        StringBuilder stringBuilder = new StringBuilder();
        treeMap.forEach((string2, arrayList) -> arrayList.forEach(string -> stringBuilder.append((String)string).append("\n\n")));
        object2 = ((String)object2).replace("[imports]", ((StringBuilder)object).toString());
        object2 = ((String)object2).replace("[indicators]", stringBuilder.toString());
        SQUtils.stringToFile(SQStructure.SNIPPETS_DIR_PATH + "Internal/" + this.fileName, (String)object2);
    }

    private void listCompiledIndicators() {
        try {
            this.compiledIndicators.clear();
            try (ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(SQStructure.SNIPPETS_JAR_PATH));){
                ZipEntry zipEntry;
                while ((zipEntry = zipInputStream.getNextEntry()) != null) {
                    String string;
                    if (zipEntry.isDirectory() || !(string = SQUtils.stripExtension(zipEntry.getName())).startsWith("SQ/Blocks/Indicators")) continue;
                    String string2 = SQStructure.SNIPPETS_DIR_PATH_WITHOUT_PACKAGE + string + ".java";
                    if (new File(string2).exists()) {
                        this.compiledIndicators.add(SnippetsUtils.normalizePath(SQUtils.stripExtension(string2)));
                        continue;
                    }
                    string2 = SQStructure.getSnippetsUserDirPath() + string + ".java";
                    if (!new File(string2).exists()) continue;
                    this.compiledIndicators.add(SnippetsUtils.normalizePath(SQUtils.stripExtension(string2)));
                }
            }
        }
        catch (Exception exception) {
            Log.error("Cannot load list of compiled Indicators. Exc.", (Throwable)exception);
        }
    }

    private void parseFilesInDirectory(Map<String, ArrayList<String>> map, List<String> list, String string, File file) throws Exception {
        FileLoader fileLoader = new FileLoader(string, "java");
        IndicatorParser indicatorParser = new IndicatorParser();
        while (fileLoader.hasNext()) {
            String string22;
            IndicatorBase indicatorBase;
            File file2 = fileLoader.getNext();
            if (this.skipFiles.contains(file2.getName()) || (indicatorBase = indicatorParser.parse(file2)) == null || file != null && !file2.getAbsolutePath().equals(file.getAbsolutePath()) && !this.isIndicatorCompiled(file2)) continue;
            for (String string22 : indicatorBase.getImportsList()) {
                if (list.contains(string22)) continue;
                list.add(string22);
            }
            String string3 = this.prvGenerateBlock(indicatorBase);
            string22 = indicatorBase.getName();
            if (!map.containsKey(string22)) {
                ArrayList<Object> arrayList = new ArrayList<Object>();
                arrayList.add(string3);
                map.put(string22, arrayList);
                continue;
            }
            Log.warn("Duplicated indicator block '{}' detected", (Object)string22);
            map.get(string22).add(string3);
        }
    }

    private String prvGenerateBlock(IndicatorBase indicatorBase) {
        Object object;
        StringBuilder stringBuilder = new StringBuilder("this.Engine+(" + SQUtils.longHashCode(indicatorBase.getName()) + "L)");
        StringBuilder stringBuilder2 = new StringBuilder();
        StringBuilder stringBuilder3 = new StringBuilder();
        StringBuilder stringBuilder4 = new StringBuilder();
        StringBuilder stringBuilder5 = new StringBuilder();
        StringBuilder stringBuilder6 = new StringBuilder();
        for (int i = 0; i < indicatorBase.getParamList().size(); ++i) {
            object = indicatorBase.getParamList().get(i);
            switch (((IndicatorParam)object).getType()) {
                case "int": {
                    stringBuilder4.append(stringBuilder4.toString().equals("") ? "" : ",").append(((IndicatorParam)object).getName());
                    break;
                }
                case "double": {
                    stringBuilder5.append(stringBuilder5.toString().equals("") ? "" : ",").append(((IndicatorParam)object).getName());
                    break;
                }
                case "boolean": {
                    stringBuilder6.append(stringBuilder6.toString().equals("") ? "" : ",").append(((IndicatorParam)object).getName());
                    break;
                }
                case "DataSeries": 
                case "ChartData": {
                    stringBuilder.append("+").append(((IndicatorParam)object).getName()).append(".chartHashCode()");
                    break;
                }
                default: {
                    stringBuilder.append("+").append(((IndicatorParam)object).getName()).append(".hashCode()");
                }
            }
            stringBuilder2.append(((IndicatorParam)object).getType()).append(" ").append(((IndicatorParam)object).getName());
            stringBuilder3.append("\t\t\tindicator.").append(((IndicatorParam)object).getName()).append(" = ").append(((IndicatorParam)object).getName()).append(";\n");
            if (i >= indicatorBase.getParamList().size() - 1) continue;
            stringBuilder2.append(", ");
        }
        if (!stringBuilder4.toString().equals("")) {
            if (stringBuilder4.toString().contains(",")) {
                stringBuilder.append("+SQUtils.intsHash(").append((CharSequence)stringBuilder4).append(")");
            } else {
                stringBuilder.append("+").append((CharSequence)stringBuilder4);
            }
        }
        if (!stringBuilder5.toString().equals("")) {
            if (stringBuilder5.toString().contains(",")) {
                stringBuilder.append("+SQUtils.doublesHash(").append((CharSequence)stringBuilder5).append(")");
            } else {
                stringBuilder.append("+((int) (37*").append((CharSequence)stringBuilder5).append("))");
            }
        }
        if (!stringBuilder6.toString().equals("")) {
            if (stringBuilder6.toString().contains(",")) {
                stringBuilder.append("+SQUtils.booleansHash(").append((CharSequence)stringBuilder6).append(")");
            } else {
                stringBuilder.append("+(").append((CharSequence)stringBuilder6).append("? 1231 : 1237)");
            }
        }
        stringBuilder.append(";");
        String string = "\tpublic [indicator_name] [indicator_name]([indicator_parameters]) throws TradingException {\n\t\tlong key = [indicator_key]\n\t\t[indicator_name] indicator;\n\n\t\tif(!indicatorsCache.containsKey(key)) {\n\t\t\tindicator = new [indicator_name]();\n[indicator_initialization]\n\t\t\tindicator.initialize(this, hasZeroShift);\n\t\t\tindicator.initializeStrategy(Strategy);\n\n\t\t\tindicatorsCache.put(key, indicator);\n\t\t}\n\n\t\tindicator = ([indicator_name]) indicatorsCache.get(key);\n\n\t\tindicator.refreshShift();\n\n\t\treturn indicator;\n\t}";
        string = string.replace("[indicator_name]", indicatorBase.getName());
        string = string.replace("[indicator_key]", stringBuilder.toString());
        string = string.replace("[indicator_parameters]", stringBuilder2.toString());
        string = string.replace("[indicator_initialization]", stringBuilder3.toString());
        object = this.getIndicatorOutputValue(indicatorBase);
        string = string.replace("[indicator_value_0]", (CharSequence)object);
        return string;
    }

    private String getIndicatorOutputValue(IndicatorBase indicatorBase) {
        if (indicatorBase.getOutputsList().isEmpty()) {
            return "";
        }
        return "indicator." + indicatorBase.getOutputsList().get(0) + ".get(0);";
    }

    private boolean isIndicatorCompiled(File file) {
        String string = SnippetsUtils.normalizePath(file.getAbsolutePath());
        string = SQUtils.stripExtension(string);
        return this.compiledIndicators.contains(string);
    }
}

