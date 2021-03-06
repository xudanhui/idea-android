package org.jetbrains.android.dom.resources;

import com.intellij.util.xml.DomElement;
import com.intellij.util.xml.GenericAttributeValue;

/**
 * @author yole
 */
public interface ResourceElement extends DomElement {
    GenericAttributeValue<String> getName();
    String getValue();
}
