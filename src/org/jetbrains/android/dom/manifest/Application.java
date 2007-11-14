package org.jetbrains.android.dom.manifest;

import com.intellij.util.xml.DomElement;

import java.util.List;

/**
 * @author yole
 */
public interface Application extends DomElement {
    List<Activity> getActivities();
}
