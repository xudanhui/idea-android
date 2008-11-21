package org.jetbrains.android.facet;

import com.intellij.facet.FacetConfiguration;
import com.intellij.facet.ui.FacetEditorContext;
import com.intellij.facet.ui.FacetEditorTab;
import com.intellij.facet.ui.FacetValidatorsManager;
import com.intellij.openapi.util.DefaultJDOMExternalizer;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.WriteExternalException;
import com.intellij.openapi.util.SystemInfo;
import com.android.ddmlib.AndroidDebugBridge;
import org.jdom.Element;

import java.io.File;

/**
 * @author yole, coyote
 */
public class AndroidFacetConfiguration implements FacetConfiguration {
    public static final String DEFAULT_SDK_PATH_PROPERTY = "AndroidSdkPath";

    private String sdkPath;
    public String RESOURCES_PATH = "res";
    private static boolean ddmLibInitialized = false;

    public String getSdkPath() {
        return sdkPath;
    }

    public String getToolPath(String toolName) {
        if (SystemInfo.isWindows) {
            toolName += ".exe";
        }
        File file = new File(new File(sdkPath, "tools"), toolName);
        return file.getAbsolutePath();
    }

    public void setSdkPath(final String sdkPath) {
        this.sdkPath = sdkPath;
        new Thread(new Runnable() {
            public void run() {
                if (!ddmLibInitialized) {
                    AndroidDebugBridge.init(true);
                }
                AndroidDebugBridge.createBridge(getToolPath("adb"), !ddmLibInitialized);
                ddmLibInitialized = true;
            }
        }).start();
    }

    public FacetEditorTab[] createEditorTabs(FacetEditorContext editorContext, FacetValidatorsManager validatorsManager) {
        return new FacetEditorTab[]{new AndroidFacetEditorTab(editorContext.getProject(), this)};
    }

    public void readExternal(Element element) throws InvalidDataException {
        DefaultJDOMExternalizer.readExternal(this, element);
    }

    public void writeExternal(Element element) throws WriteExternalException {
        DefaultJDOMExternalizer.writeExternal(this, element);
    }
}
