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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

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
        viewClassMap.put(viewClass.getName(), viewClass);
    }

    private synchronized Map<String, PsiClass> getViewClassMap() {
        if (viewClassMap == null) {
            viewClassMap = new HashMap<String, PsiClass>();
            Project project = facet.getModule().getProject();
            JavaPsiFacade facade = JavaPsiFacade.getInstance(project);
            PsiClass viewClass = facade.findClass("android.view.View", ProjectScope.getAllScope(project));

            if (viewClass != null) {
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

    public PsiClass getViewClass(String name) {
        return getViewClassMap().get(name);
    }

    public StyleableDefinition getStyleableByTagName(String tagName) {
        final AttributeDefinitions attrDefs = getAttributeDefinitions();
        final StyleableDefinition definition = attrDefs.getStyleableByName(tagName);
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
        while (definition != null && !definition.getName().equals("View")) {
            definition = definition.getSuperclass();
        }
        return definition != null;
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
        PsiClass psiClass = getViewClass(name);
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


    @NotNull
    public AttributeDefinitions getAttributeDefinitions() {
        AttributeDefinitions attributeDefinitions = super.getAttributeDefinitions();
        if (!initialized) {
            linkSuperclasses(attributeDefinitions);
            addChildren(attributeDefinitions);
            initialized = true;
        }
        return attributeDefinitions;
    }

    @NotNull
    protected String getStyleableNameByTagName(@NotNull String tagName) {
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

    public boolean isMyFile(@NotNull final XmlFile file, final Module module) {
        final LayoutDomFileDescription description = new LayoutDomFileDescription();
        return ApplicationManager.getApplication().runReadAction(new Computable<Boolean>() {
            public Boolean compute() {
                return description.isMyFile(file, module);
            }
        });
    }
}
