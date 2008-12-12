package org.jetbrains.android.dom.manifest;

/**
 * @author yole
 */
public interface IntentFilter extends ManifestElement {
    Action addAction();
    Category addCategory();
}
