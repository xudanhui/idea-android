package org.jetbrains.android.dom.manifest;

import com.intellij.psi.PsiClass;
import com.intellij.util.xml.*;
import org.jetbrains.android.dom.manifest.converters.PackageClassResolvingConverter;

import java.util.List;

/**
 * @author yole
 */
public interface Activity extends DomElement {
    @Attribute("class")
    @Required
    @Convert(PackageClassResolvingConverter.class)
    GenericAttributeValue<PsiClass> getActivityClass();

    List<IntentFilter> getIntentFilters();
}
