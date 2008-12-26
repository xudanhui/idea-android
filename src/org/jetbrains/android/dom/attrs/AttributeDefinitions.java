package org.jetbrains.android.dom.attrs;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.xml.XmlDocument;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * @author yole
 */
public class AttributeDefinitions {
    private static final Logger LOG = Logger.getInstance("#org.jetbrains.android.dom.attrs.AttributeDefinitions");

    private Map<String, AttributeDefinition> myAttrs = new HashMap<String, AttributeDefinition>();
    private Map<String, StyleableDefinition> myStyleables = new HashMap<String, StyleableDefinition>();
    private Map<StyleableDefinition, String[]> parentMap = new HashMap<StyleableDefinition, String[]>();

    public AttributeDefinitions(@NotNull XmlFile file) {
        final XmlDocument document = file.getDocument();
        if (document == null) return;
        final XmlTag rootTag = document.getRootTag();
        if (rootTag == null) return;
        for (XmlTag tag : rootTag.getSubTags()) {
            if (tag.getName().equals("attr")) {
                final AttributeDefinition def = parseAttrTag(tag);
                myAttrs.put(def.getName(), def);
            }
            else if (tag.getName().equals("declare-styleable")) {
                parseDeclareStyleableTag(tag);
            }
        }

        for (Map.Entry<StyleableDefinition, String[]> entry : parentMap.entrySet()) {
            StyleableDefinition definition = entry.getKey();
            String[] parentNames = entry.getValue();
            for (int i = 0; i < parentNames.length; i++) {
                String parentName = parentNames[i];
                StyleableDefinition parent = myStyleables.get(parentName);
                if (parent != null) {
                    parent.addChild(definition);
                    definition.addParent(parent);
                }
                else {
                    LOG.info("Found tag with unknown parent: " + parentName);
                }
            }
        }
    }

    private AttributeDefinition parseAttrTag(XmlTag tag) {
        String name = tag.getAttributeValue("name");
        if (name == null) {
            LOG.info("Found attr tag with no name: " + tag.getText());
            return null;
        }
        List<AttributeFormat> parsedFormats = null;
        List<AttributeFormat> formats = new ArrayList<AttributeFormat>();
        XmlTag[] values = tag.findSubTags("enum");
        String format = tag.getAttributeValue("format");
        if (format != null) {
            parsedFormats = parseAttrFormat(format);
            if (parsedFormats != null) formats.addAll(parsedFormats);
        }
        if (values.length > 0) {
            formats.add(AttributeFormat.Enum);
        }
        else {
            values = tag.findSubTags("flag");
            if (values.length > 0) {
                formats.add(AttributeFormat.Flag);
            }
        }
        if (formats.isEmpty() && parsedFormats == null) {
            LOG.info("Unknown format for tag: " + tag.getText());
            return null;
        }
        AttributeDefinition def = new AttributeDefinition(name, formats);
        parseAttrValues(def, values);
        return def;
    }

    private List<AttributeFormat> parseAttrFormat(String formatString) {
        List<AttributeFormat> result = new ArrayList<AttributeFormat>();
        final String[] formats = formatString.split("\\|");
        for (String format : formats) {
            final AttributeFormat attributeFormat;
            try {
                attributeFormat = AttributeFormat.valueOf(StringUtil.capitalize(format));
            }
            catch (IllegalArgumentException e) {
                return null;
            }
            result.add(attributeFormat);
        }
        return result;
    }

    private void parseAttrValues(AttributeDefinition def, XmlTag[] values) {
        for (XmlTag value : values) {
            final String valueName = value.getAttributeValue("name");
            if (valueName == null) {
                LOG.info("Unknown value for tag: " + value.getText());
            }
            else {
                def.addValue(valueName);
            }
        }
    }

    private void parseDeclareStyleableTag(XmlTag tag) {
        String name = tag.getAttributeValue("name");
        StyleableDefinition def = new StyleableDefinition(name);
        if (name == null) {
            LOG.info("Found declare-styleable tag with no name: " + tag.getText());
            return;
        }
        String parentNameAttributeValue = tag.getAttributeValue("parent");
        if (parentNameAttributeValue != null) {
            String[] parentNames = parentNameAttributeValue.split("\\s+");
            parentMap.put(def, parentNames);
        }
        myStyleables.put(name, def);
        for (XmlTag subTag : tag.findSubTags("attr")) {
            parseStyleableAttr(def, subTag);
        }
    }

    private void parseStyleableAttr(StyleableDefinition def, XmlTag tag) {
        String name = tag.getAttributeValue("name");
        if (name == null) {
            LOG.info("Found attr tag with no name: " + tag.getText());
            return;
        }
        AttributeDefinition attr = myAttrs.get(name);
        if (attr == null) {
            attr = parseAttrTag(tag);
        }
        if (attr != null) {
            def.addAttribute(attr);
        }
    }

    public StyleableDefinition removeStyleableByName(String name) {
        return myStyleables.remove(name);
    }

    public StyleableDefinition getStyleableByName(String name) {
        return myStyleables.get(name);
    }

    public Collection<String> getStyleableNames() {
        return myStyleables.keySet();
    }
}
