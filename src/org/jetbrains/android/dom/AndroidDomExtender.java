package org.jetbrains.android.dom;

import com.intellij.openapi.project.Project;
import com.intellij.psi.CommonClassNames;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiManager;
import com.intellij.psi.search.searches.ClassInheritorsSearch;
import com.intellij.psi.util.PsiModificationTracker;
import com.intellij.psi.xml.XmlTag;
import com.intellij.util.Processor;
import com.intellij.util.xml.Converter;
import com.intellij.util.xml.XmlName;
import com.intellij.util.xml.reflect.DomExtender;
import com.intellij.util.xml.reflect.DomExtension;
import com.intellij.util.xml.reflect.DomExtensionsRegistrar;
import org.jetbrains.android.AndroidManager;
import org.jetbrains.android.dom.converters.PsiEnumConverter;
import org.jetbrains.android.dom.converters.ResourceReferenceConverter;
import org.jetbrains.android.dom.converters.StaticEnumConverter;
import org.jetbrains.android.dom.layout.LayoutElement;
import org.jetbrains.android.dom.resources.ResourceValue;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author yole
 */
public class AndroidDomExtender extends DomExtender<AndroidDomElement> {
    private List<String> myViewClasses;

    public Object[] registerExtensions(@NotNull AndroidDomElement androidDomElement, @NotNull DomExtensionsRegistrar registrar) {
        /*
        if (androidDomElement instanceof Activity) {
            DomExtension extension = registrar.registerAttributeChildExtension(new XmlName("label", "android"),
                    ResourceValue.class);
            extension.setConverter(new ResourceReferenceConverter("string"));
        }
        */
        if (androidDomElement instanceof LayoutElement) {
            XmlTag tag = androidDomElement.getXmlTag();
            if (tag != null) {
                String name = tag.getName();
                Collection<String> attributes = getAttributeList(androidDomElement.getManager().getProject(), name);
                for(String attr: attributes) {
                    AndroidAttributeDescriptor descriptor = ourDescriptors.get(attr);
                    if (descriptor != null) {
                        XmlName xmlName = new XmlName(attr, AndroidManager.NAMESPACE_KEY);
                        DomExtension extension = registrar.registerGenericAttributeValueChildExtension(xmlName, descriptor.myValueClass);
                        if (descriptor.myConverter != null) {
                            extension.setConverter(descriptor.myConverter);
                        }
                    }
                }
                List<String> viewClasses = getViewClasses(androidDomElement.getManager().getProject());
                for(String s: viewClasses) {
                    registrar.registerCollectionChildrenExtension(new XmlName(s), LayoutElement.class);
                }
            }
        }
        return new Object[] {PsiModificationTracker.OUT_OF_CODE_BLOCK_MODIFICATION_COUNT };
    }

    private static Collection<String> getAttributeList(final Project project, String name) {
        Collection<String> result = new HashSet<String>();
        PsiManager manager = PsiManager.getInstance(project);
        PsiClass styleableClass = manager.findClass("android.R.styleable", project.getAllScope());
        if (styleableClass != null) {
            collectAttributesForClass(name, result, styleableClass);
            PsiClass layoutClass = manager.findClass("android.widget." + name, project.getAllScope());
            if (layoutClass != null) {
                layoutClass = layoutClass.getSuperClass();
                while(layoutClass != null &&
                        !CommonClassNames.JAVA_LANG_OBJECT.equals(layoutClass.getQualifiedName())) {
                    collectAttributesForClass(layoutClass.getName(), result, styleableClass);
                    layoutClass = layoutClass.getSuperClass();
                }
            }
        }
        return result;
    }

    private static void collectAttributesForClass(String name, Collection<String> result, PsiClass styleableClass) {
        Pattern pattern = Pattern.compile(name + "_(Layout_)?([a-z][A-Za-z_]+)");
        for(PsiField field: styleableClass.getFields()) {
            String fieldName = field.getName();
            Matcher matcher = pattern.matcher(fieldName);
            if (matcher.matches()) {
                result.add(matcher.group(2));
            }
        }
    }

    private synchronized List<String> getViewClasses(Project project) {
        if (myViewClasses == null) {
            myViewClasses = new ArrayList<String>();
            PsiClass viewClass = PsiManager.getInstance(project).findClass("android.view.View", project.getAllScope());
            if (viewClass != null) {
                myViewClasses.add(viewClass.getName());
                ClassInheritorsSearch.search(viewClass).forEach(new Processor<PsiClass>() {
                    public boolean process(PsiClass psiClass) {
                        myViewClasses.add(psiClass.getName());
                        return true;
                    }
                });
            }
        }
        return myViewClasses;
    }

    private static class AndroidAttributeDescriptor {
        private Class myValueClass;
        private Converter myConverter;

        private AndroidAttributeDescriptor(Class valueClass, Converter converter) {
            myValueClass = valueClass;
            myConverter = converter;
        }
    }

    private static final Map<String, AndroidAttributeDescriptor> ourDescriptors = new HashMap<String, AndroidAttributeDescriptor>();

    private static void registerDescriptor(String name, Class valueClass, Converter converter) {
        ourDescriptors.put(name, new AndroidAttributeDescriptor(valueClass, converter));
    }

    static {
        registerDescriptor("background", ResourceValue.class, new ResourceReferenceConverter("drawable"));
        registerDescriptor("capitalize", String.class, new PsiEnumConverter("android.text.method.TextInputMethod.Capitalize"));
        registerDescriptor("orientation", String.class, new StaticEnumConverter("horizontal", "vertical"));
        registerDescriptor("password", boolean.class, null);
        registerDescriptor("src", ResourceValue.class, new ResourceReferenceConverter("drawable"));
        registerDescriptor("text", ResourceValue.class, new ResourceReferenceConverter("string"));
        registerDescriptor("textColor", ResourceValue.class, new ResourceReferenceConverter("color"));
        registerDescriptor("textAlign", String.class, new StaticEnumConverter("start", "center", "end"));
    }
}
