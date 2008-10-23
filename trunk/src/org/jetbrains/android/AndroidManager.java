package org.jetbrains.android;

import com.intellij.openapi.util.IconLoader;

import javax.swing.*;

/**
 * @author yole
 */
public class AndroidManager {
    public static final String MANIFEST_FILE_NAME = "AndroidManifest.xml";
    public static final String CLASSES_FILE_NAME = "classes.dex";

    public static final Icon ANDROID_ICON = IconLoader.getIcon("/icons/android.png");
    public static final String NAMESPACE_KEY = "android";
    public static final String NAMESPACE = "http://schemas.android.com/apk/res/android";

}
