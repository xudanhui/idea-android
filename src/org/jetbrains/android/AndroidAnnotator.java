package org.jetbrains.android;

import com.intellij.ide.util.PackageUtil;
import com.intellij.ide.util.EditSourceUtil;
import com.intellij.lang.annotation.Annotation;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.psi.*;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import com.intellij.util.Icons;
import com.intellij.pom.Navigatable;
import org.jetbrains.android.dom.layout.LayoutStyleableProvider;
import org.jetbrains.android.dom.manifest.Manifest;
import org.jetbrains.android.facet.AndroidFacet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * @author coyote
 */
public class AndroidAnnotator implements Annotator {
    private static String R_CLASS_NAME = "R.java";
    private Project project;
    private Module module;
    private AndroidFacet facet;

    private static class JumpingRenderer extends GutterIconRenderer {
        private final PsiElement element;

        public JumpingRenderer(PsiElement element) {
            this.element = element;
        }

        @NotNull
        @Override
        public Icon getIcon() {
            return Icons.OPEN_EDIT_DIALOG_ICON;
        }

        @Override
        public AnAction getClickAction() {
            return new AnAction() {
                @Override
                public void actionPerformed(AnActionEvent e) {
                    Navigatable navigatable = EditSourceUtil.getDescriptor(element);
                    if (navigatable != null && navigatable.canNavigate()) {
                        navigatable.navigate(true);
                    }
                }
            };
        }
    }

    private boolean isLayoutField(PsiField field) {
        PsiClass layoutClass = field.getContainingClass();
        if (layoutClass != null) {
            PsiClass parentClass = layoutClass.getContainingClass();
            if (parentClass != null) {
                if ("R".equals(parentClass.getName()) && parentClass.getContainingClass() == null) {
                    if ("layout".equals(layoutClass.getName())) return true;
                }
            }
        }
        return false;
    }

    private boolean isRClassFile(PsiFile file) {
        if (file.getName().equals(R_CLASS_NAME) && file instanceof PsiJavaFile) {
            PsiJavaFile javaFile = (PsiJavaFile) file;
            Manifest manifest = facet.getManifest();
            if (manifest == null) return false;

            String manifestPackage = manifest.getPackage().getValue();
            if (javaFile.getPackageName().equals(manifestPackage)) return true;
        }
        return false;
    }

    private boolean isLayoutXmlFile(PsiFile file) {
        if (file instanceof XmlFile) {
            XmlFile xmlFile = (XmlFile) file;
            LayoutStyleableProvider provider = new LayoutStyleableProvider(facet);
            return provider.isMyFile(xmlFile, module);
        }
        return false;
    }

    public void annotate(final PsiElement psiElement, AnnotationHolder holder) {
        project = psiElement.getProject();
        PsiFile file = psiElement.getContainingFile();
        module = ModuleUtil.findModuleForPsiElement(file);
        facet = AndroidFacet.getInstance(module);
        if (facet == null) return;

        if (isRClassFile(file)) {
            if (psiElement instanceof PsiField) {
                PsiField field = (PsiField) psiElement;
                if (isLayoutField(field)) {
                    String layoutFileName = field.getName() + ".xml";
                    PsiFile layoutFile = findLayoutFile(layoutFileName);
                    if (layoutFile != null) {
                        Annotation annotation = holder.createInfoAnnotation(psiElement, "Go to " + layoutFileName);
                        annotation.setGutterIconRenderer(new JumpingRenderer(layoutFile));
                    }
                }
            }
        }
        else if (isLayoutXmlFile(file)) {
            if (!(psiElement instanceof XmlTag)) return;
            XmlTag tag = (XmlTag) psiElement;
            if (tag.getParentTag() == null) {
                String layoutFileName = file.getName();
                String layoutName = FileUtil.getNameWithoutExtension(layoutFileName);
                PsiJavaFile rClassFile = findRClassFile();
                if (rClassFile != null) {
                    PsiField layoutField = findLayoutField(rClassFile, layoutName);
                    if (layoutField != null) {
                        Annotation annotation = holder.createInfoAnnotation(psiElement, "Go to " + R_CLASS_NAME);
                        annotation.setGutterIconRenderer(new JumpingRenderer(layoutField));
                    }
                }
            }
        }
    }

    @Nullable
    private PsiJavaFile findRClassFile() {
        Manifest manifest = facet.getManifest();
        if (manifest == null) return null;
        String packageName = manifest.getPackage().getValue();
        PsiDirectory directory = PackageUtil.findPossiblePackageDirectoryInModule(module, packageName);
        if (directory == null) return null;
        VirtualFile file = directory.getVirtualFile().findChild(R_CLASS_NAME);
        if (file == null) return null;
        PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
        if (psiFile instanceof PsiJavaFile) {
            return (PsiJavaFile) psiFile;
        }
        return null;
    }

    private PsiClass findClass(PsiClass[] classes, @NotNull String name) {
        for (PsiClass c : classes) {
            if (name.equals(c.getName())) {
                return c;
            }
        }
        return null;
    }

    @Nullable
    private PsiField findLayoutField(@NotNull PsiJavaFile rClassFile, @NotNull String layoutName) {
        PsiClass rClass = findClass(rClassFile.getClasses(), "R");
        if (rClass == null) return null;
        PsiClass layoutClass = findClass(rClass.getInnerClasses(), "layout");
        if (layoutClass == null) return null;
        for (PsiField field : layoutClass.getFields()) {
            if (layoutName.equals(field.getName())) {
                PsiModifierList list = field.getModifierList();
                if (list != null) {
                    if (list.hasModifierProperty("public") && list.hasModifierProperty("static")) {
                        return field;
                    }
                }
            }
        }
        return null;
    }

    @Nullable
    private PsiFile findLayoutFile(@NotNull String fileName) {
        VirtualFile layoutDir = facet.getResourceTypeDir("layout", null);
        if (layoutDir == null || !layoutDir.isDirectory()) return null;
        final VirtualFile virtualLayoutFile = layoutDir.findChild(fileName);
        if (virtualLayoutFile == null) return null;
        return ApplicationManager.getApplication().runReadAction(new Computable<PsiFile>() {
            public PsiFile compute() {
                return PsiManager.getInstance(project).findFile(virtualLayoutFile);
            }
        });
    }
}
