package org.jetbrains.android.dom;

import org.jetbrains.android.dom.layout.LayoutStyleableProvider;

/**
 * @author coyote
 */
public class LayoutCompletionTest extends AndroidCompletionTest {
    public LayoutCompletionTest() {
        super("layout");
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        StyleableProvider provider = facet.getStyleableProvider(LayoutStyleableProvider.KEY);
        provider.setForAllFiles(true);
    }

//    public void testAttributeNameCompletion() throws Throwable {
//        myFixture.testCompletionVariants("an1.xml", "layout_height", "layout_width");
//    }
}
