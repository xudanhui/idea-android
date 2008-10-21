package org.jetbrains.android.dom.manifest;

import com.intellij.util.xml.DomElement;
import com.intellij.util.xml.GenericAttributeValue;
import com.intellij.util.xml.Convert;
import org.jetbrains.android.dom.converters.ConstantFieldConverter;
import org.jetbrains.android.dom.LookupClass;
import org.jetbrains.android.dom.LookupPrefix;

/**
 * @author yole
 */
public interface UsesPermission extends DomElement {
    @Convert(ConstantFieldConverter.class)
    @LookupClass("android.Manifest.permission")
    @LookupPrefix("")
    GenericAttributeValue<String> getId();
}
