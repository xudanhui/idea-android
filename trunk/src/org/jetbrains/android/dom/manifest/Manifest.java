package org.jetbrains.android.dom.manifest;

import com.intellij.util.xml.DomElement;
import com.intellij.util.xml.GenericAttributeValue;

/**
 * @author yole
 */
public interface Manifest extends DomElement {
    Application getApplication();

    GenericAttributeValue<String> getPackage();
}
