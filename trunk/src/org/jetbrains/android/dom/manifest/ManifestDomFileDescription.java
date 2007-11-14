package org.jetbrains.android.dom.manifest;

import com.intellij.util.xml.DomFileDescription;
import com.intellij.psi.xml.XmlFile;
import com.intellij.openapi.module.Module;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author yole
 */
public class ManifestDomFileDescription extends DomFileDescription<Manifest> {
    public ManifestDomFileDescription() {
        super(Manifest.class, "manifest");
    }

    public boolean isMyFile(@NotNull XmlFile file, @Nullable Module module) {
        return file.getName().equals("AndroidManifest.xml");
    }
}
