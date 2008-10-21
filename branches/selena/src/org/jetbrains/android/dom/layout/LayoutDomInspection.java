package org.jetbrains.android.dom.layout;

import com.intellij.util.xml.highlighting.BasicDomElementsInspection;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

/**
 * @author yole
 */
public class LayoutDomInspection extends BasicDomElementsInspection<LayoutElement> {
    public LayoutDomInspection() {
        super(LayoutElement.class);
    }

    @Nls
    @NotNull
    public String getGroupDisplayName() {
        return "Android";
    }

    @Nls
    @NotNull
    public String getDisplayName() {
        return "Android Layout Validation";
    }

    @NonNls
    @NotNull
    public String getShortName() {
        return "LayoutDomInspection";
    }
}