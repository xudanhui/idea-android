package org.jetbrains.android.dom.manifest;

import com.intellij.util.xml.*;
import com.intellij.psi.PsiClass;
import org.jetbrains.android.dom.converters.PackageClassResolvingConverter;

/**
 * @author yole
 */
public interface Instrumentation extends DomElement {
    @Attribute("class")
    @Required
    @Convert(PackageClassResolvingConverter.class)
    @ExtendClass("android.app.Instrumentation")
    GenericAttributeValue<PsiClass> getInstrumentationClass();
}