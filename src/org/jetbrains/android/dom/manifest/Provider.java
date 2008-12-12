package org.jetbrains.android.dom.manifest;

import com.intellij.psi.PsiClass;
import com.intellij.util.xml.*;
import org.jetbrains.android.dom.AndroidAttributeValue;
import org.jetbrains.android.dom.converters.PackageClassResolvingConverter;

/**
 * @author yole
 */
public interface Provider extends ManifestElementWithName {
    @Attribute("name")
    @Required
    @Convert(PackageClassResolvingConverter.class)
    @ExtendClass("android.content.ContentProvider")
    AndroidAttributeValue<PsiClass> getProviderClass();
}
