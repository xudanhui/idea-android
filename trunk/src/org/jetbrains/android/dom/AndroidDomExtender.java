package org.jetbrains.android.dom;

import com.intellij.openapi.module.Module;
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
import org.jetbrains.android.dom.converters.EmptyConverter;
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

        addToMap(resourceTypes, "string", "label", "description");
        addToMap(resourceTypes, "drawable", "icon");
        addToMap(resourceTypes, "style", "theme");
    }

    @Nullable
    private static String getResourceType(String attributeName) {
        String type = resourceTypes.get(attributeName);
        if (type != null) return type;
        if (attributeName.toLowerCase().endsWith("style")) {
            return "style";
        }
        return null;
    }

    private static String[] getManifestSkipNames(AndroidDomElement element) {
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
        Module module = element.getModule();
        if (module == null) return;
        final AndroidFacet facet = AndroidFacet.getInstance(module);
        if (facet == null) return;
        XmlTag tag = element.getXmlTag();
        if (tag == null) return;
        String tagName = tag.getName();
        if (element instanceof LayoutElement) {
            LayoutStyleableProvider provider = facet.getStyleableProvider(LayoutStyleableProvider.KEY);
            StyleableDefinition styleable = provider.getStyleableByTagName(tagName);
            if (styleable != null) {
                // id is a strange attribute and we skip it for a while
                registerStyleableAttributes(registrar, provider, styleable, tag, "id");
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
            String[] skipNames = getManifestSkipNames(element);

            registerStyleableAttributes(registrar, provider, styleable, element.getXmlTag(), skipNames);

            for (StyleableDefinition definition : styleable.getChildren()) {
                Class myClass = getClassByManifestStyleableName(definition.getName());
                if (myClass != null) {
                    registrar.registerCollectionChildrenExtension(new XmlName(tagName), myClass);
                }
            }
        }
    }

    private void registerStyleableAttributes(DomExtensionsRegistrar registrar, @NotNull StyleableProvider provider,
                                             @NotNull StyleableDefinition styleable, XmlTag tag, String... skipNames) {
        XmlTag parentTag = tag.getParentTag();
        for (XmlAttribute attribute : tag.getAttributes()) {
            if (attribute.getNamespace().equals(AndroidManager.NAMESPACE)) {
                final String localName = attribute.getLocalName();
                if (ArrayUtil.contains(localName, (Object[]) skipNames)) {
                    continue;
                }
                AttributeDefinition definition = provider.findAttribute(localName, styleable, parentTag);
                if (definition != null) {
                    XmlName xmlName = new XmlName(definition.getName(), AndroidManager.NAMESPACE_KEY);
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
        ResolvingConverter converter = null;
        String[] values = attr.getValues();
        boolean emptyConverterAdded = false;
        for (AttributeFormat format : formats) {
            switch (format) {
                case Reference:
                    String resourceType = getResourceType(attr.getName());
                    if (resourceType == null && formats.contains(AttributeFormat.Color)) {
                        resourceType = "drawable";
                    }
                    converter = new ResourceReferenceConverter(resourceType);
                    compositeConverter.addConverter(ResourceValue.class, converter);
                    break;
                case Color:
                    converter = new ResourceReferenceConverter("color");
                    compositeConverter.addConverter(ResourceValue.class, converter);
                    break;
                case Enum:
                    converter = new StaticEnumConverter(values);
                    compositeConverter.addConverter(String.class, converter);
                    break;
                default:
                    if (!emptyConverterAdded) {
                        converter = new EmptyConverter();
                        compositeConverter.addConverter(String.class, converter);
                        emptyConverterAdded = true;
                    }
            }
        }
        return compositeConverter.size() == 1 ? converter : compositeConverter;
    }

    /*private static final Map<String, AndroidAttributeDescriptor> ourDescriptors = new HashMap<String, AndroidAttributeDescriptor>();

    private static void registerDescriptor(String name, Class valueClass, Converter converter) {
        ourDescriptors.put(name, new AndroidAttributeDescriptor(valueClass, converter));
    }*/

    @Nullable
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
