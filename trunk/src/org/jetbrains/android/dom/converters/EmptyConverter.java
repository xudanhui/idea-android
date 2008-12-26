package org.jetbrains.android.dom.converters;

import com.intellij.util.xml.ConvertContext;
import com.intellij.util.xml.ResolvingConverter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.ArrayList;

/**
 * @author coyote
 */
public class EmptyConverter extends ResolvingConverter<String> {
    public String fromString(@Nullable String s, ConvertContext context) {
        return s;
    }

    public String toString(@Nullable String s, ConvertContext context) {
        return s;
    }

    @NotNull
    public Collection<? extends String> getVariants(ConvertContext context) {
        return new ArrayList<String>();
    }
}
