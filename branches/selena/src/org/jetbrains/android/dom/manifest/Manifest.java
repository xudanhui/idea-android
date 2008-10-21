package org.jetbrains.android.dom.manifest;

import com.intellij.util.xml.DomElement;
import com.intellij.util.xml.GenericAttributeValue;

import java.util.List;

/**
 * @author yole
 */
public interface Manifest extends DomElement {
    Application getApplication();
    List<Instrumentation> getInstrumentations();
    List<UsesPermission> getUsesPermissions();

    GenericAttributeValue<String> getPackage();
}
