package org.jetbrains.android.dom.manifest;

import com.intellij.util.xml.Attribute;
import com.intellij.util.xml.Convert;
import com.intellij.util.xml.DomElement;
import org.jetbrains.android.dom.AndroidAttributeValue;
import org.jetbrains.android.dom.LookupClass;
import org.jetbrains.android.dom.LookupPrefix;
import org.jetbrains.android.dom.converters.ConstantFieldConverter;

/**
 * @author yole
 */
public interface Action extends DomElement {
    @Attribute("name")
    @Convert(ConstantFieldConverter.class)
    @LookupClass("android.content.Intent")
    @LookupPrefix("android.intent.action")
    AndroidAttributeValue<String> getName();
}
    