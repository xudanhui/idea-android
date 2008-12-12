package org.jetbrains.android.dom;

import com.intellij.util.xml.Converter;

/**
 * @author coyote
*/
public class AndroidAttributeDescriptor {
    private final Class myValueClass;
    private final Converter myConverter;

    AndroidAttributeDescriptor(Class valueClass, Converter converter) {
        myValueClass = valueClass;
        myConverter = converter;
    }

    public Class getValueClass() {
        return myValueClass;
    }

    public Converter getConverter() {
        return myConverter;
    }
}
