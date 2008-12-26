package org.jetbrains.android.dom.converters;

import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import com.intellij.util.xml.ConvertContext;
import com.intellij.util.xml.ResolvingConverter;

import java.util.Set;
import java.util.HashSet;
import java.util.Collections;
import java.util.Collection;

/**
 * @author coyote
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
        return myValues.contains(s) ? s : null;
    }

    @Override
    public String toString() {
        return "StaticEnumConverter " + myValues.toString();
    }

    public String toString(@Nullable String s, ConvertContext context) {
        return s;
    }


}
