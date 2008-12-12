package org.jetbrains.android.dom.manifest;

import com.intellij.psi.PsiClass;
import com.intellij.util.xml.Attribute;
import com.intellij.util.xml.Convert;
import com.intellij.util.xml.Required;
import com.intellij.util.xml.ExtendClass;
import org.jetbrains.android.dom.AndroidAttributeValue;
import org.jetbrains.android.dom.converters.PackageClassResolvingConverter;

/**
 * @author coyote
 */
public interface ActivityAlias extends ManifestElementWithName {
    @Attribute("name")
    @Required
    @Convert(PackageClassResolvingConverter.class)
    @ExtendClass("android.app.Activity")
    AndroidAttributeValue<PsiClass> getActivityClass();
}
