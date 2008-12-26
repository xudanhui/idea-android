package org.jetbrains.android.dom.converters;

import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlElement;
import com.intellij.util.xml.ConvertContext;
import com.intellij.util.xml.ResolvingConverter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * @author coyote
 */
public class FlagsConverter extends ResolvingConverter<String> {
    private Set<String> possibleFlags = new HashSet<String>();

    public FlagsConverter(String... possibleFlags) {
        Collections.addAll(this.possibleFlags, possibleFlags);
    }

    @NotNull
    public Collection<? extends String> getVariants(ConvertContext context) {
        List<String> variants = new ArrayList<String>();
        XmlElement element = context.getXmlElement();
        if (!(element instanceof XmlAttribute)) {
            return variants;
        }
        //String attributeValue = ((XmlAttribute) element).getValue();
        return variants;
    }

    public String fromString(@Nullable String s, ConvertContext context) {
        if (s == null) return null;
        String[] flags = s.split("|");
        for (String flag : flags) {
            if (!possibleFlags.contains(flag)) return null;
        }
        return s;
    }

    public String toString(@Nullable String s, ConvertContext context) {
        return s;
    }
}
