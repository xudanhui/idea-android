package org.jetbrains.android.fileTypes;

import com.intellij.formatting.FormattingModelBuilder;
import com.intellij.ide.structureView.StructureViewBuilder;
import com.intellij.lang.*;
import com.intellij.lang.annotation.Annotator;
import com.intellij.lang.annotation.ExternalAnnotator;
import com.intellij.lang.findUsages.FindUsagesProvider;
import com.intellij.lang.folding.FoldingBuilder;
import com.intellij.lang.java.JavaLanguage;
import com.intellij.lang.parameterInfo.ParameterInfoHandler;
import com.intellij.lang.refactoring.NamesValidator;
import com.intellij.lang.refactoring.RefactoringSupportProvider;
import com.intellij.lang.surroundWith.SurroundDescriptor;
import com.intellij.openapi.fileTypes.SyntaxHighlighter;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.tree.TokenSet;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Android IDL Language.
 *
 * @author Alexey Efimov
 */
public class AndroidIdlLanguage extends Language {
    @NonNls
    private static final String ID = "AIDL";

    private final JavaLanguage myJavaLanguage;

    public AndroidIdlLanguage() {
        super(ID);
        myJavaLanguage = Language.findInstance(JavaLanguage.class);
    }

    @Override
    @Nullable
    public ParserDefinition getParserDefinition() {
        return myJavaLanguage.getParserDefinition();
    }

    @Override
    @Nullable
    public FormattingModelBuilder getFormattingModelBuilder() {
        return myJavaLanguage.getFormattingModelBuilder();
    }

    @Override
    @Nullable
    public Commenter getCommenter() {
        return myJavaLanguage.getCommenter();
    }

    @Override
    @NotNull
    public TokenSet getReadableTextContainerElements() {
        return myJavaLanguage.getReadableTextContainerElements();
    }

    @Override
    @NotNull
    public FindUsagesProvider getFindUsagesProvider() {
        return myJavaLanguage.getFindUsagesProvider();
    }

    @Override
    @Nullable
    public StructureViewBuilder getStructureViewBuilder(PsiFile psiFile) {
        return myJavaLanguage.getStructureViewBuilder(psiFile);
    }

    @Override
    @NotNull
    public RefactoringSupportProvider getRefactoringSupportProvider() {
        return myJavaLanguage.getRefactoringSupportProvider();
    }

    @Override
    @NotNull
    public SurroundDescriptor[] getSurroundDescriptors() {
        return myJavaLanguage.getSurroundDescriptors();
    }

    @Override
    @Nullable
    public ImportOptimizer getImportOptimizer() {
        return myJavaLanguage.getImportOptimizer();
    }

    @Override
    @NotNull
    public SyntaxHighlighter getSyntaxHighlighter(Project project, VirtualFile virtualFile) {
        return myJavaLanguage.getSyntaxHighlighter(project, virtualFile);
    }

    @Override
    @Nullable
    public ParameterInfoHandler[] getParameterInfoHandlers() {
        return myJavaLanguage.getParameterInfoHandlers();
    }

    @Override
    public FoldingBuilder getFoldingBuilder() {
        return myJavaLanguage.getFoldingBuilder();
    }

    @Override
    public PairedBraceMatcher getPairedBraceMatcher() {
        return myJavaLanguage.getPairedBraceMatcher();
    }

    @Override
    public Annotator getAnnotator() {
        return myJavaLanguage.getAnnotator();
    }

    @Override
    public ExternalAnnotator getExternalAnnotator() {
        return myJavaLanguage.getExternalAnnotator();
    }

    @Override
    @NotNull
    public NamesValidator getNamesValidator() {
        return myJavaLanguage.getNamesValidator();
    }

    @Override
    public FileViewProvider createViewProvider(VirtualFile file, PsiManager manager, boolean physical) {
        return myJavaLanguage.createViewProvider(file, manager, physical);
    }

    @Override
    public LanguageDialect[] getAvailableLanguageDialects() {
        return myJavaLanguage.getAvailableLanguageDialects();
    }

    @Override
    public LanguageCodeInsightActionHandler getGotoSuperHandler() {
        return myJavaLanguage.getGotoSuperHandler();
    }

    @Override
    public LanguageCodeInsightActionHandler getImplementMethodsHandler() {
        return myJavaLanguage.getImplementMethodsHandler();
    }

    @Override
    public LanguageCodeInsightActionHandler getOverrideMethodsHandler() {
        return myJavaLanguage.getOverrideMethodsHandler();
    }
}
