package org.jetbrains.android.dom.manifest;

import com.intellij.util.xml.DomElement;

import java.util.List;

/**
 * @author yole
 */
public interface IntentFilter extends DomElement {
    List<Action> getActions();
    List<Category> getCategories();

    Action addAction();
    Category addCategory();
}
