package com.strategyquant.lib.snippets.compile;

import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.app.MainApp;
import com.strategyquant.lib.snippets.SnippetsCompilerUtils;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Deprecated
public class CompilerBulk {
   public static final Logger Log = LoggerFactory.getLogger("CompilerBulk");
   public static List<String> uncompiledSnippets = new ArrayList<>();
   private final Compiler compiler;
   private CompilationResult compilationResult;
   public String sourcePath = null;
   private boolean compilationErrorsExistInOtherFile = false;
   public int compiledCount = 0;
   public int errorsCount = 0;
   private ICompilationListener listener = null;
   private boolean stop = false;

   public CompilerBulk() throws Exception {
      this.compiler = new Compiler(SnippetsCompilerUtils.loadDependencies());
      SnippetsDb.init(SQStructure.COMPILED_DIR_PATH_WITHOUT_PACKAGE);
   }

   public CompilerBulk(ICompilationListener var1) throws Exception {
      this();
      this.listener = var1;
   }

   public CompilationResult compile() {
      return this.compile(false);
   }

   public CompilationResult compile(String var1) {
      this.sourcePath = var1;
      return this.compile(false);
   }

   public CompilationResult compile(boolean var1) {
      long var2 = System.currentTimeMillis();
      CompilationResult var4 = this.prvCompile(var1);
      Log.debug(String.format("Total compilation time: %d ms", System.currentTimeMillis() - var2));
      Log.debug(String.format("Total compiled Snippets: %d", this.compiledCount));
      return var4;
   }

   public CompilationResult prvCompile(boolean var1) {
      try {
         this.stop = false;
         this.compiledCount = 0;
         this.errorsCount = 0;
         this.compilationResult = new CompilationResult();
         ArrayList<File> var2 = new ArrayList<>();
         this.searchFilesForCompilation(new File(SQStructure.SNIPPETS_DIR_PATH), var1, var2);
         if (this.listener != null) {
            this.listener.numberOfFilesToCompile(var2.size());
         }

         for (File var4 : var2) {
            if (this.stop) {
               break;
            }

            this.compileFile(var4);
         }

         if (var1) {
            this.compilationResult = new CompilationResult();
            this.compilationResult.logTabTitle = "Log - Recompile All";
         }

         String var6 = this.compiledCount == 1 ? " file." : " files.";
         var6 = "Compiled " + this.compiledCount + var6;
         this.compilationResult.addCompilationMessage(0, var6);
         String var9 = "";
         if (this.errorsCount > 0) {
            var9 = "Some files were not compiled because of errors: " + this.errorsCount;
            if (this.compiledCount > 1) {
               this.compilationResult.addCompilationMessage(20, var9);
            }
         }

         var6 = var6 + " " + var9;
         if (this.compilationErrorsExistInOtherFile) {
            this.compilationResult.addCompilationMessage(0, "There were compilation errors in some other files - check their status in Navigator.");
         }

         if (this.stop) {
            this.compilationResult.addCompilationMessage(0, "Compilation was interrupted by the user.");
         }
      } catch (Exception var5) {
         Log.error("Exc.", var5);
      }

      return this.compilationResult;
   }

   private void compileFile(File var1) {
      this.compiledCount++;
      String var3 = SQUtils.trimFilePath(var1.getAbsolutePath(), SQStructure.SNIPPETS_DIR_PATH);
      Log.debug(String.format("Compiling '%s'", var3));
      MainApp.drawSplashProgress(String.format("Compiling '%s'", var3));
      if (this.listener != null) {
         this.listener.compilationProgress(this.compiledCount, "Compiling '" + var3 + "'");
      }

      SourceFile var4 = new SourceFile(var1);
      CompilationResult var2 = this.compiler.compile(var4);
      if (var2.success) {
         File var5 = new File(var2.sourceFile.compiledErrorFilePath);
         if (var5.exists()) {
            var5.delete();
         }
      } else {
         this.errorsCount++;
         Log.info(String.format(" ! Compile of '%s' failed: %s", var3, var2.getAsString()));
         this.generateErrorFile(var2);
      }

      if (this.sourcePath != null && var1.getAbsolutePath().equalsIgnoreCase(this.sourcePath)) {
         this.compilationResult = var2;
      } else if (!var2.success) {
         this.compilationErrorsExistInOtherFile = true;
      }
   }

   private boolean checkFileChanged(File var1) {
      String var2 = SQUtils.file2md5Hash(var1);
      String var3 = SQUtils.trimFilePath(var1.getAbsolutePath(), SQStructure.SNIPPETS_DIR_PATH);
      String var4 = SnippetsDb.getHash(var3);
      if (var2.equals(var4)) {
         return false;
      }

      SnippetsDb.setHash(var3, var2);
      return true;
   }

   private void generateErrorFile(CompilationResult var1) {
      try {
         File var2 = new File(var1.sourceCodePath);
         if (!var2.exists()) {
            return;
         }

         File var3 = new File(var1.sourceFile.compiledErrorFilePath);
         File var4 = new File(var1.sourceFile.compiledClassFilePath);
         if (var4.exists()) {
            var4.delete();
         }

         var1.writeCompilationInfoToFile(var3);
      } catch (Exception var5) {
         Log.error("Exc.", var5);
      }
   }

   public void findUncompiledSnippetsList() {
      uncompiledSnippets.clear();
      this.prvFindUncompiledSnippetsList(new File(SQStructure.SNIPPETS_DIR_PATH), uncompiledSnippets);
      if (!uncompiledSnippets.isEmpty()) {
         Log.info(String.format("Some Snippets could not be compiled. Uncompiled Snippets count: %d", uncompiledSnippets.size()));
      }
   }

   private void prvFindUncompiledSnippetsList(File var1, List<String> var2) {
      if (var1 != null && var1.exists()) {
         File[] var3 = var1.listFiles();
         if (var3 != null) {
            for (File var7 : var3) {
               if (var7.isDirectory()) {
                  this.prvFindUncompiledSnippetsList(var7, var2);
               } else if (var7.getName().toLowerCase().endsWith(".java")) {
                  int var8 = SourceFile.getStatus(var7);
                  if (var8 != 1) {
                     String var9 = SQUtils.trimFilePath(var7.getAbsolutePath(), SQStructure.SNIPPETS_DIR_PATH);
                     MainApp.drawSplashProgress("Checking file status '" + var9 + "'");
                     var2.add(var9);
                  }
               }
            }
         }
      }
   }

   private void searchFilesForCompilation(File var1, boolean var2, ArrayList<File> var3) {
      if (var1 != null && var1.exists()) {
         File[] var4 = var1.listFiles();
         if (var4 != null) {
            for (File var8 : var4) {
               if (var8.isDirectory()) {
                  this.searchFilesForCompilation(var8, var2, var3);
               } else if (var8.getName().toLowerCase().endsWith(".java")
                  && (
                     var2
                        || this.checkFileChanged(var8)
                        || this.sourcePath != null && var8.getAbsolutePath().equalsIgnoreCase(this.sourcePath)
                        || SourceFile.getStatus(var8) != 1
                  )) {
                  var3.add(var8);
               }
            }
         }
      }
   }

   public void stop() {
      this.stop = true;
   }
}
