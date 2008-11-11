package org.jetbrains.android.dom.attrs;

import org.jetbrains.annotations.Nullable;

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

    @Nullable
    public AttributeFormat getFormat() {
        return myFormats.size() == 1 ? myFormats.get(0) : null;        
    }

    public String[] getValues() {
        return myValues.toArray(new String[myValues.size()]);
    }
}
