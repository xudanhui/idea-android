package org.jetbrains.android.dom.manifest;

import com.intellij.psi.PsiClass;
import com.intellij.util.xml.Attribute;
import com.intellij.util.xml.Convert;
import com.intellij.util.xml.ExtendClass;
import com.intellij.util.xml.Required;
import org.jetbrains.android.dom.AndroidAttributeValue;
import org.jetbrains.android.dom.AndroidDomElement;
import org.jetbrains.android.dom.ResourceType;
import org.jetbrains.android.dom.converters.PackageClassResolvingConverter;
import org.jetbrains.android.dom.converters.ResourceReferenceConverter;
import org.jetbrains.android.dom.resources.ResourceValue;

import java.util.List;

/**
 * @author yole
 */
public interface Activity extends AndroidDomElement {
    @Attribute("name")
    @Required
    @Convert(PackageClassResolvingConverter.class)
    @ExtendClass("android.app.Activity")
    AndroidAttributeValue<PsiClass> getActivityClass();

    @Convert(ResourceReferenceConverter.class)
    @ResourceType("string")
    AndroidAttributeValue<ResourceValue> getLabel();

    List<IntentFilter> getIntentFilters();

    IntentFilter addIntentFilter();
}
