package org.jetbrains.android.dom.layout;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.search.ProjectScope;
import com.intellij.psi.search.searches.ClassInheritorsSearch;
import com.intellij.psi.xml.XmlFile;
import com.intellij.util.Processor;
import org.jetbrains.android.dom.StyleableProvider;
import org.jetbrains.android.dom.attrs.AttributeDefinitions;
import org.jetbrains.android.dom.attrs.StyleableDefinition;
import org.jetbrains.android.facet.AndroidFacet;
import org.jetbrains.android.util.Key;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * @author coyote
 */
public class LayoutStyleableProvider extends StyleableProvider {
    public static final Key<LayoutStyleableProvider> KEY = new Key<LayoutStyleableProvider>();

    private Map<String, PsiClass> viewClassMap = null;
    private boolean initialized;

    public LayoutStyleableProvider(AndroidFacet facet) {
        super(facet);
    }

    private void addViewClassToMap(PsiClass viewClass) {
        // it can't invoke getViewClassMap()
        viewClassMap.put(viewClass.getName(), viewClass);
    }

    private synchronized Map<String, PsiClass> getViewClassMap() {
        if (viewClassMap == null) {
            Project project = facet.getModule().getProject();
            JavaPsiFacade facade = JavaPsiFacade.getInstance(project);
            PsiClass viewClass = facade.findClass("android.view.View", ProjectScope.getAllScope(project));

            if (viewClass != null) {
                viewClassMap = new HashMap<String, PsiClass>();
                addViewClassToMap(viewClass);
                ClassInheritorsSearch.search(viewClass).forEach(new Processor<PsiClass>() {
                    public boolean process(PsiClass psiClass) {
                        addViewClassToMap(psiClass);
                        return true;
                    }
                });
            }
        }
        return viewClassMap;
    }

    public Set<String> getViewClassNames() {
        return getViewClassMap().keySet();
    }

    public StyleableDefinition getStyleableByTagName(String tagName) {
        final AttributeDefinitions attrDefs = getAttributeDefinitions();
        if (attrDefs == null) return null;

        // view tag is special case
        if (tagName.equals("view")) tagName = "View";

        StyleableDefinition definition = attrDefs.getStyleableByName(tagName);
        if (definition != null) return definition;
        
        // e.g. TimePicker is not listed in attrs.xml
        return getBaseStyleable(attrDefs, tagName);
    }

    private void linkSuperclasses(AttributeDefinitions definitions) {
        for (String name : definitions.getStyleableNames()) {
            final StyleableDefinition definition = definitions.getStyleableByName(name);
            StyleableDefinition baseStyleable = getBaseStyleable(definitions, name);
            definition.setSuperclass(baseStyleable);
        }
    }

    private boolean isView(StyleableDefinition definition) {
        if (definition.getName().equals("View")) return true;
        StyleableDefinition superClass = definition.getSuperclass();
        return superClass != null && isView(superClass);
    }

    private void addChildren(AttributeDefinitions definitions) {
        StyleableDefinition viewGroupStyleable = definitions.getStyleableByName("ViewGroup");
        for (String name : definitions.getStyleableNames()) {
            StyleableDefinition definition = definitions.getStyleableByName(name);
            if (isView(definition)) {
                definition.addParent(viewGroupStyleable);
                viewGroupStyleable.addChild(definition);
            }
        }
    }

    public PsiClass findSuperclass(String name) {
        PsiClass psiClass = getViewClassMap().get(name);
        if (psiClass == null) return null;
        return psiClass.getSuperClass();
    }

    public StyleableDefinition getBaseStyleable(AttributeDefinitions definitions, String styleableName) {
        PsiClass superClass = findSuperclass(styleableName);
        while (superClass != null) {
            StyleableDefinition definition = definitions.getStyleableByName(superClass.getName());
            if (definition != null) return definition;
            superClass = superClass.getSuperClass();
        }
        return null;
    }

    private void linkLayoutStyleables(AttributeDefinitions definitions) {
        List<String> names = new ArrayList<String>(definitions.getStyleableNames());
        for (String name : names) {
            StyleableDefinition definition = definitions.getStyleableByName(name);
            if (name.endsWith("_Layout") || name.endsWith("_MarginLayout")) {
                String s = name.substring(0, name.indexOf('_'));
                StyleableDefinition layoutOwner = definitions.removeStyleableByName(s);
                if (layoutOwner != null) {
                    layoutOwner.addLayoutStyleable(definition);
                }
            }
        }
    }

    @Nullable
    public synchronized AttributeDefinitions getAttributeDefinitions() {
        AttributeDefinitions definitions = super.getAttributeDefinitions();
        if (!initialized) {
            linkSuperclasses(definitions);
            addChildren(definitions);
            linkLayoutStyleables(definitions);
            initialized = true;
        }
        return definitions;
    }

    @NotNull
    protected String getStyleableNameByTagName(@NotNull String tagName) {
        if (tagName.equals("view")) return "View";
        return tagName;
    }

    @NotNull
    protected String getTagNameByStyleableName(@NotNull String styleableName) {
        return styleableName;
    }

    @NotNull
    public String getAttrsFilename() {
        return "attrs.xml";
    }

    @Override
    public boolean isMyFile(@NotNull final XmlFile file, final Module module) {
        if (forAllFiles) return true;
        final LayoutDomFileDescription description = new LayoutDomFileDescription();
        return ApplicationManager.getApplication().runReadAction(new Computable<Boolean>() {
            public Boolean compute() {
                return description.isMyFile(file, module);
            }
        });
    }
}
