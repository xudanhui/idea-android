package org.jetbrains.android.dom;

import com.intellij.facet.FacetManager;
import com.intellij.facet.ModifiableFacetModel;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.Module;
import com.intellij.testFramework.builders.JavaModuleFixtureBuilder;
import com.intellij.testFramework.fixtures.*;
import junit.framework.TestCase;
import org.jetbrains.android.facet.AndroidFacet;

import java.io.File;

/**
 * @author coyote
 */
abstract class AndroidCompletionTest extends TestCase {
    protected CodeInsightTestFixture myFixture;
    protected ModuleFixture moduleFixture;
    protected AndroidFacet facet;
    private final String activeFolder;

    public AndroidCompletionTest(String activeFolder) {
        this.activeFolder = activeFolder;
    }

    private static String getCompletionTestDataPath() {
        return getTestDataPath() + "/completion";
    }

    private static String getTestDataPath() {
        return new File("testData").getAbsolutePath().replace('\\', '/');
    }

    private static String getTestSkdPath() {
        return getTestDataPath() + "/sdk";
    }

    public void setUp() throws Exception {
        final TestFixtureBuilder<IdeaProjectTestFixture> projectBuilder = IdeaTestFixtureFactory.getFixtureFactory().createFixtureBuilder();
        JavaModuleFixtureBuilder moduleBuilder = projectBuilder.addModule(JavaModuleFixtureBuilder.class);
        myFixture = IdeaTestFixtureFactory.getFixtureFactory().createCodeInsightFixture(projectBuilder.getFixture());
        myFixture.setTestDataPath(getCompletionTestDataPath() + '/' + activeFolder + '/');
        moduleBuilder.addContentRoot(myFixture.getTempDirPath());
        moduleBuilder.addContentRoot(getCompletionTestDataPath());
        //moduleBuilder.addLibraryJars("android", getTestSkdPath() + '/', )
        myFixture.setUp();
        moduleFixture = moduleBuilder.getFixture();
        addAndroidFacet(moduleFixture.getModule());
    }

    private void addAndroidFacet(Module module) {
        FacetManager facetManager = FacetManager.getInstance(module);
        facet = facetManager.createFacet(AndroidFacet.getFacetType(), "Android", null);
        facet.getConfiguration().setSdkPath(getTestSkdPath());
        final ModifiableFacetModel model = facetManager.createModifiableModel();
        model.addFacet(facet);
        ApplicationManager.getApplication().runWriteAction(new Runnable() {
            public void run() {
                model.commit();
            }
        });
    }

    public void tearDown() throws Exception {
        myFixture.tearDown();
    }
}

