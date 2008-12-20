package org.jetbrains.android.dom.manifest;

import com.intellij.openapi.module.Module;
import com.intellij.psi.xml.XmlFile;
import org.jetbrains.android.dom.StyleableProvider;
import org.jetbrains.android.facet.AndroidFacet;
import org.jetbrains.android.util.Key;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author coyote
 */
public class ManifestStyleableProvider extends StyleableProvider {
    public static final Key<ManifestStyleableProvider> KEY = new Key<ManifestStyleableProvider>();

    public ManifestStyleableProvider(AndroidFacet facet) {
        super(facet);
    }

    protected String getStyleableNameByTagName(@NotNull String tagName) {
        String prefix = "AndroidManifest";
        if (tagName.equals("manifest")) return prefix;
        String[] parts = tagName.split("-");
        StringBuilder builder = new StringBuilder(prefix);
        for (String part : parts) {
            char first = part.charAt(0);
            String remained = part.substring(1);
            builder.append(Character.toUpperCase(first)).append(remained);
        }
        return builder.toString(); 
    }

    protected String getTagNameByStyleableName(@NotNull String styleableName) {
        String prefix = "AndroidManifest";
        if (!styleableName.startsWith(prefix)) {
            return null;
        }
        String remained = styleableName.substring(prefix.length());
        if (remained.length() == 0) return "manifest";
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < remained.length(); i++) {
            char c = remained.charAt(i);
            if (builder.length() > 0 && Character.isUpperCase(c)) {
                builder.append('-');
            }
            builder.append(Character.toLowerCase(c));
        }
        return builder.toString();
    }

    @NotNull
    public String getAttrsFilename() {
        return "attrs_manifest.xml";
    }

    public boolean isMyFile(@NotNull XmlFile file, @Nullable Module module) {
        if (forAllFiles) return true;
        ManifestDomFileDescription description = new ManifestDomFileDescription();
        return description.isMyFile(file, module);
    }
}
