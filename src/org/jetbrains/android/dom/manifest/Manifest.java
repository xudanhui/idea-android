package org.jetbrains.android.dom.manifest;

import com.intellij.util.xml.GenericAttributeValue;

/**
 * @author yole
 */
public interface Manifest extends ManifestElement {
    Application getApplication();
    GenericAttributeValue<String> getPackage();
}
