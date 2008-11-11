package org.jetbrains.android.dom.attrs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author yole
 */
public class StyleableDefinition {
    private String myName;
    private StyleableDefinition myParent;
    private StyleableDefinition mySuperclass;
    private final Map<String, AttributeDefinition> myAttributes = new HashMap<String, AttributeDefinition>();

    public StyleableDefinition(String name, StyleableDefinition parent) {
        myName = name;
        myParent = parent;
    }

    public StyleableDefinition getSuperclass() {
        return mySuperclass;
    }

    public void setSuperclass(StyleableDefinition superclass) {
        mySuperclass = superclass;
    }

    public void addAttribute(AttributeDefinition attr) {
        myAttributes.put(attr.getName(), attr);
    }

    public List<AttributeDefinition> getAttributes() {
        List<AttributeDefinition> result = new ArrayList<AttributeDefinition>();
        StyleableDefinition def = this;
        while (def != null) {
            result.addAll(def.myAttributes.values());
            def = def.mySuperclass;
        }
        return result;
    }

    public AttributeDefinition findAttribute(String name) {
        StyleableDefinition def = this;
        while (def != null) {
            final AttributeDefinition result = def.myAttributes.get(name);
            if (result != null) {
                return result;
            }
            def = def.mySuperclass;
        }
        return null;
    }
}
