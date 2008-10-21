package org.jetbrains.android.dom.manifest;

import com.intellij.util.xml.highlighting.BasicDomElementsInspection;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

/**
 * @author yole
 */
public class ManifestDomInspection extends BasicDomElementsInspection<Manifest> {
    public ManifestDomInspection() {
        super(Manifest.class);
    }

    @Nls
    @NotNull
    public String getGroupDisplayName() {
        return "Android";
    }

    @Nls
    @NotNull
    public String getDisplayName() {
        return "Android Manifest Validation";
    }

    @NonNls
    @NotNull
    public String getShortName() {
        return "ManifestDomInspection";
    }
}
