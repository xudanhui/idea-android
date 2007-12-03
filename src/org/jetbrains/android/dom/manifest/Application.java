package org.jetbrains.android.dom.manifest;

import com.intellij.util.xml.Convert;
import com.intellij.util.xml.DomElement;
import org.jetbrains.android.dom.AndroidAttributeValue;
import org.jetbrains.android.dom.ResourceType;
import org.jetbrains.android.dom.converters.ResourceReferenceConverter;
import org.jetbrains.android.dom.resources.ResourceValue;

import java.util.List;

/**
 * @author yole
 */
public interface Application extends DomElement {
    List<Activity> getActivities();
    List<Provider> getProviders();
    List<Service> getServices();
    List<Receiver> getReceivers();

    Activity addActivity();

    @Convert(ResourceReferenceConverter.class)
    @ResourceType("string")
    AndroidAttributeValue<ResourceValue> getLabel();

    @Convert(ResourceReferenceConverter.class)
    @ResourceType("drawable")
    AndroidAttributeValue<ResourceValue> getIcon();
}
