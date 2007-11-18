package org.jetbrains.android.dom.converters;

import com.intellij.psi.PsiElement;
import com.intellij.util.xml.GenericDomValue;
import com.intellij.util.xml.impl.GenericDomValueReference;

/**
 * @author yole
 */
public class ResourceReference<T> extends GenericDomValueReference<T> {
    private GenericDomValue<T> myTarget;

    public ResourceReference(GenericDomValue<T> source, GenericDomValue<T> target) {
        super(source);
        myTarget = target;
    }

    public PsiElement resolve() {
        return myTarget != null ? myTarget.getXmlElement() : null;
    }
}
