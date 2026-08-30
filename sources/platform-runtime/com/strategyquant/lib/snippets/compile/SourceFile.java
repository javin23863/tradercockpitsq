/*
 * Decompiled with CFR 0.152.
 */
package com.strategyquant.lib.snippets.compile;

import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.snippets.compile.SQStructure;
import java.io.File;

public class SourceFile {
    public File codeFile;
    public String compiledClassFilePath;
    public String compiledErrorFilePath;

    public SourceFile(File file) {
        this.codeFile = file;
        String string = SQUtils.trimFilePath(file.getParentFile().getAbsolutePath(), SQStructure.SNIPPETS_DIR_PATH);
        String string2 = SQUtils.stripExtension(file.getName());
        String string3 = SQStructure.COMPILED_DIR_PATH + string + '/';
        this.compiledClassFilePath = string3 + string2 + ".class";
        this.compiledErrorFilePath = string3 + string2 + ".failed";
    }

    public static int getStatus(File file) {
        SourceFile sourceFile = new SourceFile(file);
        if (new File(sourceFile.compiledClassFilePath).exists()) {
            return 1;
        }
        if (new File(sourceFile.compiledErrorFilePath).exists()) {
            return 2;
        }
        return 0;
    }
}

