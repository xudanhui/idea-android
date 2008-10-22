package org.jetbrains.android.dom.manifest;

import com.intellij.util.xml.DomElement;
import com.intellij.util.xml.Attribute;
import com.intellij.util.xml.Convert;
import org.jetbrains.android.dom.converters.ConstantFieldConverter;
import org.jetbrains.android.dom.LookupClass;
import org.jetbrains.android.dom.LookupPrefix;
import org.jetbrains.android.dom.AndroidAttributeValue;

/**
 * @author yole
 */
public interface Category extends DomElement {
    @Attribute("name")
    @Convert(ConstantFieldConverter.class)
    @LookupClass("android.content.Intent")
    @LookupPrefix("android.intent.category")
    AndroidAttributeValue<String> getName();
}
