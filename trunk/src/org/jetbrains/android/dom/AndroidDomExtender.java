package org.jetbrains.android.dom;

import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlTag;
import com.intellij.util.ArrayUtil;
import com.intellij.util.xml.Converter;
import com.intellij.util.xml.ResolvingConverter;
import com.intellij.util.xml.XmlName;
import com.intellij.util.xml.reflect.DomExtender;
import com.intellij.util.xml.reflect.DomExtension;
import com.intellij.util.xml.reflect.DomExtensionsRegistrar;
import org.jetbrains.android.AndroidManager;
import org.jetbrains.android.dom.attrs.AttributeDefinition;
import org.jetbrains.android.dom.attrs.AttributeFormat;
import org.jetbrains.android.dom.attrs.StyleableDefinition;
import org.jetbrains.android.dom.converters.CompositeConverter;
import org.jetbrains.android.dom.converters.ResourceReferenceConverter;
import org.jetbrains.android.dom.converters.StaticEnumConverter;
import org.jetbrains.android.dom.layout.LayoutElement;
import org.jetbrains.android.dom.layout.LayoutStyleableProvider;
import org.jetbrains.android.dom.manifest.*;
import org.jetbrains.android.dom.resources.ResourceValue;
import org.jetbrains.android.facet.AndroidFacet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * @author yole, coyote
 */
public class AndroidDomExtender extends DomExtender<AndroidDomElement> {
    private static final Map<String, String> resourceTypes = new HashMap<String, String>();

    private static void addToMap(Map<String, String> map, String value, String... keys) {
        for (String key : keys) {
            map.put(key, value);
        }
    }

    static {
//        registerDescriptor("background", ResourceValue.class, new ResourceReferenceConverter("drawable"));
//        registerDescriptor("orientation", String.class, new StaticEnumConverter("horizontal", "vertical"));
//        registerDescriptor("password", boolean.class, null);
//        registerDescriptor("src", ResourceValue.class, new ResourceReferenceConverter("drawable"));
//        registerDescriptor("text", ResourceValue.class, new ResourceReferenceConverter("string"));
//        registerDescriptor("textColor", ResourceValue.class, new ResourceReferenceConverter("color"));
//        registerDescriptor("textAlign", String.class, new StaticEnumConverter("start", "center", "end"));

        //manifest
        addToMap(resourceTypes, "string", "label", "description");
        addToMap(resourceTypes, "drawable", "icon");
        addToMap(resourceTypes, "style", "theme");
    }

    private static String getResourceType(String attributeName) {
        String type = resourceTypes.get(attributeName);
        if (type != null) return type;
        if (attributeName.toLowerCase().endsWith("style")) {
            return "style";
        }
        return null;
    }

    private static String[] getSkipNames(AndroidDomElement element) {
        List<String> strings = new ArrayList<String>();
        if (element instanceof ManifestElementWithName) {
            strings.add("name");
        }
        if (element instanceof Activity) {
            strings.add("label");
        }
        if (element instanceof Application) {
            strings.add("manageSpaceActivity");
        }
        return strings.toArray(new String[strings.size()]);
    }

    public void registerExtensions(@NotNull AndroidDomElement element, @NotNull DomExtensionsRegistrar registrar) {
        final AndroidFacet facet = AndroidFacet.getInstance(element.getModule());
        if (facet == null) return;
        XmlTag tag = element.getXmlTag();
        if (tag == null) return;
        String tagName = tag.getName();
        if (element instanceof LayoutElement) {
            LayoutStyleableProvider provider = facet.getStyleableProvider(LayoutStyleableProvider.KEY);
            StyleableDefinition styleable = provider.getStyleableByTagName(tagName);
            if (styleable != null) {
                registerStyleableAttributes(registrar, styleable, tag);
            }
            Set<String> viewClasses = provider.getViewClassNames();
            for (String s : viewClasses) {
                registrar.registerCollectionChildrenExtension(new XmlName(s), LayoutElement.class);
            }
        }
        else if (element instanceof ManifestElement) {
            ManifestStyleableProvider provider = facet.getStyleableProvider(ManifestStyleableProvider.KEY);
            StyleableDefinition styleable = provider.getStyleableByTagName(tagName);
            if (styleable == null) return;
            String[] skipNames = getSkipNames(element);

            registerStyleableAttributes(registrar, styleable, element.getXmlTag(), skipNames);
            
            for (StyleableDefinition definition : styleable.getChildren()) {
                Class myClass = getClassByManifestStyleableName(definition.getName());
                if (myClass != null) {
                    registrar.registerCollectionChildrenExtension(new XmlName(tagName), myClass);
                }
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
                    List<AttributeFormat> formats = definition.getFormats();
                    Class valueClass = formats.size() == 1 ? getValueClass(formats.get(0)) : String.class;
                    final Converter converter = getConverter(definition);
                    DomExtension extension = registrar.registerGenericAttributeValueChildExtension(xmlName, valueClass);
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
            case Color:
                return ResourceValue.class;
            default:
                return String.class;
        }
    }

    @Nullable
    private Converter getConverter(AttributeDefinition attr) {
        List<AttributeFormat> formats = attr.getFormats();
        CompositeConverter compositeConverter = new CompositeConverter();
        boolean containsColor = formats.contains(AttributeFormat.Color);
        for (AttributeFormat format : formats) {
            ResolvingConverter converter = null;
            if (format == AttributeFormat.Enum) {
                converter = new StaticEnumConverter(attr.getValues());
                compositeConverter.addConverter(String.class, converter);
            }
            else if (format == AttributeFormat.Reference) {
                String resourceType = getResourceType(attr.getName());
                if (resourceType == null && containsColor) {
                    resourceType = "drawable";
                }
                converter = new ResourceReferenceConverter(resourceType);
                compositeConverter.addConverter(ResourceValue.class, converter);
            }
            else if (format == AttributeFormat.Color) {
                converter = new ResourceReferenceConverter("color");
                compositeConverter.addConverter(ResourceValue.class, converter);
            }
            if (formats.size() == 1) return converter;
        }
        return compositeConverter;
    }

    /*private synchronized List<String> getViewClasses(Project project) {
        if (myViewClasses == null) {
            myViewClasses = new ArrayList<String>();
            PsiClass viewClass = JavaPsiFacade.getInstance(project).findClass("android.view.View", ProjectScope.getAllScope(project));
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
    }*/

    private static final Map<String, AndroidAttributeDescriptor> ourDescriptors = new HashMap<String, AndroidAttributeDescriptor>();

    private static void registerDescriptor(String name, Class valueClass, Converter converter) {
        ourDescriptors.put(name, new AndroidAttributeDescriptor(valueClass, converter));
    }

    private static Class getClassByManifestStyleableName(String styleableName) {
        String prefix = "AndroidManifest";
        if (!styleableName.startsWith(prefix)) {
            return null;
        }
        if (styleableName.equals(prefix)) return Manifest.class;
        String remained = styleableName.substring(prefix.length());
        try {
            return Class.forName("org.jetbrains.android.dom.manifest." + remained);
        }
        catch (ClassNotFoundException e) {
            return ManifestElement.class;
        }
    }
}
