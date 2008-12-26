package org.jetbrains.android.dom.attrs;

import java.util.*;

/**
 * @author yole, coyote
 */
public class StyleableDefinition {
    private final String myName;
    private List<StyleableDefinition> parents = new ArrayList<StyleableDefinition>();
    private List<StyleableDefinition> layoutStyleables = new ArrayList<StyleableDefinition>();
    private StyleableDefinition mySuperclass;
    private final Map<String, AttributeDefinition> myAttributes = new HashMap<String, AttributeDefinition>();
    private List<StyleableDefinition> children = new ArrayList<StyleableDefinition>();

    public StyleableDefinition(String name) {
        myName = name;
    }

    public void addChild(StyleableDefinition child) {
        children.add(child);
    }

    public void addParent(StyleableDefinition parent) {
        parents.add(parent);
    }

    public void addLayoutStyleable(StyleableDefinition layoutStyleable) {
        layoutStyleables.add(layoutStyleable);
    }

    public List<AttributeDefinition> getLayoutAttributes() {
        List<AttributeDefinition> attrs = new ArrayList<AttributeDefinition>();
        StyleableDefinition def = this;
        while (def != null) {
            for (StyleableDefinition layoutStyleable : def.layoutStyleables) {
                attrs.addAll(layoutStyleable.getAttributes());
            }
            def = def.mySuperclass;
        }
        return attrs;
    }

    public AttributeDefinition findLayoutAttribute(String name) {
        StyleableDefinition def = this;
        while (def != null) {
            for (StyleableDefinition layoutStyleable : def.layoutStyleables) {
                AttributeDefinition definition = layoutStyleable.findAttribute(name);
                if (definition != null) return definition;
            }
            def = def.mySuperclass;
        }
        return null;
    }

    public List<StyleableDefinition> getParents() {
        return Collections.unmodifiableList(parents);
    }

    public List<StyleableDefinition> getChildren() {
        return children;
    }

    public String getName() {
        return myName;
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

    public void addAttributes(Collection<AttributeDefinition> attrs) {
        for (AttributeDefinition attr : attrs) {
            myAttributes.put(attr.getName(), attr);
        }
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
            AttributeDefinition result = def.myAttributes.get(name);
            if (result != null) return result;
            def = def.mySuperclass;
        }
        return null;
    }
}
