package org.jetbrains.android;

import com.intellij.codeInspection.InspectionToolProvider;
import org.jetbrains.android.dom.manifest.ManifestDomInspection;
import org.jetbrains.android.dom.layout.LayoutDomInspection;

/**
 * @author yole
 */
public class AndroidInspectionToolProvider implements InspectionToolProvider {
    public Class[] getInspectionClasses() {
        return new Class[]{
                ManifestDomInspection.class,
                LayoutDomInspection.class
        };
    }
}
