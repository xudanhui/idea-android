package org.jetbrains.android.dom.layout;

import com.intellij.util.xml.Convert;
import com.intellij.util.xml.GenericAttributeValue;
import org.jetbrains.android.dom.AndroidDomElement;
import org.jetbrains.android.dom.ResourceType;
import org.jetbrains.android.dom.converters.ResourceReferenceConverter;
import org.jetbrains.android.dom.resources.ResourceValue;

/**
 * @author yole
 */
public interface LayoutElement extends AndroidDomElement {
    String getId();

    @Convert(ResourceReferenceConverter.class)
    @ResourceType("style")
    GenericAttributeValue<ResourceValue> getStyle();
}
