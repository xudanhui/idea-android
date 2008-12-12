package org.jetbrains.android.dom.manifest;

import com.intellij.psi.PsiClass;
import com.intellij.util.xml.Attribute;
import com.intellij.util.xml.Convert;
import com.intellij.util.xml.ExtendClass;
import org.jetbrains.android.dom.AndroidAttributeValue;
import org.jetbrains.android.dom.converters.PackageClassResolvingConverter;

import java.util.List;

/**
 * @author yole
 */
public interface Application extends ManifestElement {
    List<Activity> getActivities();
    
    Activity addActivity();

    @Attribute("manageSpaceActivity")
    @Convert(PackageClassResolvingConverter.class)
    @ExtendClass("android.app.Activity")
    AndroidAttributeValue<PsiClass> getManageSpaceActivity();

//    @Convert(ResourceReferenceConverter.class)
//    @ResourceType("string")
//    AndroidAttributeValue<ResourceValue> getLabel();

//    @Convert(ResourceReferenceConverter.class)
//    @ResourceType("drawable")
//    AndroidAttributeValue<ResourceValue> getIcon();
}
