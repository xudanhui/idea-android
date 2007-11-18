package org.jetbrains.android.dom.manifest;

import com.intellij.util.xml.*;
import com.intellij.psi.PsiClass;
import org.jetbrains.android.dom.converters.PackageClassResolvingConverter;

/**
 * @author yole
 */
public interface Provider extends DomElement {
    @Attribute("class")
    @Required
    @Convert(PackageClassResolvingConverter.class)
    @ExtendClass("android.content.ContentProvider")
    GenericAttributeValue<PsiClass> getProviderClass();
}
