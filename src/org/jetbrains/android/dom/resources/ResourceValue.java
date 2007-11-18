package org.jetbrains.android.dom.resources;

/**
 * @author yole
 */
public class ResourceValue {
    private String myValue;
    private char myPrefix;
    private String myResourceType;
    private String myResourceName;

    private ResourceValue() {
    }

    public static ResourceValue literal(String value) {
        ResourceValue result = new ResourceValue();
        result.myValue = value;
        return result;
    }

    public static ResourceValue reference(String value) {
        ResourceValue result = new ResourceValue();
        assert value.length() > 0;
        result.myPrefix = value.charAt(0);
        int pos = value.indexOf('/');
        if (pos > 0) {
            result.myResourceType = value.substring(1, pos);
            result.myResourceName = value.substring(pos+1);
        }
        else {
            result.myResourceName = value.substring(1);
        }
        return result;
    }

    public boolean isReference() {
        return myValue == null;
    }

    public String getValue() {
        return myValue;
    }

    public String getResourceType() {
        return myResourceType;
    }

    public String getResourceName() {
        return myResourceName;
    }
}
