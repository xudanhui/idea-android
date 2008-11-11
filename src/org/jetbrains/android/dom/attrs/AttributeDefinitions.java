package org.jetbrains.android.dom.attrs;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.xml.XmlDocument;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;

import java.util.*;

/**
 * @author yole
 */
public class AttributeDefinitions {
    private static final Logger LOG = Logger.getInstance("#org.jetbrains.android.dom.attrs.AttributeDefinitions");

    private Map<String, AttributeDefinition> myAttrs = new HashMap<String, AttributeDefinition>();
    private Map<String, StyleableDefinition> myStyleables = new HashMap<String, StyleableDefinition>();

    public AttributeDefinitions(XmlFile file) {
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
    }

    private AttributeDefinition parseAttrTag(XmlTag tag) {
        String name = tag.getAttributeValue("name");
        if (name == null) {
            LOG.info("Found attr tag with no name: " + tag.getText());
            return null;
        }
        List<AttributeFormat> formats = null;
        XmlTag[] values = null;
        String format = tag.getAttributeValue("format");
        if (format != null) {
            formats = parseAttrFormat(format);
        }
        else {
            values = tag.findSubTags("enum");
            if (values.length > 0) {
                formats = Collections.singletonList(AttributeFormat.Enum);
            }
            else {
                values = tag.findSubTags("flag");
                if (values.length > 0) {
                    formats = Collections.singletonList(AttributeFormat.Flag);
                }
            }
        }
        if (formats == null) {
            LOG.info("Unknown format for tag: " + tag.getText());
            return null;
        }
        AttributeDefinition def = new AttributeDefinition(name, formats);
        if (values != null) {
            parseAttrValues(def, values);
        }
        return def;
    }

    private List<AttributeFormat> parseAttrFormat(String formatString) {
        List<AttributeFormat> result = new ArrayList<AttributeFormat>();
        final String[] formats = formatString.split("\\|");
        for (String format : formats) {
            final AttributeFormat attributeFormat;
            try {
                attributeFormat = AttributeFormat.valueOf(StringUtil.capitalize(format));
            } catch (IllegalArgumentException e) {
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
        if (name == null) {
            LOG.info("Found declare-styleable tag with no name: " + tag.getText());
            return;
        }
        String parentName = tag.getAttributeValue("parent");
        StyleableDefinition parent = null;
        if (parentName != null) {
            parent = myStyleables.get(parentName);
            if (parent == null) {
                LOG.info("Found declare-styleable tag with unknown parent: " + tag.getText());
                return;
            }
        }
        StyleableDefinition def = new StyleableDefinition(name, parent);
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

    public StyleableDefinition getStyleableDefinition(String name) {
        return myStyleables.get(name);
    }

    public Collection<String> getStyleableNames() {
        return myStyleables.keySet();
    }
}
