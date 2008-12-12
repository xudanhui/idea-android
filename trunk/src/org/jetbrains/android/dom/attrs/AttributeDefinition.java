package org.jetbrains.android.dom.attrs;

import java.util.ArrayList;
import java.util.List;

/**
 * @author yole
 */
public class AttributeDefinition {
    private final String myName;
    private final List<AttributeFormat> myFormats;
    private final List<String> myValues = new ArrayList<String>();

    public AttributeDefinition(String name, List<AttributeFormat> formats) {
        myName = name;
        myFormats = formats;
    }

    public void addValue(String name) {
        myValues.add(name);
    }

    public String getName() {
        return myName;
    }

    public List<AttributeFormat> getFormats() {
        return myFormats;        
    }

    public String[] getValues() {
        return myValues.toArray(new String[myValues.size()]);
    }
}
