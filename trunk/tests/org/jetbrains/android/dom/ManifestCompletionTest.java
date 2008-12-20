package org.jetbrains.android.dom;

import org.jetbrains.android.dom.manifest.ManifestStyleableProvider;

import java.util.List;
import java.util.ArrayList;

/**
 * @author coyote
 */
public class ManifestCompletionTest extends AndroidCompletionTest {
    public ManifestCompletionTest() {
        super("manifest");
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        StyleableProvider provider = facet.getStyleableProvider(ManifestStyleableProvider.KEY);
        provider.setForAllFiles(true);
    }

    private String[] withNamespace(String... arr) {
        List<String> list = new ArrayList<String>();
        for (String s : arr) {
            list.add("android:" + s);
        }
        return list.toArray(new String[list.size()]);
    }

    public void testAttributeNameCompletion() throws Throwable {
        myFixture.testCompletionVariants("an1.xml", withNamespace("icon", "label", "priority"));
        myFixture.testCompletionVariants("an2.xml", withNamespace("debuggable", "description"));
        myFixture.testCompletion("an3.xml", "an3_after.xml");
        myFixture.testCompletion("an4.xml", "an4_after.xml");
    }

    public void testTagNameCompletion() throws Throwable {
        myFixture.testCompletionVariants("tn1.xml", "uses-permission", "uses-sdk");
        myFixture.testCompletionVariants("tn2.xml", "manifest");
        myFixture.testCompletion("tn3.xml", "tn3_after.xml");
        myFixture.testCompletion("tn4.xml", "tn4_after.xml");
    }

    public void testAttributeValueCompletion() {
    }
}
