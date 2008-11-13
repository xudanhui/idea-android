package org.jetbrains.android.dom;

import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.search.ProjectScope;
import com.intellij.psi.search.searches.ClassInheritorsSearch;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlTag;
import com.intellij.util.ArrayUtil;
import com.intellij.util.Processor;
import com.intellij.util.xml.Converter;
import com.intellij.util.xml.XmlName;
import com.intellij.util.xml.reflect.DomExtender;
import com.intellij.util.xml.reflect.DomExtension;
import com.intellij.util.xml.reflect.DomExtensionsRegistrar;
import org.jetbrains.android.AndroidManager;
import org.jetbrains.android.dom.attrs.AttributeDefinition;
import org.jetbrains.android.dom.attrs.AttributeDefinitions;
import org.jetbrains.android.dom.attrs.AttributeFormat;
import org.jetbrains.android.dom.attrs.StyleableDefinition;
import org.jetbrains.android.dom.converters.ResourceReferenceConverter;
import org.jetbrains.android.dom.converters.StaticEnumConverter;
import org.jetbrains.android.dom.layout.LayoutElement;
import org.jetbrains.android.dom.manifest.Activity;
import org.jetbrains.android.dom.resources.ResourceValue;
import org.jetbrains.android.facet.AndroidFacet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author yole
 */
public class AndroidDomExtender extends DomExtender<AndroidDomElement> {
    private List<String> myViewClasses;

    public void registerExtensions(@NotNull AndroidDomElement androidDomElement, @NotNull DomExtensionsRegistrar registrar) {
        if (androidDomElement instanceof LayoutElement) {
            XmlTag tag = androidDomElement.getXmlTag();
            if (tag != null) {
                String name = tag.getName();
                final AndroidFacet facet = AndroidFacet.getInstance(androidDomElement.getModule());
                if (facet != null) {
                    final AttributeDefinitions attrDefs = facet.getLayoutAttributeDefinitions();
                    final StyleableDefinition styleable = attrDefs.getStyleableDefinition(name);
                    if (styleable != null) {
                        registerStyleableAttributes(registrar, styleable, tag);
                    }
                    else {
                        // e.g. TimePicker is not listed in attrs.xml
                        final PsiClass superClass = facet.findWidgetSuperclass(name);
                        if (superClass != null) {
                            final StyleableDefinition superStyleable = attrDefs.getStyleableDefinition(superClass.getName());
                            if (superStyleable != null) {
                                registerStyleableAttributes(registrar, superStyleable, tag);
                            }
                        }
                    }
                }

                List<String> viewClasses = getViewClasses(androidDomElement.getManager().getProject());
                for(String s: viewClasses) {
                    registrar.registerCollectionChildrenExtension(new XmlName(s), LayoutElement.class);
                }
            }
        }
        else if (androidDomElement instanceof Activity) {
            final AndroidFacet facet = AndroidFacet.getInstance(androidDomElement.getModule());
            if (facet != null) {
                final AttributeDefinitions attrDefs = facet.getManifestAttributeDefinitions();
                final StyleableDefinition styleable = attrDefs.getStyleableDefinition("AndroidManifestActivity");
                registerStyleableAttributes(registrar, styleable, androidDomElement.getXmlTag(), "label");
            }
        }

        // TODO[yole] return new Object[] {PsiModificationTracker.OUT_OF_CODE_BLOCK_MODIFICATION_COUNT };
    }

    private void registerStyleableAttributes(DomExtensionsRegistrar registrar, @NotNull StyleableDefinition styleable,
                                             XmlTag tag, String... skipNames) {
        final XmlAttribute[] attributes = tag.getAttributes();
        for (XmlAttribute attribute : attributes) {
            final String ns = attribute.getNamespace();
            if (ns.equals(AndroidManager.NAMESPACE)) {
                final String localName = attribute.getLocalName();
                if (ArrayUtil.contains(localName, (Object[]) skipNames)) {
                    continue;
                }
                final AttributeDefinition definition = styleable.findAttribute(localName);
                if (definition != null) {
                    XmlName xmlName = new XmlName(definition.getName(), AndroidManager.NAMESPACE_KEY);
                    // TODO converter for formats with multiple alternatives
                    final AttributeFormat format = definition.getFormat();
                    Class valueClass = getValueClass(format);
                    final Converter converter = getConverter(definition);
                    final DomExtension extension = registrar.registerGenericAttributeValueChildExtension(xmlName, valueClass);
                    if (converter != null) {
                        extension.setConverter(converter);
                    }
                }

            }
        }
    }

    private Class getValueClass(@Nullable AttributeFormat format) {
        if (format == null) return String.class;
        switch (format) {
            case Boolean:
                return boolean.class;
            case Integer:
                return int.class;
            case Reference:
                return ResourceValue.class;
            default:
                return String.class;
        }
    }

    @Nullable
    private Converter getConverter(AttributeDefinition attr) {
        if (attr.getFormat() == AttributeFormat.Enum) {
            return new StaticEnumConverter(attr.getValues());
        }
        if (attr.getFormat() == AttributeFormat.Reference) {
            return new ResourceReferenceConverter();
        }
        return null;
    }

    private synchronized List<String> getViewClasses(Project project) {
        if (myViewClasses == null) {
            myViewClasses = new ArrayList<String>();
            PsiClass viewClass = JavaPsiFacade.getInstance(project).findClass("android.view.View",
                    ProjectScope.getAllScope(project));
            if (viewClass != null) {
                myViewClasses.add(viewClass.getName());
                ClassInheritorsSearch.search(viewClass).forEach(new Processor<PsiClass>() {
                    public boolean process(PsiClass psiClass) {
                        myViewClasses.add(psiClass.getName());
                        return true;
                    }
                });
            }
        }
        return myViewClasses;
    }

    private static class AndroidAttributeDescriptor {
        private Class myValueClass;
        private Converter myConverter;

        private AndroidAttributeDescriptor(Class valueClass, Converter converter) {
            myValueClass = valueClass;
            myConverter = converter;
        }
    }

    private static final Map<String, AndroidAttributeDescriptor> ourDescriptors = new HashMap<String, AndroidAttributeDescriptor>();

    private static void registerDescriptor(String name, Class valueClass, Converter converter) {
        ourDescriptors.put(name, new AndroidAttributeDescriptor(valueClass, converter));
    }

    static {
        registerDescriptor("background", ResourceValue.class, new ResourceReferenceConverter("drawable"));
        registerDescriptor("orientation", String.class, new StaticEnumConverter("horizontal", "vertical"));
        registerDescriptor("password", boolean.class, null);
        registerDescriptor("src", ResourceValue.class, new ResourceReferenceConverter("drawable"));
        registerDescriptor("text", ResourceValue.class, new ResourceReferenceConverter("string"));
        registerDescriptor("textColor", ResourceValue.class, new ResourceReferenceConverter("color"));
        registerDescriptor("textAlign", String.class, new StaticEnumConverter("start", "center", "end"));
    }
}
