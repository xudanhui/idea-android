package org.jetbrains.android.dom.converters;

import com.intellij.util.xml.ConvertContext;
import com.intellij.util.xml.ResolvingConverter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * @author coyote
 */
public class CompositeConverter extends ResolvingConverter<Object> {
    private Map<Class, Set<ResolvingConverter>> converters = new HashMap<Class, Set<ResolvingConverter>>();
    private int converterCount = 0;

    public void addConverter(Class c, ResolvingConverter converter) {
        Set<ResolvingConverter> convertersForC = converters.get(c);
        if (convertersForC == null) {
            convertersForC = new HashSet<ResolvingConverter>();
            converters.put(c, convertersForC);
        }
        convertersForC.add(converter);
        converterCount++;
    }

    public void removeConverter(Class c, ResolvingConverter converter) {
        Set<ResolvingConverter> convertersForC = converters.get(c);
        if (convertersForC == null) return;
        convertersForC.remove(converter);
        converterCount--;
    }

    public Set<ResolvingConverter> getConverters(Class c) {
        return Collections.unmodifiableSet(converters.get(c));
    }

    public int size() {
        return converterCount;
    }

    @NotNull
    public Collection<?> getVariants(ConvertContext context) {
        List<Object> variants = new ArrayList<Object>();
        for (Map.Entry<Class, Set<ResolvingConverter>> entry : converters.entrySet()) {
            for (ResolvingConverter converter : entry.getValue()) {
                for (Object o : converter.getVariants(context)) {
                    variants.add(o);
                }
            }
        }
        return variants;
    }

    public Object fromString(@Nullable String s, ConvertContext context) {
        if (s == null) return null;
        for (Map.Entry<Class, Set<ResolvingConverter>> entry : converters.entrySet()) {
            for (ResolvingConverter converter : entry.getValue()) {
                Object o = converter.fromString(s, context);
                if (o != null) return o;
            }
        }
        return null;
    }

    public String toString(@Nullable Object o, ConvertContext context) {
        if (o != null && o.getClass() != null) {
            Set<ResolvingConverter> convertersForO = converters.get(o.getClass());
            if (convertersForO != null) {
                for (ResolvingConverter converter : convertersForO) {
                    String s = converter.toString(o, context);
                    if (s != null) return s;
                }
            }
        }
        return null;
    }
}
