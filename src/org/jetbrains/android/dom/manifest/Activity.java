package org.jetbrains.android.dom.manifest;

import com.intellij.psi.PsiClass;
import com.intellij.util.xml.*;
import org.jetbrains.android.dom.AndroidAttributeValue;
import org.jetbrains.android.dom.converters.PackageClassResolvingConverter;
import org.jetbrains.android.dom.converters.ResourceReferenceConverter;
import org.jetbrains.android.dom.resources.ResourceValue;

import java.util.List;

/**
 * @author yole
 */
public interface Activity extends DomElement {
    @Attribute("class")
    @Required
    @Convert(PackageClassResolvingConverter.class)
    @ExtendClass("android.app.Activity")
    GenericAttributeValue<PsiClass> getActivityClass();

    @Convert(ResourceReferenceConverter.class)
    AndroidAttributeValue<ResourceValue> getLabel();

    List<IntentFilter> getIntentFilters();
}
