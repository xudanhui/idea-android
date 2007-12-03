package org.jetbrains.android.dom.converters;

import com.intellij.util.xml.ConvertContext;
import com.intellij.util.xml.ResolvingConverter;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.HashSet;

/**
 * @author yole
 */
public class StaticEnumConverter extends ResolvingConverter<String> {
    private Set<String> myValues = new HashSet<String>();

    public StaticEnumConverter(String... values) {
        Collections.addAll(myValues, values);
    }

    @NotNull
    public Collection<? extends String> getVariants(ConvertContext context) {
        return myValues;
    }

    public String fromString(@Nullable @NonNls String s, ConvertContext context) {
        return s != null && myValues.contains(s) ? s : null;
    }

    public String toString(@Nullable String s, ConvertContext context) {
        return s;
    }
}
