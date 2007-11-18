package org.jetbrains.android.dom.resources;

import com.intellij.util.xml.DomElement;
import com.intellij.util.xml.SubTagList;

import java.util.List;

/**
 * @author yole
 */
public interface Resources extends DomElement {
    @SubTagList("string")
    List<ResourceElement> getStrings();
}
