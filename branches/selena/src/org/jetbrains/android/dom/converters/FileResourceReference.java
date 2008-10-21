package org.jetbrains.android.dom.converters;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.util.xml.GenericDomValue;
import com.intellij.util.xml.impl.GenericDomValueReference;
import org.jetbrains.android.dom.resources.ResourceValue;

/**
 * @author yole
 */
public class FileResourceReference extends GenericDomValueReference<ResourceValue> {
    private PsiFile myTarget;

    public FileResourceReference(GenericDomValue<ResourceValue> genericDomValue, PsiFile target) {
        super(genericDomValue);
        myTarget = target;
    }

    public PsiElement resolve() {
        return myTarget;
    }
}
