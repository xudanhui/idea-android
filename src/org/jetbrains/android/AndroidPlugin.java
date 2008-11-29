package org.jetbrains.android;

import com.android.ddmlib.AndroidDebugBridge;
import com.intellij.openapi.components.ApplicationComponent;
import org.jetbrains.annotations.NotNull;

/**
 * @author coyote
 */
public class AndroidPlugin implements ApplicationComponent {
    @NotNull
    public String getComponentName() {
        return "AndroidApplicationComponent";
    }

    public void initComponent() {
    }

    public void disposeComponent() {
        AndroidDebugBridge.terminate();
    }
}
