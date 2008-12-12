package org.jetbrains.android.dom.manifest;

import com.intellij.util.xml.Attribute;
import com.intellij.util.xml.Convert;
import com.intellij.util.xml.GenericAttributeValue;
import com.intellij.util.xml.Required;
import org.jetbrains.android.dom.LookupClass;
import org.jetbrains.android.dom.LookupPrefix;
import org.jetbrains.android.dom.converters.ConstantFieldConverter;

/**
 * @author yole
 */
public interface UsesPermission extends ManifestElementWithName {
    @Required
    @Attribute("name")
    @Convert(ConstantFieldConverter.class)
    @LookupClass("android.Manifest.permission")
    @LookupPrefix("")
    GenericAttributeValue<String> getId();
}
