package org.jetbrains.android.dom.resources;

/**
 * @author yole
 */
public class ResourceValue {
    private String myValue;
    private char myPrefix;
    private String myPackage;
    private String myResourceType;
    private String myResourceName;

    private ResourceValue() {
    }

    public static ResourceValue parse(String s) {
        if (s == null) {
            return null;
        }
        if (s.startsWith("@")) {
            return reference(s);
        }
        return literal(s);
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
            final String resType = value.substring(1, pos);
            int colonIndex = resType.indexOf(':');
            if (colonIndex > 0) {
                result.myPackage = resType.substring(0, colonIndex);
                result.myResourceType = resType.substring(colonIndex+1);
            }
            else {
                result.myResourceType = resType;
            }
            result.myResourceName = value.substring(pos+1);
        }
        else {
            result.myResourceName = value.substring(1);
        }
        return result;
    }

    public static ResourceValue referenceTo(char prefix, String resourceType, String resourceName) {
        ResourceValue result = new ResourceValue();
        result.myPrefix = prefix;
        result.myResourceType = resourceType;
        result.myResourceName = resourceName;
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

    public String getPackage() {
        return myPackage;
    }

    public String toString() {
        if (myValue != null) {
            return myValue;
        }
        final StringBuilder builder = new StringBuilder().append(myPrefix);
        if (myPackage != null) {
            builder.append(myPackage).append(":");
        }
        builder.append(myResourceType).append("/").append(myResourceName);
        return builder.toString();
    }
}
