package org.jetbrains.android.util;

import com.intellij.CommonBundle;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.PropertyKey;

import java.util.ResourceBundle;

/**
 * Messages bundle.
 *
 * @author Alexey Efimov
 */
public final class AndroidBundle {
    @NonNls
    private static final String BUNDLE_NAME = "org.jetbrains.android.util.AndroidBundle";
    private static final ResourceBundle BUNDLE = ResourceBundle.getBundle(BUNDLE_NAME);

    private AndroidBundle() {
    }

    public static String message(@PropertyKey(resourceBundle = BUNDLE_NAME)String key, Object... params) {
        return CommonBundle.message(BUNDLE, key, params);
    }
}
